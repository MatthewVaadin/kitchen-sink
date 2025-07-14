package org.vaadin.kitchensink.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;

import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.spring.security.RequestUtil;
import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategyConfiguration;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;

@EnableWebSecurity
@Configuration
@Import({ VaadinAwareSecurityContextHolderStrategyConfiguration.class })
@Profile("!control-center")
public class DefaultSecurityConfiguration {

    private RequestUtil requestUtil;

    @Autowired
    void setRequestUtil(RequestUtil requestUtil) {
        this.requestUtil = requestUtil;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .with(VaadinSecurityConfigurer.vaadin(),
                configurer -> configurer.loginView(LoginView.LOGIN_PATH))
                .authorizeHttpRequests(this::requestWhitelist)
                .csrf(this::ignoreInternalRequests)
                .build();
    }

    private void requestWhitelist(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry urlRegistry) {
        urlRegistry.requestMatchers("/actuator/**").permitAll();
        // Allow access to static resources (icons, images, etc.)
        urlRegistry.requestMatchers("/VAADIN/**").permitAll();
        urlRegistry.requestMatchers("/icons/**").permitAll();
        urlRegistry.requestMatchers("/images/**").permitAll();
        urlRegistry.requestMatchers("/static/**").permitAll();
        urlRegistry.requestMatchers("/line-awesome/**").permitAll();
        urlRegistry.requestMatchers("/favicon.ico").permitAll();
    }

    protected void ignoreInternalRequests(CsrfConfigurer<HttpSecurity> csrf) {
        csrf.ignoringRequestMatchers(requestUtil::isFrameworkInternalRequest);
    }

    @Bean
    UserDetailsService userDetailsService() {
        return new DefaultUserDetailsService(SampleUsers.ALL_USERS);
    }

    @Bean
    VaadinServiceInitListener defaultLoginConfigurer() {
        return serviceInitEvent -> {
            if (serviceInitEvent.getSource().getDeploymentConfiguration().isProductionMode()) {
                throw new IllegalStateException(
                        "Development profile is active but Vaadin is running in production mode. This indicates a configuration error - development profile should not be used in production.");
            }
            var routeConfiguration = RouteConfiguration.forApplicationScope();
            routeConfiguration.setRoute(LoginView.LOGIN_PATH, LoginView.class);
        };
    }
}
