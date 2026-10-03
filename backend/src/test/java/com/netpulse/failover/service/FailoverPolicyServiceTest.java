package com.netpulse.failover.service;

import com.netpulse.failover.dto.FailoverPolicyConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FailoverPolicyServiceTest {

    @Test
    @DisplayName("Should return and allow updating failover policy configuration")
    void testPolicyServiceConfig() {
        FailoverPolicyService policyService = new FailoverPolicyService();

        assertTrue(policyService.isEnabled());
        assertEquals(2, policyService.getConsecutiveFailuresRequired());
        assertTrue(policyService.isRecoveryEnabled());
        assertTrue(policyService.isAutoRecover());
        assertTrue(policyService.isHeartbeatRequiredForRecovery());
        assertEquals(2, policyService.getHealthyCyclesRequiredForRecovery());
        assertEquals(3, policyService.getMaxAttempts());
        assertEquals(15, policyService.getCooldownSeconds());

        policyService.setEnabled(false);
        policyService.setConsecutiveFailuresRequired(3);
        policyService.setMaxAttempts(5);

        FailoverPolicyConfig config = policyService.getPolicyConfig();
        assertNotNull(config);
        assertFalse(config.isEnabled());
        assertEquals(3, config.getConsecutiveFailuresRequired());
        assertEquals(5, config.getMaxAttempts());
    }
}
