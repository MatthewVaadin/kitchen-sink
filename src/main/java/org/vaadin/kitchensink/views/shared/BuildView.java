package org.vaadin.kitchensink.views.shared;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.info.BuildProperties;
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
import com.vaadin.flow.theme.lumo.LumoUtility;

@AnonymousAllowed
@PageTitle("Build")
@Route(value = "build")
@Menu(order = 15, icon = LineAwesomeIconUrl.HAMMER_SOLID)
public class BuildView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(BuildView.class);

    public static class BuildProperty {
        private final String property;
        private final String value;

        public BuildProperty(String property, String value) {
            this.property = property;
            this.value = value;
        }

        public String getProperty() {
            return property;
        }

        public String getValue() {
            return value;
        }
    }

    public BuildView(BuildProperties buildProperties) {
        setSpacing(true);
        setPadding(true);
        setSizeFull();

        add(new H1("Build Information"));

        if (buildProperties == null) {
            add(new H3("Build properties not available"));
            add(new Span("BuildProperties bean is not configured or accessible."));
            return;
        }

        // Display key build information prominently
        VerticalLayout summaryLayout = new VerticalLayout();
        summaryLayout.setPadding(false);
        summaryLayout.setSpacing(false);

        if (buildProperties.getName() != null) {
            Span appName = new Span("Application: " + buildProperties.getName());
            appName.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.SEMIBOLD);
            summaryLayout.add(appName);
        }

        if (buildProperties.getVersion() != null) {
            Span version = new Span("Version: " + buildProperties.getVersion());
            version.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.TextColor.SECONDARY);
            summaryLayout.add(version);
        }

        if (buildProperties.getTime() != null) {
            Span buildTime = new Span("Built: " + buildProperties.getTime().toString());
            buildTime.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.TERTIARY);
            summaryLayout.add(buildTime);
        }

        add(summaryLayout);

        // Create detailed properties grid
        List<BuildProperty> properties = extractBuildProperties(buildProperties);

        if (!properties.isEmpty()) {
            add(new H3("Detailed Build Properties"));

            Grid<BuildProperty> propertiesGrid = new Grid<>(BuildProperty.class, false);
            propertiesGrid.addColumn(BuildProperty::getProperty)
                    .setHeader("Property")
                    .setAutoWidth(true)
                    .setSortable(true);
            propertiesGrid.addColumn(BuildProperty::getValue)
                    .setHeader("Value")
                    .setAutoWidth(true);

            propertiesGrid.setItems(properties);
            propertiesGrid.setAllRowsVisible(true);

            add(propertiesGrid);
        }
    }

    private List<BuildProperty> extractBuildProperties(BuildProperties buildProperties) {
        List<BuildProperty> properties = new ArrayList<>();

        try {
            // Extract core properties that are guaranteed to be available
            if (buildProperties.getGroup() != null) {
                properties.add(new BuildProperty("Group", buildProperties.getGroup()));
            }

            if (buildProperties.getArtifact() != null) {
                properties.add(new BuildProperty("Artifact", buildProperties.getArtifact()));
            }

            if (buildProperties.getName() != null) {
                properties.add(new BuildProperty("Name", buildProperties.getName()));
            }

            if (buildProperties.getVersion() != null) {
                properties.add(new BuildProperty("Version", buildProperties.getVersion()));
            }

            if (buildProperties.getTime() != null) {
                Instant buildTime = buildProperties.getTime();
                properties.add(new BuildProperty("Build Time", buildTime.toString()));
                properties.add(new BuildProperty("Build Time (Epoch)", String.valueOf(buildTime.toEpochMilli())));
            }

        } catch (Exception e) {
            logger.error("Error extracting build properties", e);
            properties.add(new BuildProperty("Error", "Could not extract build properties: " + e.getMessage()));
        }

        return properties;
    }
}
