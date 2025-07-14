package org.vaadin.kitchensink.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.List;

import org.springframework.boot.info.BuildProperties;

/**
 * The main view is a top-level placeholder for other views.
 */
@Layout
@AnonymousAllowed
public class MainLayout extends AppLayout implements BeforeEnterObserver {

    private final transient BuildProperties buildProperties;
    private final transient AuthenticationContext authenticationContext;

    private H1 viewTitle;
    private Footer footer;

    public MainLayout(BuildProperties buildProperties, AuthenticationContext authenticationContext) {
        this.buildProperties = buildProperties;
        this.authenticationContext = authenticationContext;

        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle = new H1();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        addToNavbar(true, toggle, viewTitle);
    }

    private void addDrawerContent() {
        Span appName = new Span("Kitchen Sink");
        appName.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE);

        Span version = new Span(buildProperties.getVersion());
        Header header = new Header(appName, version);

        Scroller scroller = new Scroller(createNavigation());

        addToDrawer(header, scroller, createFooter());
    }

    private SideNav createNavigation() {
        SideNav nav = new SideNav();

        List<MenuEntry> menuEntries = MenuConfiguration.getMenuEntries();
        menuEntries.forEach(entry -> {
            if (entry.icon() != null) {
                nav.addItem(new SideNavItem(entry.title(), entry.path(), new SvgIcon(entry.icon())));
            } else {
                nav.addItem(new SideNavItem(entry.title(), entry.path()));
            }
        });

        return nav;
    }

    private Footer createFooter() {
        footer = new Footer();
        updateFooter();
        return footer;
    }

    private void updateFooter() {
        footer.removeAll();

        if (!authenticationContext.isAuthenticated()) {
            String currentPath = VaadinService.getCurrentRequest().getPathInfo();
            if (currentPath == null) {
                currentPath = "";
            }

            String loginUrl = "/login?redirect=" + java.net.URLEncoder.encode(currentPath, java.nio.charset.StandardCharsets.UTF_8);

            Button loginButton = new Button("Login");
            loginButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            loginButton.addClassNames(
                    LumoUtility.TextColor.PRIMARY,
                    LumoUtility.Padding.SMALL
            );
            loginButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(loginUrl)));

            footer.add(loginButton);
        } else {
            Button logoutButton = new Button("Logout");
            logoutButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            logoutButton.addClassNames(
                    LumoUtility.TextColor.PRIMARY,
                    LumoUtility.Padding.SMALL
            );
            logoutButton.addClickListener(e -> authenticationContext.logout());

            footer.add(logoutButton);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Update footer when navigating to ensure login link reflects current page
        if (footer != null) {
            updateFooter();
        }
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        return MenuConfiguration.getPageHeader(getContent()).orElse("");
    }
}
