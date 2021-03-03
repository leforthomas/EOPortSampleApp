package com.geocento.projects.eoport.examples.services.api.utils;

import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import com.obs.services.model.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

public class S3OBSUtils {

    static Logger logger = Logger.getLogger(S3OBSUtils.class);

    static String endPoint = Configuration.getProperty(Configuration.APPLICATION_SETTINGS.otcObsEndpoint);
    static String ak = Configuration.getProperty(Configuration.APPLICATION_SETTINGS.otcAccessKey);
    static String sk = Configuration.getProperty(Configuration.APPLICATION_SETTINGS.otcSecretKey);
    static boolean vhostVersion = Configuration.getBooleanProperty(Configuration.APPLICATION_SETTINGS.deliveryURIVhostFormatted);

    static String bucketname = Configuration.getProperty(Configuration.APPLICATION_SETTINGS.bucketName);

    public static String getDownloadUrl(String deliveryURI) throws Exception {

        // Create an instance of ObsClient.
        URI uri = new URI(deliveryURI);
        String serverUrl, bucketname, objectKey;
        if(vhostVersion) {
            String value = uri.getHost();
            int index = value.indexOf(".");
            serverUrl = value.substring(index + 1);
            bucketname = value.substring(0, index);
            objectKey = uri.getPath().substring(1);
        } else {
            serverUrl = uri.getHost();
            List<String> path = Arrays.asList(uri.getPath().split("/"));
            path.remove(0);
            bucketname = path.get(0);
            path.remove(0);
            objectKey = StringUtils.join(path, "/");
        }
        ObsConfiguration config = new ObsConfiguration();
        config.setEndPoint(serverUrl);
        config.setHttpsOnly(true);
        ObsClient obsClient = new ObsClient(ak, sk, config);

        try {
            // Specify the validity period of the URL to 3600 seconds.
            int expireSeconds = 60 * 60;
            V4TemporarySignatureRequest request = new V4TemporarySignatureRequest();
            request.setMethod(HttpMethodEnum.GET);
            request.setBucketName(bucketname);
            request.setObjectKey(objectKey);
            request.setExpires(expireSeconds);
            V4TemporarySignatureResponse res = obsClient.createV4TemporarySignature(request);
            return res.getSignedUrl();
        } finally {
            obsClient.close();
        }
    }

    public static void uploadFile(File zipFile) throws Exception {
        // Create an instance of ObsClient.
        String objectKey = zipFile.getName().replace(".zip", "");
        ObsConfiguration config = new ObsConfiguration();
        config.setEndPoint(endPoint);
        config.setHttpsOnly(true);
        ObsClient obsClient = new ObsClient(ak, sk, config);
        try {
            PutObjectResult response = obsClient.putObject(bucketname, objectKey, zipFile);
        } finally {
            obsClient.close();
        }
    }
}
