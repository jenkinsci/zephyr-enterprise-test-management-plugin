package com.thed.model;

/**
 * Created by prashant on 7/1/20.
 */
public class GenericAttachmentDTO {

    private String fileName;
    private String fieldName;
    private String tempFilePath;
    private String contentType;
    private byte[] byteData;
    private Long localItemId; //This variable is not used by api but used internally.

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getTempFilePath() {
        return tempFilePath;
    }

    public void setTempFilePath(String tempFilePath) {
        this.tempFilePath = tempFilePath;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public byte[] getByteData() {
        return byteData;
    }

    public void setByteData(byte[] byteData) {
        this.byteData = byteData;
    }

    public Long getLocalItemId() {
        return localItemId;
    }

    public void setLocalItemId(Long localItemId) {
        this.localItemId = localItemId;
    }
}
