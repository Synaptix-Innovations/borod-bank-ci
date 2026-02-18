package com.vlad774.borodbank.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Reads feature flags from application properties / environment variables.
 *
 * In K8s: values come from the feature-flags ConfigMap (borod-bank-cd/app/feature-flags-configmap.yml).
 * Toggle a flag: edit ConfigMap → apply → restart pod (kubectl rollout restart rollout/borod-bank).
 * No image rebuild required.
 *
 * Local dev: override in application.properties or as env vars.
 */
@Service
public class FeatureFlagService {

    // Controls visibility of the money transfer form in the dashboard UI.
    // Env var: FEATURES_TRANSFER_ENABLED
    @Value("${features.transfer-enabled:true}")
    private boolean transferEnabled;

    // Shows a maintenance banner at the top of the dashboard.
    // Env var: FEATURES_MAINTENANCE_MODE
    @Value("${features.maintenance-mode:false}")
    private boolean maintenanceMode;

    public boolean isTransferEnabled() {
        return transferEnabled;
    }

    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }
}
