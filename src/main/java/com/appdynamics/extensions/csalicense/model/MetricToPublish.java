package com.appdynamics.extensions.csalicense.model;

public class MetricToPublish {

    private String controllerHost;
    private Boolean isAvaliable;
    private Boolean isAllowedCSA;
    private int totalVCPU;

    /**
     * @return the controllerHost
     */
    public String getControllerHost() {
        return controllerHost;
    }

    /**
     * @param controllerHost the controllerHost to set
     */
    public void setControllerHost(String controllerHost) {
        this.controllerHost = controllerHost;
    }

    /**
     * @return the isAvaliable
     */
    public Boolean isAvaliable() {
        return isAvaliable;
    }

    /**
     * @param isAvaliable the isAvaliable to set
     */
    public void setIsAvaliable(Boolean isAvaliable) {
        this.isAvaliable = isAvaliable;
    }

    /**
     * @return the isAllowedCSA
     */
    public Boolean isAllowedCSA() {
        return isAllowedCSA;
    }

    /**
     * @param isAllowedCSA the isAllowedCSA to set
     */
    public void setIsAllowedCSA(Boolean isAllowedCSA) {
        this.isAllowedCSA = isAllowedCSA;
    }

    /**
     * @return the totalVCPU
     */
    public int getTotalVCPU() {
        return totalVCPU;
    }

    /**
     * @param totalVCPU the totalVCPU to set
     */
    public void setTotalVCPU(int totalVCPU) {
        this.totalVCPU = totalVCPU;
    }
}
