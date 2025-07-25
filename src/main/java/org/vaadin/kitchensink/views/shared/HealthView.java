package org.vaadin.kitchensink.views.shared;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.provider.ListDataProvider;
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
    private final ListDataProvider<HealthComponentEntry> dataProvider;
    private final Span timestamp;

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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof HealthComponentEntry that)) return false;
            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
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
        timestamp = new Span("Checked at: " + now.format(TIMESTAMP_FORMATTER));

        // Add progress bar for refresh indication
        ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setWidth("100px");
        progressBar.getStyle().set("margin-left", "1rem");

        HorizontalLayout top = new HorizontalLayout(header, timestamp, progressBar);
        top.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        add(top);

        // Create data provider with mutable list
        dataProvider = new ListDataProvider<>(new ArrayList<>());

        // Build the grid with item details renderer
        grid = new Grid<>(HealthComponentEntry.class, false);
        grid.setDataProvider(dataProvider);

        // Add chevron icon column
        grid.addComponentColumn(entry -> {
            boolean isExpanded = grid.isDetailsVisible(entry);
            Icon chevron = new Icon(isExpanded ? VaadinIcon.CHEVRON_DOWN : VaadinIcon.CHEVRON_RIGHT);
            chevron.setSize("16px");
            chevron.getStyle().set("color", "var(--lumo-secondary-text-color)");
            return chevron;
        }).setHeader("").setAutoWidth(true).setFlexGrow(0);

        grid.addColumn(HealthComponentEntry::getName)
                .setHeader("Component")
                .setAutoWidth(true);
        grid.addColumn(HealthComponentEntry::getStatus)
                .setHeader("Status")
                .setAutoWidth(true);

        // Set up item details renderer with ComponentRenderer
        grid.setItemDetailsRenderer(new ComponentRenderer<>(this::createDetailsRenderer));

        // Add listener to update chevron icons when details are toggled
        grid.addItemClickListener(event -> {
            HealthComponentEntry item = event.getItem();
            // Refresh the row to update the chevron icon
            grid.getDataProvider().refreshItem(item);
        });

        grid.setSizeFull();
        add(grid);
        expand(grid);

        // Initial load
        refreshData();

        // Poll every 5 seconds
        UI.getCurrent().setPollInterval(5_000);
        UI.getCurrent().addPollListener(event -> refreshData());
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
                .map(e -> new DetailEntry(e.getKey(), formatValue(e.getKey(), e.getValue())))
                .toList();

        detailsGrid.setItems(detailEntries);
        detailsGrid.setAllRowsVisible(true);
        detailsGrid.setHeight("auto");

        layout.add(detailsGrid);
        return layout;
    }

    private String formatValue(String key, Object value) {
        if (value instanceof Number number) {
            if (isDiskSizeKey(key)) {
                return formatBytes(number.longValue());
            }
            return value.toString();
        }
        // If value is a string that looks like a number and key is disk size
        if (isDiskSizeKey(key) && value instanceof String string) {
            try {
                long bytes = Long.parseLong(string);
                return formatBytes(bytes);
            } catch (NumberFormatException ignored) {
                // If parsing fails, return the original string
                return string;
            }
        }
        return String.valueOf(value);
    }

    private boolean isDiskSizeKey(String key) {
        String lower = key.toLowerCase();
        return lower.contains("total") || lower.contains("free") || lower.contains("threshold");
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = ("KMGTPE").charAt(exp - 1) + "B";
        double value = bytes / Math.pow(1024, exp);
        return String.format("%.1f %s", value, pre);
    }

    private void refreshData() {
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

        // Update data provider items - this should preserve expanded state better
        dataProvider.getItems().clear();
        dataProvider.getItems().addAll(entries);
        dataProvider.refreshAll();

        // Update timestamp
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
        timestamp.setText("Checked at: " + now.format(TIMESTAMP_FORMATTER));
    }
}
