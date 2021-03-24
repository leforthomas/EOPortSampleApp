package com.geocento.projects.eoport.examples.services.api.utils;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.SignerFactory;
import org.apache.log4j.Logger;

import java.io.File;

public class S3OBSUtils {

    static Logger logger = Logger.getLogger(S3OBSUtils.class);

    static String endPoint = Configuration.getProperty(Configuration.APPLICATION_SETTINGS.otcObsEndpoint);
    static String ak = Configuration.getProperty(Configuration.APPLICATION_SETTINGS.otcAccessKey);
    static String sk = Configuration.getProperty(Configuration.APPLICATION_SETTINGS.otcSecretKey);
    static boolean vhostVersion = Configuration.getBooleanProperty(Configuration.APPLICATION_SETTINGS.deliveryURIVhostFormatted);

    public static OtcObsClient getOTCOBSClient() {
        AWSCredentials credentials = new BasicAWSCredentials(ak, sk);
        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setProtocol(Protocol.HTTPS);
        clientConfig.setSignerOverride("OtcObsSigner");
        SignerFactory.registerSigner("OtcObsSigner", OtcObsSigner.class);
        return new OtcObsClient(credentials, clientConfig);
    }

    public static String uploadFile(String bucketName, File zipFile) throws Exception {
        OtcObsClient otcObsClient = S3OBSUtils.getOTCOBSClient();
        // Create an instance of ObsClient.
        String objectKey = zipFile.getName();
        otcObsClient.putObject(bucketName, objectKey, zipFile);
        return otcObsClient.getResourceUrl(bucketName, objectKey);
    }

/*
    public static URI getS3ObjectURI(String bucketName, String objectKey) throws Exception {
        URL endPointURL = new URL(endPoint);
        if(vhostVersion) {
            return new URI(endPointURL.getProtocol(),
                    bucketName + "." + endPointURL.getHost(),
                    objectKey);
        } else {
            return new URI(endPointURL.getProtocol(),
                    endPointURL.getHost(),
                    bucketName + "/" + objectKey);
        }
    }
*/
}
