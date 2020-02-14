package com.thed.model;

/**
 * Created by prashant on 8/1/20.
 */
public class TestStepDetail extends BaseEntity {

    private Long orderId;
    private String step;
    private String data;
    private String result;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
