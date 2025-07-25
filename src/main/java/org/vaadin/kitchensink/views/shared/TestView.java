package org.vaadin.kitchensink.views.shared;

import org.vaadin.lineawesome.LineAwesomeIconUrl;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@AnonymousAllowed
@PageTitle("Test")
@Route(value = "test")
@Menu(order = 1000, icon = LineAwesomeIconUrl.VIAL_SOLID)
public class TestView extends VerticalLayout {

    public TestView() {
        setSpacing(true);
        setPadding(true);
        setSizeFull();

        // Add components to test
        add(new H2("Add your test components here"));
    }
}
