package com.geocento.projects.eoport.examples.services.api.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geocento.projects.eoport.examples.services.api.dtos.ResponseProduct;
import org.apache.log4j.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

public class ProductionManagerUtil {

    static private String baseUrl = Configuration.getProperty(Configuration.APPLICATION_SETTINGS.productionManagerURL);

    static private Logger logger = Logger.getLogger(ProductionManagerUtil.class);

    public static void notifyDelivery(String objectURI) throws Exception {
        ResponseProduct responseProduct = new ResponseProduct();
        responseProduct.setObjectURI(objectURI);
        Response response = getWebTarget("/service")
                .request()
                .buildPost(Entity.json(responseProduct))
                .submit().get();
        if(response.getStatus() >= 300) {
            throw new Exception("Error processing request, message is " + response.readEntity(String.class));
        }
    }

    private static void logPayload(Object payload) {
        try {
            logger.info(new ObjectMapper() {}.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
        }
    }

    private static WebTarget getWebTarget(String path) {
        Client client = ClientBuilder.newClient();
        return client
                .target(UrlUtils.buildUrl(baseUrl, path));
    }
}
