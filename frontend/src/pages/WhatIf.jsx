import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  HelpCircle,
  Play,
  RotateCcw,
  Sparkles,
  ShieldAlert,
  ShieldCheck,
  Activity,
  Server,
  Zap,
  Cpu,
  Layers,
  ArrowRight,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Download,
  FileText,
  Clock,
  GitBranch,
  RefreshCw,
  Plus,
  Sliders,
  Eye,
  BarChart2,
  Share2,
  Trash2,
  Compass,
} from 'lucide-react';
import { whatIfService } from '../services/whatIfService';
import { nodeService } from '../services/nodeService';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { LoadingState } from '../components/common/LoadingState';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { useWebSocket } from '../context/WebSocketContext';
import { useAuth } from '../context/AuthContext';

// Scenario Type Color & Badge Map
const SCENARIO_TYPES = [
  { value: 'NODE_FAILURE', label: 'Node Failure', color: 'text-rose-400 bg-rose-500/10 border-rose-500/30' },
  { value: 'NODE_RECOVERY', label: 'Node Recovery', color: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/30' },
  { value: 'TRAFFIC_INCREASE', label: 'Traffic Surge', color: 'text-indigo-400 bg-indigo-500/10 border-indigo-500/30' },
  { value: 'TRAFFIC_DECREASE', label: 'Traffic Drop', color: 'text-blue-400 bg-blue-500/10 border-blue-500/30' },
  { value: 'CPU_SPIKE', label: 'CPU Spike', color: 'text-amber-400 bg-amber-500/10 border-amber-500/30' },
  { value: 'MEMORY_SPIKE', label: 'Memory Spike', color: 'text-purple-400 bg-purple-500/10 border-purple-500/30' },
  { value: 'LATENCY_INCREASE', label: 'Latency Increase', color: 'text-sky-400 bg-sky-500/10 border-sky-500/30' },
  { value: 'PACKET_LOSS_INCREASE', label: 'Packet Loss Surge', color: 'text-orange-400 bg-orange-500/10 border-orange-500/30' },
  { value: 'NETWORK_PARTITION', label: 'Network Partition', color: 'text-pink-400 bg-pink-500/10 border-pink-500/30' },
  { value: 'LINK_FAILURE', label: 'Link Failure', color: 'text-red-400 bg-red-500/10 border-red-500/30' },
  { value: 'ROUTING_STRATEGY_CHANGE', label: 'Routing Policy Switch', color: 'text-cyan-400 bg-cyan-500/10 border-cyan-500/30' },
  { value: 'CPU_SCHEDULER_CHANGE', label: 'CPU Scheduler Switch', color: 'text-violet-400 bg-violet-500/10 border-violet-500/30' },
  { value: 'COMBINED_SCENARIO', label: 'Compound Failure', color: 'text-yellow-400 bg-yellow-500/10 border-yellow-500/30' },
];

export const WhatIf = () => {
  const { user } = useAuth();
  const { whatIfUpdate } = useWebSocket();

  // Primary State
  const [snapshot, setSnapshot] = useState(null);
  const [templates, setTemplates] = useState([]);
  const [scenarios, setScenarios] = useState([]);
  const [activeScenario, setActiveScenario] = useState(null);
  const [latestRun, setLatestRun] = useState(null);
  const [comparison, setComparison] = useState(null);
  const [timeline, setTimeline] = useState([]);
  const [decisions, setDecisions] = useState([]);
  const [routingBenchmarks, setRoutingBenchmarks] = useState(null);
  const [schedulerBenchmarks, setSchedulerBenchmarks] = useState(null);

  // UI & Interaction State
  const [loading, setLoading] = useState(true);
  const [simulating, setSimulating] = useState(false);
  const [benchmarking, setBenchmarking] = useState(false);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('impact'); // 'impact', 'topology', 'explainability', 'benchmarks', 'history'
  const [showCustomModal, setShowCustomModal] = useState(false);
  const [selectedDecision, setSelectedDecision] = useState(null);

  // Custom Scenario Builder Form
  const [customForm, setCustomForm] = useState({
    title: '',
    description: '',
    scenarioType: 'NODE_FAILURE',
    virtualDurationSeconds: 60,
    changes: [
      { changeType: 'NODE_FAILURE', targetIdentifier: 'NODE-001', parameterValue: '100', description: 'Simulate node crash' },
    ],
  });

  // Load Initial Snapshot, Templates, Scenarios
  const loadInitialData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [snapRes, tempRes, scenRes] = await Promise.all([
        whatIfService.getSnapshot(),
        whatIfService.getTemplates(),
        whatIfService.getScenarios(),
      ]);
      setSnapshot(snapRes);
      setTemplates(tempRes || []);
      setScenarios(scenRes || []);

      if (scenRes && scenRes.length > 0) {
        // Load details of the most recent scenario
        const recent = scenRes[0];
        loadScenarioDetails(recent.scenarioId);
      }
    } catch (err) {
      console.error('Failed to load What-If simulator state:', err);
      setError('Failed to capture initial live snapshot. Please ensure backend is running.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadInitialData();
  }, [loadInitialData]);

  // Handle WebSocket updates
  useEffect(() => {
    if (!whatIfUpdate) return;
    if (activeScenario && whatIfUpdate.scenarioId === activeScenario.scenarioId) {
      if (whatIfUpdate.status === 'COMPLETED' || whatIfUpdate.status === 'FAILED') {
        setSimulating(false);
        loadScenarioDetails(activeScenario.scenarioId);
      }
    }
  }, [whatIfUpdate, activeScenario]);

  // Load Scenario Details & Run Result
  const loadScenarioDetails = async (scenarioId) => {
    try {
      const [scen, comp, time, dec] = await Promise.all([
        whatIfService.getScenarioById(scenarioId),
        whatIfService.getComparison(scenarioId).catch(() => null),
        whatIfService.getTimeline(scenarioId).catch(() => []),
        whatIfService.getDecisions(scenarioId).catch(() => []),
      ]);
      setActiveScenario(scen);
      setComparison(comp);
      setTimeline(time || []);
      setDecisions(dec || []);
    } catch (err) {
      console.error(`Failed to load scenario ${scenarioId}:`, err);
    }
  };

  // Run a Template Scenario
  const handleRunTemplate = async (template) => {
    try {
      setSimulating(true);
      setError(null);
      // 1. Create scenario from template
      const scenarioPayload = {
        title: template.title,
        description: template.description,
        scenarioType: template.scenarioType,
        virtualDurationSeconds: template.defaultVirtualDurationSeconds || 60,
        changes: template.changes || [],
      };
      const created = await whatIfService.createScenario(scenarioPayload);
      setActiveScenario(created);

      // 2. Run simulation immediately
      const runResult = await whatIfService.runSimulation(created.scenarioId);
      setLatestRun(runResult);

      // 3. Fetch comparison, timeline, decisions
      await loadScenarioDetails(created.scenarioId);

      // 4. Refresh scenarios list
      const updatedList = await whatIfService.getScenarios();
      setScenarios(updatedList);
    } catch (err) {
      console.error('Failed to execute template simulation:', err);
      setError(err.response?.data?.message || 'Simulation execution failed');
    } finally {
      setSimulating(false);
    }
  };

  // Run Custom Scenario
  const handleCreateAndRunCustom = async (e) => {
    e.preventDefault();
    try {
      setSimulating(true);
      setShowCustomModal(false);
      setError(null);

      const created = await whatIfService.createScenario(customForm);
      setActiveScenario(created);

      const runResult = await whatIfService.runSimulation(created.scenarioId);
      setLatestRun(runResult);

      await loadScenarioDetails(created.scenarioId);

      const updatedList = await whatIfService.getScenarios();
      setScenarios(updatedList);
    } catch (err) {
      console.error('Custom simulation failed:', err);
      setError(err.response?.data?.message || 'Failed to create and run custom scenario');
    } finally {
      setSimulating(false);
    }
  };

  // Re-run Active Scenario
  const handleRerunActive = async () => {
    if (!activeScenario) return;
    try {
      setSimulating(true);
      setError(null);
      const runResult = await whatIfService.runSimulation(activeScenario.scenarioId);
      setLatestRun(runResult);
      await loadScenarioDetails(activeScenario.scenarioId);
    } catch (err) {
      console.error('Failed to rerun simulation:', err);
      setError(err.response?.data?.message || 'Rerun failed');
    } finally {
      setSimulating(false);
    }
  };

  // Run Comparative Routing Benchmark
  const handleRunRoutingBenchmark = async () => {
    try {
      setBenchmarking(true);
      const result = await whatIfService.compareRouting({
        scenarioId: activeScenario?.scenarioId || null,
        simulatedTrafficSurgePercent: 25.0,
      });
      setRoutingBenchmarks(result);
    } catch (err) {
      console.error('Routing benchmark failed:', err);
      setError('Routing comparison benchmark failed');
    } finally {
      setBenchmarking(false);
    }
  };

  // Run Comparative CPU Scheduler Benchmark
  const handleRunSchedulerBenchmark = async () => {
    try {
      setBenchmarking(true);
      const result = await whatIfService.compareScheduler({
        scenarioId: activeScenario?.scenarioId || null,
        processCount: 16,
      });
      setSchedulerBenchmarks(result);
    } catch (err) {
      console.error('Scheduler benchmark failed:', err);
      setError('Scheduler comparison benchmark failed');
    } finally {
      setBenchmarking(false);
    }
  };

  // Export Report
  const handleExport = async (format) => {
    if (!activeScenario) return;
    try {
      const response = await whatIfService.exportReport(activeScenario.scenarioId, format);
      const blob = new Blob([response], { type: format === 'json' ? 'application/json' : 'text/csv' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `whatif-simulation-${activeScenario.scenarioId.toLowerCase()}.${format}`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err) {
      console.error('Export failed:', err);
    }
  };

  // Add Change item in Custom Scenario Modal
  const handleAddChange = () => {
    setCustomForm((prev) => ({
      ...prev,
      changes: [
        ...prev.changes,
        { changeType: 'TRAFFIC_INCREASE', targetIdentifier: 'SYSTEM', parameterValue: '30', description: 'Increase traffic' },
      ],
    }));
  };

  // Remove Change item in Custom Scenario Modal
  const handleRemoveChange = (index) => {
    setCustomForm((prev) => ({
      ...prev,
      changes: prev.changes.filter((_, i) => i !== index),
    }));
  };

  // Update Change item in Custom Scenario Modal
  const handleUpdateChange = (index, field, value) => {
    setCustomForm((prev) => {
      const updated = [...prev.changes];
      updated[index] = { ...updated[index], [field]: value };
      return { ...prev, changes: updated };
    });
  };

  // Helpers for formatting
  const getRiskBadge = (risk) => {
    switch (risk) {
      case 'CRITICAL':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-rose-500/20 text-rose-300 border border-rose-500/40 flex items-center gap-1"><ShieldAlert className="w-3.5 h-3.5 text-rose-400" /> CRITICAL RISK</span>;
      case 'HIGH':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-orange-500/20 text-orange-300 border border-orange-500/40 flex items-center gap-1"><AlertTriangle className="w-3.5 h-3.5 text-orange-400" /> HIGH RISK</span>;
      case 'MODERATE':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/40 flex items-center gap-1"><Activity className="w-3.5 h-3.5 text-amber-400" /> MODERATE RISK</span>;
      default:
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 flex items-center gap-1"><ShieldCheck className="w-3.5 h-3.5 text-emerald-400" /> LOW RISK</span>;
    }
  };

  const getDeltaBadge = (delta, unit = '%', invert = false) => {
    if (delta === 0 || delta === 0.0) return <span className="text-xs text-slate-400 font-mono">0.0{unit}</span>;
    const isPositive = delta > 0;
    const isGood = invert ? isPositive : !isPositive;
    const color = isGood ? 'text-emerald-400' : 'text-rose-400';
    return (
      <span className={`text-xs font-mono font-medium ${color}`}>
        {isPositive ? `+${delta.toFixed(1)}` : delta.toFixed(1)}{unit}
      </span>
    );
  };

  if (loading) {
    return <LoadingState message="Capturing point-in-time snapshot & initializing What-If engine..." />;
  }

  return (
    <div className="space-y-8 animate-fadeIn">
      {/* ── Header & Read-Only Safety Banner ── */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-3xl font-bold tracking-tight text-white flex items-center gap-3">
              <Sparkles className="w-8 h-8 text-cyan-400 animate-pulse" />
              What-If Infrastructure Simulator
            </h1>
            <span className="px-3 py-1 text-xs font-semibold uppercase tracking-wider rounded-full bg-cyan-500/10 text-cyan-400 border border-cyan-500/30">
              Scenario Analysis
            </span>
          </div>
          <p className="text-slate-400 mt-1 text-sm">
            Deterministic hypothetical simulation & Decision Explainability Engine. Zero live system mutations.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            size="sm"
            onClick={loadInitialData}
            className="flex items-center gap-2 border-slate-700 hover:border-cyan-500/50"
          >
            <RefreshCw className="w-4 h-4 text-slate-400" />
            Refresh Snapshot
          </Button>

          <Button
            variant="primary"
            size="sm"
            onClick={() => setShowCustomModal(true)}
            className="flex items-center gap-2 bg-gradient-to-r from-cyan-600 to-indigo-600 hover:from-cyan-500 hover:to-indigo-500 border-0"
          >
            <Plus className="w-4 h-4" />
            Build Custom Scenario
          </Button>
        </div>
      </div>

      {/* ── Absolute Read-Only Guarantee Alert ── */}
      <div className="p-3.5 rounded-xl bg-gradient-to-r from-cyan-950/40 via-slate-900 to-indigo-950/40 border border-cyan-500/30 flex items-center justify-between text-xs text-slate-300">
        <div className="flex items-center gap-3">
          <ShieldCheck className="w-5 h-5 text-cyan-400 flex-shrink-0" />
          <span>
            <strong className="text-cyan-300">Isolated Virtual Execution Guarantee:</strong> What-If simulations run on isolated in-memory clones with zero operational changes to live nodes, telemetry, traffic routes, or CPU workloads.
          </span>
        </div>
        <span className="text-xs font-mono text-cyan-400/80 hidden lg:inline">SNAPSHOT ID: {snapshot?.snapshotId || 'SNAP-ACTIVE'}</span>
      </div>

      {/* ── Error Banner ── */}
      {error && <ErrorMessage message={error} />}

      {/* ── Snapshot Metrics Overview Bar ── */}
      {snapshot && (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
          <Card className="bg-slate-900/60 border-slate-800 p-3.5 flex flex-col justify-between">
            <span className="text-xs text-slate-400 font-medium">Nodes (Healthy/Total)</span>
            <div className="flex items-baseline justify-between mt-1">
              <span className="text-xl font-bold text-white font-mono">
                {snapshot.healthyNodes} <span className="text-sm font-normal text-slate-500">/ {snapshot.totalNodes}</span>
              </span>
              <Server className="w-4 h-4 text-emerald-400" />
            </div>
          </Card>

          <Card className="bg-slate-900/60 border-slate-800 p-3.5 flex flex-col justify-between">
            <span className="text-xs text-slate-400 font-medium">Active Links</span>
            <div className="flex items-baseline justify-between mt-1">
              <span className="text-xl font-bold text-white font-mono">{snapshot.links?.length || 0}</span>
              <Share2 className="w-4 h-4 text-sky-400" />
            </div>
          </Card>

          <Card className="bg-slate-900/60 border-slate-800 p-3.5 flex flex-col justify-between">
            <span className="text-xs text-slate-400 font-medium">Avg CPU Load</span>
            <div className="flex items-baseline justify-between mt-1">
              <span className="text-xl font-bold text-white font-mono">{snapshot.averageCpu}%</span>
              <Cpu className="w-4 h-4 text-amber-400" />
            </div>
          </Card>

          <Card className="bg-slate-900/60 border-slate-800 p-3.5 flex flex-col justify-between">
            <span className="text-xs text-slate-400 font-medium">Avg Latency</span>
            <div className="flex items-baseline justify-between mt-1">
              <span className="text-xl font-bold text-white font-mono">{snapshot.averageLatency} <span className="text-xs text-slate-500">ms</span></span>
              <Clock className="w-4 h-4 text-indigo-400" />
            </div>
          </Card>

          <Card className="bg-slate-900/60 border-slate-800 p-3.5 flex flex-col justify-between">
            <span className="text-xs text-slate-400 font-medium">Avg Packet Loss</span>
            <div className="flex items-baseline justify-between mt-1">
              <span className="text-xl font-bold text-white font-mono">{snapshot.averagePacketLoss}%</span>
              <Activity className="w-4 h-4 text-rose-400" />
            </div>
          </Card>

          <Card className="bg-slate-900/60 border-slate-800 p-3.5 flex flex-col justify-between">
            <span className="text-xs text-slate-400 font-medium">Active Strategy</span>
            <div className="flex items-baseline justify-between mt-1">
              <span className="text-sm font-bold text-cyan-400 font-mono truncate">
                {snapshot.routingState?.activeStrategy || 'ADAPTIVE'}
              </span>
              <Compass className="w-4 h-4 text-cyan-400" />
            </div>
          </Card>
        </div>
      )}

      {/* ── Predefined Scenario Templates (Quick Start) ── */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold text-white flex items-center gap-2">
            <Sliders className="w-5 h-5 text-cyan-400" />
            Predefined Scenario Templates
          </h2>
          <span className="text-xs text-slate-400">Click any template to simulate instantly</span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
          {templates.map((tmpl) => (
            <div
              key={tmpl.templateId}
              onClick={() => !simulating && handleRunTemplate(tmpl)}
              className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-800 hover:border-cyan-500/40 hover:bg-slate-800/80 transition cursor-pointer group flex flex-col justify-between relative overflow-hidden"
            >
              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-mono px-2 py-0.5 rounded bg-slate-800 text-cyan-300 border border-slate-700">
                    {tmpl.scenarioType}
                  </span>
                  <Play className="w-3.5 h-3.5 text-slate-500 group-hover:text-cyan-400 group-hover:translate-x-0.5 transition" />
                </div>
                <h3 className="text-sm font-semibold text-white group-hover:text-cyan-300 transition line-clamp-1">
                  {tmpl.title}
                </h3>
                <p className="text-xs text-slate-400 line-clamp-2 leading-relaxed">
                  {tmpl.description}
                </p>
              </div>

              <div className="mt-3 pt-2.5 border-t border-slate-800/60 flex items-center justify-between text-xs text-slate-500">
                <span>{tmpl.changes?.length || 1} change(s)</span>
                <span className="text-cyan-400 font-mono font-medium flex items-center gap-1 group-hover:underline">
                  Simulate &rarr;
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* ── Active Simulation Banner & Controller ── */}
      {activeScenario && (
        <Card className="border-cyan-500/30 bg-slate-900/90 p-5">
          <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
            <div className="space-y-1">
              <div className="flex items-center gap-3">
                <span className="text-xs font-mono px-2.5 py-1 rounded bg-cyan-500/20 text-cyan-300 border border-cyan-500/40">
                  {activeScenario.scenarioId}
                </span>
                <h2 className="text-xl font-bold text-white">{activeScenario.title}</h2>
                <span className="text-xs px-2.5 py-0.5 rounded-full bg-slate-800 text-slate-300 font-mono">
                  {activeScenario.status}
                </span>
              </div>
              <p className="text-sm text-slate-400">{activeScenario.description}</p>
            </div>

            <div className="flex items-center gap-3">
              <Button
                variant="outline"
                size="sm"
                onClick={() => handleExport('json')}
                className="flex items-center gap-1.5 border-slate-700 hover:border-slate-600 text-slate-300"
              >
                <Download className="w-3.5 h-3.5" />
                JSON
              </Button>

              <Button
                variant="outline"
                size="sm"
                onClick={() => handleExport('csv')}
                className="flex items-center gap-1.5 border-slate-700 hover:border-slate-600 text-slate-300"
              >
                <FileText className="w-3.5 h-3.5" />
                CSV
              </Button>

              <Button
                variant="primary"
                size="sm"
                disabled={simulating}
                onClick={handleRerunActive}
                className="flex items-center gap-2 bg-cyan-600 hover:bg-cyan-500 border-0"
              >
                {simulating ? (
                  <>
                    <RefreshCw className="w-4 h-4 animate-spin" />
                    Simulating...
                  </>
                ) : (
                  <>
                    <RotateCcw className="w-4 h-4" />
                    Re-run Simulation
                  </>
                )}
              </Button>
            </div>
          </div>

          {/* Applied Hypothetical Changes Chips */}
          {activeScenario.changes && activeScenario.changes.length > 0 && (
            <div className="mt-4 pt-3 border-t border-slate-800 flex flex-wrap items-center gap-2">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Applied Changes:</span>
              {activeScenario.changes.map((ch, idx) => (
                <span
                  key={idx}
                  className="px-2.5 py-1 text-xs rounded-lg bg-slate-800/80 border border-slate-700 text-slate-300 font-mono flex items-center gap-1.5"
                >
                  <span className="text-cyan-400 font-semibold">{ch.changeType}</span>
                  <span className="text-slate-500">&bull;</span>
                  <span>{ch.targetIdentifier}</span>
                  {ch.parameterValue && <span className="text-amber-300">({ch.parameterValue})</span>}
                </span>
              ))}
            </div>
          )}
        </Card>
      )}

      {/* ── Main Tab Navigation ── */}
      <div className="flex border-b border-slate-800 gap-6">
        {[
          { id: 'impact', label: 'Impact & Delta Analysis', icon: BarChart2 },
          { id: 'topology', label: 'Topology Comparison', icon: GitBranch },
          { id: 'explainability', label: 'Decision Explainability ("Why?")', icon: HelpCircle },
          { id: 'benchmarks', label: 'Comparative Benchmarks', icon: Layers },
          { id: 'history', label: 'Scenario Archive', icon: Clock },
        ].map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`pb-3 text-sm font-medium flex items-center gap-2 transition border-b-2 ${
                isActive
                  ? 'border-cyan-400 text-cyan-400'
                  : 'border-transparent text-slate-400 hover:text-slate-200'
              }`}
            >
              <Icon className="w-4 h-4" />
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* ── TAB 1: Impact & Delta Analysis ── */}
      {activeTab === 'impact' && (
        <div className="space-y-6">
          {comparison ? (
            <>
              {/* Risk Banner */}
              <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Overall Impact Assessment:</span>
                    {getRiskBadge(comparison.riskLevel)}
                  </div>
                  <p className="text-sm text-slate-300 font-medium">{comparison.riskSummary}</p>
                  {comparison.riskRecommendation && (
                    <p className="text-xs text-cyan-300/90">&rarr; {comparison.riskRecommendation}</p>
                  )}
                </div>
                <div className="flex items-center gap-4 text-xs font-mono text-slate-400 bg-slate-950 p-3 rounded-lg border border-slate-800">
                  <div>
                    <span className="text-slate-500 block">FAILOVERS</span>
                    <span className="text-base font-bold text-rose-400">{comparison.failoverEventsCount}</span>
                  </div>
                  <div className="border-l border-slate-800 pl-4">
                    <span className="text-slate-500 block">DECISIONS</span>
                    <span className="text-base font-bold text-cyan-400">{comparison.routingDecisionsCount}</span>
                  </div>
                </div>
              </div>

              {/* Metric Delta Cards Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                {/* CPU Metric Card */}
                <Card className="bg-slate-900/80 border-slate-800 p-4 space-y-3">
                  <div className="flex items-center justify-between text-xs text-slate-400 font-medium">
                    <span className="flex items-center gap-1.5"><Cpu className="w-3.5 h-3.5 text-amber-400" /> Avg CPU Utilization</span>
                    {getDeltaBadge(comparison.metrics?.cpuUsageDelta, '%')}
                  </div>
                  <div className="flex items-baseline justify-between">
                    <div>
                      <span className="text-xs text-slate-500 block">Baseline</span>
                      <span className="text-lg font-bold font-mono text-slate-300">{comparison.baseline?.averageCpu}%</span>
                    </div>
                    <ArrowRight className="w-4 h-4 text-slate-600" />
                    <div className="text-right">
                      <span className="text-xs text-cyan-400 block">Simulated</span>
                      <span className="text-2xl font-bold font-mono text-white">{comparison.simulated?.averageCpu}%</span>
                    </div>
                  </div>
                </Card>

                {/* Memory Metric Card */}
                <Card className="bg-slate-900/80 border-slate-800 p-4 space-y-3">
                  <div className="flex items-center justify-between text-xs text-slate-400 font-medium">
                    <span className="flex items-center gap-1.5"><Server className="w-3.5 h-3.5 text-purple-400" /> Avg Memory Utilization</span>
                    {getDeltaBadge(comparison.metrics?.memoryUsageDelta, '%')}
                  </div>
                  <div className="flex items-baseline justify-between">
                    <div>
                      <span className="text-xs text-slate-500 block">Baseline</span>
                      <span className="text-lg font-bold font-mono text-slate-300">{comparison.baseline?.averageMemory}%</span>
                    </div>
                    <ArrowRight className="w-4 h-4 text-slate-600" />
                    <div className="text-right">
                      <span className="text-xs text-cyan-400 block">Simulated</span>
                      <span className="text-2xl font-bold font-mono text-white">{comparison.simulated?.averageMemory}%</span>
                    </div>
                  </div>
                </Card>

                {/* Latency Metric Card */}
                <Card className="bg-slate-900/80 border-slate-800 p-4 space-y-3">
                  <div className="flex items-center justify-between text-xs text-slate-400 font-medium">
                    <span className="flex items-center gap-1.5"><Clock className="w-3.5 h-3.5 text-sky-400" /> Avg Latency</span>
                    {getDeltaBadge(comparison.metrics?.latencyDelta, 'ms')}
                  </div>
                  <div className="flex items-baseline justify-between">
                    <div>
                      <span className="text-xs text-slate-500 block">Baseline</span>
                      <span className="text-lg font-bold font-mono text-slate-300">{comparison.baseline?.averageLatency} ms</span>
                    </div>
                    <ArrowRight className="w-4 h-4 text-slate-600" />
                    <div className="text-right">
                      <span className="text-xs text-cyan-400 block">Simulated</span>
                      <span className="text-2xl font-bold font-mono text-white">{comparison.simulated?.averageLatency} ms</span>
                    </div>
                  </div>
                </Card>

                {/* Packet Loss Metric Card */}
                <Card className="bg-slate-900/80 border-slate-800 p-4 space-y-3">
                  <div className="flex items-center justify-between text-xs text-slate-400 font-medium">
                    <span className="flex items-center gap-1.5"><Activity className="w-3.5 h-3.5 text-rose-400" /> Avg Packet Loss</span>
                    {getDeltaBadge(comparison.metrics?.packetLossDelta, '%')}
                  </div>
                  <div className="flex items-baseline justify-between">
                    <div>
                      <span className="text-xs text-slate-500 block">Baseline</span>
                      <span className="text-lg font-bold font-mono text-slate-300">{comparison.baseline?.averagePacketLoss}%</span>
                    </div>
                    <ArrowRight className="w-4 h-4 text-slate-600" />
                    <div className="text-right">
                      <span className="text-xs text-cyan-400 block">Simulated</span>
                      <span className="text-2xl font-bold font-mono text-white">{comparison.simulated?.averagePacketLoss}%</span>
                    </div>
                  </div>
                </Card>
              </div>

              {/* Node-by-Node Metric Breakdown Table */}
              <Card className="bg-slate-900/90 border-slate-800 p-5 space-y-4">
                <h3 className="text-sm font-semibold text-white flex items-center gap-2">
                  <Server className="w-4 h-4 text-cyan-400" />
                  Simulated Node Telemetry State
                </h3>

                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs text-slate-300">
                    <thead className="bg-slate-950/60 text-slate-400 font-mono uppercase border-b border-slate-800">
                      <tr>
                        <th className="py-2.5 px-3">Node ID</th>
                        <th className="py-2.5 px-3">Status</th>
                        <th className="py-2.5 px-3">Simulated CPU</th>
                        <th className="py-2.5 px-3">Simulated Memory</th>
                        <th className="py-2.5 px-3">Simulated Latency</th>
                        <th className="py-2.5 px-3">Packet Loss</th>
                        <th className="py-2.5 px-3">Active Conns</th>
                        <th className="py-2.5 px-3">Health Score</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800/60 font-mono">
                      {comparison.simulated?.nodes?.map((node) => {
                        const tel = comparison.simulated?.telemetry?.find((t) => t.nodeId === node.nodeId);
                        const health = comparison.simulated?.health?.find((h) => h.nodeId === node.nodeId);
                        const isFailed = node.status === 'FAILED' || node.status === 'OFFLINE';

                        return (
                          <tr key={node.nodeId} className={isFailed ? 'bg-rose-950/20 text-rose-200' : 'hover:bg-slate-800/40'}>
                            <td className="py-3 px-3 font-semibold text-white flex items-center gap-2">
                              {isFailed ? <XCircle className="w-3.5 h-3.5 text-rose-400" /> : <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />}
                              {node.nodeId} ({node.name})
                            </td>
                            <td className="py-3 px-3">
                              <span className={`px-2 py-0.5 rounded text-[11px] ${
                                isFailed ? 'bg-rose-500/20 text-rose-300 border border-rose-500/40' : 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40'
                              }`}>
                                {node.status}
                              </span>
                            </td>
                            <td className="py-3 px-3">{tel ? `${tel.cpuUsage.toFixed(1)}%` : '—'}</td>
                            <td className="py-3 px-3">{tel ? `${tel.memoryUsage.toFixed(1)}%` : '—'}</td>
                            <td className="py-3 px-3">{tel ? `${tel.latency.toFixed(1)} ms` : '—'}</td>
                            <td className="py-3 px-3">{tel ? `${tel.packetLoss.toFixed(1)}%` : '—'}</td>
                            <td className="py-3 px-3">{tel ? tel.activeConnections : '—'}</td>
                            <td className="py-3 px-3">
                              <span className={`font-bold ${
                                (health?.healthScore || 0) >= 80 ? 'text-emerald-400' : (health?.healthScore || 0) >= 50 ? 'text-amber-400' : 'text-rose-400'
                              }`}>
                                {health ? `${health.healthScore.toFixed(0)} / 100` : '—'}
                              </span>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </Card>
            </>
          ) : (
            <div className="text-center py-16 bg-slate-900/40 rounded-xl border border-dashed border-slate-800 space-y-3">
              <Sparkles className="w-10 h-10 text-slate-600 mx-auto" />
              <h3 className="text-base font-semibold text-slate-300">No Simulation Executed Yet</h3>
              <p className="text-sm text-slate-500 max-w-md mx-auto">
                Select a scenario template from the top bar or create a custom hypothetical change set to begin.
              </p>
            </div>
          )}
        </div>
      )}

      {/* ── TAB 2: Topology Comparison (Baseline vs Simulated) ── */}
      {activeTab === 'topology' && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Baseline Topology */}
          <Card className="bg-slate-900/90 border-slate-800 p-5 space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-base font-semibold text-white flex items-center gap-2">
                <Server className="w-4 h-4 text-emerald-400" />
                Baseline Topology (Live Snapshot)
              </h3>
              <span className="text-xs font-mono px-2 py-0.5 rounded bg-slate-800 text-slate-400">
                {snapshot?.totalNodes || 0} Nodes
              </span>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              {snapshot?.nodes?.map((n) => (
                <div key={n.nodeId} className="p-3 rounded-lg bg-slate-950 border border-slate-800 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="font-mono text-xs font-bold text-white">{n.nodeId}</span>
                    <span className="w-2 h-2 rounded-full bg-emerald-400"></span>
                  </div>
                  <div className="text-[11px] text-slate-400 truncate">{n.name}</div>
                  <div className="text-[10px] text-slate-500 font-mono">{n.host}:{n.port}</div>
                </div>
              ))}
            </div>

            <div className="pt-3 border-t border-slate-800 text-xs text-slate-400 space-y-1">
              <span className="font-semibold text-slate-300">Topology Links:</span>
              <div className="flex flex-wrap gap-2 mt-1">
                {snapshot?.links?.map((l) => (
                  <span key={l.linkId} className="px-2 py-0.5 rounded bg-slate-800/80 text-[11px] font-mono text-slate-400">
                    {l.sourceNodeId} &harr; {l.targetNodeId} ({l.bandwidth} Mbps)
                  </span>
                ))}
              </div>
            </div>
          </Card>

          {/* Simulated Topology */}
          <Card className="bg-slate-900/90 border-cyan-500/30 p-5 space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-base font-semibold text-cyan-300 flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-cyan-400" />
                Hypothetical Simulated State
              </h3>
              <span className="text-xs font-mono px-2 py-0.5 rounded bg-cyan-500/20 text-cyan-300">
                {comparison?.simulated?.totalNodes || 0} Nodes
              </span>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              {comparison?.simulated?.nodes?.map((n) => {
                const isDown = n.status === 'FAILED' || n.status === 'OFFLINE';
                return (
                  <div
                    key={n.nodeId}
                    className={`p-3 rounded-lg border transition space-y-1 ${
                      isDown
                        ? 'bg-rose-950/40 border-rose-500/50 text-rose-200'
                        : 'bg-slate-950 border-slate-800 text-slate-200'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-mono text-xs font-bold text-white">{n.nodeId}</span>
                      <span className={`w-2 h-2 rounded-full ${isDown ? 'bg-rose-500 animate-ping' : 'bg-emerald-400'}`}></span>
                    </div>
                    <div className="text-[11px] text-slate-400 truncate">{n.name}</div>
                    <div className={`text-[10px] font-mono font-semibold ${isDown ? 'text-rose-400' : 'text-emerald-400'}`}>
                      {n.status}
                    </div>
                  </div>
                );
              }) || (
                <div className="col-span-3 text-center py-8 text-xs text-slate-500">
                  Run a simulation to observe hypothetical topological impacts.
                </div>
              )}
            </div>

            <div className="pt-3 border-t border-slate-800 text-xs text-slate-400 space-y-1">
              <span className="font-semibold text-slate-300">Simulated Link Connectivity:</span>
              <div className="flex flex-wrap gap-2 mt-1">
                {comparison?.simulated?.links?.map((l) => (
                  <span
                    key={l.linkId}
                    className={`px-2 py-0.5 rounded text-[11px] font-mono ${
                      l.status === 'DOWN'
                        ? 'bg-rose-500/20 text-rose-300 border border-rose-500/40 line-through'
                        : 'bg-slate-800 text-slate-300'
                    }`}
                  >
                    {l.sourceNodeId} &harr; {l.targetNodeId} ({l.status})
                  </span>
                ))}
              </div>
            </div>
          </Card>
        </div>
      )}

      {/* ── TAB 3: Decision Explainability ("Why?") ── */}
      {activeTab === 'explainability' && (
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-base font-semibold text-white flex items-center gap-2">
                <HelpCircle className="w-5 h-5 text-cyan-400" />
                Adaptive Decision Explainability Engine
              </h3>
              <p className="text-xs text-slate-400 mt-0.5">
                Full transparency into composite cost scores, rejected alternative candidates, and explicit routing rationales.
              </p>
            </div>
            <span className="text-xs font-mono text-slate-400 bg-slate-900 px-3 py-1 rounded border border-slate-800">
              {decisions.length} Decisions Logged
            </span>
          </div>

          {decisions.length > 0 ? (
            <div className="space-y-4">
              {decisions.map((dec) => (
                <Card
                  key={dec.decisionId}
                  className="bg-slate-900/90 border-slate-800 p-4 hover:border-slate-700 transition space-y-3"
                >
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-800/80 pb-3">
                    <div className="flex items-center gap-3">
                      <span className="text-xs font-mono px-2.5 py-0.5 rounded bg-cyan-500/10 text-cyan-400 border border-cyan-500/30">
                        {dec.decisionType}
                      </span>
                      <span className="text-xs font-mono text-slate-400">T+{dec.virtualTimestampMs}ms</span>
                    </div>

                    <div className="flex items-center gap-2">
                      <span className="text-xs text-slate-400">Selected Candidate:</span>
                      <span className="text-xs font-mono font-bold text-emerald-400 bg-emerald-500/10 px-2.5 py-1 rounded border border-emerald-500/30">
                        {dec.selectedNodeId || 'N/A'} (Score: {dec.compositeScore?.toFixed(2) || '0.00'})
                      </span>
                    </div>
                  </div>

                  {/* Why Rationale text */}
                  <div className="p-3 rounded-lg bg-slate-950 border border-slate-800/80 text-xs text-slate-300 leading-relaxed font-sans">
                    <strong className="text-cyan-300 font-medium">Why this decision was made: </strong>
                    {dec.selectionRationale}
                  </div>

                  {/* Candidate Evaluations Breakdown Table */}
                  {dec.candidateEvaluations && dec.candidateEvaluations.length > 0 && (
                    <div className="space-y-2 pt-1">
                      <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                        Evaluated Candidate Pool ({dec.candidateEvaluations.length} evaluated):
                      </span>
                      <div className="overflow-x-auto">
                        <table className="w-full text-left text-xs font-mono">
                          <thead className="bg-slate-950 text-slate-400 uppercase border-b border-slate-800">
                            <tr>
                              <th className="py-2 px-3">Candidate Node</th>
                              <th className="py-2 px-3">Candidate CPU</th>
                              <th className="py-2 px-3">Memory</th>
                              <th className="py-2 px-3">Latency</th>
                              <th className="py-2 px-3">Loss</th>
                              <th className="py-2 px-3">Score</th>
                              <th className="py-2 px-3">Status / Rejection Reason</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-slate-800">
                            {dec.candidateEvaluations.map((cand, cIdx) => (
                              <tr
                                key={cIdx}
                                className={cand.selected ? 'bg-emerald-950/20 text-emerald-300' : 'text-slate-400 hover:bg-slate-800/30'}
                              >
                                <td className="py-2 px-3 font-semibold flex items-center gap-1.5">
                                  {cand.selected ? (
                                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                                  ) : (
                                    <XCircle className="w-3.5 h-3.5 text-slate-500" />
                                  )}
                                  {cand.nodeId}
                                </td>
                                <td className="py-2 px-3">{cand.cpuUsage?.toFixed(1)}%</td>
                                <td className="py-2 px-3">{cand.memoryUsage?.toFixed(1)}%</td>
                                <td className="py-2 px-3">{cand.latency?.toFixed(1)} ms</td>
                                <td className="py-2 px-3">{cand.packetLoss?.toFixed(1)}%</td>
                                <td className="py-2 px-3 font-bold text-white">{cand.compositeScore?.toFixed(2)}</td>
                                <td className="py-2 px-3">
                                  {cand.selected ? (
                                    <span className="text-emerald-400 font-semibold">SELECTED BEST FIT</span>
                                  ) : (
                                    <span className="text-rose-400/90">{cand.rejectionReason || 'Suboptimal score'}</span>
                                  )}
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  )}
                </Card>
              ))}
            </div>
          ) : (
            <div className="text-center py-16 bg-slate-900/40 rounded-xl border border-dashed border-slate-800 text-slate-500 text-sm">
              No decisions recorded. Run a simulation scenario with traffic routing or failover to view decision explainability trees.
            </div>
          )}
        </div>
      )}

      {/* ── TAB 4: Comparative Benchmarks ── */}
      {activeTab === 'benchmarks' && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Routing Strategies Benchmark */}
            <Card className="bg-slate-900/90 border-slate-800 p-5 space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-base font-semibold text-white flex items-center gap-2">
                    <Compass className="w-4 h-4 text-cyan-400" />
                    Routing Strategy Showdown
                  </h3>
                  <p className="text-xs text-slate-400 mt-0.5">
                    Execute all 6 routing algorithms against identical system snapshot.
                  </p>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={benchmarking}
                  onClick={handleRunRoutingBenchmark}
                  className="border-cyan-500/40 text-cyan-300 hover:bg-cyan-500/10"
                >
                  {benchmarking ? 'Evaluating...' : 'Run Benchmark'}
                </Button>
              </div>

              {routingBenchmarks ? (
                <div className="space-y-3">
                  <div className="p-3 rounded bg-emerald-950/20 border border-emerald-500/30 text-xs text-emerald-300">
                    <strong>Recommended Strategy: </strong>
                    {routingBenchmarks.recommendedStrategy} &mdash; {routingBenchmarks.recommendationReason}
                  </div>

                  <div className="space-y-2">
                    {routingBenchmarks.comparisons?.map((rc) => (
                      <div
                        key={rc.strategy}
                        className={`p-3 rounded-lg border flex items-center justify-between text-xs font-mono ${
                          rc.strategy === routingBenchmarks.recommendedStrategy
                            ? 'bg-slate-850 border-cyan-500/50 text-white'
                            : 'bg-slate-950 border-slate-800 text-slate-300'
                        }`}
                      >
                        <div>
                          <span className="font-bold text-cyan-300 block">{rc.strategy}</span>
                          <span className="text-slate-400 text-[11px]">{rc.notes}</span>
                        </div>
                        <div className="text-right">
                          <span className="text-slate-400 block text-[10px]">AVG LATENCY / SCORE</span>
                          <span className="font-bold text-white">{rc.averageLatency?.toFixed(1)} ms | {rc.efficiencyScore?.toFixed(0)} pts</span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : (
                <div className="text-center py-10 text-xs text-slate-500">
                  Click &ldquo;Run Benchmark&rdquo; to compare Adaptive, Round Robin, Least Connections, Least Load, Latency-Aware, and Weighted strategies.
                </div>
              )}
            </Card>

            {/* CPU Schedulers Benchmark */}
            <Card className="bg-slate-900/90 border-slate-800 p-5 space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-base font-semibold text-white flex items-center gap-2">
                    <Cpu className="w-4 h-4 text-violet-400" />
                    OS CPU Schedulers Showdown
                  </h3>
                  <p className="text-xs text-slate-400 mt-0.5">
                    Compare FCFS, SJF, SRTF, Priority, Round Robin, MLQ, and CFS.
                  </p>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={benchmarking}
                  onClick={handleRunSchedulerBenchmark}
                  className="border-violet-500/40 text-violet-300 hover:bg-violet-500/10"
                >
                  {benchmarking ? 'Evaluating...' : 'Run Benchmark'}
                </Button>
              </div>

              {schedulerBenchmarks ? (
                <div className="space-y-3">
                  <div className="p-3 rounded bg-violet-950/20 border border-violet-500/30 text-xs text-violet-300">
                    <strong>Optimal Scheduler: </strong>
                    {schedulerBenchmarks.recommendedAlgorithm} &mdash; {schedulerBenchmarks.recommendationReason}
                  </div>

                  <div className="space-y-2">
                    {schedulerBenchmarks.comparisons?.map((sc) => (
                      <div
                        key={sc.algorithm}
                        className={`p-3 rounded-lg border flex items-center justify-between text-xs font-mono ${
                          sc.algorithm === schedulerBenchmarks.recommendedAlgorithm
                            ? 'bg-slate-850 border-violet-500/50 text-white'
                            : 'bg-slate-950 border-slate-800 text-slate-300'
                        }`}
                      >
                        <div>
                          <span className="font-bold text-violet-300 block">{sc.algorithm}</span>
                          <span className="text-slate-400 text-[11px]">{sc.notes}</span>
                        </div>
                        <div className="text-right">
                          <span className="text-slate-400 block text-[10px]">AVG WAIT / TURNAROUND</span>
                          <span className="font-bold text-white">{sc.avgWaitingTime?.toFixed(1)}ms | {sc.avgTurnaroundTime?.toFixed(1)}ms</span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : (
                <div className="text-center py-10 text-xs text-slate-500">
                  Click &ldquo;Run Benchmark&rdquo; to simulate OS process scheduling metrics across all 7 algorithms.
                </div>
              )}
            </Card>
          </div>
        </div>
      )}

      {/* ── TAB 5: Scenario Archive / History ── */}
      {activeTab === 'history' && (
        <Card className="bg-slate-900/90 border-slate-800 p-5 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-base font-semibold text-white flex items-center gap-2">
              <Clock className="w-4 h-4 text-cyan-400" />
              What-If Simulation Archive
            </h3>
            <span className="text-xs font-mono text-slate-400">{scenarios.length} Scenarios</span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-300">
              <thead className="bg-slate-950 text-slate-400 font-mono uppercase border-b border-slate-800">
                <tr>
                  <th className="py-2.5 px-3">Scenario ID</th>
                  <th className="py-2.5 px-3">Title</th>
                  <th className="py-2.5 px-3">Type</th>
                  <th className="py-2.5 px-3">Status</th>
                  <th className="py-2.5 px-3">Changes</th>
                  <th className="py-2.5 px-3">Created</th>
                  <th className="py-2.5 px-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800 font-mono">
                {scenarios.map((sc) => (
                  <tr key={sc.scenarioId} className="hover:bg-slate-800/40">
                    <td className="py-3 px-3 font-bold text-cyan-400">{sc.scenarioId}</td>
                    <td className="py-3 px-3 font-sans text-white">{sc.title}</td>
                    <td className="py-3 px-3">
                      <span className="px-2 py-0.5 rounded bg-slate-800 text-slate-300">{sc.scenarioType}</span>
                    </td>
                    <td className="py-3 px-3">
                      <span className={`px-2 py-0.5 rounded text-[11px] ${
                        sc.status === 'COMPLETED' ? 'bg-emerald-500/20 text-emerald-300' : 'bg-slate-800 text-slate-400'
                      }`}>
                        {sc.status}
                      </span>
                    </td>
                    <td className="py-3 px-3">{sc.changes?.length || 0}</td>
                    <td className="py-3 px-3 text-slate-500">
                      {sc.createdAt ? new Date(sc.createdAt).toLocaleTimeString() : '—'}
                    </td>
                    <td className="py-3 px-3 text-right space-x-2">
                      <Button
                        variant="ghost"
                        size="xs"
                        onClick={() => {
                          loadScenarioDetails(sc.scenarioId);
                          setActiveTab('impact');
                        }}
                        className="text-cyan-400 hover:text-cyan-300"
                      >
                        Inspect
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* ── Custom Scenario Builder Modal ── */}
      {showCustomModal && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4 animate-fadeIn">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-2xl max-h-[90vh] overflow-y-auto p-6 space-y-6 shadow-2xl">
            <div className="flex items-center justify-between border-b border-slate-800 pb-4">
              <div className="flex items-center gap-3">
                <Sparkles className="w-6 h-6 text-cyan-400" />
                <h3 className="text-lg font-bold text-white">Build Hypothetical What-If Scenario</h3>
              </div>
              <button
                onClick={() => setShowCustomModal(false)}
                className="text-slate-400 hover:text-white text-lg font-bold p-1"
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleCreateAndRunCustom} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
                  Scenario Title
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Node 02 Outage + 40% Peak Traffic Surge"
                  value={customForm.title}
                  onChange={(e) => setCustomForm({ ...customForm, title: e.target.value })}
                  className="w-full px-3.5 py-2.5 rounded-lg bg-slate-950 border border-slate-800 text-white text-sm focus:border-cyan-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
                  Description / Hypothesis
                </label>
                <textarea
                  rows={2}
                  placeholder="Explain the hypothesis being tested..."
                  value={customForm.description}
                  onChange={(e) => setCustomForm({ ...customForm, description: e.target.value })}
                  className="w-full px-3.5 py-2 rounded-lg bg-slate-950 border border-slate-800 text-white text-sm focus:border-cyan-500 focus:outline-none"
                />
              </div>

              {/* Changes Builder */}
              <div className="space-y-3 pt-2">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-semibold text-slate-300 uppercase tracking-wider">
                    Hypothetical Change Set ({customForm.changes.length})
                  </label>
                  <Button
                    type="button"
                    variant="outline"
                    size="xs"
                    onClick={handleAddChange}
                    className="flex items-center gap-1 text-cyan-400 border-cyan-500/30"
                  >
                    <Plus className="w-3.5 h-3.5" /> Add Change
                  </Button>
                </div>

                <div className="space-y-3 max-h-56 overflow-y-auto pr-1">
                  {customForm.changes.map((ch, idx) => (
                    <div
                      key={idx}
                      className="p-3 rounded-lg bg-slate-950 border border-slate-800 space-y-2 relative"
                    >
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-mono text-cyan-400 font-semibold">Change #{idx + 1}</span>
                        {customForm.changes.length > 1 && (
                          <button
                            type="button"
                            onClick={() => handleRemoveChange(idx)}
                            className="text-rose-400 hover:text-rose-300 text-xs flex items-center gap-1"
                          >
                            <Trash2 className="w-3 h-3" /> Remove
                          </button>
                        )}
                      </div>

                      <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                        <select
                          value={ch.changeType}
                          onChange={(e) => handleUpdateChange(idx, 'changeType', e.target.value)}
                          className="px-2.5 py-1.5 rounded bg-slate-900 border border-slate-700 text-white text-xs"
                        >
                          {SCENARIO_TYPES.map((st) => (
                            <option key={st.value} value={st.value}>
                              {st.label}
                            </option>
                          ))}
                        </select>

                        <input
                          type="text"
                          placeholder="Target (e.g. NODE-001)"
                          value={ch.targetIdentifier}
                          onChange={(e) => handleUpdateChange(idx, 'targetIdentifier', e.target.value)}
                          className="px-2.5 py-1.5 rounded bg-slate-900 border border-slate-700 text-white text-xs"
                        />

                        <input
                          type="text"
                          placeholder="Param (e.g. 50, 200ms)"
                          value={ch.parameterValue}
                          onChange={(e) => handleUpdateChange(idx, 'parameterValue', e.target.value)}
                          className="px-2.5 py-1.5 rounded bg-slate-900 border border-slate-700 text-white text-xs"
                        />
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setShowCustomModal(false)}
                  className="border-slate-700 text-slate-300"
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  variant="primary"
                  size="sm"
                  className="bg-cyan-600 hover:bg-cyan-500 border-0 flex items-center gap-2"
                >
                  <Play className="w-4 h-4" /> Run What-If Simulation
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default WhatIf;
