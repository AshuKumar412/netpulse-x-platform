import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  HeartPulse,
  ShieldCheck,
  ShieldAlert,
  RefreshCw,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  Clock,
  Activity,
  Server,
  ArrowRight,
  RotateCcw,
  Play,
  Settings,
  Wifi,
  WifiOff,
} from 'lucide-react';
import { failoverService } from '../services/failoverService';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { LoadingState } from '../components/common/LoadingState';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { useWebSocket } from '../context/WebSocketContext';
import { useAuth } from '../context/AuthContext';

// ─── State colour mapping ──────────────────────────────────────────────────
const STATE_META = {
  HEALTHY:               { color: 'text-emerald-400', bg: 'bg-emerald-500/10 border-emerald-500/30' },
  MONITORING:            { color: 'text-sky-400',     bg: 'bg-sky-500/10 border-sky-500/30' },
  FAILURE_SUSPECTED:     { color: 'text-amber-400',   bg: 'bg-amber-500/10 border-amber-500/30' },
  FAILURE_CONFIRMED:     { color: 'text-rose-400',    bg: 'bg-rose-500/10 border-rose-500/30' },
  ISOLATING:             { color: 'text-orange-400',  bg: 'bg-orange-500/10 border-orange-500/30' },
  FAILOVER_IN_PROGRESS:  { color: 'text-purple-400',  bg: 'bg-purple-500/10 border-purple-500/30' },
  REROUTED:              { color: 'text-cyan-400',     bg: 'bg-cyan-500/10 border-cyan-500/30' },
  RECOVERY_IN_PROGRESS:  { color: 'text-blue-400',    bg: 'bg-blue-500/10 border-blue-500/30' },
  RECOVERY_VERIFICATION: { color: 'text-indigo-400',  bg: 'bg-indigo-500/10 border-indigo-500/30' },
  RESTORED:              { color: 'text-emerald-400', bg: 'bg-emerald-500/10 border-emerald-500/30' },
  RECOVERY_FAILED:       { color: 'text-rose-400',    bg: 'bg-rose-500/10 border-rose-500/30' },
  FAILOVER_FAILED:       { color: 'text-rose-400',    bg: 'bg-rose-500/10 border-rose-500/30' },
  COOLDOWN:              { color: 'text-slate-400',   bg: 'bg-slate-800/60 border-slate-700' },
  RETIRED:               { color: 'text-slate-400',   bg: 'bg-slate-800/60 border-slate-700' },
};

function StateBadge({ state }) {
  const meta = STATE_META[state] || { color: 'text-slate-400', bg: 'bg-slate-800/60 border-slate-700' };
  return (
    <span className={`px-2 py-0.5 rounded text-[10px] font-bold border font-mono ${meta.color} ${meta.bg}`}>
      {state}
    </span>
  );
}

function MetricCard({ label, value, subtext, icon: Icon, valueClass = 'text-white', isEmpty }) {
  return (
    <Card className="border-slate-800 bg-[#111622]/90">
      <div className="flex items-center justify-between">
        <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">{label}</p>
        <Icon className="w-5 h-5 text-slate-500" />
      </div>
      <h3 className={`text-2xl font-bold mt-1.5 font-mono ${isEmpty ? 'text-slate-500' : valueClass}`}>
        {isEmpty ? '—' : value}
      </h3>
      <p className="text-[11px] text-slate-400 mt-2 font-mono">{subtext}</p>
    </Card>
  );
}

