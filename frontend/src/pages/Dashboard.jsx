import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  Server,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  Activity,
  Plus,
  Radio,
  RefreshCw,
  Layers,
  Database,
  Cpu,
  HardDrive,
  Clock,
  Zap,
  Play,
  Square,
  ShieldAlert,
  ShieldCheck,
} from 'lucide-react';
import { nodeService } from '../services/nodeService';
import { telemetryService } from '../services/telemetryService';
import { healthService } from '../services/healthService';
import { heartbeatService } from '../services/heartbeatService';
import { monitoringService } from '../services/monitoringService';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { LoadingState } from '../components/common/LoadingState';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { useWebSocket } from '../context/WebSocketContext';
import { useAuth } from '../context/AuthContext';

export const Dashboard = () => {
  const [summary, setSummary] = useState(null);
  const [nodes, setNodes] = useState([]);
  const [telemetryMap, setTelemetryMap] = useState({});
  const [healthMap, setHealthMap] = useState({});
  const [heartbeatMap, setHeartbeatMap] = useState({});
  const [monitoringStatus, setMonitoringStatus] = useState(null);
  const [selectedChartNode, setSelectedChartNode] = useState(null);
  const [nodeHistory, setNodeHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const { connectionStatus, telemetryUpdate, healthUpdate, heartbeatUpdate, monitoringUpdate, sendPing } = useWebSocket();
  const { user, isOperator } = useAuth();

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const [sumRes, nodesRes, telemRes, healthRes, hbRes, monRes] = await Promise.all([
        nodeService.getNodeSummary().catch(() => ({ data: null })),
        nodeService.getAllNodes().catch(() => ({ data: [] })),
        telemetryService.getAllTelemetry().catch(() => ({ data: [] })),
        healthService.getAllNodeHealth().catch(() => ({ data: [] })),
        heartbeatService.getAllHeartbeatStatuses().catch(() => ({ data: [] })),
        monitoringService.getStatus().catch(() => ({ data: null })),
      ]);

      if (sumRes?.data) setSummary(sumRes.data);
      if (nodesRes?.data) {
        setNodes(nodesRes.data);
        if (nodesRes.data.length > 0 && !selectedChartNode) {
          setSelectedChartNode(nodesRes.data[0].nodeId);
        }
      }

      if (telemRes?.data) {
        const tMap = {};
        telemRes.data.forEach((t) => {
          tMap[t.nodeId] = t;
        });
        setTelemetryMap(tMap);
      }

      if (healthRes?.data) {
        const hMap = {};
        healthRes.data.forEach((h) => {
          hMap[h.nodeId] = h;
        });
        setHealthMap(hMap);
      }

      if (hbRes?.data) {
        const hbMap = {};
        hbRes.data.forEach((hb) => {
          hbMap[hb.nodeId] = hb;
        });
        setHeartbeatMap(hbMap);
      }

      if (monRes?.data) setMonitoringStatus(monRes.data);
    } catch (err) {
      setError(err.message || 'Failed to load telemetry dashboard data');
    } finally {
      setLoading(false);
    }
  }, [selectedChartNode]);

  // Initial load
  useEffect(() => {
    loadData();
  }, [loadData]);

  // Load selected node history for charts
  useEffect(() => {
    if (!selectedChartNode) return;
    let active = true;
    telemetryService.getNodeTelemetryHistory(selectedChartNode, 25)
      .then((res) => {
        if (active && res?.data) {
          setNodeHistory(res.data);
        }
      })
      .catch(() => {});
    return () => {
      active = false;
    };
  }, [selectedChartNode, telemetryUpdate]);

  // Listen to live WebSocket telemetry broadcasts
  useEffect(() => {
    if (telemetryUpdate && Array.isArray(telemetryUpdate)) {
      const tMap = { ...telemetryMap };
      telemetryUpdate.forEach((t) => {
        tMap[t.nodeId] = t;
      });
      setTelemetryMap(tMap);
    }
  }, [telemetryUpdate]);

  // Listen to live health updates
  useEffect(() => {
    if (healthUpdate && Array.isArray(healthUpdate)) {
      const hMap = { ...healthMap };
      healthUpdate.forEach((h) => {
        hMap[h.nodeId] = h;
      });
      setHealthMap(hMap);
    }
  }, [healthUpdate]);

  // Listen to live heartbeat updates
  useEffect(() => {
    if (heartbeatUpdate && Array.isArray(heartbeatUpdate)) {
      const hbMap = { ...heartbeatMap };
      heartbeatUpdate.forEach((hb) => {
        hbMap[hb.nodeId] = hb;
      });
      setHeartbeatMap(hbMap);
    }
  }, [heartbeatUpdate]);

  // Listen to monitoring updates
  useEffect(() => {
    if (monitoringUpdate) {
      setMonitoringStatus(monitoringUpdate);
    }
  }, [monitoringUpdate]);

  const handleStartMonitoring = async () => {
    try {
      const res = await monitoringService.startMonitoring();
      if (res.data) setMonitoringStatus(res.data);
    } catch (err) {
      alert('Failed to start monitoring: ' + err.message);
    }
  };

  const handleStopMonitoring = async () => {
    try {
      const res = await monitoringService.stopMonitoring();
      if (res.data) setMonitoringStatus(res.data);
    } catch (err) {
      alert('Failed to stop monitoring: ' + err.message);
    }
  };

  const handleToggleSuppression = async (nodeId, currentSuppressed) => {
    try {
      await heartbeatService.toggleHeartbeatSuppression(nodeId, !currentSuppressed);
      loadData();
    } catch (err) {
      alert('Failed to toggle heartbeat suppression: ' + err.message);
    }
  };

  if (loading && !summary) {
    return <LoadingState message="Connecting to telemetry engine and gathering metrics..." />;
  }

  if (error) {
    return <ErrorMessage message={error} onRetry={loadData} />;
  }

  const hasNodes = nodes.length > 0;
  const telemetryList = Object.values(telemetryMap);
  const hasTelemetry = telemetryList.length > 0;

  // Calculate real aggregates from backend telemetry
  const avgCpu = hasTelemetry
    ? (telemetryList.reduce((acc, t) => acc + t.cpuUsage, 0) / telemetryList.length).toFixed(1)
    : null;
  const avgMemory = hasTelemetry
    ? (telemetryList.reduce((acc, t) => acc + t.memoryUsage, 0) / telemetryList.length).toFixed(1)
    : null;
  const avgLatency = hasTelemetry
    ? (telemetryList.reduce((acc, t) => acc + t.latency, 0) / telemetryList.length).toFixed(1)
    : null;
  const avgPacketLoss = hasTelemetry
    ? (telemetryList.reduce((acc, t) => acc + t.packetLoss, 0) / telemetryList.length).toFixed(1)
    : null;
  const totalActiveConns = hasTelemetry
    ? telemetryList.reduce((acc, t) => acc + t.activeConnections, 0)
    : null;
  const avgErrorRate = hasTelemetry
    ? (telemetryList.reduce((acc, t) => acc + t.errorRate, 0) / telemetryList.length).toFixed(1)
    : null;

  return (
    <div className="space-y-8">
      {/* Top Header & Monitoring Lifecycle Controls */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 p-5 rounded-2xl bg-slate-900/80 border border-slate-800/80 shadow-lg">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold tracking-tight text-white">Live Telemetry & Heartbeat Monitoring</h1>
            <span className={`text-[10px] font-mono px-2.5 py-0.5 rounded-full font-bold border ${
              connectionStatus === 'CONNECTED'
                ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                : connectionStatus === 'CONNECTING'
                ? 'bg-amber-500/10 text-amber-400 border-amber-500/30'
                : 'bg-rose-500/10 text-rose-400 border-rose-500/30'
            }`}>
              {connectionStatus === 'CONNECTED' ? '● WEBSOCKET LIVE' : connectionStatus}
            </span>
          </div>
          <p className="text-xs text-slate-400 font-mono mt-1">
            Phase 3 Deterministic Telemetry Engine &bull; Liveness Heartbeats &bull; Node Health Engine
          </p>
        </div>

        {/* Monitoring Controls */}
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-950 border border-slate-800 text-xs font-mono text-slate-300">
            <span className="text-slate-400">ENGINE:</span>
            <span className={monitoringStatus?.status === 'RUNNING' ? 'text-emerald-400 font-bold' : 'text-amber-400 font-bold'}>
              {monitoringStatus?.status || 'STOPPED'}
            </span>
            <span className="text-slate-600">|</span>
            <span className="text-slate-400">CYCLES:</span>
            <span className="text-sky-400 font-bold">{monitoringStatus?.cycleCount ?? 0}</span>
          </div>

          {isOperator && (
            <>
              {monitoringStatus?.status === 'RUNNING' ? (
                <Button variant="danger" size="sm" onClick={handleStopMonitoring} icon={Square}>
                  Stop Engine
                </Button>
              ) : (
                <Button variant="primary" size="sm" onClick={handleStartMonitoring} icon={Play}>
                  Start Engine
                </Button>
              )}
            </>
          )}

          <Button variant="outline" size="sm" onClick={loadData} icon={RefreshCw}>
            Refresh
          </Button>

          {isOperator && (
            <Link to="/nodes">
              <Button variant="primary" size="sm" icon={Plus}>
                Provision Node
              </Button>
            </Link>
          )}
        </div>
      </div>

      {/* Real Live Telemetry Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-6 gap-4">
        {/* CPU */}
        <Card className="border-slate-800 bg-[#111622]/90 hover:border-slate-700 transition-colors">
          <div className="flex items-center justify-between">
            <p className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">Cluster CPU</p>
            <Cpu className="w-4 h-4 text-sky-400" />
          </div>
          <h3 className="text-2xl font-bold text-white mt-2 font-mono">
            {avgCpu !== null ? `${avgCpu}%` : '—'}
          </h3>
          <p className="text-[10px] text-slate-400 mt-2 font-mono">
            {hasTelemetry ? 'Live average utilization' : 'No telemetry data'}
          </p>
        </Card>

        {/* Memory */}
        <Card className="border-slate-800 bg-[#111622]/90 hover:border-slate-700 transition-colors">
          <div className="flex items-center justify-between">
            <p className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">Cluster Memory</p>
            <HardDrive className="w-4 h-4 text-purple-400" />
          </div>
          <h3 className="text-2xl font-bold text-white mt-2 font-mono">
            {avgMemory !== null ? `${avgMemory}%` : '—'}
          </h3>
          <p className="text-[10px] text-slate-400 mt-2 font-mono">
            {hasTelemetry ? 'Live memory consumption' : 'No telemetry data'}
          </p>
        </Card>

        {/* Latency */}
        <Card className="border-slate-800 bg-[#111622]/90 hover:border-slate-700 transition-colors">
          <div className="flex items-center justify-between">
            <p className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">Avg Latency</p>
            <Clock className="w-4 h-4 text-emerald-400" />
          </div>
          <h3 className="text-2xl font-bold text-white mt-2 font-mono">
            {avgLatency !== null ? `${avgLatency} ms` : '—'}
          </h3>
          <p className="text-[10px] text-slate-400 mt-2 font-mono">
            {hasTelemetry ? 'Runtime round-trip measurement' : 'No telemetry data'}
          </p>
        </Card>

        {/* Packet Loss */}
        <Card className="border-slate-800 bg-[#111622]/90 hover:border-slate-700 transition-colors">
          <div className="flex items-center justify-between">
            <p className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">Packet Loss</p>
            <AlertTriangle className="w-4 h-4 text-amber-400" />
          </div>
          <h3 className="text-2xl font-bold text-white mt-2 font-mono">
            {avgPacketLoss !== null ? `${avgPacketLoss}%` : '—'}
          </h3>
          <p className="text-[10px] text-slate-400 mt-2 font-mono">
            {hasTelemetry ? 'Runtime dropped packets' : 'No telemetry data'}
          </p>
        </Card>

        {/* Active Connections */}
        <Card className="border-slate-800 bg-[#111622]/90 hover:border-slate-700 transition-colors">
          <div className="flex items-center justify-between">
            <p className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">Active Conns</p>
            <Zap className="w-4 h-4 text-cyan-400" />
          </div>
          <h3 className="text-2xl font-bold text-white mt-2 font-mono">
            {totalActiveConns !== null ? totalActiveConns.toLocaleString() : '—'}
          </h3>
          <p className="text-[10px] text-slate-400 mt-2 font-mono">
            {hasTelemetry ? 'Total monitored connections' : 'No telemetry data'}
          </p>
        </Card>

        {/* Error Rate */}
        <Card className="border-slate-800 bg-[#111622]/90 hover:border-slate-700 transition-colors">
          <div className="flex items-center justify-between">
            <p className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">Error Rate</p>
            <ShieldAlert className="w-4 h-4 text-rose-400" />
          </div>
          <h3 className="text-2xl font-bold text-white mt-2 font-mono">
            {avgErrorRate !== null ? `${avgErrorRate}%` : '—'}
          </h3>
          <p className="text-[10px] text-slate-400 mt-2 font-mono">
            {hasTelemetry ? 'Failed request ratio' : 'No telemetry data'}
          </p>
        </Card>
      </div>

      {/* Historical Telemetry Charts (Real Data from PostgreSQL) */}
      <Card
        title="Node Telemetry History & Time-Series Metrics"
        subtitle="Real-time time series recorded in PostgreSQL and updated via WebSocket"
        headerAction={
          hasNodes && (
            <div className="flex items-center gap-2">
              <label className="text-xs text-slate-400 font-mono">Selected Node:</label>
              <select
                value={selectedChartNode || ''}
                onChange={(e) => setSelectedChartNode(e.target.value)}
                className="bg-slate-950 border border-slate-700 text-xs text-white rounded px-2.5 py-1 font-mono focus:outline-none focus:border-sky-500"
              >
                {nodes.map((n) => (
                  <option key={n.nodeId} value={n.nodeId}>
                    {n.name} ({n.nodeId})
                  </option>
                ))}
              </select>
            </div>
          )
        }
      >
        {!hasNodes ? (
          <div className="py-12 text-center">
            <Server className="w-10 h-10 text-slate-600 mx-auto mb-2" />
            <p className="text-sm text-slate-300 font-medium">No network nodes configured.</p>
            <p className="text-xs text-slate-400 mt-1">Provision a network node to begin recording real-time telemetry.</p>
          </div>
        ) : nodeHistory.length === 0 ? (
          <div className="py-12 text-center">
            <Activity className="w-10 h-10 text-slate-600 mx-auto mb-2 animate-pulse" />
            <p className="text-sm text-slate-300 font-medium">Telemetry initialization in progress.</p>
            <p className="text-xs text-slate-400 mt-1">
              {monitoringStatus?.status === 'RUNNING'
                ? 'Awaiting first telemetry ticks from the backend monitoring engine...'
                : 'Monitoring engine is currently STOPPED. Click "Start Engine" above to resume.'}
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 pt-2">
            {/* CPU & Memory SVG Chart */}
            <div className="p-4 rounded-xl bg-slate-950/70 border border-slate-800">
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-semibold text-white">CPU & Memory Utilization (%)</span>
                </div>
                <div className="flex items-center gap-4 text-[11px] font-mono">
                  <span className="flex items-center gap-1.5 text-sky-400">
                    <span className="w-2.5 h-2.5 rounded-full bg-sky-400"></span> CPU
                  </span>
                  <span className="flex items-center gap-1.5 text-purple-400">
                    <span className="w-2.5 h-2.5 rounded-full bg-purple-400"></span> Memory
                  </span>
                </div>
              </div>
              <div className="h-44 w-full flex items-end">
                <svg className="w-full h-full overflow-visible" viewBox="0 0 400 120" preserveAspectRatio="none">
                  {/* Grid Lines */}
                  <line x1="0" y1="0" x2="400" y2="0" stroke="#1e293b" strokeDasharray="3 3" />
                  <line x1="0" y1="60" x2="400" y2="60" stroke="#1e293b" strokeDasharray="3 3" />
                  <line x1="0" y1="120" x2="400" y2="120" stroke="#334155" />

                  {/* CPU Line */}
                  <polyline
                    fill="none"
                    stroke="#38bdf8"
                    strokeWidth="2.5"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    points={nodeHistory
                      .map((h, i) => {
                        const x = (i / Math.max(1, nodeHistory.length - 1)) * 400;
                        const y = 120 - (h.cpuUsage / 100) * 120;
                        return `${x},${y}`;
                      })
                      .join(' ')}
                  />

                  {/* Memory Line */}
                  <polyline
                    fill="none"
                    stroke="#c084fc"
                    strokeWidth="2.5"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    points={nodeHistory
                      .map((h, i) => {
                        const x = (i / Math.max(1, nodeHistory.length - 1)) * 400;
                        const y = 120 - (h.memoryUsage / 100) * 120;
                        return `${x},${y}`;
                      })
                      .join(' ')}
                  />
                </svg>
              </div>
              <div className="flex justify-between text-[10px] font-mono text-slate-400 mt-2">
                <span>Earliest recorded</span>
                <span>{nodeHistory.length} data points</span>
                <span>Latest ({nodeHistory[nodeHistory.length - 1]?.cpuUsage}%)</span>
              </div>
            </div>

            {/* Latency & Packet Loss SVG Chart */}
            <div className="p-4 rounded-xl bg-slate-950/70 border border-slate-800">
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-semibold text-white">Latency (ms) & Packet Loss (%)</span>
                </div>
                <div className="flex items-center gap-4 text-[11px] font-mono">
                  <span className="flex items-center gap-1.5 text-emerald-400">
                    <span className="w-2.5 h-2.5 rounded-full bg-emerald-400"></span> Latency (ms)
                  </span>
                  <span className="flex items-center gap-1.5 text-amber-400">
                    <span className="w-2.5 h-2.5 rounded-full bg-amber-400"></span> Loss (%)
                  </span>
                </div>
              </div>
              <div className="h-44 w-full flex items-end">
                <svg className="w-full h-full overflow-visible" viewBox="0 0 400 120" preserveAspectRatio="none">
                  {/* Grid Lines */}
                  <line x1="0" y1="0" x2="400" y2="0" stroke="#1e293b" strokeDasharray="3 3" />
                  <line x1="0" y1="60" x2="400" y2="60" stroke="#1e293b" strokeDasharray="3 3" />
                  <line x1="0" y1="120" x2="400" y2="120" stroke="#334155" />

                  {/* Latency Line (scaled max 200ms) */}
                  <polyline
                    fill="none"
                    stroke="#34d399"
                    strokeWidth="2.5"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    points={nodeHistory
                      .map((h, i) => {
                        const x = (i / Math.max(1, nodeHistory.length - 1)) * 400;
                        const y = 120 - Math.min(120, (h.latency / 200) * 120);
                        return `${x},${y}`;
                      })
                      .join(' ')}
                  />

                  {/* Packet Loss Line (scaled max 10%) */}
                  <polyline
                    fill="none"
                    stroke="#fbbf24"
                    strokeWidth="2"
                    strokeDasharray="4 2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    points={nodeHistory
                      .map((h, i) => {
                        const x = (i / Math.max(1, nodeHistory.length - 1)) * 400;
                        const y = 120 - Math.min(120, (h.packetLoss / 10) * 120);
                        return `${x},${y}`;
                      })
                      .join(' ')}
                  />
                </svg>
              </div>
              <div className="flex justify-between text-[10px] font-mono text-slate-400 mt-2">
                <span>Earliest recorded</span>
                <span>Max scale 200ms</span>
                <span>Latest ({nodeHistory[nodeHistory.length - 1]?.latency} ms)</span>
              </div>
            </div>
          </div>
        )}
      </Card>

      {/* Node Health & Heartbeat Liveness Table */}
      <Card
        title="Infrastructure Liveness & Evaluated Node Health"
        subtitle="Live heartbeat status, timeout tracking, and multi-metric health evaluation"
      >
        {!hasNodes ? (
          <div className="py-8 text-center text-slate-400 text-xs font-mono">
            No network nodes registered.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="text-[11px] uppercase tracking-wider text-slate-400 bg-slate-900/60 border-b border-slate-800">
                <tr>
                  <th className="py-3 px-4">Node ID & Host</th>
                  <th className="py-3 px-3">Heartbeat Liveness</th>
                  <th className="py-3 px-3">Last Seen</th>
                  <th className="py-3 px-3">Health Status</th>
                  <th className="py-3 px-3">Live CPU / RAM</th>
                  <th className="py-3 px-3">Latency / Loss</th>
                  <th className="py-3 px-4">Health Evaluation Reason</th>
                  {isOperator && <th className="py-3 px-3 text-right">Testing Action</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-mono">
                {nodes.map((node) => {
                  const telem = telemetryMap[node.nodeId];
                  const health = healthMap[node.nodeId];
                  const hb = heartbeatMap[node.nodeId];

                  const liveness = hb?.liveness || 'UNREACHABLE';
                  const healthStatus = health?.healthStatus || (node.status === 'OFFLINE' ? 'OFFLINE' : 'HEALTHY');

                  return (
                    <tr key={node.nodeId} className="hover:bg-slate-900/40 transition-colors">
                      <td className="py-3 px-4 font-medium text-white">
                        <div className="flex items-center gap-2">
                          <Server className="w-3.5 h-3.5 text-slate-400" />
                          <div>
                            <p className="font-bold text-slate-200">{node.name}</p>
                            <p className="text-[10px] text-slate-400">{node.nodeId} &bull; {node.host}:{node.port}</p>
                          </div>
                        </div>
                      </td>

                      {/* Heartbeat Liveness */}
                      <td className="py-3 px-3">
                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold inline-flex items-center gap-1 ${
                          liveness === 'ALIVE'
                            ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                            : liveness === 'SUSPECTED'
                            ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30'
                            : 'bg-rose-500/20 text-rose-400 border border-rose-500/30'
                        }`}>
                          <span className={`w-1.5 h-1.5 rounded-full ${
                            liveness === 'ALIVE' ? 'bg-emerald-400 animate-pulse' : liveness === 'SUSPECTED' ? 'bg-amber-400' : 'bg-rose-400'
                          }`}></span>
                          {liveness}
                        </span>
                      </td>

                      {/* Last Seen */}
                      <td className="py-3 px-3 text-slate-300 text-[11px]">
                        {hb?.ageSeconds !== null && hb?.ageSeconds !== undefined
                          ? `${hb.ageSeconds}s ago`
                          : 'Never'}
                      </td>

                      {/* Evaluated Health Status */}
                      <td className="py-3 px-3">
                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                          healthStatus === 'HEALTHY'
                            ? 'bg-emerald-500/20 text-emerald-400'
                            : healthStatus === 'WARNING'
                            ? 'bg-amber-500/20 text-amber-400'
                            : healthStatus === 'DEGRADED'
                            ? 'bg-orange-500/20 text-orange-400'
                            : healthStatus === 'FAILED'
                            ? 'bg-rose-500/20 text-rose-400'
                            : 'bg-slate-700/40 text-slate-400'
                        }`}>
                          {healthStatus}
                        </span>
                      </td>

                      {/* CPU / RAM */}
                      <td className="py-3 px-3 text-slate-300">
                        {telem ? (
                          <span>{telem.cpuUsage}% / {telem.memoryUsage}%</span>
                        ) : (
                          <span className="text-slate-400">—</span>
                        )}
                      </td>

                      {/* Latency / Loss */}
                      <td className="py-3 px-3 text-slate-300">
                        {telem ? (
                          <span>{telem.latency}ms / {telem.packetLoss}%</span>
                        ) : (
                          <span className="text-slate-400">—</span>
                        )}
                      </td>

                      {/* Reason */}
                      <td className="py-3 px-4 text-[11px] text-slate-400 font-sans max-w-xs truncate">
                        {health?.reason || 'Optimal operating conditions'}
                      </td>

                      {/* Testing Action for Operators */}
                      {isOperator && (
                        <td className="py-3 px-3 text-right">
                          <button
                            onClick={() => handleToggleSuppression(node.nodeId, hb?.isSuppressed)}
                            className={`px-2.5 py-1 rounded text-[10px] font-medium transition-colors ${
                              hb?.isSuppressed
                                ? 'bg-amber-500/20 text-amber-300 hover:bg-amber-500/30 border border-amber-500/40'
                                : 'bg-slate-800 text-slate-300 hover:bg-slate-700 border border-slate-700'
                            }`}
                            title="Test heartbeat timeout detection without killing infrastructure"
                          >
                            {hb?.isSuppressed ? 'Resume Heartbeat' : 'Suppress Heartbeat'}
                          </button>
                        </td>
                      )}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
};

export default Dashboard;
