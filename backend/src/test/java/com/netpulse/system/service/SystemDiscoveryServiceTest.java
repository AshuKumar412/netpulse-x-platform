package com.netpulse.system.service;

import com.netpulse.system.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemDiscoveryServiceTest {

    private SystemDiscoveryService discoveryService;

    @BeforeEach
    void setUp() {
        discoveryService = new SystemDiscoveryService();
    }

    @Test
    @DisplayName("Should dynamically discover host OS profile with valid parameters")
    void testDiscoverOsProfile() {
        OsProfileDto os = discoveryService.discoverOsProfile();
        assertNotNull(os);
        assertNotNull(os.osName());
        assertFalse(os.osName().isBlank());
        assertNotNull(os.osArch());
        assertTrue(os.uptimeSeconds() >= 0);
    }

    @Test
    @DisplayName("Should dynamically discover host CPU profile with core counts")
    void testDiscoverCpuProfile() {
        CpuProfileDto cpu = discoveryService.discoverCpuProfile();
        assertNotNull(cpu);
        assertNotNull(cpu.modelName());
        assertTrue(cpu.physicalCores() > 0, "Physical cores must be greater than 0");
        assertTrue(cpu.logicalCores() > 0, "Logical cores must be greater than 0");
        assertNotNull(cpu.perCoreLoads());
        assertTrue(cpu.currentCpuLoadPercent() >= 0.0 && cpu.currentCpuLoadPercent() <= 100.0);
    }

    @Test
    @DisplayName("Should dynamically discover host memory profile")
    void testDiscoverMemoryProfile() {
        MemoryProfileDto mem = discoveryService.discoverMemoryProfile();
        assertNotNull(mem);
        assertTrue(mem.totalBytes() > 0, "Total RAM must be greater than 0");
        assertTrue(mem.availableBytes() >= 0);
        assertTrue(mem.usedBytes() >= 0);
        assertTrue(mem.utilizationPercent() >= 0.0 && mem.utilizationPercent() <= 100.0);
    }

    @Test
    @DisplayName("Should discover local storage drives")
    void testDiscoverStorageDrives() {
        var drives = discoveryService.discoverStorageDrives();
        assertNotNull(drives);
        assertFalse(drives.isEmpty(), "Host must have at least one storage root");
        StorageDriveDto primary = drives.get(0);
        assertNotNull(primary.name());
        assertTrue(primary.totalBytes() > 0);
    }

    @Test
    @DisplayName("Should discover network interfaces without errors")
    void testDiscoverNetworkInterfaces() {
        var interfaces = discoveryService.discoverNetworkInterfaces();
        assertNotNull(interfaces);
    }

    @Test
    @DisplayName("Should generate safe machine identity and adaptive hardware tier")
    void testGetLocalSystemIdentity() {
        LocalSystemIdentityDto identity = discoveryService.getLocalSystemIdentity();
        assertNotNull(identity);
        assertNotNull(identity.safeMachineId());
        assertTrue(identity.safeMachineId().startsWith("NPX-"));
        assertNotNull(identity.hostname());
        assertNotNull(identity.capabilityTier());
        assertTrue(identity.recommendedPollingIntervalMs() >= 1000);
        assertTrue(identity.recommendedMaxThreads() >= 4);
    }

    @Test
    @DisplayName("Should assemble full HostSystemProfileDto snapshot")
    void testDiscoverSystemProfile() {
        HostSystemProfileDto profile = discoveryService.discoverSystemProfile();
        assertNotNull(profile);
        assertNotNull(profile.identity());
        assertNotNull(profile.os());
        assertNotNull(profile.cpu());
        assertNotNull(profile.memory());
        assertNotNull(profile.storageDrives());
        assertNotNull(profile.networkInterfaces());
        assertNotNull(profile.javaVersion());
        assertNotNull(profile.discoveryTimestamp());
    }

    @Test
    @DisplayName("Should collect live metrics payload for real-time telemetry")
    void testCollectLiveMetrics() {
        HostLiveMetricsDto live = discoveryService.collectLiveMetrics();
        assertNotNull(live);
        assertTrue(live.cpuLoadPercent() >= 0.0);
        assertTrue(live.totalMemoryBytes() > 0);
        assertTrue(live.usedMemoryBytes() >= 0);
        assertNotNull(live.perCoreCpuLoads());
        assertNotNull(live.timestamp());
    }
}
