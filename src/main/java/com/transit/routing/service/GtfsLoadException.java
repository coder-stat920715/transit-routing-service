package com.transit.routing.service;

public class GtfsLoadException extends RuntimeException {
    public GtfsLoadException(String message) {
        super(message);
    }

    public GtfsLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
