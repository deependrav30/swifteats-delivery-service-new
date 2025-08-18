package com.swifteats.simulator;

public class SimConfig {
    private int drivers = 5;
    private int updatesPerSecond = 1;
    private String mode = "http"; // http or rabbit
    private String ingestUrl = "http://localhost:8085/tracking/location"; // default tracking-ingest port
    private String rabbitExchange = "driver.location.v1";
    private String rabbitHost = "localhost";
    private int rabbitPort = 5672;

    public int getDrivers() {
        return drivers;
    }

    public void setDrivers(int drivers) {
        this.drivers = drivers;
    }

    public int getUpdatesPerSecond() {
        return updatesPerSecond;
    }

    public void setUpdatesPerSecond(int updatesPerSecond) {
        this.updatesPerSecond = updatesPerSecond;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getIngestUrl() {
        return ingestUrl;
    }

    public void setIngestUrl(String ingestUrl) {
        this.ingestUrl = ingestUrl;
    }

    public String getRabbitExchange() {
        return rabbitExchange;
    }

    public void setRabbitExchange(String rabbitExchange) {
        this.rabbitExchange = rabbitExchange;
    }

    public String getRabbitHost() {
        return rabbitHost;
    }

    public void setRabbitHost(String rabbitHost) {
        this.rabbitHost = rabbitHost;
    }

    public int getRabbitPort() {
        return rabbitPort;
    }

    public void setRabbitPort(int rabbitPort) {
        this.rabbitPort = rabbitPort;
    }
}
