package com.geocento.projects.eoport.examples.services.api;


import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.geocento.projects.eoport.examples.services.api.dtos.InputProduct;
import com.geocento.projects.eoport.examples.services.api.dtos.Metadata;
import com.geocento.projects.eoport.examples.services.api.dtos.ResponseProduct;
import com.geocento.projects.eoport.examples.services.api.dtos.UsageReport;
import com.geocento.projects.eoport.examples.services.api.utils.*;
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
import java.net.URI;
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

    static String bucketname = Configuration.getProperty(Configuration.APPLICATION_SETTINGS.bucketName);

    public ServiceResource() {
    }

    @POST
    @Path("/service")
    @Consumes("application/json")
    @Produces("application/json")
    public void processProduct(InputProduct inputProduct) {
        try {
            // TODO - create task and schedule pod creation
            final String taskID = inputProduct.getTaskID();
            final String pipelineID = inputProduct.getPipelineID();
            final String dumpID = inputProduct.getDumpID();
            final Boolean taskFinished = inputProduct.getTaskFinished();
            // the downstream URI is the URL the service should send the result back
            final String downstreamURI = inputProduct.getDownstreamURI();

            // for the sake of the demo we just create a thread to send a message back after a while
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        File inputDirectory = new File(Configuration.getProperty(Configuration.APPLICATION_SETTINGS.pathToTmp));
                        File file = S3OBSUtils.downloadFromURI(inputProduct.getObjectURI(), inputDirectory);
                        // send back a file which is a geojson of the input coordinates
                        String boundary = inputProduct.getMetadata().get("boundaryCoordinates");
                        boundary = boundary.trim();
                        // remove leading and training CR
                        if(boundary.startsWith("\n")) {
                            boundary = boundary.substring("\n".length());
                        }
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

                        String objectURI = S3OBSUtils.uploadFile(bucketname, zipFile);

                        // send notification to PM
                        ResponseProduct responseProduct = new ResponseProduct();
                        responseProduct.setTaskID(taskID);
                        responseProduct.setPipelineID(pipelineID);
                        responseProduct.setDumpID(dumpID);
                        responseProduct.setTaskFinished(taskFinished);
                        // add some metadata if needed
                        Metadata metadata = new Metadata();
                        metadata.put("property", "example");
                        responseProduct.setMetadata(metadata);
                        // set the result URI
                        responseProduct.setObjectURI(objectURI);
                        // add usage information
                        UsageReport usageReport = new UsageReport();
                        // example value
                        usageReport.setAmount("10");
                        usageReport.setRecordDate(new Date());
                        // units need to match how you set up your service
                        usageReport.setUnitType("km2");
                        responseProduct.setUsage(usageReport);

                        // now send the message to the production manager
                        ProductionManagerUtil.notifyDelivery(downstreamURI, responseProduct);

                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }, 1000);
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
