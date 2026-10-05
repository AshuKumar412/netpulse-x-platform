import React, { useState, useEffect, useCallback } from 'react';
import systemService from '../services/systemService';
import { useWebSocket } from '../context/WebSocketContext';
import { Card } from '../components/common/Card';
import { StatusBadge } from '../components/common/StatusBadge';
import { LoadingState } from '../components/common/LoadingState';
import { ErrorMessage } from '../components/common/ErrorMessage';

const formatBytes = (bytes, decimals = 2) => {
  if (!bytes || bytes === 0) return '0 B';
  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
};

const formatUptime = (seconds) => {
  if (!seconds || seconds <= 0) return '0m';
  const days = Math.floor(seconds / (3600 * 24));
  const hrs = Math.floor((seconds % (3600 * 24)) / 3600);
  const mins = Math.floor((seconds % 3600) / 60);
  const secs = Math.floor(seconds % 60);

  const parts = [];
  if (days > 0) parts.push(`${days}d`);
  if (hrs > 0 || days > 0) parts.push(`${hrs}h`);
  parts.push(`${mins}m`);
  parts.push(`${secs}s`);
  return parts.join(' ');
};

export default function SystemOverview() {
  const { systemMetricsUpdate, connectionStatus } = useWebSocket();
  const [profile, setProfile] = useState(null);
  const [liveMetrics, setLiveMetrics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [refreshing, setRefreshing] = useState(false);

  const fetchProfileAndMetrics = useCallback(async (isInitial = false) => {
    try {
      if (isInitial) setLoading(true);
      else setRefreshing(true);
      setError(null);

      const [profData, metricsData] = await Promise.all([
        systemService.getSystemProfile(),
        systemService.getLiveMetrics(),
      ]);

      setProfile(profData);
      setLiveMetrics(metricsData);
    } catch (err) {
      console.error('Failed to load host system discovery:', err);
      setError(err.response?.data?.message || 'Unable to connect to host system discovery service.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    fetchProfileAndMetrics(true);
  }, [fetchProfileAndMetrics]);

  // Sync real-time metrics when received via WebSocket
  useEffect(() => {
    if (systemMetricsUpdate) {
      setLiveMetrics((prev) => ({
        ...prev,
        ...systemMetricsUpdate,
      }));
    }
  }, [systemMetricsUpdate]);

  if (loading) {
    return <LoadingState message="Discovering host machine hardware and operating system profile..." />;
  }

  if (error && !profile) {
    return <ErrorMessage message={error} onRetry={() => fetchProfileAndMetrics(true)} />;
  }

  const identity = profile?.identity || {};
  const os = profile?.os || {};
  const cpu = profile?.cpu || {};
  const memory = profile?.memory || {};
  const storageDrives = liveMetrics?.storageDrives || profile?.storageDrives || [];
  const networkInterfaces = profile?.networkInterfaces || [];

  const currentCpuLoad = liveMetrics?.cpuLoadPercent ?? cpu.currentCpuLoadPercent ?? 0;
  const perCoreLoads = liveMetrics?.perCoreCpuLoads || cpu.perCoreLoads || [];
  const currentMemUsed = liveMetrics?.usedMemoryBytes ?? memory.usedBytes ?? 0;
  const currentMemTotal = liveMetrics?.totalMemoryBytes ?? memory.totalBytes ?? 1;
  const currentMemUtil = liveMetrics?.memoryUtilizationPercent ?? memory.utilizationPercent ?? 0;
  const currentUptime = liveMetrics?.uptimeSeconds ?? os.uptimeSeconds ?? 0;

  const getTierColor = (tier) => {
    switch (tier) {
      case 'ENTERPRISE_GRADE':
        return 'bg-purple-900/40 text-purple-300 border-purple-500/40';
      case 'HIGH_END':
        return 'bg-emerald-900/40 text-emerald-300 border-emerald-500/40';
      case 'MID_RANGE':
        return 'bg-blue-900/40 text-blue-300 border-blue-500/40';
      default:
        return 'bg-amber-900/40 text-amber-300 border-amber-500/40';
    }
  };

  return (
    <div className="space-y-6 pb-12">
      {/* Top Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-slate-900/80 border border-slate-800 p-6 rounded-2xl backdrop-blur-md shadow-xl">
        <div className="space-y-1">
          <div className="flex items-center gap-3">
            <span className="p-2 rounded-xl bg-cyan-500/10 text-cyan-400 border border-cyan-500/20">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
              </svg>
            </span>
            <div>
              <h1 className="text-2xl font-bold text-white tracking-tight">Host System Overview</h1>
              <p className="text-sm text-slate-400">
                Dynamic runtime discovery & live performance telemetry for this host computer
              </p>
            </div>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-800/80 border border-slate-700 text-xs text-slate-300">
            <span className="text-slate-400">Identity:</span>
            <span className="font-mono font-bold text-cyan-400">{identity.safeMachineId || 'LOCAL-SYSTEM'}</span>
          </div>

          <div className={`px-3 py-1.5 rounded-lg border text-xs font-semibold uppercase tracking-wider ${getTierColor(identity.capabilityTier)}`}>
            {identity.capabilityTier || 'STANDARD'}
          </div>

          <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-800/80 border border-slate-700 text-xs">
            <span className={`w-2 h-2 rounded-full ${connectionStatus === 'CONNECTED' ? 'bg-emerald-400 animate-pulse' : 'bg-amber-400'}`} />
            <span className="text-slate-300">{connectionStatus === 'CONNECTED' ? 'Live Stream' : 'Polling'}</span>
          </div>

          <button
            onClick={() => fetchProfileAndMetrics(false)}
            disabled={refreshing}
            className="flex items-center gap-2 px-3.5 py-1.5 rounded-lg bg-cyan-600 hover:bg-cyan-500 text-white text-xs font-medium transition shadow-lg shadow-cyan-600/20 disabled:opacity-50"
          >
            <svg className={`w-4 h-4 ${refreshing ? 'animate-spin' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            <span>{refreshing ? 'Scanning...' : 'Refresh'}</span>
          </button>
        </div>
      </div>

      {/* Live KPIs Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {/* CPU Load Card */}
        <Card className="p-5 border-slate-800 bg-slate-900/60 relative overflow-hidden">
          <div className="flex justify-between items-start mb-3">
            <div>
              <p className="text-xs uppercase font-medium tracking-wider text-slate-400">Host CPU Utilization</p>
              <h3 className="text-3xl font-extrabold text-white mt-1">
                {currentCpuLoad.toFixed(1)}%
              </h3>
            </div>
            <span className={`p-2 rounded-xl ${currentCpuLoad > 85 ? 'bg-red-500/10 text-red-400' : 'bg-cyan-500/10 text-cyan-400'}`}>
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </span>
          </div>
          <div className="w-full bg-slate-800 rounded-full h-2 overflow-hidden mb-2">
            <div
              className={`h-full transition-all duration-500 rounded-full ${
                currentCpuLoad > 85 ? 'bg-red-500' : currentCpuLoad > 60 ? 'bg-amber-500' : 'bg-cyan-400'
              }`}
              style={{ width: `${Math.min(100, Math.max(0, currentCpuLoad))}%` }}
            />
          </div>
          <div className="flex justify-between text-xs text-slate-400 font-medium">
            <span>{cpu.physicalCores || 1} Physical / {cpu.logicalCores || 1} Logical Cores</span>
            <span>{cpu.vendor || 'Hardware'}</span>
          </div>
        </Card>

        {/* Memory Card */}
        <Card className="p-5 border-slate-800 bg-slate-900/60 relative overflow-hidden">
          <div className="flex justify-between items-start mb-3">
            <div>
              <p className="text-xs uppercase font-medium tracking-wider text-slate-400">Physical Memory (RAM)</p>
              <h3 className="text-3xl font-extrabold text-white mt-1">
                {currentMemUtil.toFixed(1)}%
              </h3>
            </div>
            <span className="p-2 rounded-xl bg-purple-500/10 text-purple-400">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
              </svg>
            </span>
          </div>
          <div className="w-full bg-slate-800 rounded-full h-2 overflow-hidden mb-2">
            <div
              className={`h-full transition-all duration-500 rounded-full ${
                currentMemUtil > 85 ? 'bg-red-500' : currentMemUtil > 70 ? 'bg-purple-500' : 'bg-purple-400'
              }`}
              style={{ width: `${Math.min(100, Math.max(0, currentMemUtil))}%` }}
            />
          </div>
          <div className="flex justify-between text-xs text-slate-400 font-medium">
            <span>Used: {formatBytes(currentMemUsed)}</span>
            <span>Total: {formatBytes(currentMemTotal)}</span>
          </div>
        </Card>

        {/* Uptime Card */}
        <Card className="p-5 border-slate-800 bg-slate-900/60 relative overflow-hidden">
          <div className="flex justify-between items-start mb-3">
            <div>
              <p className="text-xs uppercase font-medium tracking-wider text-slate-400">System Uptime</p>
              <h3 className="text-2xl font-extrabold text-white mt-1 font-mono">
                {formatUptime(currentUptime)}
              </h3>
            </div>
            <span className="p-2 rounded-xl bg-emerald-500/10 text-emerald-400">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-2">
            Host: <span className="text-slate-200 font-medium">{identity.hostname || 'Localhost'}</span>
          </p>
          <p className="text-xs text-slate-400">
            Platform: <span className="text-slate-200 font-medium">{os.osArch || 'x64'}</span>
          </p>
        </Card>

        {/* Storage Volume Card */}
        <Card className="p-5 border-slate-800 bg-slate-900/60 relative overflow-hidden">
          <div className="flex justify-between items-start mb-3">
            <div>
              <p className="text-xs uppercase font-medium tracking-wider text-slate-400">Primary Storage</p>
              <h3 className="text-2xl font-extrabold text-white mt-1 font-mono">
                {storageDrives.length > 0 ? `${storageDrives[0].utilizationPercent}%` : 'N/A'}
              </h3>
            </div>
            <span className="p-2 rounded-xl bg-blue-500/10 text-blue-400">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 7v10c0 2 1.5 3 3.5 3h9c2 0 3.5-1 3.5-3V7c0-2-1.5-3-3.5-3h-9C5.5 4 4 5 4 7z" />
              </svg>
            </span>
          </div>
          <div className="w-full bg-slate-800 rounded-full h-2 overflow-hidden mb-2">
            <div
              className="h-full bg-blue-400 rounded-full transition-all duration-500"
              style={{ width: `${storageDrives.length > 0 ? storageDrives[0].utilizationPercent : 0}%` }}
            />
          </div>
          <div className="flex justify-between text-xs text-slate-400 font-medium">
            <span>{storageDrives.length} Volumes Mounted</span>
            <span>{storageDrives.length > 0 ? formatBytes(storageDrives[0].freeBytes) + ' Free' : ''}</span>
          </div>
        </Card>
      </div>

      {/* Main Hardware Details Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Operating System & Runtime Card */}
        <Card className="p-6 border-slate-800 bg-slate-900/60 space-y-4">
          <div className="flex items-center gap-3 border-b border-slate-800 pb-3">
            <span className="p-2 rounded-lg bg-slate-800 text-cyan-400">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
            </span>
            <div>
              <h3 className="text-base font-bold text-white">Operating System & Runtime</h3>
              <p className="text-xs text-slate-400">Discovered OS version, architecture, and runtime environment</p>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4 text-sm">
            <div className="bg-slate-800/40 p-3 rounded-xl border border-slate-800">
              <span className="text-xs text-slate-400 block mb-0.5">Operating System</span>
              <span className="font-semibold text-white">{os.osName || 'Host OS'}</span>
            </div>
            <div className="bg-slate-800/40 p-3 rounded-xl border border-slate-800">
              <span className="text-xs text-slate-400 block mb-0.5">Architecture</span>
              <span className="font-semibold text-white">{os.osArch || 'x86_64'}</span>
            </div>
            <div className="bg-slate-800/40 p-3 rounded-xl border border-slate-800">
              <span className="text-xs text-slate-400 block mb-0.5">Build / Version</span>
              <span className="font-mono text-xs text-slate-300">{os.osVersion || 'Standard'}</span>
            </div>
            <div className="bg-slate-800/40 p-3 rounded-xl border border-slate-800">
              <span className="text-xs text-slate-400 block mb-0.5">Hostname</span>
              <span className="font-mono text-xs text-cyan-300">{identity.hostname || 'Localhost'}</span>
            </div>
          </div>

          <div className="bg-slate-800/20 p-3 rounded-xl border border-slate-800 flex items-center justify-between text-xs">
            <span className="text-slate-400">Java Runtime:</span>
            <span className="text-slate-300 font-mono">JDK {profile?.javaVersion || '25'} ({profile?.javaVendor || 'Oracle'})</span>
          </div>
        </Card>

        {/* CPU Model & Cores Breakdown Card */}
        <Card className="p-6 border-slate-800 bg-slate-900/60 space-y-4">
          <div className="flex items-center gap-3 border-b border-slate-800 pb-3">
            <span className="p-2 rounded-lg bg-slate-800 text-purple-400">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z" />
              </svg>
            </span>
            <div>
              <h3 className="text-base font-bold text-white">Processor Architecture</h3>
              <p className="text-xs text-slate-400">{cpu.modelName || 'Host Processor'}</p>
            </div>
          </div>

          <div className="grid grid-cols-3 gap-3 text-center">
            <div className="bg-slate-800/40 p-3 rounded-xl border border-slate-800">
              <span className="text-xs text-slate-400 block mb-1">Physical Cores</span>
              <span className="text-xl font-extrabold text-white">{cpu.physicalCores || 1}</span>
            </div>
            <div className="bg-slate-800/40 p-3 rounded-xl border border-slate-800">
              <span className="text-xs text-slate-400 block mb-1">Logical Threads</span>
              <span className="text-xl font-extrabold text-purple-400">{cpu.logicalCores || 1}</span>
            </div>
            <div className="bg-slate-800/40 p-3 rounded-xl border border-slate-800">
              <span className="text-xs text-slate-400 block mb-1">Max Speed</span>
              <span className="text-xl font-extrabold text-cyan-400">
                {cpu.maxFrequencyHz > 0 ? (cpu.maxFrequencyHz / 1e9).toFixed(2) + ' GHz' : 'Dynamic'}
              </span>
            </div>
          </div>

          {/* Per-Core Load Gauges */}
          {perCoreLoads.length > 0 && (
            <div className="space-y-2 pt-1">
              <span className="text-xs font-medium text-slate-400 block">Individual Core Loads:</span>
              <div className="grid grid-cols-4 sm:grid-cols-8 gap-2">
                {perCoreLoads.map((load, idx) => (
                  <div key={idx} className="bg-slate-800/60 p-2 rounded-lg border border-slate-700/50 text-center">
                    <span className="text-[10px] text-slate-400 block">C{idx}</span>
                    <span className="text-xs font-bold text-slate-200">{load.toFixed(0)}%</span>
                    <div className="w-full bg-slate-900 rounded-full h-1 mt-1 overflow-hidden">
                      <div
                        className="h-full bg-cyan-400 rounded-full"
                        style={{ width: `${Math.min(100, Math.max(0, load))}%` }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </Card>
      </div>

      {/* Storage Volumes & Attached Filesystems */}
      <Card className="p-6 border-slate-800 bg-slate-900/60 space-y-4">
        <div className="flex items-center gap-3 border-b border-slate-800 pb-3">
          <span className="p-2 rounded-lg bg-slate-800 text-blue-400">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4" />
            </svg>
          </span>
          <div>
            <h3 className="text-base font-bold text-white">Attached Storage Volumes & Disks</h3>
            <p className="text-xs text-slate-400">Discovered local storage partitions and available filesystem space</p>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {storageDrives.map((drive, idx) => (
            <div key={idx} className="bg-slate-800/40 p-4 rounded-xl border border-slate-800 space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="text-sm font-bold text-white">{drive.name || drive.mountPoint}</span>
                  <span className="text-[10px] px-2 py-0.5 rounded bg-slate-700 text-slate-300 font-mono">
                    {drive.fileSystemType || 'Local'}
                  </span>
                </div>
                <span className="text-xs font-bold text-cyan-400">{drive.utilizationPercent}% Used</span>
              </div>

              <div className="w-full bg-slate-800 rounded-full h-2 overflow-hidden">
                <div
                  className="h-full bg-blue-500 rounded-full transition-all duration-500"
                  style={{ width: `${Math.min(100, Math.max(0, drive.utilizationPercent))}%` }}
                />
              </div>

              <div className="flex justify-between text-xs text-slate-400">
                <span>Free: {formatBytes(drive.freeBytes)}</span>
                <span>Total: {formatBytes(drive.totalBytes)}</span>
              </div>
            </div>
          ))}
        </div>
      </Card>

      {/* Network Interfaces */}
      <Card className="p-6 border-slate-800 bg-slate-900/60 space-y-4">
        <div className="flex items-center gap-3 border-b border-slate-800 pb-3">
          <span className="p-2 rounded-lg bg-slate-800 text-emerald-400">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 12a9 9 0 01-9 9m9-9a9 9 0 00-9-9m9 9H3m9 9a9 9 0 01-9-9m9 9c1.657 0 3-4.03 3-9s-1.343-9-3-9m0 18c-1.657 0-3-4.03-3-9s1.343-9 3-9m-9 9a9 9 0 019-9" />
            </svg>
          </span>
          <div>
            <h3 className="text-base font-bold text-white">Discovered Network Adapters</h3>
            <p className="text-xs text-slate-400">Physical and virtual network interfaces detected on this machine</p>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {networkInterfaces.map((iface, idx) => (
            <div key={idx} className="bg-slate-800/40 p-4 rounded-xl border border-slate-800 space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className={`w-2.5 h-2.5 rounded-full ${iface.isUp ? 'bg-emerald-400' : 'bg-slate-500'}`} />
                  <span className="text-sm font-bold text-white">{iface.displayName || iface.name}</span>
                </div>
                <StatusBadge status={iface.isUp ? 'HEALTHY' : 'OFFLINE'} label={iface.isUp ? 'UP' : 'DOWN'} />
              </div>

              <div className="text-xs space-y-1 text-slate-300 font-mono">
                {iface.ipv4Addresses && iface.ipv4Addresses.length > 0 && (
                  <div className="flex justify-between">
                    <span className="text-slate-400">IPv4:</span>
                    <span>{iface.ipv4Addresses.join(', ')}</span>
                  </div>
                )}
                {iface.macAddress && iface.macAddress !== 'N/A' && (
                  <div className="flex justify-between">
                    <span className="text-slate-400">MAC:</span>
                    <span>{iface.macAddress}</span>
                  </div>
                )}
                {iface.speedBps > 0 && (
                  <div className="flex justify-between">
                    <span className="text-slate-400">Link Speed:</span>
                    <span>{(iface.speedBps / 1e6).toFixed(0)} Mbps</span>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      </Card>

      {/* Adaptive Hardware Profile Policy */}
      <Card className="p-6 border-slate-800 bg-slate-900/60 space-y-4">
        <div className="flex items-center gap-3 border-b border-slate-800 pb-3">
          <span className="p-2 rounded-lg bg-slate-800 text-amber-400">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4" />
            </svg>
          </span>
          <div>
            <h3 className="text-base font-bold text-white">Dynamic Hardware Adaptation Policy</h3>
            <p className="text-xs text-slate-400">
              NetPulse X automatically scales its monitoring frequency and thread concurrency based on this computer's specs
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-sm">
          <div className="bg-slate-800/40 p-4 rounded-xl border border-slate-800">
            <span className="text-xs text-slate-400 block mb-1">Recommended Telemetry Interval</span>
            <span className="text-lg font-bold text-cyan-400">{identity.recommendedPollingIntervalMs || 2000} ms</span>
            <p className="text-[11px] text-slate-400 mt-1">Calibrated to avoid CPU contention</p>
          </div>
          <div className="bg-slate-800/40 p-4 rounded-xl border border-slate-800">
            <span className="text-xs text-slate-400 block mb-1">Max Concurrency Worker Threads</span>
            <span className="text-lg font-bold text-purple-400">{identity.recommendedMaxThreads || 8} Threads</span>
            <p className="text-[11px] text-slate-400 mt-1">Allocated across available processor cores</p>
          </div>
          <div className="bg-slate-800/40 p-4 rounded-xl border border-slate-800">
            <span className="text-xs text-slate-400 block mb-1">Local Host Identity Isolation</span>
            <span className="text-lg font-bold text-emerald-400">Protected Profile</span>
            <p className="text-[11px] text-slate-400 mt-1">Zero development credentials or hardcoded states</p>
          </div>
        </div>
      </Card>
    </div>
  );
}
