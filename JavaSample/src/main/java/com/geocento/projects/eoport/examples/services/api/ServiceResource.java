package com.geocento.projects.eoport.examples.services.api;


import com.geocento.projects.eoport.examples.services.api.dtos.InputProduct;
import com.geocento.projects.eoport.examples.services.api.utils.Configuration;
import com.geocento.projects.eoport.examples.services.api.utils.S3OBSUtils;
import com.geocento.projects.eoport.examples.services.api.utils.Utils;
import org.apache.log4j.Logger;
import org.geotools.data.*;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeatureType;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.*;


@Path("/")
public class ServiceResource extends BaseResource {

    @Context
    ServletContext servletContext;

    @Context
    HttpServletResponse response;

    @Context
    SecurityContext securityContext;

    // create the logger
    static {
        logger = Logger.getLogger(ServiceResource.class);
    }

    public ServiceResource() {
        logger.info("Starting the geopublisher api service");
    }

    @POST
    @Path("/service")
    @Consumes("application/json")
    @Produces("application/json")
    public void processProduct(InputProduct inputProduct) {
        try {
            // TODO - create task and schedule pod creation
            final String taskID = inputProduct.getTaskID();
            // for the sake of the demo we just create a thread to send a message back after a while
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        // send back a file which is a geojson of the input coordinates
                        String boundary = inputProduct.getMetadata().getBoundaryCoordinates();
                        String[] latLngs = boundary.split(" ");
                        SimpleFeatureType TYPE = DataUtilities.createType(
                                "example", "the_geom:Point:srid=4326," + "name:String");
                        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
                        DefaultFeatureCollection collection = new DefaultFeatureCollection();
                        GeometryFactory geometryFactory
                                = JTSFactoryFinder.getGeometryFactory(null);
                        Arrays.asList(latLngs).stream().map(latLng -> {
                            String[] latLngValue = latLng.split(",");
                            Point point = geometryFactory.createPoint(
                                    new Coordinate(Double.valueOf(latLngValue[1]), Double.valueOf(latLngValue[0])));
                            featureBuilder.add(point);
                            featureBuilder.add("Point for " + taskID);
                            return featureBuilder.buildFeature(null);
                        }).forEach(collection::add);
                        ShapefileDataStoreFactory dataStoreFactory
                                = new ShapefileDataStoreFactory();

                        File directory = new File(Configuration.getProperty(Configuration.APPLICATION_SETTINGS.pathToTmp), taskID);
                        if(!directory.exists()) {
                            directory.mkdirs();
                        }
                        File shapeFile = new File(directory,
                                "test_" + taskID + "_" + new Date().getTime() + ".shp");
                        Map<String, Serializable> params = new HashMap<>();
                        params.put("url", shapeFile.toURI().toURL());
                        params.put("create spatial index", Boolean.TRUE);
                        ShapefileDataStore dataStore
                                = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
                        dataStore.createSchema(TYPE);
                        Transaction transaction = new DefaultTransaction("create");

                        String typeName = dataStore.getTypeNames()[0];
                        SimpleFeatureSource featureSource
                                = dataStore.getFeatureSource(typeName);

                        SimpleFeatureStore featureStore
                                = (SimpleFeatureStore) featureSource;

                        featureStore.setTransaction(transaction);
                        try {
                            featureStore.addFeatures(collection);
                            transaction.commit();
                        } catch (Exception problem) {
                            transaction.rollback();
                            throw new Exception("Could not generate shapefile");
                        } finally {
                            transaction.close();
                        }

                        // now zip and save on S3
                        File zipFile = new File(Configuration.getProperty(Configuration.APPLICATION_SETTINGS.pathToTmp), taskID + ".zip");
                        Utils.zipFiles(zipFile, Arrays.asList(directory.listFiles()));
                        S3OBSUtils.uploadFile(zipFile);

                        // send notification to PM


                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }, 10000);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @POST
    @Path("/schedule")
    @Consumes("application/json")
    @Produces("application/json")
    public void scheduleProcess(InputProduct inputProduct) {
        try {
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
