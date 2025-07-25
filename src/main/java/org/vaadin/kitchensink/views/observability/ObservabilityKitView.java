package org.vaadin.kitchensink.views.observability;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@AnonymousAllowed
@PageTitle("Observability Kit")
@Route(value = "observability-kit")
@Menu(order = 400, icon = LineAwesomeIconUrl.CHART_LINE_SOLID)
@Profile("observability-kit")
public class ObservabilityKitView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger logger = LoggerFactory.getLogger(ObservabilityKitView.class);
    private final Properties otelProperties;

    public static class ObservabilityProperty {
        private final String key;
        private final String value;

        public ObservabilityProperty(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }

    public static class EndpointStatus {
        private final String name;
        private final String url;
        private final String status;
        private final int statusCode;
        public EndpointStatus(String name, String url, String status, int statusCode) {
            this.name = name;
            this.url = url;
            this.status = status;
            this.statusCode = statusCode;
        }
        public String getName() { return name; }
        public String getUrl() { return url; }
        public String getStatus() { return status; }
        public int getStatusCode() { return statusCode; }
    }

    public ObservabilityKitView() {
        // Load properties once at construction
        this.otelProperties = loadObservabilityKitProperties();

        setSpacing(true);
        setPadding(true);
        setSizeFull();

        add(new H1("Observability Kit Configuration"));

        // Get the configuration file path from the system property
        String configFilePath = System.getProperty("otel.javaagent.configuration-file");
        if (configFilePath != null) {
            add(new Span("Configuration file path: " + configFilePath));
        }

        // Create grid for observability properties
        Grid<ObservabilityProperty> propertiesGrid = new Grid<>(ObservabilityProperty.class, false);
        propertiesGrid.addColumn(ObservabilityProperty::getKey)
                .setHeader("Property Key")
                .setAutoWidth(true)
                .setSortable(true);
        propertiesGrid.addColumn(ObservabilityProperty::getValue)
                .setHeader("Value")
                .setAutoWidth(true);
        propertiesGrid.setSizeFull();

        // Convert properties to list for display
        List<ObservabilityProperty> observabilityProperties = otelProperties.entrySet().stream()
                .sorted((e1, e2) -> e1.getKey().toString().compareToIgnoreCase(e2.getKey().toString()))
                .map(entry -> new ObservabilityProperty(entry.getKey().toString(), entry.getValue().toString()))
                .toList();

        if (observabilityProperties.isEmpty()) {
            add(new H3("No observability kit properties found"));
            add(new Span("The observability-kit.properties file could not be loaded or is empty."));
            return;
        }

        // Set initial data to the grid
        propertiesGrid.setItems(observabilityProperties);
        add(propertiesGrid);

        // Add endpoint status grid below
        endpointGrid = new Grid<>(EndpointStatus.class, false);
        endpointGrid.setSizeFull();
        endpointGrid.addColumn(EndpointStatus::getName).setHeader("Endpoint").setAutoWidth(true);
        endpointGrid.addColumn(EndpointStatus::getUrl).setHeader("URL").setFlexGrow(1);
        endpointGrid.addColumn(EndpointStatus::getStatus).setHeader("Status").setAutoWidth(true);
        endpointGrid.addColumn(EndpointStatus::getStatusCode).setHeader("Status Code").setAutoWidth(true);
        endpointGrid.setAllRowsVisible(true);
        add(new H3("OpenTelemetry Endpoint Status"));
        add(endpointGrid);
    }

    private Properties loadObservabilityKitProperties() {
        Properties props = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("observability-kit.properties")) {
            if (inputStream != null) {
                props.load(inputStream);
                logger.info("Loaded {} observability kit properties", props.size());
            } else {
                logger.warn("observability-kit.properties file not found in classpath");
            }
        } catch (IOException e) {
            logger.warn("Could not load observability-kit.properties: {}", e.getMessage());
        }
        return props;
    }

    private String getOtelEndpointHost() {
        // Use otel.exporter.otlp.endpoint if present, else default to localhost
        String endpoint = otelProperties.getProperty("otel.exporter.otlp.endpoint");
        if (endpoint != null && !endpoint.isBlank()) {
            try {
                URL url = new URI(endpoint).toURL();
                return url.getHost();
            } catch (Exception e) {
                logger.warn("Invalid endpoint URL: {}", endpoint);
            }
        }
        return "localhost";
    }

    private int getOtelEndpointPort() {
        // Use otel.exporter.otlp.endpoint if present, else default to 4318
        String endpoint = otelProperties.getProperty("otel.exporter.otlp.endpoint");
        if (endpoint != null && !endpoint.isBlank()) {
            try {
                URL url = new URI(endpoint).toURL();
                int port = url.getPort();
                if (port > 0) return port;
            } catch (Exception e) {
                logger.warn("Invalid endpoint URL: {}", endpoint);
            }
        }
        return 4318;
    }

    private String getOtelBaseUrl() {
        String host = getOtelEndpointHost();
        int port = getOtelEndpointPort();
        return "http://" + host + ":" + port;
    }

    private EndpointStatus checkEndpoint(String name, String endpointPath) {
        String endpointUrl = getOtelBaseUrl() + endpointPath;
        int code = -1;
        String status = "Unavailable";
        try {
            HttpURLConnection connection = (HttpURLConnection) new URI(endpointUrl).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-protobuf");
            // Send a minimal valid OTLP protobuf payload (empty message)
            connection.getOutputStream().write(new byte[0]);
            code = connection.getResponseCode();
            if (code >= 200 && code < 400) {
                status = "Available";
            } else if (code == 405) {
                status = "Method Not Allowed (405)";
            }
        } catch (Exception e) {
            logger.debug("Endpoint check failed for {}: {}", endpointUrl, e.getMessage());
        }
        return new EndpointStatus(name, endpointUrl, status, code);
    }

    private List<EndpointStatus> getEndpointStatuses() {
        List<EndpointStatus> endpoints = new ArrayList<>();
        endpoints.add(checkEndpoint("Traces Endpoint", "/v1/traces"));
        endpoints.add(checkEndpoint("Metrics Endpoint", "/v1/metrics"));
        endpoints.add(checkEndpoint("Logs Endpoint", "/v1/logs"));
        return endpoints;
    }

    private Grid<EndpointStatus> endpointGrid;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Refresh endpoint statuses when entering the view
        endpointGrid.setItems(getEndpointStatuses());
    }
}
