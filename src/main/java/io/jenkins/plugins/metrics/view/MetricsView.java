package io.jenkins.plugins.metrics.view;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.export.ExportedBean;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.model.User;

import io.jenkins.plugins.datatables.DefaultAsyncTableContentProvider;
import io.jenkins.plugins.datatables.TableModel;
import io.jenkins.plugins.metrics.extension.MetricsProvider;
import io.jenkins.plugins.metrics.extension.MetricsProviderFactory;
import io.jenkins.plugins.metrics.model.ClassMetricsMeasurement;
import io.jenkins.plugins.metrics.model.MetricDefinition;
import io.jenkins.plugins.metrics.model.MetricDefinition.Scope;
import io.jenkins.plugins.metrics.model.MetricsMeasurement;

/**
 * Build view for displaying metrics.
 *
 * @author Inci Amin
 */
@ExportedBean
public class MetricsView extends DefaultAsyncTableContentProvider implements ModelObject {
    private final Run<?, ?> owner;
    private final List<ClassMetricsMeasurement> metricsMeasurements;
    private final List<MetricDefinition> supportedMetrics;
    private final DashboardLayoutService dashboardLayoutService = new DashboardLayoutService();

    /**
     * Create a new {@link MetricsView}.
     *
     * @param build
     *         the {@link Run} that is shown in the view
     */
    public MetricsView(final Run<?, ?> build) {
        super();

        this.owner = build;
        metricsMeasurements = MetricsProviderFactory.findAllFor(build).stream()
                .map(MetricsProvider::getMetricsMeasurements)
                .flatMap(List::stream)
                .filter(ClassMetricsMeasurement.class::isInstance)
                .collect(Collectors.groupingBy(MetricsMeasurement::getQualifiedClassName))
                .values().stream()
                .map(measurementsPerFile -> (ClassMetricsMeasurement) measurementsPerFile.stream()
                        .reduce(MetricsMeasurement::merge).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        supportedMetrics = MetricsProviderFactory.findAllAvailableMetricsFor(build)
                .stream()
                .filter(metricDefinition -> metricDefinition.isValidForScope(Scope.CLASS))
                .collect(Collectors.toList());
    }

    @Override
    public String getDisplayName() {
        return Messages.metrics();
    }

    /**
     * Returns the build as owner of this object.
     *
     * @return the owner
     */
    @SuppressWarnings("unused") // used by jelly view
    public final Run<?, ?> getOwner() {
        return owner;
    }

    /**
     * If a metric is a percentage metric.
     *
     * @param metricId
     *         the metric wanted
     *
     * @return true if it is a percentage metric
     */
    @JavaScriptMethod
    public boolean isPercentageMetric(final String metricId) {
        return supportedMetrics.stream()
                .filter(m -> m.getId().equals(metricId))
                .map(MetricDefinition::getKindOfValue)
                .filter(Objects::nonNull)
                .map(Class::getSimpleName)
                .anyMatch("PercentageMetric"::equals);
    }

    /**
     * Get the name and value of a metric with int value.
     *
     * @param metricId
     *         the metric wanted
     *
     * @return name and value of the metric
     */
    @JavaScriptMethod
    public Map<String, Object> getNameAndValueForIntValues(final String metricId) {
        int value = metricsMeasurements.stream()
                .map(m -> m.getMetric(metricId).orElse(0.0))
                .mapToInt(Number::intValue)
                .filter(Double::isFinite)
                .sum();

        String metricDisplayName = getDisplayNameOfMetric(metricId);

        Map<String, Object> result = new HashMap<>();
        result.put("name", metricDisplayName);
        result.put("value", value);
        result.put("isPercentage", false);

        return result;
    }

    /**
     * Get the name and value of a metric with percentage value.
     *
     * @param metricId
     *         the metric wanted
     *
     * @return name and value of the metric
     */
    @JavaScriptMethod
    public Map<String, Object> getNameAndValueForPercentageValues(final String metricId) {
        double value = metricsMeasurements.stream()
                .map(m -> m.getMetric(metricId).orElse(null))
                .filter(Objects::nonNull)
                .map(Number::doubleValue)
                .filter(Double::isFinite)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        String metricDisplayName = getDisplayNameOfMetric(metricId);

        Map<String, Object> result = new HashMap<>();
        result.put("name", metricDisplayName);
        result.put("value", value);
        result.put("isPercentage", true);

        return result;
    }

    /**
     * Get the name and value of a metric.
     *
     * @param metricId
     *         the metric wanted
     *
     * @return name and value of the metric
     */
    @JavaScriptMethod
    @SuppressWarnings("unused") // used by jelly view
    public Map<String, Object> getNameAndValue(final String metricId) {
        Map<String, Object> result = new HashMap<>();
        if (isPercentageMetric(metricId)) {
            return getNameAndValueForPercentageValues(metricId);
        }
        return getNameAndValueForIntValues(metricId);
    }

    /**
     * Get the name and value of a metric for the last builds.
     *
     * @param metricId
     *         the metric wanted
     * @param numberOfBuilds
     *         the amount of builds wanted
     *
     * @return name and value of the metric
     */
    @JavaScriptMethod
    @SuppressWarnings("unused") // used by jelly view
    public Map<String, Object> getNameAndValuesForLastBuilds(final String metricId, final int numberOfBuilds) {
        String metricDisplayName = getDisplayNameOfMetric(metricId);

        boolean isPercentage = isPercentageMetric(metricId);

        List<Map<String, Object>> values = new ArrayList<>();

        Run<?, ?> current = owner;
        for (int i = 0; i < numberOfBuilds && current != null; i++) {
            double value = getValueForRun(metricId, current, isPercentage);

            Map<String, Object> point = new HashMap<>();
            point.put("build", current.getNumber());
            point.put("value", value);
            values.add(point);

            current = current.getPreviousBuild();
        }

        java.util.Collections.reverse(values);

        Map<String, Object> result = new HashMap<>();
        result.put("name", metricDisplayName);
        result.put("values", values);
        result.put("isPercentage", isPercentage);
        return result;
    }

    private double getValueForRun(final String metricId, final Run<?, ?> run, final boolean isPercentage) {
        List<ClassMetricsMeasurement> runMeasurements = MetricsProviderFactory.findAllFor(run).stream()
                .map(MetricsProvider::getMetricsMeasurements)
                .flatMap(List::stream)
                .filter(ClassMetricsMeasurement.class::isInstance)
                .collect(Collectors.groupingBy(MetricsMeasurement::getQualifiedClassName))
                .values().stream()
                .map(measurementsPerFile -> (ClassMetricsMeasurement) measurementsPerFile.stream()
                        .reduce(MetricsMeasurement::merge).orElse(null))
                .filter(Objects::nonNull)
                .toList();

        if (isPercentage) {
            return runMeasurements.stream()
                    .map(m -> m.getMetric(metricId).orElse(null))
                    .filter(Objects::nonNull)
                    .map(Number::doubleValue)
                    .filter(Double::isFinite)
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
        }
        else {
            return runMeasurements.stream()
                    .map(m -> m.getMetric(metricId).orElse(0.0))
                    .mapToInt(Number::intValue)
                    .sum();
        }
    }

    /**
     * Returns the dashboard layout for a user or a template JSON.
     *
     * @return layout as JSON
     */
    @JavaScriptMethod
    @SuppressWarnings("unused") // used by jelly view
    public JSONObject getDashboardLayout() {
        User current = User.current();
        String json = dashboardLayoutService.getEffectiveLayoutJson(current);

        try {
            return JSONObject.fromObject(json);
        }
        catch (NullPointerException e) {
            return JSONObject.fromObject(dashboardLayoutService.getEffectiveLayoutJson(null));
        }
    }

    /**
     * Saves given dashboard layout for a user.
     *
     * @param layout
     *         the given layout
     *
     * @return ok when saved successfully
     */
    @JavaScriptMethod
    @SuppressWarnings("unused") // used by jelly view
    public JSONObject saveDashboardLayout(final JSONObject layout) {
        JSONObject result = new JSONObject();

        try {
            User current = User.current();
            if (layout == null) {
                throw new IllegalArgumentException("layout is null");
            }

            dashboardLayoutService.saveLayoutJson(current, layout.toString());

            result.put("ok", true);
        }
        catch (IOException | IllegalArgumentException | IllegalStateException e) {
            result.put("ok", false);
        }

        return result;
    }

    /**
     * Get the name and id of all available metrics.
     *
     * @return name and id of all metrics
     */
    @SuppressWarnings("unused")
    public String getMetricDropdownOptionsJSON() {
        return toJson(
                supportedMetrics.stream()
                        .map(m -> Map.of(
                                "id", m.getId(),
                                "label", m.getDisplayName()
                        ))
                        .toList()
        );
    }

    private String toJson(final Object object) {
        var facade = new JacksonFacade();
        return facade.toJson(object);
    }

    /**
     * Get a tree consisting of {@link MetricsTreeNode}s for a specific metric.
     *
     * @param metricId
     *         the given metric
     *
     * @return the hierarchy as a list
     */
    @JavaScriptMethod
    @SuppressWarnings("unused") // used by jelly view
    public List<Map<String, Object>> getPackageClassHierarchy(final String metricId) {
        TreeMap<String, TreeMap<String, Double>> grouped = new TreeMap<>();

        for (var mm : metricsMeasurements) {
            Number n = mm.getMetric(metricId).orElse(null);
            if (n == null) {
                continue;
            }

            double value = n.doubleValue();
            if (!Double.isFinite(value)) {
                continue;
            }

            String pkg = safeString(mm.getPackageName());
            String clsSimple = toSimpleName(safeString(mm.getClassName()));

            grouped.computeIfAbsent(pkg, k -> new TreeMap<>())
                    .merge(clsSimple, value, Double::sum);
        }

        List<Map<String, Object>> packages = new ArrayList<>();

        for (var pkgEntry : grouped.entrySet()) {
            Map<String, Object> pkgNode = new HashMap<>();
            pkgNode.put("name", pkgEntry.getKey());

            List<Map<String, Object>> children = new ArrayList<>();
            for (var clsEntry : pkgEntry.getValue().entrySet()) {
                Map<String, Object> clsNode = new HashMap<>();
                clsNode.put("name", clsEntry.getKey());
                clsNode.put("value", roundToTwoDecimals(clsEntry.getValue()));
                children.add(clsNode);
            }

            pkgNode.put("children", children);
            packages.add(pkgNode);
        }

        return packages;
    }

    private static String safeString(final String s) {
        return (s == null || s.isBlank()) ? "(unknown)" : s;
    }

    private static String toSimpleName(final String fqcn) {
        if (fqcn == null || fqcn.isBlank()) {
            return "(unknown)";
        }
        int idx = fqcn.lastIndexOf('.');
        return idx >= 0 ? fqcn.substring(idx + 1) : fqcn;
    }

    private static double roundToTwoDecimals(final double value) {
        double factor = Math.pow(10, 2);
        return Math.round(value * factor) / factor;
    }

    /**
     * Get the display name of a given metric.
     *
     * @param metricId
     *         the given metric
     *
     * @return the display name as a String
     */
    @JavaScriptMethod
    public String getDisplayNameOfMetric(final String metricId) {
        return supportedMetrics.stream()
                .filter(m -> m.getId().equals(metricId))
                .map(MetricDefinition::getDisplayName)
                .findFirst()
                .orElse(metricId);
    }

    /**
     * Get the table model for the metrics details table.
     *
     * @param id
     *         the id of the table to retrieve
     *
     * @return the {@link MetricsTableModel} containing all metrics
     */
    @Override
    public TableModel getTableModel(final String id) {
        return new MetricsTableModel(id, supportedMetrics, metricsMeasurements);
    }
}
