package org.vaadin.kitchensink.security;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.DescriptionList;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;

/**
 * A view for user login, allowing users to log in with sample credentials.
 * This view is accessible without authentication and provides a simple login
 * form along with example user credentials.
 */
@AnonymousAllowed
@PageTitle("Login")
// No @Route annotation - the route is registered dynamically by DefaultSecurityConfig.
public class LoginView extends Main implements BeforeEnterObserver {

    static final String LOGIN_PATH = "login";
    private static final String CALLOUT_HIDDEN_KEY = "login-callout-hidden";

    private final transient AuthenticationContext authenticationContext;
    private final LoginForm login;

    LoginView(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;

        // Create the components
        login = new LoginForm();
        login.setAction(LOGIN_PATH);
        login.setForgotPasswordButtonVisible(false);

        var exampleUsers = new Div(new Div("Use the following details to login"));
        SampleUsers.ALL_USERS.forEach(user -> exampleUsers.add(createSampleUserCard(user)));

        // Configure the view
        setSizeFull();
        addClassNames("dev-login-view");

        exampleUsers.addClassNames("dev-users");

        var contentDiv = new Div(login, exampleUsers);
        contentDiv.addClassNames("dev-content-div");
        add(contentDiv);

        var devModeMenuDiv = new Div("You can also use the Dev Mode Menu here to impersonate any user");
        devModeMenuDiv.addClassNames("dev-mode-speech-bubble");
        // Hide the callout when clicked
        devModeMenuDiv.addClickListener(event -> {
            WebStorage.setItem(WebStorage.Storage.LOCAL_STORAGE, CALLOUT_HIDDEN_KEY, "1");
            devModeMenuDiv.setVisible(false);
        });
        devModeMenuDiv.setVisible(false);
        add(devModeMenuDiv);

        // Don't show the callout if already hidden once
        WebStorage.getItem(WebStorage.Storage.LOCAL_STORAGE, CALLOUT_HIDDEN_KEY,
                value -> devModeMenuDiv.setVisible(value == null));
    }

    private Component createSampleUserCard(User user) {
        var card = new Div();
        card.addClassNames("dev-user-card");

        var fullName = new H3(user.getAppUser().getFullName());

        var credentials = new DescriptionList();
        credentials.add(new DescriptionList.Term("Username"), new DescriptionList.Description(user.getUsername()));
        credentials.add(new DescriptionList.Term("Password"),
                new DescriptionList.Description(SampleUsers.SAMPLE_PASSWORD));

        // Make it easier to log in while still going through the normal authentication process.
        var loginButton = new Button(VaadinIcon.SIGN_IN.create(), event ->
            login.getElement().executeJs("""
                    document.getElementById("vaadinLoginUsername").value = $0;
                    document.getElementById("vaadinLoginPassword").value = $1;
                    document.forms[0].submit();
                    """, user.getUsername(), SampleUsers.SAMPLE_PASSWORD));
        loginButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY);

        card.add(new Div(fullName, credentials));
        card.add(loginButton);

        return card;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (authenticationContext.isAuthenticated()) {
            // Redirect to the main view if the user is already logged in. This makes impersonation easier to work with.
            event.forwardTo("");
            return;
        }

        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            login.setError(true);
        }
    }
}
