package org.vaadin.kitchensink.views.shared;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import org.vaadin.lineawesome.LineAwesomeIcon;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * View that displays all available routes in the application, including their
 * metadata such as URL, target class, aliases, page title, menu order, security
 * annotations, layout, and whether they have parameters.
 */
@AnonymousAllowed
@PageTitle("Routes")
@Route(value = "routes")
@Menu(order = 45, icon = LineAwesomeIconUrl.SITEMAP_SOLID)
public class RoutesView extends VerticalLayout {

    /**
     * Data class representing route information.
     */
    public static class RouteInfo {
        private final String url;
        private final String canonicalName;
        private final String aliases;
        private final String pageTitle;
        private final String menuOrder;
        private final String securityAnnotations;
        private final String layout;
        private final boolean hasParameters;

        public RouteInfo(String url, String canonicalName, String aliases, String pageTitle,
                        String menuOrder, String securityAnnotations, String layout, boolean hasParameters) {
            this.url = url;
            this.canonicalName = canonicalName;
            this.aliases = aliases;
            this.pageTitle = pageTitle;
            this.menuOrder = menuOrder;
            this.securityAnnotations = securityAnnotations;
            this.layout = layout;
            this.hasParameters = hasParameters;
        }

        public String getUrl() { return url; }
        public String getCanonicalName() { return canonicalName; }
        public String getAliases() { return aliases; }
        public String getPageTitle() { return pageTitle; }
        public String getMenuOrder() { return menuOrder; }
        public String getSecurityAnnotations() { return securityAnnotations; }
        public String getLayout() { return layout; }
        public boolean isHasParameters() { return hasParameters; }
    }

    /**
     * Constructor for the RoutesView.
     * Initializes the layout, adds a title, filter field, and grid to display
     * route information.
     */
    public RoutesView() {
        setSpacing(true);
        setPadding(true);
        setSizeFull();

        add(new H2("Routes"));

        // Create filter text field
        TextField filterField = new TextField();
        filterField.setClearButtonVisible(true);
        filterField.setPlaceholder("Filter routes...");
        filterField.setPrefixComponent(LineAwesomeIcon.SEARCH_SOLID.create());
        filterField.setValueChangeMode(ValueChangeMode.LAZY);
        filterField.setWidthFull();

        Grid<RouteInfo> grid = new Grid<>(RouteInfo.class, false);
        grid.setSizeFull();
        grid.addColumn(RouteInfo::getUrl)
            .setHeader("Route URL")
            .setAutoWidth(true)
            .setSortable(true);
        grid.addColumn(RouteInfo::getCanonicalName)
            .setHeader("Target Class")
            .setAutoWidth(true)
            .setSortable(true);
        grid.addColumn(RouteInfo::getAliases)
            .setHeader("Aliases")
            .setAutoWidth(true)
            .setSortable(true);

        grid.addColumn(RouteInfo::getPageTitle)
            .setHeader("Page Title")
            .setAutoWidth(true)
            .setSortable(true);

        grid.addColumn(RouteInfo::getMenuOrder)
            .setHeader("Menu Order")
            .setAutoWidth(true)
            .setSortable(true);

        grid.addColumn(RouteInfo::getSecurityAnnotations)
            .setHeader("Security")
            .setAutoWidth(true)
            .setSortable(true);

        grid.addColumn(RouteInfo::getLayout)
            .setHeader("Layout")
            .setAutoWidth(true)
            .setSortable(true);

        grid.addColumn(route -> route.isHasParameters() ? "Yes" : "No")
            .setHeader("Has Parameters")
            .setAutoWidth(true)
            .setSortable(true);

        List<RouteInfo> allRoutes = new ArrayList<>();
        try {
            RouteConfiguration routeConfiguration = RouteConfiguration.forRegistry(
                    VaadinService.getCurrent().getRouter().getRegistry());
            routeConfiguration.getAvailableRoutes().stream()
                .map(this::createRouteInfo)
                .forEach(allRoutes::add);
        } catch (Exception e) {
            // Handle error case - could add error notification here
            allRoutes.add(new RouteInfo("Error", e.getMessage(), "", "", "", "", "", false));
        }
        grid.setItems(allRoutes);

        // Add filter functionality
        filterField.addValueChangeListener(event -> {
            String filterText = event.getValue();
            if (filterText == null || filterText.trim().isEmpty()) {
                // Show all routes when filter is empty
                grid.setItems(allRoutes);
            } else {
                // Filter routes by URL, target class, and aliases (case-insensitive)
                String lowerCaseFilter = filterText.toLowerCase();
                List<RouteInfo> filteredRoutes = allRoutes.stream()
                        .filter(route ->
                            route.getUrl().toLowerCase().contains(lowerCaseFilter) ||
                            route.getCanonicalName().toLowerCase().contains(lowerCaseFilter) ||
                            route.getAliases().toLowerCase().contains(lowerCaseFilter))
                        .toList();
                grid.setItems(filteredRoutes);
            }
        });

        add(filterField, grid);
    }

    private RouteInfo createRouteInfo(RouteData routeData) {
        String url = routeData.getTemplate().isEmpty() ? "/" : "/" + routeData.getTemplate();
        String canonicalName = routeData.getNavigationTarget().getCanonicalName();

        // Get aliases from route aliases
        String aliases = routeData.getRouteAliases().stream()
            .map(aliasData -> {
                String alias = aliasData.getTemplate();
                return alias.isEmpty() ? "/" : "/" + alias;
            })
            .collect(Collectors.joining(", "));

        if (aliases.isEmpty()) {
            aliases = "None";
        }

        // Extract information from annotations
        Class<?> targetClass = routeData.getNavigationTarget();

        String pageTitle = extractPageTitle(targetClass);
        String menuOrder = extractMenuOrder(targetClass);
        String securityAnnotations = extractSecurityAnnotations(targetClass);
        String layout = extractLayout(targetClass);
        boolean hasParameters = routeData.getTemplate().contains("{");

        return new RouteInfo(url, canonicalName, aliases, pageTitle, menuOrder, securityAnnotations, layout, hasParameters);
    }

    private String extractPageTitle(Class<?> targetClass) {
        PageTitle pageTitle = targetClass.getAnnotation(PageTitle.class);
        return pageTitle != null ? pageTitle.value() : "None";
    }

    private String extractMenuOrder(Class<?> targetClass) {
        Menu menu = targetClass.getAnnotation(Menu.class);
        return menu != null ? String.valueOf(menu.order()) : "None";
    }

    private String extractSecurityAnnotations(Class<?> targetClass) {
        if (targetClass.getAnnotation(AnonymousAllowed.class) != null) {
            return "Anonymous access";
        }

        if (targetClass.getAnnotation(PermitAll.class) != null) {
            return "All authenticated users";
        }

        if (targetClass.getAnnotation(DenyAll.class) != null) {
            return "Access denied";
        }

        RolesAllowed rolesAllowed =
            targetClass.getAnnotation(RolesAllowed.class);
        if (rolesAllowed != null) {
            String roles = String.join(", ", rolesAllowed.value());
            return "Roles allowed: " + roles;
        }

        // If no security annotation is found, it's protected by default
        return "Default (Authenticated users)";
    }

    private String extractLayout(Class<?> targetClass) {
        Route route = targetClass.getAnnotation(Route.class);
        if (route != null && route.layout() != com.vaadin.flow.component.UI.class) {
            return route.layout().getSimpleName();
        }
        return "None";
    }
}
