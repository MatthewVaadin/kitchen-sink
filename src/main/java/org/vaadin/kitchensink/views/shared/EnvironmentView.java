package org.vaadin.kitchensink.views.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;

import org.springframework.core.env.Environment;
import org.vaadin.lineawesome.LineAwesomeIcon;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

/**
 * A view that displays the current Spring Environment properties.
 * It shows active profiles and all properties with their values and sources.
 * Provides a filter to search through the properties by key.
 */
@AnonymousAllowed
@PageTitle("Environment")
@Route(value = "environment")
@Menu(order = 20, icon = LineAwesomeIconUrl.FILE_INVOICE_DOLLAR_SOLID)
public class EnvironmentView extends VerticalLayout {

    /**
     * Data class representing a single property entry in the environment.
     */
    public static class PropertyEntry {
        private final String key;
        private final String value;
        private final String source;

        public PropertyEntry(String key, String value, String source) {
            this.key = key;
            this.value = value;
            this.source = source;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public String getSource() {
            return source;
        }
    }

    /**
     * Extracts a readable source name from property source names.
     * For Config resources, extracts the location and filename.
     */
    private String extractSourceName(String sourceName) {
        if (sourceName == null) {
            return "Unknown";
        }

        // Handle Config resource names like "Config resource 'class path resource [application.properties]'"
        if (sourceName.startsWith("Config resource")) {
            // Extract the part between the last '[' and ']'
            int startBracket = sourceName.lastIndexOf('[');
            int endBracket = sourceName.lastIndexOf(']');

            if (startBracket != -1 && endBracket != -1 && endBracket > startBracket) {
                String resourcePath = sourceName.substring(startBracket + 1, endBracket);

                // Extract just the filename from the path
                int lastSlash = resourcePath.lastIndexOf('/');
                if (lastSlash != -1) {
                    return resourcePath.substring(lastSlash + 1);
                }
                return resourcePath;
            }
        }

        return sourceName;
    }

    /**
     * Constructs the EnvironmentView with the current Spring Environment.
     *
     * @param environment
     *            the Spring Environment to display
     */
    public EnvironmentView(Environment environment) {
        setSpacing(true);
        setPadding(true);
        setSizeFull();

        String activeProfilesText = environment.getActiveProfiles().length > 0
                ? String.join(", ", environment.getActiveProfiles())
                : "none";
        Span activeProfiles = new Span(
                "Active Spring Profiles: " + activeProfilesText);
        add(activeProfiles);

        // Create filter text field
        TextField filterField = new TextField();
        filterField.setClearButtonVisible(true);
        filterField.setPlaceholder("Filter properties...");
        filterField.setPrefixComponent(LineAwesomeIcon.SEARCH_SOLID.create());
        filterField.setValueChangeMode(ValueChangeMode.LAZY);
        filterField.setWidthFull();

        // Create a single grid for all properties
        Grid<PropertyEntry> propertiesGrid = new Grid<>(PropertyEntry.class, false);
        propertiesGrid.setSizeFull();
        propertiesGrid.addColumn(PropertyEntry::getKey)
                .setHeader("Property Key")
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setSortable(true);
        propertiesGrid.addColumn(PropertyEntry::getSource)
                .setHeader("Source")
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setSortable(true);
        propertiesGrid.addColumn(PropertyEntry::getValue)
                .setHeader("Value");

        // Collect all properties from different sources
        List<PropertyEntry> allProperties = new ArrayList<>();

        MutablePropertySources propSources = ((ConfigurableEnvironment) environment).getPropertySources();
        StreamSupport.stream(propSources.spliterator(), false)
                .filter(EnumerablePropertySource.class::isInstance)
                .map(EnumerablePropertySource.class::cast)
                .forEach(propertySource -> {
                    String sourceName = extractSourceName(propertySource.getName());
                    for (String key : propertySource.getPropertyNames()) {
                        Object value = propertySource.getProperty(key);
                        if (value != null) {
                            allProperties.add(new PropertyEntry(key, value.toString(), sourceName));
                        }
                    }
                });

        // Sort all properties by key
        allProperties.sort((p1, p2) -> p1.getKey().compareToIgnoreCase(p2.getKey()));

        // Set initial data to the grid
        propertiesGrid.setItems(allProperties);

        // Add filter functionality
        filterField.addValueChangeListener(event -> {
            String filterText = event.getValue();
            if (filterText == null || filterText.trim().isEmpty()) {
                // Show all properties when filter is empty
                propertiesGrid.setItems(allProperties);
            } else {
                // Filter properties by key (case-insensitive)
                String lowerCaseFilter = filterText.toLowerCase();
                List<PropertyEntry> filteredProperties = allProperties.stream()
                        .filter(property -> property.getKey().toLowerCase().contains(lowerCaseFilter))
                        .toList();
                propertiesGrid.setItems(filteredProperties);
            }
        });

        add(new Span("Total properties: " + allProperties.size()));

        add(filterField,propertiesGrid);
    }
}
