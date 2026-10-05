import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  Zap,
  ShieldCheck,
  ShieldAlert,
  Flame,
  Activity,
  Server,
  RotateCcw,
  Play,
  Square,
  Clock,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  RefreshCw,
  GitFork,
  ArrowRight,
  Info,
  Sliders,
  Timer,
} from 'lucide-react';
import { chaosService } from '../services/chaosService';
import { nodeService } from '../services/nodeService';
import { topologyService } from '../services/topologyService';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { LoadingState } from '../components/common/LoadingState';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { useWebSocket } from '../context/WebSocketContext';
import { useAuth } from '../context/AuthContext';

// ── Scenario Metadata & Presets ─────────────────────────────────────────────
const SCENARIOS = [
  {
    type: 'NODE_FAILURE',
    title: 'Node Outage / Crash',
    desc: 'Simulate complete node outage and heartbeat termination to validate automated failover recovery.',
    icon: Flame,
    color: 'text-rose-400',
    border: 'border-rose-500/30',
    bg: 'bg-rose-500/10',
    defaultSeverity: 100,
    unit: '%',
    min: 100,
    max: 100,
    step: 1,
    defaultDuration: 45,
  },
  {
    type: 'CPU_SPIKE',
    title: 'CPU Saturation Spike',
    desc: 'Inject high CPU pressure (70% - 99%) to trigger telemetry degradation and adaptive load shedding.',
    icon: Activity,
    color: 'text-amber-400',
    border: 'border-amber-500/30',
    bg: 'bg-amber-500/10',
    defaultSeverity: 95,
    unit: '%',
    min: 50,
    max: 99,
    step: 5,
    defaultDuration: 60,
  },
  {
    type: 'MEMORY_SPIKE',
    title: 'Memory Pressure',
    desc: 'Simulate memory exhaustion (75% - 99%) influencing health scoring and traffic suitability.',
    icon: Server,
    color: 'text-purple-400',
    border: 'border-purple-500/30',
    bg: 'bg-purple-500/10',
    defaultSeverity: 90,
    unit: '%',
    min: 50,
    max: 99,
    step: 5,
    defaultDuration: 60,
  },
  {
    type: 'HIGH_LATENCY',
    title: 'Network Delay / Latency',
    desc: 'Inject artificial round-trip latency (+100ms to +1000ms) to test Latency-Aware routing rerouting.',
    icon: Clock,
    color: 'text-sky-400',
    border: 'border-sky-500/30',
    bg: 'bg-sky-500/10',
    defaultSeverity: 350,
    unit: 'ms',
    min: 50,
    max: 2000,
    step: 50,
    defaultDuration: 60,
  },
  {
    type: 'PACKET_LOSS',
    title: 'Packet Loss Injection',
    desc: 'Inject packet loss (5% - 75%) to trigger error penalties in adaptive routing candidate evaluation.',
    icon: AlertTriangle,
    color: 'text-orange-400',
    border: 'border-orange-500/30',
    bg: 'bg-orange-500/10',
    defaultSeverity: 25,
    unit: '%',
    min: 1,
    max: 75,
    step: 5,
    defaultDuration: 60,
  },
  {
    type: 'TRAFFIC_SPIKE',
    title: 'Surge Traffic Load',
    desc: 'Simulate heavy connection surge against a node to verify Least-Connections redistribution.',
    icon: Zap,
    color: 'text-yellow-400',
    border: 'border-yellow-500/30',
    bg: 'bg-yellow-500/10',
    defaultSeverity: 200,
    unit: '%',
    min: 50,
    max: 400,
    step: 25,
    defaultDuration: 60,
  },
  {
    type: 'NETWORK_PARTITION',
    title: 'Network Link Partition',
    desc: 'Sever network link connectivity to test graph isolation and topology connected component splitting.',
    icon: GitFork,
    color: 'text-cyan-400',
    border: 'border-cyan-500/30',
    bg: 'bg-cyan-500/10',
    defaultSeverity: 100,
    unit: '%',
    min: 100,
    max: 100,
    step: 1,
    defaultDuration: 45,
  },
];

