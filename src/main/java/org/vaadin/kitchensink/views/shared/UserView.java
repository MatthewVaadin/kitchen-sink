package org.vaadin.kitchensink.views.shared;

import org.springframework.security.core.userdetails.UserDetails;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.vaadin.kitchensink.security.User;

@AnonymousAllowed
@PageTitle("User")
@Route(value = "user")
@Menu(order = 60, icon = LineAwesomeIconUrl.USER_SOLID)
public class UserView extends VerticalLayout {

    public UserView(AuthenticationContext authenticationContext) {
        setSpacing(true);
        setPadding(true);
        setSizeFull();

        if (!authenticationContext.isAuthenticated()) {
            add(new H2("Not authenticated"));
            return;
        }

        // Try to get the User instance first for rich information
        authenticationContext.getAuthenticatedUser(User.class).ifPresentOrElse(user -> {
            add(new H1("User Information"));

            // User Identity
            add(new H3("Identity"));
            add(new Span("User ID: " + user.getAppUser().getUserId()));
            add(new Span("Username: " + user.getUsername()));
            add(new Span("Full Name: " + user.getAppUser().getFullName()));

            // Contact Information
            add(new H3("Contact Information"));
            String email = user.getAppUser().getEmail();
            add(new Span("Email: " + (email != null ? email : "Not provided")));

            // Profile URLs
            add(new H3("Profile"));
            String profileUrl = user.getAppUser().getProfileUrl();
            add(new Span("Profile URL: " + (profileUrl != null ? profileUrl : "Not available")));
            String pictureUrl = user.getAppUser().getPictureUrl();
            add(new Span("Picture URL: " + (pictureUrl != null ? pictureUrl : "Not available")));

            // Preferences
            add(new H3("Preferences"));
            add(new Span("Time Zone: " + user.getAppUser().getZoneId()));
            add(new Span("Locale: " + user.getAppUser().getLocale()));

            // Security Information
            add(new H3("Security"));
            add(new Span("Authorities: " + user.getAuthorities()));
            add(new Span("Account Non-Expired: " + user.isAccountNonExpired()));
            add(new Span("Account Non-Locked: " + user.isAccountNonLocked()));
            add(new Span("Credentials Non-Expired: " + user.isCredentialsNonExpired()));
            add(new Span("Enabled: " + user.isEnabled()));

        }, () ->
            // Fallback to basic UserDetails if User instance is not available
            authenticationContext.getAuthenticatedUser(UserDetails.class).ifPresent(user -> {
                add(new H1("User Information (Basic)"));
                add(new Span("Username: " + user.getUsername()));
                add(new Span("Authorities: " + user.getAuthorities()));
                add(new Span("Account Non-Expired: " + user.isAccountNonExpired()));
                add(new Span("Account Non-Locked: " + user.isAccountNonLocked()));
                add(new Span("Credentials Non-Expired: " + user.isCredentialsNonExpired()));
                add(new Span("Enabled: " + user.isEnabled())));
        });
    }
}
