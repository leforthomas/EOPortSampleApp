package com.geocento.projects.eoport.examples.services.api.dtos;

/**
 * specific to the NRT SAR stream
 */
public class Metadata {

    String boundaryCoordinates;
    String polarization;
    String polCounter;
    String productType;
    String subscriptionID;
    String binaryFile;

    public Metadata() {
    }

    public String getBoundaryCoordinates() {
        return boundaryCoordinates;
    }

    public void setBoundaryCoordinates(String boundaryCoordinates) {
        this.boundaryCoordinates = boundaryCoordinates;
    }

    public String getPolarization() {
        return polarization;
    }

    public void setPolarization(String polarization) {
        this.polarization = polarization;
    }

    public String getPolCounter() {
        return polCounter;
    }

    public void setPolCounter(String polCounter) {
        this.polCounter = polCounter;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getSubscriptionID() {
        return subscriptionID;
    }

    public void setSubscriptionID(String subscriptionID) {
        this.subscriptionID = subscriptionID;
    }

    public String getBinaryFile() {
        return binaryFile;
    }

    public void setBinaryFile(String binaryFile) {
        this.binaryFile = binaryFile;
    }
}
