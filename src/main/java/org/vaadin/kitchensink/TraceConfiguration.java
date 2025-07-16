package org.vaadin.kitchensink;

import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TraceConfiguration {

    @Bean
    public HttpExchangeRepository httpTraceRepository() {
        InMemoryHttpExchangeRepository repo = new InMemoryHttpExchangeRepository();
        repo.setCapacity(50);       // keep only the last 50 traces, for example
        return repo;
    }
}
