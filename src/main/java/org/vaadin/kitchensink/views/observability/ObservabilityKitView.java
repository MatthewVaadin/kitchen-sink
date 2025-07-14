package org.vaadin.kitchensink.views.observability;

import java.io.IOException;
import java.io.InputStream;
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
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@AnonymousAllowed
@PageTitle("Observability Kit")
@Route(value = "observability-kit")
@Menu(order = 400, icon = LineAwesomeIconUrl.CHART_LINE_SOLID)
@Profile("observability-kit")
public class ObservabilityKitView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(ObservabilityKitView.class);

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

    public ObservabilityKitView() {
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

        // Load observability kit properties
        List<ObservabilityProperty> observabilityProperties = loadObservabilityKitProperties();

        if (observabilityProperties.isEmpty()) {
            add(new H3("No observability kit properties found"));
            add(new Span("The observability-kit.properties file could not be loaded or is empty."));
            return;
        }

        // Set initial data to the grid
        propertiesGrid.setItems(observabilityProperties);

        add(propertiesGrid);
    }

    private List<ObservabilityProperty> loadObservabilityKitProperties() {
        List<ObservabilityProperty> properties = new ArrayList<>();

        try {
            // Try to load from classpath
            InputStream inputStream = getClass().getClassLoader()
                    .getResourceAsStream("observability-kit.properties");

            if (inputStream != null) {
                Properties props = new Properties();
                props.load(inputStream);

                props.entrySet().stream()
                        .sorted((e1, e2) -> e1.getKey().toString().compareToIgnoreCase(e2.getKey().toString()))
                        .forEach(entry -> properties.add(
                                new ObservabilityProperty(entry.getKey().toString(), entry.getValue().toString())));

                inputStream.close();
                logger.info("Loaded {} observability kit properties", properties.size());
            } else {
                logger.warn("observability-kit.properties file not found in classpath");
            }
        } catch (IOException e) {
            logger.error("Failed to load observability-kit.properties", e);
        }

        return properties;
    }
}
