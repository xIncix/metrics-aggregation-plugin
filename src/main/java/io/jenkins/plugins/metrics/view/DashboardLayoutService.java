package io.jenkins.plugins.metrics.view;

import java.io.IOException;

import hudson.model.User;

/**
 * Service to store and get the JSON for the Muuri Dashboard.
 */
public class DashboardLayoutService {
    private static final int MAX_JSON_CHARS = 200_000;

    private static final String DEFAULT_LAYOUT_JSON = """
            {
              "version": 1,
              "widgets": [
                {
                  "id": "1",
                  "chartType": "half-doughnut",
                  "metricId": "WARNING_NORMAL"
                },
                {
                  "id": "2",
                  "chartType": "half-doughnut",
                  "metricId": "AUTHORS"
                },
                {
                  "id": "3",
                  "chartType": "bar",
                  "metricId": "LINE_COVERAGE"
                },
                {
                  "id": "4",
                  "chartType": "tree-map",
                  "metricId": "LOC"
                }
              ]
            }
            """;

    /**
     * Returns the layout JSON for current user. If there is no user or saved JSON then a template will be returned.
     *
     * @param user
     *         the user
     *
     * @return
     *      the layout JSON
     */
    public String getEffectiveLayoutJson(final User user) {
        if (user == null) {
            return DEFAULT_LAYOUT_JSON;
        }

        DashboardLayoutProperty prop = user.getProperty(DashboardLayoutProperty.class);
        if (prop == null) {
            return DEFAULT_LAYOUT_JSON;
        }

        String stored = prop.getLayoutJson();
        if (stored == null || stored.isBlank()) {
            return DEFAULT_LAYOUT_JSON;
        }

        return stored;
    }

    /**
     * Saves given layout JSON for a user.
     *
     * @param user
     *         the user
     * @param layoutJson
     *         the layout
     */
    public void saveLayoutJson(final User user, final String layoutJson) throws IOException {
        if (user == null) {
            throw new IllegalStateException("Anonymous users cannot save a dashboard layout.");
        }
        if (layoutJson == null || layoutJson.isBlank()) {
            throw new IllegalArgumentException("layoutJson is empty.");
        }
        if (layoutJson.length() > MAX_JSON_CHARS) {
            throw new IllegalArgumentException("layoutJson too large.");
        }

        DashboardLayoutProperty prop = user.getProperty(DashboardLayoutProperty.class);
        if (prop == null) {
            prop = new DashboardLayoutProperty();
            user.addProperty(prop);
        }

        prop.setLayoutJson(layoutJson);
        user.save();
    }
}
