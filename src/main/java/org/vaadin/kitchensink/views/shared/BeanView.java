package org.vaadin.kitchensink.views.shared;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.vaadin.lineawesome.LineAwesomeIcon;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.component.html.Span;

@AnonymousAllowed
@PageTitle("Spring Beans")
@Route(value = "beans")
@Menu(order = 30, icon = LineAwesomeIconUrl.CAPSULES_SOLID)
public class BeanView extends VerticalLayout {

    public static class BeanInfo {
        private final String name;
        private final String className;

        public BeanInfo(String name, String className) {
            this.name = name;
            this.className = className;
        }

        public String getName() {
            return name;
        }

        public String getClassName() {
            return className;
        }
    }

    public BeanView(ApplicationContext applicationContext) {
        setSpacing(true);
        setPadding(true);
        setSizeFull();

        // Create filter field
        TextField filterField = new TextField();
        filterField.setPrefixComponent(LineAwesomeIcon.SEARCH_SOLID.create());
        filterField.setPlaceholder("Filter beans...");
        filterField.setClearButtonVisible(true);
        filterField.setValueChangeMode(ValueChangeMode.LAZY);
        filterField.setWidthFull();

        // Get all beans from application context
        List<BeanInfo> beans = new ArrayList<>();
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            try {
                Object bean = applicationContext.getBean(beanName);
                String className = bean.getClass().getName();
                beans.add(new BeanInfo(beanName, className));
            } catch (Exception e) {
                // Skip beans that cannot be instantiated
                beans.add(new BeanInfo(beanName, "Unable to determine class"));
            }
        }

        // Sort beans by name
        beans.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        // Add total bean count
        add(new Span("Total beans: " + beans.size()));

        // Create grid to display beans
        Grid<BeanInfo> grid = new Grid<>();
        grid.addColumn(BeanInfo::getName)
            .setHeader("Bean Name")
            .setSortable(true)
            .setResizable(true);

        grid.addColumn(BeanInfo::getClassName)
            .setHeader("Class Name")
            .setSortable(true)
            .setResizable(true);

        grid.setSizeFull();

        grid.setItems(beans);

        // Add filter functionality
        filterField.addValueChangeListener(event -> {
            String filterText = event.getValue().toLowerCase();
            if (filterText.trim().isEmpty()) {
                grid.setItems(beans);
            } else {
                List<BeanInfo> filteredBeans = beans.stream()
                    .filter(bean ->
                        bean.getName().toLowerCase().contains(filterText) ||
                        bean.getClassName().toLowerCase().contains(filterText))
                    .toList();
                grid.setItems(filteredBeans);
            }
        });

        add(filterField, grid);
    }
}