const STATUS_BADGES = {
  RUNNING:   { text: 'RUNNING',   class: 'bg-amber-500/10 text-amber-400 border-amber-500/30 animate-pulse' },
  COMPLETED: { text: 'COMPLETED', class: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30' },
  CANCELLED: { text: 'ROLLED BACK', class: 'bg-slate-800 text-slate-300 border-slate-700' },
  FAILED:    { text: 'FAILED',    class: 'bg-rose-500/10 text-rose-400 border-rose-500/30' },
};

export const Chaos = () => {
  const [status, setStatus]               = useState(null);
  const [experiments, setExperiments]     = useState([]);
  const [activeExperiments, setActiveExperiments] = useState([]);
  const [nodes, setNodes]                 = useState([]);
  const [links, setLinks]                 = useState([]);
  const [policies, setPolicies]           = useState(null);
  const [loading, setLoading]             = useState(true);
  const [error, setError]                 = useState(null);

  // Form State
  const [selectedType, setSelectedType]   = useState('NODE_FAILURE');
  const [selectedTarget, setSelectedTarget] = useState('');
  const [selectedLink, setSelectedLink]   = useState('');
  const [severity, setSeverity]           = useState(100);
  const [duration, setDuration]           = useState(45);
  const [submitting, setSubmitting]       = useState(false);

  // Feedback State
  const [feedback, setFeedback]           = useState(null); // { type: 'success' | 'error', message }
  const [selectedExpDetail, setSelectedExpDetail] = useState(null);

  const { connectionStatus, chaosUpdate } = useWebSocket();
  const { isOperator, isAdmin } = useAuth();
  const canOperate = isOperator || isAdmin;

  const currentScenario = SCENARIOS.find((s) => s.type === selectedType) || SCENARIOS[0];

  // Load initial data
  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const [statusRes, expRes, activeRes, nodesRes, linksRes, polRes] = await Promise.all([
        chaosService.getStatus().catch(() => null),
        chaosService.getExperiments(0, 30).catch(() => null),
        chaosService.getActiveExperiments().catch(() => null),
        nodeService.getAllNodes().catch(() => null),
        topologyService.getAllLinks().catch(() => null),
        chaosService.getPolicies().catch(() => null),
      ]);

      if (statusRes?.data) setStatus(statusRes.data);
      if (expRes?.data) setExperiments(Array.isArray(expRes.data) ? expRes.data : []);
      if (activeRes?.data) setActiveExperiments(Array.isArray(activeRes.data) ? activeRes.data : []);
      if (nodesRes?.data) {
        setNodes(nodesRes.data);
        if (!selectedTarget && nodesRes.data.length > 0) {
          setSelectedTarget(nodesRes.data[0].nodeId);
        }
      }
      if (linksRes?.data) {
        setLinks(linksRes.data);
        if (!selectedLink && linksRes.data.length > 0) {
          setSelectedLink(linksRes.data[0].linkId);
        }
      }
      if (polRes?.data) setPolicies(polRes.data);
    } catch (err) {
      setError(err.message || 'Failed to initialize Chaos Engineering console');
    } finally {
      setLoading(false);
    }
  }, [selectedTarget, selectedLink]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Sync WebSocket Real-Time Chaos Updates
  useEffect(() => {
    if (!chaosUpdate) return;
    // Prepend or update experiment in history list
    setExperiments((prev) => [
      chaosUpdate,
      ...prev.filter((e) => e.experimentId !== chaosUpdate.experimentId),
    ].slice(0, 50));

    // Refresh active list and platform status
    chaosService.getActiveExperiments().then((res) => {
      if (res?.data) setActiveExperiments(Array.isArray(res.data) ? res.data : []);
    }).catch(() => {});

    chaosService.getStatus().then((res) => {
      if (res?.data) setStatus(res.data);
    }).catch(() => {});
  }, [chaosUpdate]);

  // When scenario changes, update default severity & duration
  const handleScenarioSelect = (type) => {
    setSelectedType(type);
    const scen = SCENARIOS.find((s) => s.type === type);
    if (scen) {
      setSeverity(scen.defaultSeverity);
      setDuration(scen.defaultDuration);
    }
  };

  // Actions
  const handleStartExperiment = async (e) => {
    e.preventDefault();
    if (!canOperate) return;
    setSubmitting(true);
    setFeedback(null);

    try {
      const payload = {
        experimentType: selectedType,
        targetNodeId: selectedType === 'NETWORK_PARTITION' && selectedLink ? null : selectedTarget,
        targetLinkId: selectedType === 'NETWORK_PARTITION' ? selectedLink : null,
        severity: Number(severity),
        durationSeconds: Number(duration),
      };

      const res = await chaosService.startExperiment(payload);
      setFeedback({
        type: 'success',
        message: `Chaos experiment ${res.data?.experimentId} initiated successfully. Fault is active.`,
      });
      loadData();
    } catch (err) {
      setFeedback({
        type: 'error',
        message: err.message || 'Failed to start chaos experiment',
      });
    } finally {
      setSubmitting(false);
    }
  };

  const handleStopExperiment = async (expId) => {
    setFeedback(null);
    try {
      const res = await chaosService.stopExperiment(expId, 'Operator requested immediate conclusion');
      setFeedback({
        type: 'success',
        message: `Experiment ${expId} concluded. Infrastructure state restored.`,
      });
      loadData();
    } catch (err) {
      setFeedback({
        type: 'error',
        message: err.message || 'Failed to stop experiment',
      });
    }
  };

  const handleRollbackExperiment = async (expId) => {
    setFeedback(null);
    try {
      const res = await chaosService.rollbackExperiment(expId);
      setFeedback({
        type: 'success',
        message: `Emergency rollback completed for ${expId}.`,
      });
      loadData();
    } catch (err) {
      setFeedback({
        type: 'error',
        message: err.message || 'Failed to rollback experiment',
      });
    }
  };

  if (loading && !status) {
    return <LoadingState message="Connecting to Chaos Engineering Engine..." />;
  }

  if (error) {
    return <ErrorMessage message={error} onRetry={loadData} />;
  }

  const enabled = status?.enabled ?? false;
  const activeCount = status?.activeExperimentsCount ?? 0;
  const totalCount = status?.totalExperimentsCount ?? 0;
  const successCount = status?.successfulCount ?? 0;
  const failedCount = status?.failedCount ?? 0;

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 p-5 rounded-2xl bg-slate-900/80 border border-slate-800 shadow-lg">
        <div>
          <div className="flex items-center gap-3 flex-wrap">
            <div className="p-2 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-400">
              <Zap className="w-6 h-6" />
            </div>
            <h1 className="text-2xl font-bold tracking-tight text-white">Chaos Engineering Console</h1>
            <span className={`text-[10px] font-mono px-2.5 py-0.5 rounded-full font-bold border ${
              connectionStatus === 'CONNECTED'
                ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                : 'bg-rose-500/10 text-rose-400 border-rose-500/30'
            }`}>
              {connectionStatus === 'CONNECTED' ? '● ENGINE ONLINE' : connectionStatus}
            </span>
            <span className={`text-[10px] font-mono px-2.5 py-0.5 rounded-full font-bold border ${
              enabled
                ? 'bg-rose-500/10 text-rose-400 border-rose-500/30'
                : 'bg-slate-800 text-slate-400 border-slate-700'
            }`}>
              {enabled ? 'FAULT INJECTION ARMED' : 'FAULT INJECTION DISABLED'}
            </span>
          </div>
          <p className="text-xs text-slate-400 font-mono mt-1.5">
            Inject Controlled Failures · Trigger Real Telemetry · Verify Adaptive Routing & Self-Healing
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Button variant="outline" size="sm" onClick={loadData} icon={RefreshCw}>
            Refresh
          </Button>
        </div>
      </div>

      {/* Feedback Banner */}
      {feedback && (
        <div className={`p-4 rounded-xl border flex items-center justify-between gap-3 text-xs font-mono ${
          feedback.type === 'success'
            ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300'
            : 'bg-rose-500/10 border-rose-500/30 text-rose-300'
        }`}>
          <div className="flex items-center gap-2.5">
            {feedback.type === 'success' ? <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" /> : <AlertTriangle className="w-4 h-4 text-rose-400 shrink-0" />}
            <span>{feedback.message}</span>
          </div>
          <button onClick={() => setFeedback(null)} className="text-slate-400 hover:text-white">✕</button>
        </div>
      )}

      {/* Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <Card className="border-slate-800 bg-[#111622]/90">
          <div className="flex items-center justify-between">
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-400 font-mono">Active Faults</p>
            <Zap className={`w-5 h-5 ${activeCount > 0 ? 'text-amber-400 animate-pulse' : 'text-slate-500'}`} />
          </div>
          <h3 className={`text-2xl font-bold mt-1.5 font-mono ${activeCount > 0 ? 'text-amber-400' : 'text-white'}`}>
            {activeCount}
          </h3>
          <p className="text-[11px] text-slate-400 mt-2 font-mono">
            {activeCount > 0 ? 'Experiments currently running' : 'Infrastructure operating normally'}
          </p>
        </Card>

        <Card className="border-slate-800 bg-[#111622]/90">
          <div className="flex items-center justify-between">
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-400 font-mono">Total Experiments</p>
            <Activity className="w-5 h-5 text-slate-500" />
          </div>
          <h3 className="text-2xl font-bold mt-1.5 font-mono text-white">
            {totalCount}
          </h3>
          <p className="text-[11px] text-slate-400 mt-2 font-mono">Lifetime chaos runs executed</p>
        </Card>

        <Card className="border-slate-800 bg-[#111622]/90">
          <div className="flex items-center justify-between">
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-400 font-mono">Verified Recoveries</p>
            <ShieldCheck className="w-5 h-5 text-emerald-400" />
          </div>
          <h3 className="text-2xl font-bold mt-1.5 font-mono text-emerald-400">
            {successCount}
          </h3>
          <p className="text-[11px] text-slate-400 mt-2 font-mono">Failover & recovery completed</p>
        </Card>

        <Card className="border-slate-800 bg-[#111622]/90">
          <div className="flex items-center justify-between">
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-400 font-mono">Safety Policies</p>
            <ShieldAlert className="w-5 h-5 text-sky-400" />
          </div>
          <h3 className="text-sm font-bold mt-2 font-mono text-sky-400">
            MAX {policies?.maxDurationSeconds || 300}s / {policies?.maxConcurrentExperiments || 3} CONCURRENT
          </h3>
          <p className="text-[11px] text-slate-400 mt-2 font-mono">
            Auto-rollback & protected node bounds active
          </p>
        </Card>
      </div>

      {/* Active Experiments Live Control */}
      <Card
        title="Active Chaos Experiments"
        subtitle="Live running faults impacting telemetry, routing weights, and node health in real-time"
      >
        {activeExperiments.length === 0 ? (
          <div className="py-8 text-center text-slate-400 text-xs font-mono flex flex-col items-center gap-2">
            <ShieldCheck className="w-8 h-8 text-emerald-400/40" />
            <span>No chaos experiments currently running. All nodes operating under standard conditions.</span>
          </div>
        ) : (
          <div className="space-y-4">
            {activeExperiments.map((exp) => (
              <div
                key={exp.experimentId}
                className="p-4 rounded-xl bg-amber-500/5 border border-amber-500/30 flex flex-col md:flex-row md:items-center justify-between gap-4"
              >
                <div className="space-y-1.5 flex-1 min-w-0">
                  <div className="flex items-center gap-2.5 flex-wrap">
                    <span className="text-xs font-bold font-mono text-amber-400 px-2 py-0.5 rounded bg-amber-500/10 border border-amber-500/30">
                      {exp.experimentType}
                    </span>
                    <span className="font-mono text-sm font-bold text-white">{exp.experimentId}</span>
                    <span className="text-xs text-slate-400 font-mono">
                      Target: <span className="text-sky-400 font-bold">{exp.targetNodeId || exp.targetLinkId}</span>
                    </span>
                    <span className="text-xs text-slate-400 font-mono">
                      Severity: <span className="text-white font-bold">{exp.severity}</span>
                    </span>
                  </div>
                  <p className="text-[11px] text-slate-400 font-mono">
                    Started at: {new Date(exp.startedAt).toLocaleTimeString()} · Duration: {exp.durationSeconds}s
                  </p>
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  {canOperate && (
                    <>
                      <Button
                        variant="outline"
                        size="sm"
                        icon={Square}
                        onClick={() => handleStopExperiment(exp.experimentId)}
                      >
                        Stop Experiment
                      </Button>
                      <Button
                        variant="danger"
                        size="sm"
                        icon={RotateCcw}
                        onClick={() => handleRollbackExperiment(exp.experimentId)}
                      >
                        Emergency Rollback
                      </Button>
                    </>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>

      {/* Scenario Launcher */}
      <Card
        title="Failure Injection Scenario Launcher"
        subtitle="Select a failure scenario, configure parameters, and inject real internal stress into the cluster"
      >
        {/* Scenario Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3 mb-6">
          {SCENARIOS.map((scen) => {
            const Icon = scen.icon;
            const isSelected = selectedType === scen.type;
            return (
              <button
                key={scen.type}
                type="button"
                onClick={() => handleScenarioSelect(scen.type)}
                className={`text-left p-3.5 rounded-xl border transition-all ${
                  isSelected
                    ? `${scen.bg} ${scen.border} ring-1 ring-sky-500/50`
                    : 'bg-slate-950/60 border-slate-800 hover:border-slate-700'
                }`}
              >
                <div className="flex items-center gap-2.5 mb-2">
                  <div className={`p-1.5 rounded-lg ${scen.bg} ${scen.color}`}>
                    <Icon className="w-4 h-4" />
                  </div>
                  <span className="text-xs font-bold text-white font-mono">{scen.title}</span>
                </div>
                <p className="text-[11px] text-slate-400 line-clamp-2">{scen.desc}</p>
              </button>
            );
          })}
        </div>

        {/* Experiment Configuration Form */}
        <form onSubmit={handleStartExperiment} className="p-5 rounded-xl bg-slate-950/80 border border-slate-800 space-y-5">
          <div className="flex items-center justify-between border-b border-slate-800 pb-3">
            <div>
              <h4 className="text-sm font-bold text-white font-mono flex items-center gap-2">
                <Sliders className="w-4 h-4 text-sky-400" />
                Configure: <span className="text-sky-400">{currentScenario.title}</span>
              </h4>
              <p className="text-[11px] text-slate-400 font-mono mt-0.5">{currentScenario.desc}</p>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
            {/* Target Selector */}
            {selectedType === 'NETWORK_PARTITION' ? (
              <div>
                <label className="block text-xs font-mono uppercase tracking-wider text-slate-400 mb-1.5">
                  Target Link / Segment
                </label>
                <select
                  value={selectedLink}
                  onChange={(e) => setSelectedLink(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs font-mono text-white focus:outline-none focus:border-sky-500"
                >
                  {links.map((l) => (
                    <option key={l.linkId} value={l.linkId}>
                      {l.linkId} ({l.sourceNodeId} ↔ {l.targetNodeId})
                    </option>
                  ))}
                </select>
              </div>
            ) : (
              <div>
                <label className="block text-xs font-mono uppercase tracking-wider text-slate-400 mb-1.5">
                  Target Network Node
                </label>
                <select
                  value={selectedTarget}
                  onChange={(e) => setSelectedTarget(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs font-mono text-white focus:outline-none focus:border-sky-500"
                >
                  {nodes.map((n) => (
                    <option key={n.nodeId} value={n.nodeId}>
                      {n.nodeId} - {n.name} ({n.status})
                    </option>
                  ))}
                </select>
              </div>
            )}

            {/* Severity Input */}
            <div>
              <label className="block text-xs font-mono uppercase tracking-wider text-slate-400 mb-1.5">
                Fault Intensity / Severity ({currentScenario.unit})
              </label>
              <div className="flex items-center gap-3">
                <input
                  type="number"
                  min={currentScenario.min}
                  max={currentScenario.max}
                  step={currentScenario.step}
                  value={severity}
                  onChange={(e) => setSeverity(e.target.value)}
                  disabled={currentScenario.min === currentScenario.max}
                  className="w-24 bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs font-mono text-white focus:outline-none focus:border-sky-500"
                />
                {currentScenario.min !== currentScenario.max && (
                  <input
                    type="range"
                    min={currentScenario.min}
                    max={currentScenario.max}
                    step={currentScenario.step}
                    value={severity}
                    onChange={(e) => setSeverity(e.target.value)}
                    className="flex-1 accent-sky-500"
                  />
                )}
              </div>
            </div>

            {/* Duration Input */}
            <div>
              <label className="block text-xs font-mono uppercase tracking-wider text-slate-400 mb-1.5">
                Experiment Duration (Seconds)
              </label>
              <select
                value={duration}
                onChange={(e) => setDuration(Number(e.target.value))}
                className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs font-mono text-white focus:outline-none focus:border-sky-500"
              >
                <option value={15}>15 Seconds (Rapid Check)</option>
                <option value={30}>30 Seconds</option>
                <option value={45}>45 Seconds</option>
                <option value={60}>60 Seconds (1 Minute)</option>
                <option value={120}>120 Seconds (2 Minutes)</option>
                <option value={300}>300 Seconds (5 Minutes Max)</option>
              </select>
            </div>
          </div>

          <div className="flex items-center justify-between pt-3 border-t border-slate-800">
            <div className="text-[11px] text-slate-400 font-mono flex items-center gap-1.5">
              <Info className="w-3.5 h-3.5 text-sky-400" />
              <span>Fault automatically rolls back when duration expires. Real telemetry is broadcasted live.</span>
            </div>
            <Button
              type="submit"
              variant="danger"
              size="md"
              loading={submitting}
              disabled={!canOperate || !enabled}
              icon={Play}
            >
              {canOperate ? 'Inject Chaos Fault' : 'Operator Access Required'}
            </Button>
          </div>
        </form>
      </Card>

      {/* Experiment Explainability & History */}
      <Card
        title="Chaos Experiment History & Verification Audit"
        subtitle="Persistent audit records of failure injections, telemetry reactions, and failover outcomes"
      >
        {experiments.length === 0 ? (
          <div className="py-10 text-center text-slate-400 text-xs font-mono flex flex-col items-center gap-2">
            <Clock className="w-8 h-8 text-slate-600" />
            <span>No chaos experiments recorded yet.</span>
            <span className="text-slate-500">Inject a fault above to observe live detection, routing, and recovery.</span>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="text-[11px] uppercase tracking-wider text-slate-400 bg-slate-900/60 border-b border-slate-800 font-mono">
                <tr>
                  <th className="py-3 px-4">Experiment ID</th>
                  <th className="py-3 px-3">Scenario</th>
                  <th className="py-3 px-3">Target</th>
                  <th className="py-3 px-3">Severity</th>
                  <th className="py-3 px-3">Status</th>
                  <th className="py-3 px-3">Result</th>
                  <th className="py-3 px-4">Explainability & Observations</th>
                  <th className="py-3 px-3 text-right">Started</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-mono">
                {experiments.map((exp) => {
                  const badge = STATUS_BADGES[exp.status] || STATUS_BADGES.COMPLETED;
                  return (
                    <tr key={exp.experimentId} className="hover:bg-slate-900/40 transition-colors">
                      <td className="py-3 px-4 font-bold text-sky-400">{exp.experimentId}</td>
                      <td className="py-3 px-3">
                        <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-800 text-slate-300 border border-slate-700">
                          {exp.experimentType}
                        </span>
                      </td>
                      <td className="py-3 px-3 font-bold text-white">
                        {exp.targetNodeId || exp.targetLinkId || 'Cluster'}
                      </td>
                      <td className="py-3 px-3 text-slate-300">
                        {exp.severity != null ? exp.severity : '—'}
                      </td>
                      <td className="py-3 px-3">
                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold border ${badge.class}`}>
                          {badge.text}
                        </span>
                      </td>
                      <td className="py-3 px-3">
                        {exp.result === 'SUCCESS' && (
                          <span className="text-emerald-400 flex items-center gap-1 text-[10px] font-bold">
                            <CheckCircle2 className="w-3 h-3" /> SUCCESS
                          </span>
                        )}
                        {exp.result === 'PARTIAL_SUCCESS' && (
                          <span className="text-amber-400 flex items-center gap-1 text-[10px] font-bold">
                            <AlertTriangle className="w-3 h-3" /> PARTIAL
                          </span>
                        )}
                        {exp.result === 'FAILED' && (
                          <span className="text-rose-400 flex items-center gap-1 text-[10px] font-bold">
                            <XCircle className="w-3 h-3" /> FAILED
                          </span>
                        )}
                        {!exp.result && (
                          <span className="text-slate-400 text-[10px]">IN PROGRESS</span>
                        )}
                      </td>
                      <td className="py-3 px-4 text-[11px] text-slate-300 font-sans max-w-sm">
                        <p className="line-clamp-2">{exp.explanation || 'Fault injected. Monitoring real-time telemetry reaction...'}</p>
                      </td>
                      <td className="py-3 px-3 text-right text-slate-400 text-[10px] whitespace-nowrap">
                        {new Date(exp.startedAt).toLocaleTimeString()}
                      </td>
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

export default Chaos;
