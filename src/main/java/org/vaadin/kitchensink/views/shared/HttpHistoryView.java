package org.vaadin.kitchensink.views.shared;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.boot.actuate.web.exchanges.HttpExchange;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@AnonymousAllowed
@PageTitle("HTTP History")
@Route(value = "http/history")
@Menu(order = 11, icon = LineAwesomeIconUrl.HISTORY_SOLID)
public class HttpHistoryView extends VerticalLayout {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd HH:mm:ss");

    public HttpHistoryView(HttpExchangeRepository exchangeRepository) {
        setSpacing(true);
        setPadding(true);
        setSizeFull();

        // 1) Fetch HTTP exchanges from repository
        List<HttpExchange> exchanges = exchangeRepository.findAll();
        // Create a mutable copy before reversing to avoid UnsupportedOperationException
        List<HttpExchange> mutableExchanges = new ArrayList<>(exchanges);
        Collections.reverse(mutableExchanges); // Show most recent first
        List<HttpExchange> recent = mutableExchanges.stream()
                .limit(50)
                .toList();

        // 2) Create the master grid
        Grid<HttpExchange> grid = new Grid<>();
        grid.setSizeFull();
        grid.addColumn(exchange -> formatTimestamp(exchange.getTimestamp()))
                .setHeader("Time")
                .setAutoWidth(true);
        grid.addColumn(exchange -> exchange.getRequest().getMethod())
                .setHeader("Method")
                .setAutoWidth(true);
        grid.addColumn(exchange -> exchange.getRequest().getUri().getPath())
                .setHeader("URI")
                .setFlexGrow(1);
        grid.addColumn(exchange -> exchange.getResponse() != null ?
                exchange.getResponse().getStatus() : "No Response")
                .setHeader("Status")
                .setAutoWidth(true);
        grid.addColumn(exchange -> exchange.getTimeTaken() != null ?
                exchange.getTimeTaken().toMillis() + "ms" : "N/A")
                .setHeader("Duration")
                .setAutoWidth(true);

        grid.setItems(recent);

        // 3) Create MasterDetailLayout
        MasterDetailLayout masterDetailLayout = new MasterDetailLayout();
        masterDetailLayout.setSizeFull();
        masterDetailLayout.setMaster(grid);

        grid.asSingleSelect().addValueChangeListener(e -> {
           HttpExchange selectedExchange = e.getValue();
           if (selectedExchange != null) {
               Component detailContent = createDetailContent(selectedExchange);
               masterDetailLayout.setDetail(detailContent);
           } else {
               // Clear detail view if no selection
               masterDetailLayout.setDetail(null);
           }
        });

        add(masterDetailLayout);
    }

    private Component createDetailContent(HttpExchange exchange) {
        if (exchange == null) {
            VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();
            layout.setPadding(false);
            layout.setSpacing(false);

            Span placeholder = new Span("Select an HTTP exchange to view details");
            placeholder.getStyle().set("color", "var(--lumo-secondary-text-color)");
            placeholder.getStyle().set("font-style", "italic");
            layout.add(placeholder);
            layout.setJustifyContentMode(JustifyContentMode.CENTER);
            layout.setAlignItems(Alignment.CENTER);

            return layout;
        }

        // Create tabs for Request/Response/Headers
        Tab requestTab = new Tab("Request");
        Tab responseTab = new Tab("Response");
        Tab headersTab = new Tab("Headers");
        Tabs tabs = new Tabs(requestTab, responseTab, headersTab);

        Div request = new Div();
        request.setSizeFull();
        Div response = new Div();
        response.setSizeFull();
        Div headers = new Div();
        headers.setSizeFull();

        Map<Tab, Component> detailViews = Map.of(
                requestTab, request,
                responseTab, response,
                headersTab, headers
        );

        // Populate content
        populateRequestDetails(request, exchange);
        populateResponseDetails(response, exchange);
        populateHeadersDetails(headers, exchange);

        // Hide all by default, show first tab
        detailViews.values().forEach(c -> c.setVisible(false));
        detailViews.get(requestTab).setVisible(true);

        // Tab switching logic
        tabs.addSelectedChangeListener(evt -> {
            detailViews.values().forEach(c -> c.setVisible(false));
            Component selectedView = detailViews.get(evt.getSelectedTab());
            if (selectedView != null) {
                selectedView.setVisible(true);
            }
        });

        VerticalLayout detailLayout = new VerticalLayout(tabs, request, response, headers);
        detailLayout.setSizeFull();
        detailLayout.setPadding(false);
        detailLayout.setSpacing(false);

        return detailLayout;
    }

    private String formatTimestamp(Instant timestamp) {
        if (timestamp == null) return "Unknown";
        LocalDateTime dateTime = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
        return dateTime.format(TIME_FORMATTER);
    }

    private void populateRequestDetails(Div requestDiv, HttpExchange exchange) {
        requestDiv.removeAll();

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);

        // Request line
        Span requestLine = new Span(String.format("%s %s",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getUri()));
        requestLine.getStyle().set("font-weight", "bold");
        layout.add(requestLine);

        // Remote address
        if (exchange.getRequest().getRemoteAddress() != null) {
            layout.add(new Span("Remote Address: " + exchange.getRequest().getRemoteAddress()));
        }

        requestDiv.add(layout);
    }

    private void populateResponseDetails(Div responseDiv, HttpExchange exchange) {
        responseDiv.removeAll();

        if (exchange.getResponse() == null) {
            responseDiv.add(new Span("No response available"));
            return;
        }

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);

        // Status
        Span statusLine = new Span("Status: " + exchange.getResponse().getStatus());
        statusLine.getStyle().set("font-weight", "bold");
        layout.add(statusLine);

        // Duration
        if (exchange.getTimeTaken() != null) {
            layout.add(new Span("Duration: " + exchange.getTimeTaken().toMillis() + "ms"));
        }

        responseDiv.add(layout);
    }

    private void populateHeadersDetails(Div headersDiv, HttpExchange exchange) {
        headersDiv.removeAll();

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        // Request headers
        if (!exchange.getRequest().getHeaders().isEmpty()) {
            Span requestHeadersTitle = new Span("Request Headers:");
            requestHeadersTitle.getStyle().set("font-weight", "bold");
            layout.add(requestHeadersTitle);

            Grid<Map.Entry<String, List<String>>> requestHeadersGrid = buildKeyValueGrid(
                    exchange.getRequest().getHeaders());
            requestHeadersGrid.setHeight("200px");
            layout.add(requestHeadersGrid);
        }

        // Response headers
        if (exchange.getResponse() != null && !exchange.getResponse().getHeaders().isEmpty()) {
            Span responseHeadersTitle = new Span("Response Headers:");
            responseHeadersTitle.getStyle().set("font-weight", "bold");
            layout.add(responseHeadersTitle);

            Grid<Map.Entry<String, List<String>>> responseHeadersGrid = buildKeyValueGrid(
                    exchange.getResponse().getHeaders());
            responseHeadersGrid.setHeight("200px");
            layout.add(responseHeadersGrid);
        }

        headersDiv.add(layout);
    }

    private Grid<Map.Entry<String, List<String>>> buildKeyValueGrid(Map<String, List<String>> headers) {
        Grid<Map.Entry<String, List<String>>> grid = new Grid<>();
        grid.addColumn(Map.Entry::getKey)
                .setHeader("Name")
                .setAutoWidth(true);
        grid.addColumn(entry -> String.join(", ", entry.getValue()))
                .setHeader("Value")
                .setFlexGrow(1);
        grid.setItems(headers.entrySet());
        grid.setAllRowsVisible(true);
        return grid;
    }
}