// ─── Manual Recovery Modal ─────────────────────────────────────────────────
function ManualRecoveryModal({ nodeId, onClose, onSubmit, loading }) {
  const [reason, setReason] = useState('');
  const [force, setForce] = useState(false);

  function handleSubmit(e) {
    e.preventDefault();
    onSubmit(nodeId, reason.trim() || null, force);
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm">
      <div className="w-full max-w-md rounded-2xl bg-[#0d111a] border border-slate-700 p-6 shadow-2xl">
        <h2 className="text-lg font-bold text-white mb-1">Initiate Manual Recovery</h2>
        <p className="text-xs text-slate-400 font-mono mb-5">
          Node: <span className="text-sky-400 font-bold">{nodeId}</span>
        </p>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="text-xs text-slate-400 font-mono block mb-1.5">Recovery Reason (optional)</label>
            <textarea
              rows={3}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="e.g. Planned maintenance window completed"
              className="w-full bg-slate-950 border border-slate-700 rounded-lg text-xs text-white px-3 py-2 font-mono resize-none focus:outline-none focus:border-sky-500"
            />
          </div>
          <label className="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={force}
              onChange={(e) => setForce(e.target.checked)}
              className="rounded border-slate-600 bg-slate-900 text-sky-500 focus:ring-sky-500"
            />
            <span className="text-xs text-slate-300 font-mono">Force recovery (bypass cooldown protection)</span>
          </label>
          <div className="flex justify-end gap-3 pt-2">
            <Button variant="outline" size="sm" onClick={onClose} disabled={loading}>
              Cancel
            </Button>
            <Button variant="primary" size="sm" type="submit" disabled={loading} icon={Play}>
              {loading ? 'Initiating...' : 'Initiate Recovery'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ─── Main Page ─────────────────────────────────────────────────────────────
export const SelfHealing = () => {
  const [status, setStatus]         = useState(null);
  const [events, setEvents]         = useState([]);
  const [activeEvents, setActiveEvents] = useState([]);
  const [policies, setPolicies]     = useState(null);
  const [loading, setLoading]       = useState(true);
  const [error, setError]           = useState(null);
  const [actionError, setActionError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);

  const [recoveryModal, setRecoveryModal] = useState(null); // nodeId or null
  const [recoveryLoading, setRecoveryLoading] = useState(false);

  const { connectionStatus, failoverUpdate } = useWebSocket();
  const { isOperator, isAdmin } = useAuth();

  const canOperate = isOperator || isAdmin;

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const [statusRes, eventsRes, activeRes, policiesRes] = await Promise.all([
        failoverService.getStatus().catch(() => null),
        failoverService.getEvents(0, 30).catch(() => null),
        failoverService.getActiveEvents().catch(() => null),
        failoverService.getPolicies().catch(() => null),
      ]);

      if (statusRes?.data) setStatus(statusRes.data);
      if (eventsRes?.data) setEvents(Array.isArray(eventsRes.data) ? eventsRes.data : []);
      if (activeRes?.data) setActiveEvents(Array.isArray(activeRes.data) ? activeRes.data : []);
      if (policiesRes?.data) setPolicies(policiesRes.data);
    } catch (err) {
      setError(err.message || 'Failed to load self-healing engine data');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // React to live WebSocket failover events
  useEffect(() => {
    if (!failoverUpdate) return;
    // Prepend new event to the list (deduplicate by eventId)
    setEvents((prev) => [
      failoverUpdate,
      ...prev.filter((e) => e.eventId !== failoverUpdate.eventId),
    ].slice(0, 50));

    // Refresh status and active events list on each WS update
    failoverService.getStatus().then((res) => { if (res?.data) setStatus(res.data); }).catch(() => {});
    failoverService.getActiveEvents().then((res) => {
      if (res?.data) setActiveEvents(Array.isArray(res.data) ? res.data : []);
    }).catch(() => {});
  }, [failoverUpdate]);

  // Dismiss action feedback after 4 s
  const dismissTimer = useRef(null);
  useEffect(() => {
    if (actionSuccess || actionError) {
      clearTimeout(dismissTimer.current);
      dismissTimer.current = setTimeout(() => {
        setActionSuccess(null);
        setActionError(null);
      }, 4000);
    }
    return () => clearTimeout(dismissTimer.current);
  }, [actionSuccess, actionError]);

  // ── Handlers ──────────────────────────────────────────────────────────────
  async function handleInitiateRecovery(nodeId, reason, force) {
    setRecoveryLoading(true);
    setActionError(null);
    setActionSuccess(null);
    try {
      const res = await failoverService.initiateRecovery(nodeId, reason, force);
      if (res?.data) {
        setActionSuccess(res.data.message || `Recovery initiated for ${nodeId}`);
        setRecoveryModal(null);
        loadData();
      }
    } catch (err) {
      setActionError(err.message || 'Recovery initiation failed');
    } finally {
      setRecoveryLoading(false);
    }
  }

  async function handleRetryRecovery(eventId) {
    setActionError(null);
    setActionSuccess(null);
    try {
      const res = await failoverService.retryRecovery(eventId);
      if (res?.data) {
        setActionSuccess(res.data.message || `Retry started for event ${eventId}`);
        loadData();
      }
    } catch (err) {
      setActionError(err.message || 'Retry failed');
    }
  }

  // ── Loading / Error ───────────────────────────────────────────────────────
  if (loading && !status) {
    return <LoadingState message="Initializing self-healing engine console..." />;
  }

  if (error) {
    return <ErrorMessage message={error} onRetry={loadData} />;
  }

  const totalEvents   = status?.totalFailoverEvents ?? 0;
  const recovered     = status?.recoveredNodesCount ?? 0;
  const failedRec     = status?.failedRecoveriesCount ?? 0;
  const activeCount   = status?.activeFailoversCount ?? 0;
  const successRate   = status?.recoverySuccessRate;    // null = no data
  const avgDuration   = status?.averageRecoveryDurationMs; // null = no data
  const engineEnabled = status?.failoverEnabled ?? false;
  const nodeStates    = status?.nodeFailoverStates ?? {};

  return (
    <div className="space-y-8">
      {/* Recovery Modal */}
      {recoveryModal && (
        <ManualRecoveryModal
          nodeId={recoveryModal}
          onClose={() => setRecoveryModal(null)}
          onSubmit={handleInitiateRecovery}
          loading={recoveryLoading}
        />
      )}

      {/* Action Feedback */}
      {(actionSuccess || actionError) && (
        <div className={`flex items-center gap-3 px-4 py-3 rounded-xl border text-sm font-mono ${
          actionSuccess
            ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300'
            : 'bg-rose-500/10 border-rose-500/30 text-rose-300'
        }`}>
          {actionSuccess ? <CheckCircle2 className="w-4 h-4 shrink-0" /> : <XCircle className="w-4 h-4 shrink-0" />}
          <span>{actionSuccess || actionError}</span>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 p-5 rounded-2xl bg-slate-900/80 border border-slate-800/80 shadow-lg">
        <div>
          <div className="flex items-center gap-3 flex-wrap">
            <h1 className="text-2xl font-bold tracking-tight text-white">Self-Healing Failover Console</h1>
            <span className={`text-[10px] font-mono px-2.5 py-0.5 rounded-full font-bold border ${
              connectionStatus === 'CONNECTED'
                ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                : 'bg-rose-500/10 text-rose-400 border-rose-500/30'
            }`}>
              {connectionStatus === 'CONNECTED'
                ? <span className="flex items-center gap-1"><span>●</span> ENGINE LIVE</span>
                : connectionStatus}
            </span>
            <span className={`text-[10px] font-mono px-2.5 py-0.5 rounded-full font-bold border ${
              engineEnabled
                ? 'bg-sky-500/10 text-sky-400 border-sky-500/30'
                : 'bg-amber-500/10 text-amber-400 border-amber-500/30'
            }`}>
              {engineEnabled ? 'FAILOVER ENABLED' : 'FAILOVER DISABLED'}
            </span>
          </div>
          <p className="text-xs text-slate-400 font-mono mt-1">
            Detect · Isolate · Reroute · Verify · Restore · Audit
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <Button variant="outline" size="sm" onClick={loadData} icon={RefreshCw}>
            Refresh
          </Button>
        </div>
      </div>

      {/* Summary Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <MetricCard
          label="Total Failover Events"
          value={totalEvents}
          subtext="Lifetime failover cycles recorded"
          icon={Activity}
          valueClass="text-white"
          isEmpty={totalEvents === 0}
        />
        <MetricCard
          label="Nodes Recovered"
          value={recovered}
          subtext="Successfully restored to HEALTHY"
          icon={CheckCircle2}
          valueClass="text-emerald-400"
          isEmpty={totalEvents === 0}
        />
        <MetricCard
          label="Recovery Success Rate"
          value={successRate !== null ? `${successRate.toFixed(1)}%` : '—'}
          subtext={successRate !== null ? `${recovered} successful / ${failedRec} failed` : 'No completed attempts yet'}
          icon={ShieldCheck}
          valueClass={successRate >= 80 ? 'text-emerald-400' : successRate >= 50 ? 'text-amber-400' : 'text-rose-400'}
          isEmpty={successRate === null}
        />
        <MetricCard
          label="Active Operations"
          value={activeCount}
          subtext={activeCount > 0 ? 'Failover/recovery in progress' : 'No active operations'}
          icon={activeCount > 0 ? ShieldAlert : ShieldCheck}
          valueClass={activeCount > 0 ? 'text-amber-400' : 'text-emerald-400'}
          isEmpty={false}
        />
      </div>

      {/* Average Duration card if data available */}
      {avgDuration !== null && avgDuration !== undefined && (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
          <MetricCard
            label="Avg Recovery Duration"
            value={`${(avgDuration / 1000).toFixed(1)}s`}
            subtext="Average time from failure detection to node restoration"
            icon={Clock}
            valueClass="text-cyan-400"
            isEmpty={false}
          />
        </div>
      )}

      {/* Active Operations */}
      <Card
        title="Active Failover & Recovery Operations"
        subtitle="Real-time operations currently in progress — sourced from backend state machine"
      >
        {activeEvents.length === 0 ? (
          <div className="py-10 text-center text-slate-400 text-xs font-mono flex flex-col items-center gap-2">
            <ShieldCheck className="w-8 h-8 text-emerald-400/40" />
            <span>No active recovery operations. All monitored nodes are stable.</span>
          </div>
        ) : (
          <div className="space-y-3">
            {activeEvents.map((ev) => (
              <div
                key={ev.eventId}
                className="p-4 rounded-xl bg-slate-950/60 border border-slate-800 flex flex-col sm:flex-row sm:items-center gap-4"
              >
                <div className="flex-1 min-w-0 space-y-1.5">
                  <div className="flex items-center flex-wrap gap-2">
                    <span className="font-bold text-white text-sm font-mono">{ev.failureNodeId}</span>
                    <ArrowRight className="w-3.5 h-3.5 text-slate-500" />
                    <span className="text-sm font-mono text-cyan-400">
                      {ev.replacementNodeId || 'Searching...'}
                    </span>
                    <StateBadge state={ev.currentState} />
                  </div>
                  <p className="text-[11px] text-slate-400 font-mono truncate">
                    <span className="text-slate-500">Trigger:</span> {ev.trigger} &nbsp;·&nbsp;
                    <span className="text-slate-500">Cycle:</span> {ev.recoveryCycleId}
                  </p>
                  <p className="text-[11px] text-slate-300 font-mono line-clamp-2">{ev.reason}</p>
                </div>
                <div className="flex gap-2 shrink-0">
                  {canOperate && ev.currentState === 'RECOVERY_FAILED' && (
                    <Button
                      variant="outline"
                      size="sm"
                      icon={RotateCcw}
                      onClick={() => handleRetryRecovery(ev.eventId)}
                    >
                      Retry
                    </Button>
                  )}
                  {canOperate && (ev.currentState === 'FAILOVER_FAILED' || ev.currentState === 'RECOVERY_FAILED') && (
                    <Button
                      variant="primary"
                      size="sm"
                      icon={Play}
                      onClick={() => setRecoveryModal(ev.failureNodeId)}
                    >
                      Manual Recovery
                    </Button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>

      {/* Node Failover State Map */}
      {Object.keys(nodeStates).length > 0 && (
        <Card
          title="Node Failover State Map"
          subtitle="In-memory state machine states for all tracked nodes"
        >
          <div className="flex flex-wrap gap-2">
            {Object.entries(nodeStates).map(([nodeId, state]) => (
              <div
                key={nodeId}
                className="flex items-center gap-2 px-3 py-2 rounded-lg bg-slate-950/60 border border-slate-800"
              >
                <Server className="w-3.5 h-3.5 text-slate-400" />
                <span className="text-xs font-mono text-slate-200">{nodeId}</span>
                <StateBadge state={state} />
                {canOperate && (state === 'FAILURE_CONFIRMED' || state === 'FAILOVER_FAILED' || state === 'RECOVERY_FAILED') && (
                  <button
                    onClick={() => setRecoveryModal(nodeId)}
                    className="text-[10px] font-mono text-sky-400 hover:text-sky-300 underline ml-1"
                  >
                    Recover
                  </button>
                )}
              </div>
            ))}
          </div>
        </Card>
      )}

      {/* Policy Configuration */}
      {policies && (
        <Card
          title="Failover Policy Configuration"
          subtitle="Active policy governing automatic failure detection, rerouting and recovery behaviour"
        >
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
            {[
              { label: 'Engine Enabled',           value: policies.enabled ? 'YES' : 'NO',           color: policies.enabled ? 'text-emerald-400' : 'text-rose-400' },
              { label: 'Consecutive Failures Req.', value: policies.consecutiveFailuresRequired,      color: 'text-white' },
              { label: 'Auto-Recover',             value: policies.autoRecover ? 'YES' : 'NO',       color: policies.autoRecover ? 'text-emerald-400' : 'text-amber-400' },
              { label: 'Recovery Enabled',         value: policies.recoveryEnabled ? 'YES' : 'NO',   color: policies.recoveryEnabled ? 'text-emerald-400' : 'text-amber-400' },
              { label: 'Max Attempts',             value: policies.maxAttempts,                       color: 'text-white' },
              { label: 'Healthy Cycles Required',  value: policies.healthyCyclesRequired,             color: 'text-white' },
              { label: 'Cooldown (sec)',            value: policies.cooldownSeconds,                   color: 'text-white' },
            ].map(({ label, value, color }) => (
              <div key={label} className="p-3 rounded-lg bg-slate-950/60 border border-slate-800">
                <p className="text-[10px] font-mono text-slate-400 uppercase tracking-wide">{label}</p>
                <p className={`text-sm font-bold font-mono mt-1 ${color}`}>{value}</p>
              </div>
            ))}
          </div>
        </Card>
      )}

      {/* Events History Table */}
      <Card
        title="Failover Events History"
        subtitle="Persistent audit log of all failover lifecycle events — sourced from PostgreSQL"
      >
        {events.length === 0 ? (
          <div className="py-10 text-center text-slate-400 text-xs font-mono flex flex-col items-center gap-2">
            <Activity className="w-8 h-8 text-slate-600" />
            <span>No recovery events recorded yet.</span>
            <span className="text-slate-500">Events will appear here once the self-healing engine detects and processes a node failure.</span>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="text-[11px] uppercase tracking-wider text-slate-400 bg-slate-900/60 border-b border-slate-800">
                <tr>
                  <th className="py-3 px-4">Event / Cycle ID</th>
                  <th className="py-3 px-3">Failed Node</th>
                  <th className="py-3 px-3">Replacement</th>
                  <th className="py-3 px-3">Trigger</th>
                  <th className="py-3 px-3">State</th>
                  <th className="py-3 px-3">Sessions</th>
                  <th className="py-3 px-3">Result</th>
                  <th className="py-3 px-4">Reason</th>
                  <th className="py-3 px-3 text-right">Started At</th>
                  {canOperate && <th className="py-3 px-3 text-right">Actions</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-mono">
                {events.map((ev) => {
                  const isActive = [
                    'FAILURE_CONFIRMED', 'ISOLATING', 'FAILOVER_IN_PROGRESS',
                    'REROUTED', 'RECOVERY_IN_PROGRESS', 'RECOVERY_VERIFICATION',
                  ].includes(ev.currentState);

                  const isFailure = ev.currentState === 'RECOVERY_FAILED' || ev.currentState === 'FAILOVER_FAILED';

                  return (
                    <tr
                      key={ev.eventId}
                      className={`transition-colors ${
                        isActive
                          ? 'bg-amber-500/5 hover:bg-amber-500/8 border-l-2 border-l-amber-400'
                          : isFailure
                          ? 'bg-rose-500/5 hover:bg-rose-500/8 border-l-2 border-l-rose-500'
                          : ev.currentState === 'RESTORED'
                          ? 'bg-emerald-500/5 hover:bg-emerald-500/8 border-l-2 border-l-emerald-500'
                          : 'hover:bg-slate-900/40'
                      }`}
                    >
                      <td className="py-3 px-4">
                        <p className="font-bold text-sky-400">{ev.eventId}</p>
                        <p className="text-[10px] text-slate-500">{ev.recoveryCycleId}</p>
                      </td>

                      <td className="py-3 px-3">
                        <p className="font-bold text-rose-300">{ev.failureNodeId}</p>
                        <p className="text-[10px] text-slate-400">{ev.failureNodeName}</p>
                      </td>

                      <td className="py-3 px-3">
                        {ev.replacementNodeId ? (
                          <>
                            <p className="font-bold text-cyan-400">{ev.replacementNodeId}</p>
                            <p className="text-[10px] text-slate-400">{ev.replacementNodeName}</p>
                          </>
                        ) : (
                          <span className="text-slate-500 text-[10px]">None</span>
                        )}
                      </td>

                      <td className="py-3 px-3 text-slate-300 text-[11px]">{ev.trigger}</td>

                      <td className="py-3 px-3">
                        <StateBadge state={ev.currentState} />
                      </td>

                      <td className="py-3 px-3 text-slate-400">
                        <span className="text-emerald-400 font-bold">{ev.reroutedSessionCount ?? 0}</span>
                        {' / '}
                        <span>{ev.affectedSessionCount ?? 0}</span>
                      </td>

                      <td className="py-3 px-3">
                        {ev.success === true && (
                          <span className="flex items-center gap-1 text-emerald-400 text-[10px] font-bold">
                            <CheckCircle2 className="w-3 h-3" /> SUCCESS
                          </span>
                        )}
                        {ev.success === false && (
                          <span className="flex items-center gap-1 text-rose-400 text-[10px] font-bold">
                            <XCircle className="w-3 h-3" /> FAILED
                          </span>
                        )}
                        {ev.success === null && (
                          <span className="flex items-center gap-1 text-amber-400 text-[10px]">
                            <Clock className="w-3 h-3" /> IN PROGRESS
                          </span>
                        )}
                      </td>

                      <td className="py-3 px-4 text-[11px] text-slate-400 font-sans max-w-xs">
                        <p className="line-clamp-2">{ev.reason}</p>
                        {ev.failureReason && (
                          <p className="text-rose-400 text-[10px] mt-0.5 line-clamp-1">{ev.failureReason}</p>
                        )}
                      </td>

                      <td className="py-3 px-3 text-right text-slate-400 text-[10px]">
                        {new Date(ev.startedAt).toLocaleString()}
                        {ev.completedAt && (
                          <p className="text-slate-500">→ {new Date(ev.completedAt).toLocaleTimeString()}</p>
                        )}
                      </td>

                      {canOperate && (
                        <td className="py-3 px-3 text-right">
                          <div className="flex items-center justify-end gap-1">
                            {ev.currentState === 'RECOVERY_FAILED' && (
                              <button
                                onClick={() => handleRetryRecovery(ev.eventId)}
                                className="text-[10px] font-mono text-sky-400 hover:text-sky-300 underline whitespace-nowrap"
                              >
                                Retry
                              </button>
                            )}
                            {(ev.currentState === 'FAILOVER_FAILED' || ev.currentState === 'RECOVERY_FAILED') && (
                              <button
                                onClick={() => setRecoveryModal(ev.failureNodeId)}
                                className="text-[10px] font-mono text-emerald-400 hover:text-emerald-300 underline whitespace-nowrap ml-1"
                              >
                                Recover
                              </button>
                            )}
                          </div>
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

export default SelfHealing;
