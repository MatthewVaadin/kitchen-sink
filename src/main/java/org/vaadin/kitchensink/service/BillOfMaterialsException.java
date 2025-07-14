package org.vaadin.kitchensink.service;

public class BillOfMaterialsException extends RuntimeException {

    public BillOfMaterialsException(String message) {
        super(message);
    }

    public BillOfMaterialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
