package com.sionyx.plexus.ui.profile;

public class QRScanModel {
    private String productName;
    private String sku;
    private String UniversalProductCode;
    private String Description;
    private String Model;
    private String Classification;
    private String DateCode;
    private String SerialNumber;

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getUniversalProductCode() {
        return UniversalProductCode;
    }

    public void setUniversalProductCode(String universalProductCode) {
        UniversalProductCode = universalProductCode;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }

    public String getModel() {
        return Model;
    }

    public void setModel(String model) {
        Model = model;
    }

    public String getClassification() {
        return Classification;
    }

    public void setClassification(String classification) {
        Classification = classification;
    }

    public String getDateCode() {
        return DateCode;
    }

    public void setDateCode(String dateCode) {
        DateCode = dateCode;
    }

    public String getSerialNumber() {
        return SerialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        SerialNumber = serialNumber;
    }
}
