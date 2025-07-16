package org.vaadin.kitchensink.views.shared;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@AnonymousAllowed
@PageTitle("Application Health")
@Route(value = "health")
@Menu(order = 19, icon = LineAwesomeIconUrl.HEARTBEAT_SOLID)
public class HealthView extends VerticalLayout {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm:ss");

    private final transient HealthEndpoint healthEndpoint;
    private final Grid<HealthComponentEntry> grid;

    public static class HealthComponentEntry {
        private final String name;
        private final String status;
        private final Map<String, Object> details;

        public HealthComponentEntry(String name, String status, Map<String, Object> details) {
            this.name = name;
            this.status = status;
            this.details = details;
        }

        public String getName() {
            return name;
        }

        public String getStatus() {
            return status;
        }

        public Map<String, Object> getDetails() {
            return details;
        }
    }

    public static class DetailEntry {
        private final String key;
        private final String value;

        public DetailEntry(String key, String value) {
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

    public HealthView(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
        setSpacing(true);
        setPadding(true);
        setSizeFull();

        HealthComponent overall = healthEndpoint.health();
        H1 header = new H1("Overall Status: " + overall.getStatus());

        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
        Span timestamp = new Span("Checked at: " + now.format(TIMESTAMP_FORMATTER));

        HorizontalLayout top = new HorizontalLayout(header, timestamp);
        top.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        add(top);

        // Build the grid with item details renderer
        grid = new Grid<>(HealthComponentEntry.class, false);
        grid.addColumn(HealthComponentEntry::getName)
                .setHeader("Component")
                .setAutoWidth(true);
        grid.addColumn(HealthComponentEntry::getStatus)
                .setHeader("Status")
                .setAutoWidth(true);

        // Set up item details renderer with ComponentRenderer
        grid.setItemDetailsRenderer(new ComponentRenderer<>(this::createDetailsRenderer));

        grid.setSizeFull();
        add(grid);
        expand(grid);

        // Initial load
        refreshGrid();

        // Poll every 5 seconds but preserve expanded items
        UI.getCurrent().setPollInterval(5_000);
        UI.getCurrent().addPollListener(event -> refreshGridPreservingExpansion());
    }

    private VerticalLayout createDetailsRenderer(HealthComponentEntry entry) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(false);
        layout.setPadding(false);

        if (entry.getDetails().isEmpty()) {
            layout.add(new Span("No details available"));
            return layout;
        }

        // Create a grid for the details
        Grid<DetailEntry> detailsGrid = new Grid<>(DetailEntry.class, false);
        detailsGrid.addColumn(DetailEntry::getKey)
                .setHeader("Property")
                .setAutoWidth(true);
        detailsGrid.addColumn(DetailEntry::getValue)
                .setHeader("Value")
                .setFlexGrow(1);

        // Convert details map to list of DetailEntry objects
        List<DetailEntry> detailEntries = entry.getDetails().entrySet().stream()
                .map(e -> new DetailEntry(e.getKey(), formatValue(e.getValue())))
                .toList();

        detailsGrid.setItems(detailEntries);
        detailsGrid.setAllRowsVisible(true);
        detailsGrid.setHeight("auto");

        layout.add(detailsGrid);
        return layout;
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Map || value instanceof List) {
            return value.toString();
        }
        return String.valueOf(value);
    }

    private void refreshGrid() {
        HealthComponent h = healthEndpoint.health();
        List<HealthComponentEntry> entries;

        if (h instanceof CompositeHealth compositeHealth) {
            entries = compositeHealth.getComponents().entrySet().stream()
                    .filter(e -> e.getValue() instanceof Health)
                    .map(e -> {
                        Health sub = (Health) e.getValue();
                        return new HealthComponentEntry(
                                e.getKey(),
                                sub.getStatus().toString(),
                                sub.getDetails()
                        );
                    })
                    .toList();
        } else if (h instanceof Health health) {
            // Single health component
            entries = List.of(new HealthComponentEntry(
                    "Application",
                    health.getStatus().toString(),
                    health.getDetails()
            ));
        } else {
            // Fallback for unknown health component type
            entries = List.of(new HealthComponentEntry(
                    "Application",
                    h.getStatus().toString(),
                    Map.of()
            ));
        }

        grid.setItems(entries);
    }

    private void refreshGridPreservingExpansion() {
        // Store names of currently expanded items
        Set<String> expandedItemNames = new HashSet<>();
        grid.getDataProvider().fetch(new Query<>()).forEach(item -> {
            if (grid.isDetailsVisible(item)) {
                expandedItemNames.add(item.getName());
            }
        });

        // Refresh the data
        refreshGrid();

        // Restore expanded state for items with matching names
        if (!expandedItemNames.isEmpty()) {
            grid.getDataProvider().fetch(new Query<>()).forEach(newItem -> {
                if (expandedItemNames.contains(newItem.getName())) {
                    grid.setDetailsVisible(newItem, true);
                }
            });
        }
    }
}
