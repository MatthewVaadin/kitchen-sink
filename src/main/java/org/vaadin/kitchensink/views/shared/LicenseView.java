package org.vaadin.kitchensink.views.shared;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * View to display Vaadin license information, including status and details.
 * This view checks for various types of licenses (Pro, Development, Offline)
 * and displays their status in a user-friendly format.
 */
@AnonymousAllowed
@PageTitle("License")
@Route(value = "license")
@Menu(order = 50, icon = LineAwesomeIconUrl.AWARD_SOLID)
public class LicenseView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(LicenseView.class);

    private static final String FOUND = "Found";
    private static final String SET_VIA_SYSTEM_PROPERTY = "Set via system property";

    /**
     * Data class representing license information.
     */
    public static class LicenseInfo {
        private final String type;
        private final String status;
        private final String details;

        public LicenseInfo(String type, String status, String details) {
            this.type = type;
            this.status = status;
            this.details = details;
        }

        public String getType() { return type; }
        public String getStatus() { return status; }
        public String getDetails() { return details; }
    }

    /**
     * Data class to encapsulate the result of the license check.
     */
    private static class LicenseCheckResult {
        private final boolean valid;
        private final String summary;
        private final String details;
        private final List<LicenseInfo> licenseInfos;

        public LicenseCheckResult(boolean valid, String summary, String details, List<LicenseInfo> licenseInfos) {
            this.valid = valid;
            this.summary = summary;
            this.details = details;
            this.licenseInfos = licenseInfos;
        }

        public boolean isValid() { return valid; }
        public String getSummary() { return summary; }
        public String getDetails() { return details; }
        public List<LicenseInfo> getLicenseInfos() { return licenseInfos; }
    }

    /**
     * Constructor for LicenseView.
     * Initializes the view, checks the Vaadin license status, and displays
     * the results in a grid format.
     */
    public LicenseView() {
        setSpacing(true);
        setPadding(true);
        setSizeFull();

        add(new H1("Vaadin License Information"));

        // Check license status
        LicenseCheckResult licenseResult = checkVaadinLicense();

        // Display license status summary
        HorizontalLayout statusLayout = new HorizontalLayout();
        statusLayout.setAlignItems(Alignment.CENTER);

        Icon statusIcon = licenseResult.isValid() ?
            VaadinIcon.CHECK_CIRCLE.create() :
            VaadinIcon.EXCLAMATION_CIRCLE.create();
        statusIcon.setColor(licenseResult.isValid() ? "green" : "orange");

        Span statusText = new Span(licenseResult.getSummary());
        statusText.addClassName(licenseResult.isValid() ?
            LumoUtility.TextColor.SUCCESS :
            LumoUtility.TextColor.WARNING);

        statusLayout.add(statusIcon, statusText);
        add(statusLayout);

        // Add details if available
        if (licenseResult.getDetails() != null && !licenseResult.getDetails().isEmpty()) {
            add(new H3("License Details"));
            add(new Span(licenseResult.getDetails()));
        }

        // Display license information in a grid
        Grid<LicenseInfo> licenseGrid = new Grid<>(LicenseInfo.class, false);
        licenseGrid.addColumn(LicenseInfo::getType).setHeader("License Type").setAutoWidth(true);
        licenseGrid.addColumn(LicenseInfo::getStatus).setHeader("Status").setAutoWidth(true);
        licenseGrid.addColumn(LicenseInfo::getDetails).setHeader("Details").setAutoWidth(true);

        licenseGrid.setItems(licenseResult.getLicenseInfos());
        licenseGrid.setAllRowsVisible(true);

        add(new H3("License Check Results"));
        add(licenseGrid);
    }

    private LicenseCheckResult checkVaadinLicense() {
        List<LicenseInfo> licenseInfos = new ArrayList<>();
        LicenseStatus licenseStatus = new LicenseStatus();

        try {
            checkSystemPropertyLicenses(licenseInfos, licenseStatus);
            checkEnvironmentVariableLicenses(licenseInfos, licenseStatus);
            checkLicenseChecker(licenseInfos);
            handleNoLicenseFound(licenseInfos, licenseStatus);
        } catch (Exception e) {
            return handleLicenseCheckError(e, licenseInfos);
        }

        return new LicenseCheckResult(
            licenseStatus.hasValidLicense,
            licenseStatus.summary,
            licenseStatus.details,
            licenseInfos
        );
    }

    private void checkSystemPropertyLicenses(List<LicenseInfo> licenseInfos, LicenseStatus status) {
        checkProKey(licenseInfos, status);
        checkDevelopmentLicense(licenseInfos, status);
        checkOfflineLicense(licenseInfos, status);
    }

    private void checkProKey(List<LicenseInfo> licenseInfos, LicenseStatus status) {
        String proKey = System.getProperty("vaadin.proKey");
        if (isValidLicenseValue(proKey)) {
            licenseInfos.add(new LicenseInfo("Pro Key", FOUND, SET_VIA_SYSTEM_PROPERTY));
            updateLicenseStatus(status, "Vaadin Pro license found (Pro Key)");
        }
    }

    private void checkDevelopmentLicense(List<LicenseInfo> licenseInfos, LicenseStatus status) {
        String devLicense = System.getProperty("vaadin.developmentmode.licensekey");
        if (isValidLicenseValue(devLicense)) {
            licenseInfos.add(new LicenseInfo("Development License", FOUND, SET_VIA_SYSTEM_PROPERTY));
            updateLicenseStatusIfNotSet(status, "Vaadin development license found");
        }
    }

    private void checkOfflineLicense(List<LicenseInfo> licenseInfos, LicenseStatus status) {
        String offlineLicense = System.getProperty("vaadin.offline.key");
        if (isValidLicenseValue(offlineLicense)) {
            licenseInfos.add(new LicenseInfo("Offline License", FOUND, SET_VIA_SYSTEM_PROPERTY));
            updateLicenseStatusIfNotSet(status, "Vaadin offline license found");
        }
    }

    private void checkEnvironmentVariableLicenses(List<LicenseInfo> licenseInfos, LicenseStatus status) {
        String envProKey = System.getenv("VAADIN_PRO_KEY");
        if (isValidLicenseValue(envProKey)) {
            licenseInfos.add(new LicenseInfo("Pro Key (ENV)", FOUND, "Set via environment variable"));
            updateLicenseStatusIfNotSet(status, "Vaadin Pro license found (Environment)");
        }
    }

    private void checkLicenseChecker(List<LicenseInfo> licenseInfos) {
        try {
            Class<?> licenseCheckerClass = Class.forName("com.vaadin.pro.licensechecker.LicenseChecker");
            Method checkLicenseMethod = licenseCheckerClass.getMethod("checkLicense", String.class, String.class);
            checkLicenseMethod.invoke(null, "vaadin-core", "24.8.3");
            licenseInfos.add(new LicenseInfo("License Checker", "Available", "Vaadin license checker found"));
        } catch (Exception e) {
            logger.debug("Vaadin license checker not available or accessible: {}", e.getMessage());
            licenseInfos.add(new LicenseInfo("License Checker", "Not Available",
                "License checker not found - using open source components"));
        }
    }

    private void handleNoLicenseFound(List<LicenseInfo> licenseInfos, LicenseStatus status) {
        if (!status.hasValidLicense) {
            status.details = "The application is running with open source Vaadin components only. " +
                           "Premium features require a valid Vaadin license.";
            licenseInfos.add(new LicenseInfo("Open Source", "Active", "Using free Vaadin components"));
        }
    }

    private LicenseCheckResult handleLicenseCheckError(Exception e, List<LicenseInfo> licenseInfos) {
        logger.error("Error checking Vaadin license", e);
        licenseInfos.add(new LicenseInfo("Error", "Failed", "Could not check license: " + e.getMessage()));
        return new LicenseCheckResult(false, "Error checking license status", "", licenseInfos);
    }

    private boolean isValidLicenseValue(String licenseValue) {
        return licenseValue != null && !licenseValue.trim().isEmpty();
    }

    private void updateLicenseStatus(LicenseStatus status, String summary) {
        status.hasValidLicense = true;
        status.summary = summary;
    }

    private void updateLicenseStatusIfNotSet(LicenseStatus status, String summary) {
        if (!status.hasValidLicense) {
            updateLicenseStatus(status, summary);
        }
    }

    /**
     * Helper class to track license checking state
     */
    private static class LicenseStatus {
        boolean hasValidLicense = false;
        String summary = "No Vaadin license detected";
        String details = "";
    }
}
