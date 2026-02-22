package com.vlad774.borodbank.unit.service;

import com.vlad774.borodbank.service.FeatureFlagService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureFlagServiceTest {

    private final FeatureFlagService service = new FeatureFlagService();

    @Test
    void isTransferEnabled_returnsTrueByDefault() {
        ReflectionTestUtils.setField(service, "transferEnabled", true);
        assertThat(service.isTransferEnabled()).isTrue();
    }

    @Test
    void isTransferEnabled_returnsFalseWhenDisabled() {
        ReflectionTestUtils.setField(service, "transferEnabled", false);
        assertThat(service.isTransferEnabled()).isFalse();
    }

    @Test
    void isMaintenanceMode_returnsFalseByDefault() {
        ReflectionTestUtils.setField(service, "maintenanceMode", false);
        assertThat(service.isMaintenanceMode()).isFalse();
    }

    @Test
    void isMaintenanceMode_returnsTrueWhenEnabled() {
        ReflectionTestUtils.setField(service, "maintenanceMode", true);
        assertThat(service.isMaintenanceMode()).isTrue();
    }
}
