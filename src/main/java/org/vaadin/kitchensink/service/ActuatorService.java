package org.vaadin.kitchensink.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class ActuatorService {

    private final ObjectMapper mapper;

    public ActuatorService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public <T> T getActuatorData(String url, Class<T> valueType) {
        try {
            return mapper.readValue(new URI(url).toURL(), valueType);
        } catch (IOException | URISyntaxException e) {
            throw new ActuatorException("Failed to read actuator data from URL: " + url, e);
        }
    }

    public <T> T getActuatorData(String url, TypeReference<T> valueTypeRef) {
        try {
            return mapper.readValue(new URI(url).toURL(), valueTypeRef);
        } catch (IOException | URISyntaxException e) {
            throw new ActuatorException("Failed to read actuator data from URL: " + url, e);
        }
    }
}
