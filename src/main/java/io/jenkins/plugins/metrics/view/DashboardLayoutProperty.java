package io.jenkins.plugins.metrics.view;

import hudson.model.UserProperty;

/**
 * Store the dashboard layout JSON in the User Property.
 */
public class DashboardLayoutProperty extends UserProperty {
    private String layoutJson;

    public String getLayoutJson() {
        return layoutJson;
    }

    public void setLayoutJson(final String layoutJson) {
        this.layoutJson = layoutJson;
    }
}
