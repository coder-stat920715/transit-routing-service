package com.transit.routing.service;

public class NoJourneyFoundException extends RuntimeException {
    public NoJourneyFoundException(String message) {
        super(message);
    }
}
