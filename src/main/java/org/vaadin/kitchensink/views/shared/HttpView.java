package org.vaadin.kitchensink.views.shared;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.vaadin.lineawesome.LineAwesomeIconUrl;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;

/**
 * View that displays HTTP request and response information, including headers,
 * cookies, and other details.
 * <p>
 * This view is accessible to anonymous users and provides detailed insights into
 * the current HTTP request and response.
 * </p>
 */
@AnonymousAllowed
@PageTitle("HTTP")
@Route(value = "http")
@Menu(order = 10, icon = LineAwesomeIconUrl.EXCHANGE_ALT_SOLID)
public class HttpView extends VerticalLayout {

    private static final String STATUS = "Status";

    /**
     * Data class for HTTP header information.
     */
    public static class HeaderInfo {
        private final String name;
        private final String value;

        public HeaderInfo(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Data class for general HTTP request/response information.
     */
    public static class InfoItem {
        private final String property;
        private final String value;

        public InfoItem(String property, String value) {
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

    /**
     * Data class for cookie information.
     */
    public static class CookieInfo {
        private final String name;
        private final String value;
        private final String domain;
        private final String path;
        private final int maxAge;
        private final boolean secure;
        private final boolean httpOnly;

        public CookieInfo(String name, String value, String domain, String path, int maxAge, boolean secure, boolean httpOnly) {
            this.name = name;
            this.value = value;
            this.domain = domain;
            this.path = path;
            this.maxAge = maxAge;
            this.secure = secure;
            this.httpOnly = httpOnly;
        }

        public String getName() { return name; }
        public String getValue() { return value; }
        public String getDomain() { return domain; }
        public String getPath() { return path; }
        public int getMaxAge() { return maxAge; }
        public boolean isSecure() { return secure; }
        public boolean isHttpOnly() { return httpOnly; }
    }

    /**
     * Constructor for the HttpView.
     * Initializes the layout, creates grids for request and response information,
     * and adds them to an accordion for better organization.
     */
    public HttpView() {
        setSpacing(true);
        setPadding(true);

        Accordion accordion = new Accordion();
        accordion.setWidthFull();

        VaadinRequest req = VaadinService.getCurrentRequest();
        VaadinResponse res = VaadinService.getCurrentResponse();

        addRequestInformation(accordion, req);
        addRequestHeaders(accordion, req);
        addRequestCookies(accordion, req);
        addResponseInformation(accordion, res);
        addResponseHeaders(accordion, res);
        addResponseCookies(accordion, res);

        add(accordion);
    }

    private void addRequestInformation(Accordion accordion, VaadinRequest req) {
        Grid<InfoItem> requestInfoGrid = createInfoGrid();
        List<InfoItem> requestInfo = buildRequestInfo(req);
        requestInfoGrid.setItems(requestInfo);
        accordion.add("Request Information", requestInfoGrid);
    }

    private List<InfoItem> buildRequestInfo(VaadinRequest req) {
        List<InfoItem> requestInfo = new ArrayList<>();

        if (req == null) {
            requestInfo.add(new InfoItem(STATUS, "No VaadinRequest available"));
            return requestInfo;
        }

        requestInfo.add(new InfoItem("Request Method", req.getMethod()));
        requestInfo.add(new InfoItem("Request URI", req.getPathInfo()));
        requestInfo.add(new InfoItem("Context Path", req.getContextPath()));
        requestInfo.add(new InfoItem("Remote Addr", req.getRemoteAddr()));
        requestInfo.add(new InfoItem("Remote Host", req.getRemoteHost()));

        addHttpServletRequestInfo(requestInfo, req);
        return requestInfo;
    }

    private void addHttpServletRequestInfo(List<InfoItem> requestInfo, VaadinRequest req) {
        if (!(req instanceof VaadinServletRequest vaadinServletRequest)) {
            return;
        }

        HttpServletRequest httpReq = vaadinServletRequest.getHttpServletRequest();
        requestInfo.add(new InfoItem("Protocol", httpReq.getProtocol()));
        requestInfo.add(new InfoItem("Scheme", httpReq.getScheme()));
        requestInfo.add(new InfoItem("Server Name", httpReq.getServerName()));
        requestInfo.add(new InfoItem("Server Port", String.valueOf(httpReq.getServerPort())));
        requestInfo.add(new InfoItem("Query String", httpReq.getQueryString()));
        requestInfo.add(new InfoItem("Content Type", httpReq.getContentType()));
        requestInfo.add(new InfoItem("Content Length", String.valueOf(httpReq.getContentLengthLong())));
    }

    private void addRequestHeaders(Accordion accordion, VaadinRequest req) {
        if (req == null) {
            return;
        }

        Grid<HeaderInfo> requestHeadersGrid = createHeaderGrid();
        List<HeaderInfo> requestHeaders = buildRequestHeaders(req);
        requestHeadersGrid.setItems(requestHeaders);
        accordion.add("Request Headers", requestHeadersGrid);
    }

    private List<HeaderInfo> buildRequestHeaders(VaadinRequest req) {
        List<HeaderInfo> requestHeaders = new ArrayList<>();
        req.getHeaderNames().asIterator().forEachRemaining(name ->
            requestHeaders.add(new HeaderInfo(name, req.getHeader(name))));

        requestHeaders.sort(Comparator.comparing(HeaderInfo::getName));
        return requestHeaders;
    }

    private void addRequestCookies(Accordion accordion, VaadinRequest req) {
        if (!(req instanceof VaadinServletRequest vaadinServletRequest)) {
            return;
        }

        HttpServletRequest httpReq = vaadinServletRequest.getHttpServletRequest();
        Cookie[] cookies = httpReq.getCookies();

        if (cookies == null || cookies.length == 0) {
            addNoCookiesSection(accordion, "Request Cookies (0)", "No cookies found in the request");
            return;
        }

        Grid<CookieInfo> cookiesGrid = createCookieGrid();
        List<CookieInfo> cookieList = buildCookieList(cookies);
        cookiesGrid.setItems(cookieList);
        accordion.add("Request Cookies (" + cookies.length + ")", cookiesGrid);
    }

    private List<CookieInfo> buildCookieList(Cookie[] cookies) {
        List<CookieInfo> cookieList = new ArrayList<>();
        for (Cookie cookie : cookies) {
            cookieList.add(new CookieInfo(
                cookie.getName(),
                cookie.getValue(),
                cookie.getDomain(),
                cookie.getPath(),
                cookie.getMaxAge(),
                cookie.getSecure(),
                cookie.isHttpOnly()
            ));
        }
        cookieList.sort(Comparator.comparing(CookieInfo::getName));
        return cookieList;
    }

    private void addResponseInformation(Accordion accordion, VaadinResponse res) {
        Grid<InfoItem> responseInfoGrid = createInfoGrid();
        List<InfoItem> responseInfo = buildResponseInfo(res);
        responseInfoGrid.setItems(responseInfo);
        accordion.add("Response Information", responseInfoGrid);
    }

    private List<InfoItem> buildResponseInfo(VaadinResponse res) {
        List<InfoItem> responseInfo = new ArrayList<>();

        if (res == null) {
            responseInfo.add(new InfoItem(STATUS, "No VaadinResponse available"));
            return responseInfo;
        }

        responseInfo.add(new InfoItem(STATUS, "Response object available"));
        addHttpServletResponseInfo(responseInfo, res);
        return responseInfo;
    }

    private void addHttpServletResponseInfo(List<InfoItem> responseInfo, VaadinResponse res) {
        if (!(res instanceof VaadinServletResponse vaadinServletResponse)) {
            return;
        }

        HttpServletResponse httpRes = vaadinServletResponse.getHttpServletResponse();
        responseInfo.add(new InfoItem("Response Code", String.valueOf(httpRes.getStatus())));
        responseInfo.add(new InfoItem("Character Encoding", httpRes.getCharacterEncoding()));
        responseInfo.add(new InfoItem("Content Type", httpRes.getContentType()));
        responseInfo.add(new InfoItem("Buffer Size", String.valueOf(httpRes.getBufferSize())));
        responseInfo.add(new InfoItem("Is Committed", String.valueOf(httpRes.isCommitted())));
        responseInfo.add(new InfoItem("Locale", String.valueOf(httpRes.getLocale())));
    }

    private void addResponseHeaders(Accordion accordion, VaadinResponse res) {
        if (!(res instanceof VaadinServletResponse vaadinServletResponse)) {
            return;
        }

        HttpServletResponse httpRes = vaadinServletResponse.getHttpServletResponse();
        Grid<HeaderInfo> responseHeadersGrid = createHeaderGrid();
        List<HeaderInfo> responseHeaders = buildResponseHeaders(httpRes);
        responseHeadersGrid.setItems(responseHeaders);
        accordion.add("Response Headers", responseHeadersGrid);
    }

    private List<HeaderInfo> buildResponseHeaders(HttpServletResponse httpRes) {
        List<HeaderInfo> responseHeaders = new ArrayList<>();
        httpRes.getHeaderNames().forEach(name ->
            responseHeaders.add(new HeaderInfo(name, httpRes.getHeader(name))));

        responseHeaders.sort(Comparator.comparing(HeaderInfo::getName));
        return responseHeaders;
    }

    private void addResponseCookies(Accordion accordion, VaadinResponse res) {
        if (!(res instanceof VaadinServletResponse vaadinServletResponse)) {
            return;
        }

        HttpServletResponse httpRes = vaadinServletResponse.getHttpServletResponse();
        List<String> setCookieHeaders = new ArrayList<>(httpRes.getHeaders("Set-Cookie"));

        if (setCookieHeaders.isEmpty()) {
            addNoCookiesSection(accordion, "Response Cookies (0)", "No cookies being set in the response");
            return;
        }

        Grid<HeaderInfo> responseCookiesGrid = createHeaderGrid();
        List<HeaderInfo> responseCookies = buildResponseCookieHeaders(setCookieHeaders);
        responseCookiesGrid.setItems(responseCookies);
        accordion.add("Response Cookies (" + setCookieHeaders.size() + ")", responseCookiesGrid);
    }

    private List<HeaderInfo> buildResponseCookieHeaders(List<String> setCookieHeaders) {
        List<HeaderInfo> responseCookies = new ArrayList<>();
        for (int i = 0; i < setCookieHeaders.size(); i++) {
            String cookieHeader = setCookieHeaders.get(i);
            String cookieName = extractCookieName(cookieHeader, i);
            responseCookies.add(new HeaderInfo(cookieName, cookieHeader));
        }
        return responseCookies;
    }

    private String extractCookieName(String cookieHeader, int index) {
        if (cookieHeader.contains("=")) {
            return cookieHeader.substring(0, cookieHeader.indexOf("="));
        }
        return "Cookie " + (index + 1);
    }

    private Grid<InfoItem> createInfoGrid() {
        Grid<InfoItem> grid = new Grid<>(InfoItem.class, false);
        grid.addColumn(InfoItem::getProperty).setHeader("Property").setAutoWidth(true);
        grid.addColumn(InfoItem::getValue).setHeader("Value").setAutoWidth(true);
        grid.setAllRowsVisible(true);
        return grid;
    }

    private Grid<HeaderInfo> createHeaderGrid() {
        Grid<HeaderInfo> grid = new Grid<>(HeaderInfo.class, false);
        grid.addColumn(HeaderInfo::getName).setHeader("Header Name").setAutoWidth(true);
        grid.addColumn(HeaderInfo::getValue).setHeader("Header Value").setAutoWidth(true);
        grid.setAllRowsVisible(true);
        return grid;
    }

    private Grid<CookieInfo> createCookieGrid() {
        Grid<CookieInfo> grid = new Grid<>(CookieInfo.class, false);
        grid.addColumn(CookieInfo::getName).setHeader("Name").setAutoWidth(true).setSortable(true);
        grid.addColumn(CookieInfo::getValue).setHeader("Value").setAutoWidth(true);
        grid.addColumn(CookieInfo::getDomain).setHeader("Domain").setAutoWidth(true);
        grid.addColumn(CookieInfo::getPath).setHeader("Path").setAutoWidth(true);
        grid.addColumn(CookieInfo::getMaxAge).setHeader("Max Age").setAutoWidth(true);
        grid.addColumn(CookieInfo::isSecure).setHeader("Secure").setAutoWidth(true);
        grid.addColumn(CookieInfo::isHttpOnly).setHeader("HTTP Only").setAutoWidth(true);
        grid.setAllRowsVisible(true);
        return grid;
    }

    private void addNoCookiesSection(Accordion accordion, String title, String message) {
        VerticalLayout noCookiesLayout = new VerticalLayout();
        noCookiesLayout.setPadding(false);
        noCookiesLayout.add(new Span(message));
        accordion.add(title, noCookiesLayout);
    }
}
