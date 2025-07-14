package org.vaadin.kitchensink.views.shared;

import org.cyclonedx.model.Bom;
import org.vaadin.lineawesome.LineAwesomeIcon;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.vaadin.kitchensink.service.BillOfMaterialsService;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import org.vaadin.kitchensink.service.BillOfMaterialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

/**
 * View for displaying project dependencies in a tree structure.
 * This view fetches the bill of materials (BOM) and displays the dependencies
 * in a hierarchical format, allowing users to filter and explore them.
 */
@AnonymousAllowed
@PageTitle("Dependencies")
@Route(value = "dependencies")
@Menu(order = 17, icon = LineAwesomeIconUrl.PROJECT_DIAGRAM_SOLID)
public class DependenciesView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(DependenciesView.class);

    private static final String UNKNOWN = "Unknown";

    /**
     * Data class representing a dependency in the BOM.
     */
    public static class DependencyInfo {
        private final String name;
        private final String version;
        private final String type;
        private final String scope;

        public DependencyInfo(String name, String version, String type, String scope) {
            this.name = name;
            this.version = version;
            this.type = type;
            this.scope = scope;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public String getType() {
            return type;
        }

        public String getScope() {
            return scope;
        }
    }

    /**
     * Constructs the DependenciesView and initializes it with the BOM data.
     *
     * @param billOfMaterialsService
     *            Service to fetch the BOM.
     */
    public DependenciesView(BillOfMaterialsService billOfMaterialsService) {
        setSpacing(true);
        setPadding(true);
        setSizeFull();

        try {
            Bom bom = billOfMaterialsService.getBom();
            initializeView(bom);
        } catch (BillOfMaterialsException e) {
            handleError(e);
        }
    }

    private void initializeView(Bom bom) {
        if (bom.getComponents() == null || bom.getComponents().isEmpty()) {
            add(new Span("No dependencies found in the BOM."));
            return;
        }

        TextField filterField = createFilterField();
        TreeGrid<DependencyInfo> dependencyTree = createDependencyTree();
        TreeData<DependencyInfo> treeData = buildTreeData(bom);

        setupTreeGrid(dependencyTree, treeData);
        setupFilterFunctionality(filterField, dependencyTree, treeData);

        int totalDependencies = calculateTotalDependencies(bom);

        add(new Span("Total dependencies: " + totalDependencies));
        add(filterField);
        add(dependencyTree);
    }

    private TextField createFilterField() {
        TextField filterField = new TextField();
        filterField.setClearButtonVisible(true);
        filterField.setPlaceholder("Filter dependencies...");
        filterField.setPrefixComponent(LineAwesomeIcon.SEARCH_SOLID.create());
        filterField.setValueChangeMode(ValueChangeMode.LAZY);
        filterField.setWidthFull();
        return filterField;
    }

    private TreeGrid<DependencyInfo> createDependencyTree() {
        TreeGrid<DependencyInfo> dependencyTree = new TreeGrid<>(DependencyInfo.class, false);
        dependencyTree.addHierarchyColumn(DependencyInfo::getName).setHeader("Name").setAutoWidth(true);
        dependencyTree.addColumn(DependencyInfo::getVersion).setHeader("Version").setAutoWidth(true);
        dependencyTree.addColumn(DependencyInfo::getType).setHeader("Type").setAutoWidth(true);
        dependencyTree.addColumn(DependencyInfo::getScope).setHeader("Scope").setAutoWidth(true);
        dependencyTree.setSizeFull();
        return dependencyTree;
    }

    private TreeData<DependencyInfo> buildTreeData(Bom bom) {
        TreeData<DependencyInfo> treeData = new TreeData<>();
        Map<String, Component> bomRefToComponent = createComponentReferenceMap(bom);

        List<Dependency> dependencies = bom.getDependencies();
        if (dependencies != null && !dependencies.isEmpty()) {
            buildTreeFromDependencies(treeData, dependencies, bomRefToComponent);
        } else {
            buildTreeFromComponents(treeData, bom.getComponents());
        }

        return treeData;
    }

    private Map<String, Component> createComponentReferenceMap(Bom bom) {
        Map<String, Component> bomRefToComponent = new HashMap<>();
        for (Component component : bom.getComponents()) {
            String bomRef = component.getBomRef();
            if (bomRef != null) {
                bomRefToComponent.put(bomRef, component);
            }
        }
        return bomRefToComponent;
    }

    private void buildTreeFromDependencies(TreeData<DependencyInfo> treeData, List<Dependency> dependencies,
                                         Map<String, Component> bomRefToComponent) {
        // Sort dependencies by the name of their referenced component
        dependencies.sort((d1, d2) -> compareDependencyNames(d1, d2, bomRefToComponent));

        for (Dependency dependency : dependencies) {
            addDependencyToTree(treeData, dependency, bomRefToComponent);
        }
    }

    private int compareDependencyNames(Dependency d1, Dependency d2, Map<String, Component> bomRefToComponent) {
        Component c1 = bomRefToComponent.get(d1.getRef());
        Component c2 = bomRefToComponent.get(d2.getRef());
        String name1 = getComponentName(c1);
        String name2 = getComponentName(c2);
        return name1.compareToIgnoreCase(name2);
    }

    private void addDependencyToTree(TreeData<DependencyInfo> treeData, Dependency dependency,
                                   Map<String, Component> bomRefToComponent) {
        Component parentComponent = bomRefToComponent.get(dependency.getRef());
        if (parentComponent == null) return;

        DependencyInfo parentInfo = createDependencyInfo(parentComponent);
        treeData.addItem(null, parentInfo);

        addChildDependencies(treeData, parentInfo, dependency.getDependencies(), bomRefToComponent);
    }

    private void addChildDependencies(TreeData<DependencyInfo> treeData, DependencyInfo parentInfo,
                                    List<Dependency> childDependencies, Map<String, Component> bomRefToComponent) {
        if (childDependencies == null) return;

        // Sort child dependencies by name
        childDependencies.sort((d1, d2) -> compareDependencyNames(d1, d2, bomRefToComponent));

        for (Dependency childDep : childDependencies) {
            Component childComponent = bomRefToComponent.get(childDep.getRef());
            if (childComponent != null) {
                DependencyInfo childInfo = createDependencyInfo(childComponent);
                treeData.addItem(parentInfo, childInfo);
            }
        }
    }

    private void buildTreeFromComponents(TreeData<DependencyInfo> treeData, List<Component> components) {
        for (Component component : components) {
            DependencyInfo rootDep = createDependencyInfo(component);
            treeData.addItem(null, rootDep);

            // Add child dependencies if they exist
            if (component.getComponents() != null) {
                for (Component childComponent : component.getComponents()) {
                    DependencyInfo childDep = createDependencyInfo(childComponent);
                    treeData.addItem(rootDep, childDep);
                }
            }
        }
    }

    private DependencyInfo createDependencyInfo(Component component) {
        return new DependencyInfo(
            getComponentName(component),
            getComponentVersion(component),
            getComponentType(component),
            getComponentScope(component)
        );
    }

    private String getComponentName(Component component) {
        return component != null && component.getName() != null ? component.getName() : UNKNOWN;
    }

    private String getComponentVersion(Component component) {
        return component != null && component.getVersion() != null ? component.getVersion() : UNKNOWN;
    }

    private String getComponentType(Component component) {
        return component != null && component.getType() != null ? component.getType().getTypeName() : UNKNOWN;
    }

    private String getComponentScope(Component component) {
        return component != null && component.getScope() != null ? component.getScope().getScopeName() : UNKNOWN;
    }

    private void setupTreeGrid(TreeGrid<DependencyInfo> dependencyTree, TreeData<DependencyInfo> treeData) {
        dependencyTree.setDataProvider(new TreeDataProvider<>(treeData));
        dependencyTree.setWidthFull();
    }

    private void setupFilterFunctionality(TextField filterField, TreeGrid<DependencyInfo> dependencyTree,
                                        TreeData<DependencyInfo> originalTreeData) {
        filterField.addValueChangeListener(event -> {
            String filterText = event.getValue();
            if (filterText == null || filterText.trim().isEmpty()) {
                dependencyTree.setDataProvider(new TreeDataProvider<>(originalTreeData));
            } else {
                TreeData<DependencyInfo> filteredData = createFilteredTreeData(originalTreeData, filterText.toLowerCase());
                dependencyTree.setDataProvider(new TreeDataProvider<>(filteredData));
            }
        });
    }

    private TreeData<DependencyInfo> createFilteredTreeData(TreeData<DependencyInfo> originalTreeData, String lowerCaseFilter) {
        TreeData<DependencyInfo> newFilteredData = new TreeData<>();

        for (DependencyInfo rootItem : originalTreeData.getRootItems()) {
            boolean parentMatches = rootItem.getName().toLowerCase().contains(lowerCaseFilter);
            boolean hasMatchingChild = hasMatchingChildItem(originalTreeData, rootItem, lowerCaseFilter);

            if (parentMatches || hasMatchingChild) {
                newFilteredData.addItem(null, rootItem);
                addFilteredChildren(originalTreeData, newFilteredData, rootItem, lowerCaseFilter, parentMatches);
            }
        }

        return newFilteredData;
    }

    private boolean hasMatchingChildItem(TreeData<DependencyInfo> treeData, DependencyInfo rootItem, String filter) {
        return treeData.getChildren(rootItem).stream()
            .anyMatch(child -> child.getName().toLowerCase().contains(filter));
    }

    private void addFilteredChildren(TreeData<DependencyInfo> originalTreeData, TreeData<DependencyInfo> filteredData,
                                   DependencyInfo rootItem, String filter, boolean parentMatches) {
        for (DependencyInfo childItem : originalTreeData.getChildren(rootItem)) {
            if (parentMatches || childItem.getName().toLowerCase().contains(filter)) {
                filteredData.addItem(rootItem, childItem);
            }
        }
    }

    private int calculateTotalDependencies(Bom bom) {
        List<Dependency> dependencies = bom.getDependencies();
        return dependencies != null ? dependencies.size() : bom.getComponents().size();
    }

    private void handleError(BillOfMaterialsException e) {
        logger.error("Failed to load bill of materials", e);

        H3 errorHeader = new H3("Error loading dependencies");
        errorHeader.getStyle().set("color", "red");
        add(errorHeader);

        Span errorMessage = new Span("Unable to load project dependencies: " + e.getMessage());
        errorMessage.getStyle().set("color", "red");
        add(errorMessage);
    }
}
