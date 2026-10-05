import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  GitFork,
  Server,
  Play,
  Activity,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  Clock,
  RefreshCw,
  Cpu,
  HardDrive,
  Zap,
  Sliders,
  ShieldCheck,
  ShieldAlert,
  ArrowRight,
  ListOrdered,
  Layers,
  Scale,
  Gauge,
  Network,
} from 'lucide-react';
import { routingService } from '../services/routingService';
import { nodeService } from '../services/nodeService';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { LoadingState } from '../components/common/LoadingState';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { useWebSocket } from '../context/WebSocketContext';
import { useAuth } from '../context/AuthContext';

const STRATEGY_DESCRIPTIONS = {
  ADAPTIVE: 'Composite scoring combining CPU, memory, latency, packet loss, connections, and health penalties.',
  ROUND_ROBIN: 'Sequential deterministic distribution across all healthy eligible nodes.',
  LEAST_CONNECTIONS: 'Dispatches traffic to the eligible node with the lowest active connection load.',
  LEAST_LOAD: 'Normalizes and minimizes overall CPU, memory, and connection resource utilization.',
  LATENCY_AWARE: 'Prioritizes the eligible node with the lowest measured round-trip network latency.',
  WEIGHTED: 'Smooth weighted deficit scheduling proportional to configured node bandwidth capacity.',
};

