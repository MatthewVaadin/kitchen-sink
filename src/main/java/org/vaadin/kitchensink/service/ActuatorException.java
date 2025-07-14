package org.vaadin.kitchensink.service;

public class ActuatorException extends RuntimeException {

    public ActuatorException(String message) {
        super(message);
    }

    public ActuatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
