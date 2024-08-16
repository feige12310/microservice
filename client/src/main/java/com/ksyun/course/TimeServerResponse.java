package com.ksyun.course;

public class TimeServerResponse {
    private String result;
    private String serviceId;

    public TimeServerResponse() {}
    public TimeServerResponse(String result, String serviceId) {
        this.result = result;
        this.serviceId = serviceId;
    }

    public String getResult() {
        return result;
    }

    public String getServiceId() {
        return serviceId;
    }

    @Override
    public String toString() {
        return "TimeServerResponse{" +
                "result='" + result + '\'' +
                ", serviceId='" + serviceId + '\'' +
                '}';
    }
}