export const Routing = () => {
  const [status, setStatus] = useState(null);
  const [candidates, setCandidates] = useState([]);
  const [lastDecision, setLastDecision] = useState(null);
  const [history, setHistory] = useState([]);
  const [selectedStrategy, setSelectedStrategy] = useState('ADAPTIVE');
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState(null);

  const { connectionStatus, routingUpdate } = useWebSocket();
  const { isOperator, isAdmin } = useAuth();

  const loadRoutingData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const [statusRes, candidatesRes, historyRes] = await Promise.all([
        routingService.getStatus().catch(() => ({ data: null })),
        routingService.getCandidates().catch(() => ({ data: [] })),
        routingService.getHistory(20).catch(() => ({ data: [] })),
      ]);

      if (statusRes?.data) {
        setStatus(statusRes.data);
        if (statusRes.data.activeStrategy) {
          setSelectedStrategy(statusRes.data.activeStrategy);
        }
      }

      if (candidatesRes?.data) {
        setCandidates(candidatesRes.data);
      }

      if (historyRes?.data) {
        setHistory(historyRes.data);
        if (historyRes.data.length > 0 && !lastDecision) {
          const latest = historyRes.data[0];
          setLastDecision({
            requestId: latest.requestId,
            strategy: latest.strategy,
            status: latest.selectedNodeId ? 'SUCCESS' : 'NO_ELIGIBLE_NODE',
            selectedNodeId: latest.selectedNodeId,
            selectedNodeName: latest.selectedNodeName,
            totalCandidates: latest.totalCandidates,
            eligibleCandidates: latest.eligibleCandidates,
            score: latest.score,
            decisionReason: latest.decisionReason,
            timestamp: latest.timestamp,
            evaluatedCandidates: candidatesRes?.data || [],
          });
        }
      }
    } catch (err) {
      setError(err.message || 'Failed to load traffic routing data');
    } finally {
      setLoading(false);
    }
  }, [lastDecision]);

  useEffect(() => {
    loadRoutingData();
  }, [loadRoutingData]);

  // Listen to live WebSocket routing decision broadcasts
  useEffect(() => {
    if (routingUpdate) {
      setLastDecision(routingUpdate);
      setHistory((prev) => [routingUpdate, ...prev.filter((h) => h.requestId !== routingUpdate.requestId)].slice(0, 30));
      // Refresh candidates evaluation
      routingService.getCandidates().then((res) => {
        if (res?.data) setCandidates(res.data);
      }).catch(() => {});
    }
  }, [routingUpdate]);

  const handleUpdateStrategy = async (newStrat) => {
    try {
      setSelectedStrategy(newStrat);
      await routingService.updateStrategy(newStrat);
      loadRoutingData();
    } catch (err) {
      alert('Failed to update active strategy: ' + err.message);
    }
  };

  const handleExecuteRouting = async () => {
    try {
      setActionLoading(true);
      const res = await routingService.executeRoutingRequest(selectedStrategy);
      if (res.data) {
        setLastDecision(res.data);
        setHistory((prev) => [res.data, ...prev].slice(0, 30));
        // Refresh candidates
        const cRes = await routingService.getCandidates();
        if (cRes?.data) setCandidates(cRes.data);
      }
    } catch (err) {
      alert('Failed to execute routing decision: ' + err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleDryRunDecide = async () => {
    try {
      setActionLoading(true);
      const res = await routingService.decideRouting(selectedStrategy);
      if (res.data) {
        setLastDecision(res.data);
      }
    } catch (err) {
      alert('Failed to evaluate routing: ' + err.message);
    } finally {
      setActionLoading(false);
    }
  };

  if (loading && !status) {
    return <LoadingState message="Initializing adaptive traffic routing engine..." />;
  }

  if (error) {
    return <ErrorMessage message={error} onRetry={loadRoutingData} />;
  }

  const eligibleCount = candidates.filter((c) => c.eligible).length;

  return (
    <div className="space-y-8">
      {/* Top Header & Strategy Selection Header */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 p-5 rounded-2xl bg-slate-900/80 border border-slate-800/80 shadow-lg">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold tracking-tight text-white">Adaptive Traffic Routing Console</h1>
            <span className={`text-[10px] font-mono px-2.5 py-0.5 rounded-full font-bold border ${
              connectionStatus === 'CONNECTED'
                ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                : 'bg-rose-500/10 text-rose-400 border-rose-500/30'
            }`}>
              {connectionStatus === 'CONNECTED' ? '● ENGINE ONLINE' : connectionStatus}
            </span>
          </div>
          <p className="text-xs text-slate-400 font-mono mt-1">
            Real-Time Telemetry Consumption &bull; Multi-Strategy Dispatching &bull; Explainable Decisions
          </p>
        </div>

        {/* Strategy Controls */}
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2">
            <label className="text-xs text-slate-400 font-mono">STRATEGY:</label>
            <select
              value={selectedStrategy}
              onChange={(e) => isOperator ? handleUpdateStrategy(e.target.value) : setSelectedStrategy(e.target.value)}
              className="bg-slate-950 border border-slate-700 text-xs text-white rounded-lg px-3 py-1.5 font-mono focus:outline-none focus:border-sky-500"
            >
              <option value="ADAPTIVE">ADAPTIVE (Multi-Metric)</option>
              <option value="ROUND_ROBIN">ROUND ROBIN</option>
              <option value="LEAST_CONNECTIONS">LEAST CONNECTIONS</option>
              <option value="LEAST_LOAD">LEAST LOAD</option>
              <option value="LATENCY_AWARE">LATENCY AWARE</option>
              <option value="WEIGHTED">WEIGHTED CAPACITY</option>
            </select>
          </div>

          {isOperator && (
            <Button
              variant="primary"
              size="sm"
              onClick={handleExecuteRouting}
              disabled={actionLoading}
              icon={Play}
            >
              {actionLoading ? 'Dispatching...' : 'Dispatch Traffic Request'}
            </Button>
          )}

          <Button
            variant="outline"
            size="sm"
            onClick={handleDryRunDecide}
            disabled={actionLoading}
            icon={Sliders}
          >
            Evaluate (Dry-Run)
          </Button>

          <Button variant="outline" size="sm" onClick={loadRoutingData} icon={RefreshCw}>
            Refresh
          </Button>
        </div>
      </div>

      {/* Metric Cards Overview */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {/* Active Strategy */}
        <Card className="border-slate-800 bg-[#111622]/90">
          <div className="flex items-center justify-between">
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Active Strategy</p>
            <GitFork className="w-5 h-5 text-sky-400" />
          </div>
          <h3 className="text-xl font-bold text-white mt-1.5 font-mono truncate">{selectedStrategy}</h3>
          <p className="text-[11px] text-slate-400 mt-2 line-clamp-2">
            {STRATEGY_DESCRIPTIONS[selectedStrategy] || 'Dynamic traffic routing strategy'}
          </p>
        </Card>

        {/* Total Decisions */}
        <Card className="border-slate-800 bg-[#111622]/90">
          <div className="flex items-center justify-between">
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Decisions Processed</p>
            <Layers className="w-5 h-5 text-purple-400" />
          </div>
          <h3 className="text-2xl font-bold text-white mt-1.5 font-mono">
            {status?.totalDecisionsCount ?? history.length}
          </h3>
          <p className="text-[11px] text-slate-400 mt-2 font-mono">
            Server recorded routing executions
          </p>
        </Card>

        {/* Eligible Candidates */}
        <Card className="border-slate-800 bg-[#111622]/90">
          <div className="flex items-center justify-between">
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Eligible Pool</p>
            <Server className="w-5 h-5 text-emerald-400" />
          </div>
          <h3 className="text-2xl font-bold text-emerald-400 mt-1.5 font-mono">
            {eligibleCount} <span className="text-xs text-slate-400 font-normal">/ {candidates.length} nodes</span>
          </h3>
          <p className="text-[11px] text-slate-400 mt-2 font-mono">
            Filtered by health & liveness policy
          </p>
        </Card>

        {/* Selected Target Node */}
        <Card className="border-slate-800 bg-[#111622]/90">
          <div className="flex items-center justify-between">
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Last Routed Target</p>
            <Network className="w-5 h-5 text-cyan-400" />
          </div>
          <h3 className="text-xl font-bold text-cyan-400 mt-1.5 font-mono truncate">
            {lastDecision?.selectedNodeId || 'None'}
          </h3>
          <p className="text-[11px] text-slate-400 mt-2 font-mono truncate">
            {lastDecision?.selectedNodeName || 'Awaiting decision execution'}
          </p>
        </Card>
      </div>

      {/* Decision Explainability & Flow Visualizer */}
      <Card
        title="Routing Decision Flow & Explainability"
        subtitle="Transparent deterministic candidate filtering, metric normalization, and scoring breakdown"
      >
        {lastDecision ? (
          <div className="space-y-6">
            {/* Decision Pipeline Flow */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-3 p-4 rounded-xl bg-slate-950/80 border border-slate-800 text-xs font-mono">
              <div className="p-3 rounded-lg bg-slate-900/60 border border-slate-800 flex flex-col justify-between">
                <span className="text-slate-400 text-[10px] uppercase">1. Traffic Request</span>
                <span className="font-bold text-sky-400 mt-1">{lastDecision.requestId}</span>
                <span className="text-[10px] text-slate-400 mt-1">{lastDecision.strategy}</span>
              </div>

              <div className="p-3 rounded-lg bg-slate-900/60 border border-slate-800 flex flex-col justify-between">
                <span className="text-slate-400 text-[10px] uppercase">2. Candidate Filter</span>
                <span className="font-bold text-emerald-400 mt-1">
                  {lastDecision.eligibleCandidates} of {lastDecision.totalCandidates} Eligible
                </span>
                <span className="text-[10px] text-slate-400 mt-1">Excludes OFFLINE/UNREACHABLE</span>
              </div>

              <div className="p-3 rounded-lg bg-slate-900/60 border border-slate-800 flex flex-col justify-between">
                <span className="text-slate-400 text-[10px] uppercase">3. Strategy Scoring</span>
                <span className="font-bold text-purple-400 mt-1">
                  {lastDecision.score !== null ? `Score: ${lastDecision.score}` : 'Sequential Rank'}
                </span>
                <span className="text-[10px] text-slate-400 mt-1">Deterministic Tie-Breaking</span>
              </div>

              <div className="p-3 rounded-lg bg-sky-500/10 border border-sky-500/30 flex flex-col justify-between">
                <span className="text-sky-400 text-[10px] uppercase font-bold">4. Selected Target</span>
                <span className="font-bold text-white text-sm mt-1">{lastDecision.selectedNodeId || 'NO_ELIGIBLE_NODE'}</span>
                <span className="text-[10px] text-sky-300 mt-1 truncate">{lastDecision.selectedNodeName || 'No Target'}</span>
              </div>
            </div>

            {/* Explainability Detail Banner */}
            <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800">
              <div className="flex items-start gap-3">
                <ShieldCheck className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
                <div>
                  <h4 className="text-sm font-semibold text-white">Decision Justification</h4>
                  <p className="text-xs text-slate-300 font-mono mt-1">
                    {lastDecision.decisionReason}
                  </p>
                  <p className="text-[10px] text-slate-400 font-mono mt-1.5">
                    Evaluated at: {new Date(lastDecision.timestamp).toLocaleString()} &bull; Request ID: {lastDecision.requestId}
                  </p>
                </div>
              </div>
            </div>
          </div>
        ) : (
          <div className="py-8 text-center text-slate-400 text-xs font-mono">
            No routing decisions executed yet. Dispatch a traffic request above to inspect the decision pipeline.
          </div>
        )}
      </Card>

      {/* Candidate Evaluation Table */}
      <Card
        title="Candidate Pool Evaluation & Scored Metrics"
        subtitle="Live evaluation breakdown showing why nodes were selected or excluded by policy"
      >
        {candidates.length === 0 ? (
          <div className="py-8 text-center text-slate-400 text-xs font-mono">
            No network nodes registered in topology.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="text-[11px] uppercase tracking-wider text-slate-400 bg-slate-900/60 border-b border-slate-800">
                <tr>
                  <th className="py-3 px-4">Node ID & Host</th>
                  <th className="py-3 px-3">Eligibility</th>
                  <th className="py-3 px-3">Health / Liveness</th>
                  <th className="py-3 px-3">CPU / RAM</th>
                  <th className="py-3 px-3">Latency / Loss</th>
                  <th className="py-3 px-3">Conns / Capacity</th>
                  <th className="py-3 px-3">Strategy Score</th>
                  <th className="py-3 px-4">Evaluation / Exclusion Reason</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-mono">
                {candidates.map((node) => {
                  const isSelected = lastDecision?.selectedNodeId === node.nodeId;

                  return (
                    <tr
                      key={node.nodeId}
                      className={`transition-colors ${
                        isSelected
                          ? 'bg-sky-500/10 hover:bg-sky-500/15 border-l-2 border-l-sky-400'
                          : 'hover:bg-slate-900/40'
                      }`}
                    >
                      <td className="py-3 px-4 font-medium text-white">
                        <div className="flex items-center gap-2">
                          <Server className={`w-3.5 h-3.5 ${isSelected ? 'text-sky-400' : 'text-slate-400'}`} />
                          <div>
                            <p className="font-bold text-slate-200">
                              {node.name} {isSelected && <span className="text-[10px] text-sky-400 font-mono font-bold">[SELECTED]</span>}
                            </p>
                            <p className="text-[10px] text-slate-400">{node.nodeId}</p>
                          </div>
                        </div>
                      </td>

                      {/* Eligibility Status */}
                      <td className="py-3 px-3">
                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                          node.eligible
                            ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                            : 'bg-rose-500/20 text-rose-400 border border-rose-500/30'
                        }`}>
                          {node.eligible ? 'ELIGIBLE' : 'EXCLUDED'}
                        </span>
                      </td>

                      {/* Health & Liveness */}
                      <td className="py-3 px-3">
                        <div className="flex flex-col gap-0.5">
                          <span className={`text-[10px] font-bold ${
                            node.healthStatus === 'HEALTHY' ? 'text-emerald-400' :
                            node.healthStatus === 'WARNING' ? 'text-amber-400' :
                            node.healthStatus === 'DEGRADED' ? 'text-orange-400' :
                            'text-rose-400'
                          }`}>
                            {node.healthStatus}
                          </span>
                          <span className="text-[10px] text-slate-400">{node.livenessStatus}</span>
                        </div>
                      </td>

                      {/* CPU / RAM */}
                      <td className="py-3 px-3 text-slate-300">
                        {node.cpuUsage}% / {node.memoryUsage}%
                      </td>

                      {/* Latency / Loss */}
                      <td className="py-3 px-3 text-slate-300">
                        {node.latency}ms / {node.packetLoss}%
                      </td>

                      {/* Conns / Capacity */}
                      <td className="py-3 px-3 text-slate-300">
                        {node.activeConnections} / {node.capacity}
                      </td>

                      {/* Score */}
                      <td className="py-3 px-3 text-sky-400 font-bold">
                        {node.score !== null && node.score !== undefined ? node.score : '—'}
                      </td>

                      {/* Reason */}
                      <td className="py-3 px-4 text-[11px] text-slate-400 font-sans max-w-xs truncate">
                        {node.evaluationReason}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {/* Historical Decisions Log */}
      <Card
        title="Routing Decisions Log"
        subtitle="Persistent immutable audit log of real-time traffic routing decisions"
      >
        {history.length === 0 ? (
          <div className="py-8 text-center text-slate-400 text-xs font-mono">
            No historical routing decisions recorded yet.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="text-[11px] uppercase tracking-wider text-slate-400 bg-slate-900/60 border-b border-slate-800">
                <tr>
                  <th className="py-2.5 px-4">Request ID</th>
                  <th className="py-2.5 px-3">Strategy</th>
                  <th className="py-2.5 px-3">Selected Node</th>
                  <th className="py-2.5 px-3">Eligible / Total</th>
                  <th className="py-2.5 px-3">Score</th>
                  <th className="py-2.5 px-4">Decision Reason</th>
                  <th className="py-2.5 px-3 text-right">Timestamp</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-mono">
                {history.map((h) => (
                  <tr key={h.requestId} className="hover:bg-slate-900/40 transition-colors">
                    <td className="py-2.5 px-4 font-bold text-sky-400">{h.requestId}</td>
                    <td className="py-2.5 px-3 text-slate-300 font-bold">{h.strategy}</td>
                    <td className="py-2.5 px-3 font-bold text-white">{h.selectedNodeId || 'NO_ELIGIBLE_NODE'}</td>
                    <td className="py-2.5 px-3 text-slate-400">{h.eligibleCandidates} / {h.totalCandidates}</td>
                    <td className="py-2.5 px-3 text-purple-400">{h.score !== null ? h.score : '—'}</td>
                    <td className="py-2.5 px-4 text-[11px] text-slate-400 font-sans max-w-sm truncate">{h.decisionReason}</td>
                    <td className="py-2.5 px-3 text-right text-slate-400 text-[10px]">
                      {new Date(h.timestamp).toLocaleTimeString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
};

export default Routing;
