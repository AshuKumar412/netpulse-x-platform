import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  Cpu,
  Play,
  Pause,
  Square,
  RotateCcw,
  StepForward,
  Plus,
  Layers,
  BarChart3,
  ListOrdered,
  Clock,
  Activity,
  AlertTriangle,
  CheckCircle2,
  RefreshCw,
  Zap,
  Sliders,
  Sparkles,
  Server,
  Trash2,
  HelpCircle,
} from 'lucide-react';
import { schedulerService } from '../services/schedulerService';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { LoadingState } from '../components/common/LoadingState';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { useWebSocket } from '../context/WebSocketContext';
import { useAuth } from '../context/AuthContext';

// ── Algorithm Definitions ──────────────────────────────────────────────────
const ALGORITHMS = [
  {
    key: 'FCFS',
    name: 'First-Come, First-Served (FCFS)',
    desc: 'Non-preemptive. Executes processes in exact order of arrival. Simple but vulnerable to Convoy Effect.',
    type: 'Non-Preemptive',
    badge: 'bg-slate-500/20 text-slate-300 border-slate-500/30',
  },
  {
    key: 'SJF',
    name: 'Shortest Job First (SJF)',
    desc: 'Non-preemptive. Schedules the ready process with shortest total CPU burst time. Minimizes average waiting time.',
    type: 'Non-Preemptive',
    badge: 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30',
  },
  {
    key: 'SRTF',
    name: 'Shortest Remaining Time First (SRTF)',
    desc: 'Preemptive SJF. Preempts running job if newly arrived process has strictly shorter remaining CPU burst.',
    type: 'Preemptive',
    badge: 'bg-teal-500/20 text-teal-300 border-teal-500/30',
  },
  {
    key: 'PRIORITY',
    name: 'Preemptive Priority (with Aging)',
    desc: 'Priority 1 (Highest) to 10 (Lowest). Dynamic priority aging prevents starvation for low-priority tasks.',
    type: 'Preemptive + Aging',
    badge: 'bg-amber-500/20 text-amber-300 border-amber-500/30',
  },
  {
    key: 'ROUND_ROBIN',
    name: 'Round Robin (RR)',
    desc: 'Preemptive time-sliced scheduling. Slices CPU execution by deterministic Time Quantum for uniform fairness.',
    type: 'Preemptive Time-Sliced',
    badge: 'bg-blue-500/20 text-blue-300 border-blue-500/30',
  },
  {
    key: 'MULTILEVEL_QUEUE',
    name: 'Multilevel Queue (MLQ)',
    desc: '3 Priority Tiers: Q1 High (Interactive RR q=2) -> Q2 Medium (I/O RR q=4) -> Q3 Low (Background FCFS).',
    type: 'Multi-Level Priority',
    badge: 'bg-purple-500/20 text-purple-300 border-purple-500/30',
  },
  {
    key: 'CFS_INSPIRED',
    name: 'CFS-Inspired (Virtual Runtime)',
    desc: 'Completely Fair Scheduler model tracking virtual runtime scaled by priority weights to guarantee proportional fairness.',
    type: 'Proportional Share',
    badge: 'bg-rose-500/20 text-rose-300 border-rose-500/30',
  },
];

const PROCESS_TYPES = ['CPU_BOUND', 'IO_BOUND', 'NETWORK_BOUND', 'INTERACTIVE', 'BACKGROUND'];

export const Scheduler = () => {
  const { user } = useAuth();
  const { schedulerUpdate } = useWebSocket();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [statusData, setStatusData] = useState(null);
  const [policyConfig, setPolicyConfig] = useState(null);

  // Modals & Active Tabs
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showBatchModal, setShowBatchModal] = useState(false);
  const [showBenchmarkModal, setShowBenchmarkModal] = useState(false);
  const [activeQueueTab, setActiveQueueTab] = useState('READY'); // READY, RUNNING, WAITING, COMPLETED
  const [benchmarkLoading, setBenchmarkLoading] = useState(false);
  const [benchmarkResult, setBenchmarkResult] = useState(null);

  // Form states
  const [newProcess, setNewProcess] = useState({
    processName: 'Worker_Task',
    processType: 'CPU_BOUND',
    priority: 5,
    burstTime: 20,
    targetNodeId: '',
  });

  const [batchCount, setBatchCount] = useState(8);

  // Fetch initial data
  const fetchData = useCallback(async () => {
    try {
      setError(null);
      const [status, policy] = await Promise.all([
        schedulerService.getStatus(),
        schedulerService.getPolicy(),
      ]);
      setStatusData(status);
      setPolicyConfig(policy);
    } catch (err) {
      console.error('Failed to load scheduler data:', err);
      setError('Failed to connect to OS CPU Scheduler Engine.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // WebSocket Live Sync
  useEffect(() => {
    if (schedulerUpdate) {
      setStatusData(schedulerUpdate);
      if (schedulerUpdate.policyConfig) {
        setPolicyConfig(schedulerUpdate.policyConfig);
      }
    }
  }, [schedulerUpdate]);

  // Actions
  const handleStart = async () => {
    try {
      await schedulerService.start();
      fetchData();
    } catch (err) {
      setError('Failed to start scheduler simulation.');
    }
  };

  const handlePause = async () => {
    try {
      await schedulerService.pause();
      fetchData();
    } catch (err) {
      setError('Failed to pause scheduler simulation.');
    }
  };

  const handleResume = async () => {
    try {
      await schedulerService.resume();
      fetchData();
    } catch (err) {
      setError('Failed to resume scheduler simulation.');
    }
  };

  const handleStop = async () => {
    try {
      await schedulerService.stop();
      fetchData();
    } catch (err) {
      setError('Failed to stop scheduler simulation.');
    }
  };

  const handleReset = async () => {
    try {
      await schedulerService.reset();
      fetchData();
    } catch (err) {
      setError('Failed to reset scheduler simulation.');
    }
  };

  const handleStep = async () => {
    try {
      await schedulerService.step();
      fetchData();
    } catch (err) {
      setError('Failed to advance single step.');
    }
  };

  const handleAlgorithmChange = async (algo) => {
    try {
      await schedulerService.switchAlgorithm(algo);
      fetchData();
    } catch (err) {
      setError('Failed to switch scheduling algorithm.');
    }
  };

  const handleQuantumChange = async (quantum) => {
    try {
      const updated = { ...policyConfig, timeQuantum: parseInt(quantum, 10) };
      await schedulerService.updatePolicy(updated);
      setPolicyConfig(updated);
    } catch (err) {
      setError('Failed to update time quantum.');
    }
  };

  const handleCreateProcess = async (e) => {
    e.preventDefault();
    try {
      await schedulerService.createProcess(newProcess);
      setShowCreateModal(false);
      fetchData();
    } catch (err) {
      setError('Failed to create process.');
    }
  };

  const handleBatchCreate = async (e) => {
    e.preventDefault();
    try {
      await schedulerService.batchCreateProcesses({ count: parseInt(batchCount, 10) });
      setShowBatchModal(false);
      fetchData();
    } catch (err) {
      setError('Failed to batch create processes.');
    }
  };

  const handleTerminateProcess = async (processId) => {
    try {
      await schedulerService.terminateProcess(processId);
      fetchData();
    } catch (err) {
      setError(`Failed to terminate process ${processId}.`);
    }
  };

  const handleRunBenchmark = async () => {
    setBenchmarkLoading(true);
    try {
      const res = await schedulerService.runBenchmark([]);
      setBenchmarkResult(res);
      setShowBenchmarkModal(true);
    } catch (err) {
      setError('Failed to run benchmark comparison.');
    } finally {
      setBenchmarkLoading(false);
    }
  };

  if (loading) {
    return <LoadingState message="Initializing OS Process & CPU Scheduler Subsystem..." />;
  }

  const metrics = statusData?.metrics || {};
  const cores = statusData?.cores || [];
  const readyQueue = statusData?.readyQueue || [];
  const runningProcesses = statusData?.runningProcesses || [];
  const waitingQueue = statusData?.waitingQueue || [];
  const recentGantt = statusData?.recentGanttBlocks || [];
  const recentCS = statusData?.recentContextSwitches || [];
  const currentAlgo = statusData?.algorithm || 'ROUND_ROBIN';
  const currentStatus = statusData?.status || 'RUNNING';

  const getPriorityColor = (p) => {
    if (p <= 3) return 'text-rose-400 bg-rose-500/10 border-rose-500/30';
    if (p <= 7) return 'text-amber-400 bg-amber-500/10 border-amber-500/30';
    return 'text-blue-400 bg-blue-500/10 border-blue-500/30';
  };

  return (
    <div className="space-y-6">
      {/* ── Page Header & Controls ── */}
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4 bg-slate-900/60 p-5 rounded-2xl border border-slate-800 backdrop-blur-xl">
        <div>
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-gradient-to-tr from-cyan-500/20 to-blue-500/20 border border-cyan-500/30 rounded-xl text-cyan-400 shadow-lg shadow-cyan-500/10">
              <Cpu className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-xl font-bold text-white tracking-tight">OS Process & CPU Scheduler</h1>
                <span className="px-2.5 py-0.5 text-xs font-semibold rounded-full bg-cyan-500/10 text-cyan-400 border border-cyan-500/20">
                  Phase 7 Active
                </span>
                <span className={`px-2.5 py-0.5 text-xs font-semibold rounded-full border ${
                  currentStatus === 'RUNNING'
                    ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20 animate-pulse'
                    : currentStatus === 'PAUSED'
                    ? 'bg-amber-500/10 text-amber-400 border-amber-500/20'
                    : 'bg-slate-500/10 text-slate-400 border-slate-500/20'
                }`}>
                  {currentStatus} (Tick #{statusData?.currentTick || 0})
                </span>
              </div>
              <p className="text-sm text-slate-400 mt-0.5">
                Deterministic Operating System process dispatching, multi-core scheduling, and context-switching simulation.
              </p>
            </div>
          </div>
        </div>

        {/* Engine Control Buttons */}
        <div className="flex flex-wrap items-center gap-2">
          {currentStatus === 'RUNNING' ? (
            <Button variant="secondary" size="sm" onClick={handlePause} className="flex items-center gap-1.5">
              <Pause className="w-4 h-4 text-amber-400" /> Pause
            </Button>
          ) : (
            <Button variant="primary" size="sm" onClick={handleResume} className="flex items-center gap-1.5">
              <Play className="w-4 h-4 text-emerald-400" /> Resume
            </Button>
          )}

          <Button variant="secondary" size="sm" onClick={handleStep} className="flex items-center gap-1.5" title="Advance discrete 1 tick">
            <StepForward className="w-4 h-4 text-cyan-400" /> Step (+1)
          </Button>

          <Button variant="secondary" size="sm" onClick={handleStop} className="flex items-center gap-1.5">
            <Square className="w-4 h-4 text-rose-400" /> Stop
          </Button>

          <Button variant="secondary" size="sm" onClick={handleReset} className="flex items-center gap-1.5">
            <RotateCcw className="w-4 h-4 text-slate-400" /> Reset
          </Button>

          <div className="h-6 w-px bg-slate-800 mx-1" />

          <Button variant="primary" size="sm" onClick={() => setShowCreateModal(true)} className="flex items-center gap-1.5 bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-white">
            <Plus className="w-4 h-4" /> New Process
          </Button>

          <Button variant="secondary" size="sm" onClick={() => setShowBatchModal(true)} className="flex items-center gap-1.5">
            <Layers className="w-4 h-4 text-purple-400" /> Batch Workload
          </Button>

          <Button
            variant="secondary"
            size="sm"
            onClick={handleRunBenchmark}
            disabled={benchmarkLoading}
            className="flex items-center gap-1.5 border-cyan-500/40 text-cyan-300 hover:bg-cyan-500/10"
          >
            <Sparkles className="w-4 h-4 text-cyan-400" />
            {benchmarkLoading ? 'Benchmarking...' : 'Benchmark 7 Algorithms'}
          </Button>
        </div>
      </div>

      {error && <ErrorMessage message={error} onDismiss={() => setError(null)} />}

      {/* ── Algorithm Selector & Policy Bar ── */}
      <Card className="p-4 bg-slate-900/50 border-slate-800">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <Sliders className="w-5 h-5 text-cyan-400" />
            <div>
              <span className="text-xs uppercase tracking-wider text-slate-400 font-bold">Active CPU Scheduling Strategy:</span>
              <div className="flex flex-wrap items-center gap-2 mt-1">
                {ALGORITHMS.map((algo) => (
                  <button
                    key={algo.key}
                    onClick={() => handleAlgorithmChange(algo.key)}
                    className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition-all border ${
                      currentAlgo === algo.key
                        ? 'bg-cyan-500/20 text-cyan-300 border-cyan-500 shadow-md shadow-cyan-500/20'
                        : 'bg-slate-800/40 text-slate-400 border-slate-700 hover:bg-slate-800 hover:text-slate-200'
                    }`}
                  >
                    {algo.name.split(' (')[0]}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Time Quantum for RR / MLQ */}
          {(currentAlgo === 'ROUND_ROBIN' || currentAlgo === 'MULTILEVEL_QUEUE') && (
            <div className="flex items-center gap-3 bg-slate-800/60 px-4 py-2 rounded-xl border border-slate-700">
              <span className="text-xs text-slate-300 whitespace-nowrap font-medium">Time Quantum:</span>
              <input
                type="range"
                min="1"
                max="10"
                value={policyConfig?.timeQuantum || 4}
                onChange={(e) => handleQuantumChange(e.target.value)}
                className="w-24 accent-cyan-400 cursor-pointer"
              />
              <span className="text-xs font-mono font-bold text-cyan-300 bg-cyan-500/20 px-2 py-0.5 rounded border border-cyan-500/30">
                {policyConfig?.timeQuantum || 4} ticks
              </span>
            </div>
          )}
        </div>
      </Card>

      {/* ── Metric Cards ── */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
        <Card className="p-4 bg-slate-900/50 border-slate-800">
          <div className="flex items-center justify-between text-slate-400 mb-1">
            <span className="text-xs font-medium uppercase tracking-wider">Avg Waiting Time</span>
            <Clock className="w-4 h-4 text-cyan-400" />
          </div>
          <div className="text-2xl font-bold text-white font-mono">{metrics.averageWaitingTime || 0} <span className="text-xs text-slate-400">ticks</span></div>
          <p className="text-[11px] text-slate-500 mt-1">Ready queue duration</p>
        </Card>

        <Card className="p-4 bg-slate-900/50 border-slate-800">
          <div className="flex items-center justify-between text-slate-400 mb-1">
            <span className="text-xs font-medium uppercase tracking-wider">Avg Turnaround</span>
            <Activity className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-2xl font-bold text-white font-mono">{metrics.averageTurnaroundTime || 0} <span className="text-xs text-slate-400">ticks</span></div>
          <p className="text-[11px] text-slate-500 mt-1">Arrival to completion</p>
        </Card>

        <Card className="p-4 bg-slate-900/50 border-slate-800">
          <div className="flex items-center justify-between text-slate-400 mb-1">
            <span className="text-xs font-medium uppercase tracking-wider">Avg Response</span>
            <Zap className="w-4 h-4 text-amber-400" />
          </div>
          <div className="text-2xl font-bold text-white font-mono">{metrics.averageResponseTime || 0} <span className="text-xs text-slate-400">ticks</span></div>
          <p className="text-[11px] text-slate-500 mt-1">Arrival to first dispatch</p>
        </Card>

        <Card className="p-4 bg-slate-900/50 border-slate-800">
          <div className="flex items-center justify-between text-slate-400 mb-1">
            <span className="text-xs font-medium uppercase tracking-wider">CPU Utilization</span>
            <Cpu className="w-4 h-4 text-purple-400" />
          </div>
          <div className="text-2xl font-bold text-white font-mono">{metrics.cpuUtilizationPercent || 0}%</div>
          <p className="text-[11px] text-slate-500 mt-1">Busy vs Idle cycles</p>
        </Card>

        <Card className="p-4 bg-slate-900/50 border-slate-800">
          <div className="flex items-center justify-between text-slate-400 mb-1">
            <span className="text-xs font-medium uppercase tracking-wider">Throughput</span>
            <BarChart3 className="w-4 h-4 text-blue-400" />
          </div>
          <div className="text-2xl font-bold text-white font-mono">{metrics.throughputPerMinute || 0}</div>
          <p className="text-[11px] text-slate-500 mt-1">Completed proc / min</p>
        </Card>

        <Card className="p-4 bg-slate-900/50 border-slate-800">
          <div className="flex items-center justify-between text-slate-400 mb-1">
            <span className="text-xs font-medium uppercase tracking-wider">Context Switches</span>
            <RefreshCw className="w-4 h-4 text-rose-400" />
          </div>
          <div className="text-2xl font-bold text-white font-mono">{metrics.totalContextSwitches || 0}</div>
          <p className="text-[11px] text-slate-500 mt-1">Dispatches & Preemptions</p>
        </Card>
      </div>

      {/* ── Multi-Core CPU Dashboard ── */}
      <div>
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-base font-semibold text-white flex items-center gap-2">
            <Cpu className="w-5 h-5 text-cyan-400" /> Multi-Core CPU Workers
          </h2>
          <span className="text-xs text-slate-400">{cores.length} Total Cores across Topology</span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {cores.map((core) => {
            const isBusy = core.state === 'RUNNING';
            const isOffline = core.state === 'OFFLINE';

            return (
              <Card
                key={core.coreId}
                className={`p-4 transition-all duration-300 border ${
                  isOffline
                    ? 'bg-rose-950/20 border-rose-900/30'
                    : isBusy
                    ? 'bg-slate-900/80 border-cyan-500/40 shadow-lg shadow-cyan-500/5'
                    : 'bg-slate-900/40 border-slate-800'
                }`}
              >
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-bold text-slate-300 font-mono">CORE-{core.coreId}</span>
                    <span className="text-[10px] text-slate-500 bg-slate-800 px-1.5 py-0.5 rounded border border-slate-700">
                      {core.nodeId}
                    </span>
                  </div>
                  <span
                    className={`text-[11px] px-2 py-0.5 rounded-full font-semibold border ${
                      isOffline
                        ? 'bg-rose-500/10 text-rose-400 border-rose-500/30'
                        : isBusy
                        ? 'bg-cyan-500/10 text-cyan-300 border-cyan-500/30 animate-pulse'
                        : 'bg-slate-800 text-slate-400 border-slate-700'
                    }`}
                  >
                    {core.state}
                  </span>
                </div>

                {isBusy ? (
                  <div className="space-y-2 mt-3">
                    <div className="flex items-center justify-between text-xs">
                      <span className="text-slate-300 font-medium truncate max-w-[140px]">
                        {core.currentProcessName || core.currentProcessId}
                      </span>
                      <span className={`text-[10px] px-1.5 py-0.5 rounded border font-mono ${getPriorityColor(core.currentProcessPriority)}`}>
                        Pri {core.currentProcessPriority}
                      </span>
                    </div>

                    <div className="space-y-1">
                      <div className="flex justify-between text-[11px] text-slate-400 font-mono">
                        <span>Remaining Burst:</span>
                        <span className="text-cyan-400 font-bold">{core.currentProcessRemainingBurst} ticks</span>
                      </div>
                      <div className="w-full bg-slate-800 h-1.5 rounded-full overflow-hidden">
                        <div
                          className="bg-gradient-to-r from-cyan-400 to-blue-500 h-full rounded-full transition-all duration-300"
                          style={{
                            width: `${Math.min(100, Math.max(10, 100 - (core.currentProcessRemainingBurst * 5)))}%`,
                          }}
                        />
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="py-4 text-center text-xs text-slate-500 font-mono">
                    {isOffline ? 'NODE OUTAGE / CORE OFFLINE' : 'IDLE (AWAITING DISPATCH)'}
                  </div>
                )}

                <div className="mt-3 pt-2 border-t border-slate-800/80 flex items-center justify-between text-[11px] text-slate-400 font-mono">
                  <span>Core Load:</span>
                  <span className="text-slate-200 font-semibold">{core.coreUtilizationPercent ? core.coreUtilizationPercent.toFixed(1) : 0}%</span>
                </div>
              </Card>
            );
          })}
        </div>
      </div>

      {/* ── Real-Time Interactive Gantt Timeline ── */}
      <Card className="p-5 bg-slate-900/60 border-slate-800">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <BarChart3 className="w-5 h-5 text-cyan-400" />
            <h2 className="text-base font-semibold text-white">Real-Time CPU Execution Gantt Chart</h2>
          </div>
          <span className="text-xs text-slate-400 font-mono">Window: Last {recentGantt.length} Dispatched Blocks</span>
        </div>

        {recentGantt.length === 0 ? (
          <div className="py-8 text-center text-slate-500 text-sm">
            No execution blocks recorded yet. Start simulation or create workload to view live timeline.
          </div>
        ) : (
          <div className="overflow-x-auto pb-2">
            <div className="min-w-[700px] flex items-center gap-1.5 bg-slate-950/60 p-3 rounded-xl border border-slate-800">
              {recentGantt.slice(-25).map((block, idx) => {
                const duration = Math.max(1, block.endTick - block.startTick);
                return (
                  <div
                    key={idx}
                    className="group relative flex-1 min-w-[48px] h-14 bg-gradient-to-t from-cyan-950/40 to-slate-900 border border-cyan-500/30 rounded-lg p-1.5 flex flex-col justify-between hover:border-cyan-400 hover:scale-105 transition-all cursor-pointer shadow-sm"
                    title={`Process: ${block.processName} (${block.processId})\nCore: ${block.coreId}\nRange: Tick ${block.startTick} -> ${block.endTick} (${duration} ticks)\nPriority: ${block.priority}`}
                  >
                    <div className="flex items-center justify-between text-[10px] font-mono text-cyan-300 truncate">
                      <span className="truncate font-bold">{block.processName.split('_')[0]}</span>
                    </div>
                    <div className="flex items-center justify-between text-[9px] font-mono text-slate-400">
                      <span>C{block.coreId}</span>
                      <span className="text-cyan-400">{duration}t</span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </Card>

      {/* ── Process Control Blocks & Queues ── */}
      <div className="space-y-3">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <ListOrdered className="w-5 h-5 text-cyan-400" />
            <h2 className="text-base font-semibold text-white">Process Control Block (PCB) Queues</h2>
          </div>

          <div className="flex items-center gap-1 bg-slate-900/80 p-1 rounded-xl border border-slate-800">
            <button
              onClick={() => setActiveQueueTab('READY')}
              className={`px-3 py-1 text-xs font-semibold rounded-lg transition-all ${
                activeQueueTab === 'READY'
                  ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/30'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Ready Queue ({readyQueue.length})
            </button>
            <button
              onClick={() => setActiveQueueTab('RUNNING')}
              className={`px-3 py-1 text-xs font-semibold rounded-lg transition-all ${
                activeQueueTab === 'RUNNING'
                  ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/30'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Running ({runningProcesses.length})
            </button>
            <button
              onClick={() => setActiveQueueTab('WAITING')}
              className={`px-3 py-1 text-xs font-semibold rounded-lg transition-all ${
                activeQueueTab === 'WAITING'
                  ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/30'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              I/O Waiting ({waitingQueue.length})
            </button>
          </div>
        </div>

        <Card className="bg-slate-900/60 border-slate-800 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-950/60 text-slate-400 font-semibold border-b border-slate-800 uppercase tracking-wider">
                <tr>
                  <th className="p-3">Process</th>
                  <th className="p-3">Type</th>
                  <th className="p-3">Priority</th>
                  <th className="p-3">Burst (Rem / Total)</th>
                  <th className="p-3">Wait Time</th>
                  <th className="p-3">Target Node</th>
                  <th className="p-3">vRuntime / State</th>
                  <th className="p-3 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 text-slate-300">
                {activeQueueTab === 'READY' &&
                  (readyQueue.length === 0 ? (
                    <tr>
                      <td colSpan="8" className="p-6 text-center text-slate-500">
                        Ready queue is empty. Click "New Process" or "Batch Workload" to dispatch jobs.
                      </td>
                    </tr>
                  ) : (
                    readyQueue.map((pcb) => (
                      <tr key={pcb.processId} className="hover:bg-slate-800/30 transition-colors">
                        <td className="p-3 font-medium font-mono text-white">
                          <div className="flex items-center gap-1.5">
                            <span>{pcb.processName}</span>
                            {pcb.starvationRisk && (
                              <span className="text-[10px] bg-rose-500/20 text-rose-300 border border-rose-500/30 px-1 rounded flex items-center gap-0.5" title="Starvation risk detected: Priority dynamically aged">
                                <AlertTriangle className="w-3 h-3" /> Aged
                              </span>
                            )}
                          </div>
                          <span className="text-[10px] text-slate-500 font-normal">{pcb.processId}</span>
                        </td>
                        <td className="p-3">
                          <span className="text-[11px] px-2 py-0.5 rounded bg-slate-800 border border-slate-700 font-mono">
                            {pcb.processType}
                          </span>
                        </td>
                        <td className="p-3">
                          <span className={`px-2 py-0.5 rounded border font-mono font-bold ${getPriorityColor(pcb.effectivePriority)}`}>
                            {pcb.effectivePriority} {pcb.effectivePriority !== pcb.priority && `(base ${pcb.priority})`}
                          </span>
                        </td>
                        <td className="p-3 font-mono">
                          <span className="text-cyan-400 font-bold">{pcb.remainingBurstTime}</span> / {pcb.burstTime}t
                        </td>
                        <td className="p-3 font-mono text-slate-400">{pcb.waitingTime} ticks</td>
                        <td className="p-3 font-mono text-slate-400">{pcb.targetNodeId}</td>
                        <td className="p-3 font-mono text-slate-400">
                          {pcb.virtualRuntime ? pcb.virtualRuntime.toFixed(2) : '0.00'} vr
                        </td>
                        <td className="p-3 text-right">
                          <button
                            onClick={() => handleTerminateProcess(pcb.processId)}
                            className="p-1.5 text-rose-400 hover:text-rose-300 hover:bg-rose-500/10 rounded-lg transition-all"
                            title="Kill Process (SIGKILL)"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </td>
                      </tr>
                    ))
                  ))}

                {activeQueueTab === 'RUNNING' &&
                  (runningProcesses.length === 0 ? (
                    <tr>
                      <td colSpan="8" className="p-6 text-center text-slate-500">
                        No active processes executing on CPU cores.
                      </td>
                    </tr>
                  ) : (
                    runningProcesses.map((pcb) => (
                      <tr key={pcb.processId} className="hover:bg-slate-800/30 transition-colors">
                        <td className="p-3 font-medium font-mono text-white">
                          <div>{pcb.processName}</div>
                          <span className="text-[10px] text-slate-500 font-normal">{pcb.processId}</span>
                        </td>
                        <td className="p-3">
                          <span className="text-[11px] px-2 py-0.5 rounded bg-slate-800 border border-slate-700 font-mono">
                            {pcb.processType}
                          </span>
                        </td>
                        <td className="p-3">
                          <span className={`px-2 py-0.5 rounded border font-mono font-bold ${getPriorityColor(pcb.effectivePriority)}`}>
                            {pcb.effectivePriority}
                          </span>
                        </td>
                        <td className="p-3 font-mono">
                          <span className="text-cyan-400 font-bold">{pcb.remainingBurstTime}</span> / {pcb.burstTime}t
                        </td>
                        <td className="p-3 font-mono text-slate-400">{pcb.waitingTime} ticks</td>
                        <td className="p-3 font-mono text-slate-400">{pcb.targetNodeId}</td>
                        <td className="p-3">
                          <span className="px-2 py-0.5 rounded bg-cyan-500/10 text-cyan-300 border border-cyan-500/30 font-semibold font-mono animate-pulse">
                            CORE-{pcb.allocatedCoreId}
                          </span>
                        </td>
                        <td className="p-3 text-right">
                          <button
                            onClick={() => handleTerminateProcess(pcb.processId)}
                            className="p-1.5 text-rose-400 hover:text-rose-300 hover:bg-rose-500/10 rounded-lg transition-all"
                            title="Kill Process (SIGKILL)"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </td>
                      </tr>
                    ))
                  ))}

                {activeQueueTab === 'WAITING' &&
                  (waitingQueue.length === 0 ? (
                    <tr>
                      <td colSpan="8" className="p-6 text-center text-slate-500">
                        No processes currently blocked in I/O wait.
                      </td>
                    </tr>
                  ) : (
                    waitingQueue.map((pcb) => (
                      <tr key={pcb.processId} className="hover:bg-slate-800/30 transition-colors">
                        <td className="p-3 font-medium font-mono text-white">
                          <div>{pcb.processName}</div>
                          <span className="text-[10px] text-slate-500 font-normal">{pcb.processId}</span>
                        </td>
                        <td className="p-3">
                          <span className="text-[11px] px-2 py-0.5 rounded bg-amber-500/10 text-amber-300 border border-amber-500/30 font-mono">
                            {pcb.processType}
                          </span>
                        </td>
                        <td className="p-3">
                          <span className={`px-2 py-0.5 rounded border font-mono font-bold ${getPriorityColor(pcb.effectivePriority)}`}>
                            {pcb.effectivePriority}
                          </span>
                        </td>
                        <td className="p-3 font-mono">
                          <span className="text-cyan-400">{pcb.remainingBurstTime}</span> / {pcb.burstTime}t
                        </td>
                        <td className="p-3 font-mono text-slate-400">{pcb.waitingTime} ticks</td>
                        <td className="p-3 font-mono text-slate-400">{pcb.targetNodeId}</td>
                        <td className="p-3 font-mono text-amber-400">
                          {pcb.ioWaitRemaining} ticks I/O wait
                        </td>
                        <td className="p-3 text-right">
                          <button
                            onClick={() => handleTerminateProcess(pcb.processId)}
                            className="p-1.5 text-rose-400 hover:text-rose-300 hover:bg-rose-500/10 rounded-lg transition-all"
                            title="Kill Process"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </td>
                      </tr>
                    ))
                  ))}
              </tbody>
            </table>
          </div>
        </Card>
      </div>

      {/* ── Context Switch Log ── */}
      <Card className="p-5 bg-slate-900/60 border-slate-800">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <RefreshCw className="w-5 h-5 text-rose-400" />
            <h2 className="text-base font-semibold text-white">Live Context Switch Audit Trail</h2>
          </div>
          <span className="text-xs text-slate-400 font-mono">Real-time kernel dispatch log</span>
        </div>

        <div className="overflow-x-auto max-h-60 overflow-y-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-950/60 text-slate-400 font-semibold border-b border-slate-800 uppercase tracking-wider sticky top-0">
              <tr>
                <th className="p-2.5">Tick</th>
                <th className="p-2.5">Core</th>
                <th className="p-2.5">From Process</th>
                <th className="p-2.5">To Process</th>
                <th className="p-2.5">Switch Reason</th>
                <th className="p-2.5">Cost</th>
                <th className="p-2.5">Algorithm</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/40 text-slate-300 font-mono">
              {recentCS.length === 0 ? (
                <tr>
                  <td colSpan="7" className="p-4 text-center text-slate-500 font-sans">
                    No context switch events recorded yet.
                  </td>
                </tr>
              ) : (
                recentCS.map((cs, idx) => (
                  <tr key={idx} className="hover:bg-slate-800/20">
                    <td className="p-2.5 text-cyan-400 font-bold">#{cs.simulationTick}</td>
                    <td className="p-2.5 text-slate-300">CORE-{cs.cpuCoreNumber}</td>
                    <td className="p-2.5 text-slate-400">{cs.fromProcessId || '(IDLE)'}</td>
                    <td className="p-2.5 text-emerald-400 font-semibold">{cs.toProcessId || '(IDLE)'}</td>
                    <td className="p-2.5">
                      <span className="px-2 py-0.5 rounded text-[10px] bg-slate-800 border border-slate-700 text-slate-300">
                        {cs.reason}
                      </span>
                    </td>
                    <td className="p-2.5 text-slate-400">{cs.switchDurationMs} ms</td>
                    <td className="p-2.5 text-slate-400">{cs.algorithm}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* ── Create Process Modal ── */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-md p-6 shadow-2xl space-y-4">
            <h3 className="text-lg font-bold text-white flex items-center gap-2">
              <Plus className="w-5 h-5 text-cyan-400" /> Create Simulated Process
            </h3>

            <form onSubmit={handleCreateProcess} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-1">
                  Process Name
                </label>
                <input
                  type="text"
                  required
                  value={newProcess.processName}
                  onChange={(e) => setNewProcess({ ...newProcess, processName: e.target.value })}
                  className="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-cyan-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-1">
                    Process Type
                  </label>
                  <select
                    value={newProcess.processType}
                    onChange={(e) => setNewProcess({ ...newProcess, processType: e.target.value })}
                    className="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-cyan-500"
                  >
                    {PROCESS_TYPES.map((t) => (
                      <option key={t} value={t}>{t}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-1">
                    Priority (1 Highest, 10 Lowest)
                  </label>
                  <input
                    type="number"
                    min="1"
                    max="10"
                    value={newProcess.priority}
                    onChange={(e) => setNewProcess({ ...newProcess, priority: parseInt(e.target.value, 10) })}
                    className="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-cyan-500 font-mono"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-1">
                  CPU Burst Time (Simulation Ticks)
                </label>
                <input
                  type="number"
                  min="1"
                  max="100"
                  value={newProcess.burstTime}
                  onChange={(e) => setNewProcess({ ...newProcess, burstTime: parseInt(e.target.value, 10) })}
                  className="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-cyan-500 font-mono"
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-800">
                <Button variant="secondary" type="button" onClick={() => setShowCreateModal(false)}>
                  Cancel
                </Button>
                <Button variant="primary" type="submit" className="bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-bold">
                  Create Process
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── Batch Workload Modal ── */}
      {showBatchModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-md p-6 shadow-2xl space-y-4">
            <h3 className="text-lg font-bold text-white flex items-center gap-2">
              <Layers className="w-5 h-5 text-purple-400" /> Batch Workload Generator
            </h3>
            <p className="text-xs text-slate-400">
              Generates a diverse synthetic batch of CPU-bound, I/O-bound, and Network-bound processes with varied priorities to stress-test the scheduler.
            </p>

            <form onSubmit={handleBatchCreate} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-1">
                  Process Count
                </label>
                <input
                  type="number"
                  min="1"
                  max="50"
                  value={batchCount}
                  onChange={(e) => setBatchCount(e.target.value)}
                  className="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-cyan-500 font-mono"
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-800">
                <Button variant="secondary" type="button" onClick={() => setShowBatchModal(false)}>
                  Cancel
                </Button>
                <Button variant="primary" type="submit" className="bg-purple-600 hover:bg-purple-500 text-white font-bold">
                  Generate Workload
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── 7-Algorithm Benchmark Comparison Modal ── */}
      {showBenchmarkModal && benchmarkResult && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-5xl p-6 shadow-2xl space-y-6 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-slate-800 pb-4">
              <div className="flex items-center gap-3">
                <div className="p-2.5 bg-cyan-500/10 border border-cyan-500/30 rounded-xl text-cyan-400">
                  <Sparkles className="w-6 h-6" />
                </div>
                <div>
                  <h3 className="text-lg font-bold text-white">7-Algorithm Deterministic Benchmark Comparison</h3>
                  <p className="text-xs text-slate-400">
                    Evaluated against {benchmarkResult.workloadProcessCount} identical simulated processes across all strategies.
                  </p>
                </div>
              </div>

              <Button variant="secondary" size="sm" onClick={() => setShowBenchmarkModal(false)}>
                Close
              </Button>
            </div>

            {/* Summary Highlights */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="p-3.5 bg-emerald-950/20 border border-emerald-500/30 rounded-xl">
                <span className="text-[11px] text-emerald-400 font-semibold uppercase tracking-wider">Lowest Avg Waiting Time</span>
                <div className="text-base font-bold text-emerald-300 mt-1 font-mono">{benchmarkResult.bestWaitingTimeAlgorithm}</div>
              </div>

              <div className="p-3.5 bg-blue-950/20 border border-blue-500/30 rounded-xl">
                <span className="text-[11px] text-blue-400 font-semibold uppercase tracking-wider">Highest Throughput</span>
                <div className="text-base font-bold text-blue-300 mt-1 font-mono">{benchmarkResult.bestThroughputAlgorithm}</div>
              </div>

              <div className="p-3.5 bg-purple-950/20 border border-purple-500/30 rounded-xl">
                <span className="text-[11px] text-purple-400 font-semibold uppercase tracking-wider">Optimal Proportional Fairness</span>
                <div className="text-base font-bold text-purple-300 mt-1 font-mono">{benchmarkResult.bestFairnessAlgorithm}</div>
              </div>
            </div>

            {/* Comparative Table */}
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-950/80 text-slate-400 font-semibold border-b border-slate-800 uppercase tracking-wider">
                  <tr>
                    <th className="p-3">Algorithm</th>
                    <th className="p-3">Avg Wait (ticks)</th>
                    <th className="p-3">Avg Turnaround</th>
                    <th className="p-3">Avg Response</th>
                    <th className="p-3">CPU Util</th>
                    <th className="p-3">Context Switches</th>
                    <th className="p-3">Throughput</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60 text-slate-300 font-mono">
                  {benchmarkResult.results.map((res, idx) => (
                    <tr key={idx} className="hover:bg-slate-800/30">
                      <td className="p-3 font-sans font-semibold text-white flex items-center gap-2">
                        <span>{res.algorithmName}</span>
                      </td>
                      <td className="p-3 text-cyan-300 font-bold">{res.averageWaitingTime}</td>
                      <td className="p-3 text-slate-300">{res.averageTurnaroundTime}</td>
                      <td className="p-3 text-slate-300">{res.averageResponseTime}</td>
                      <td className="p-3 text-slate-300">{res.cpuUtilizationPercent}%</td>
                      <td className="p-3 text-rose-300">{res.totalContextSwitches}</td>
                      <td className="p-3 text-emerald-300">{res.throughput}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Algorithmic Explanations */}
            <div className="space-y-2 border-t border-slate-800 pt-4">
              <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider">Strategy Findings & Analysis</h4>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs text-slate-300">
                {benchmarkResult.results.map((res, idx) => (
                  <div key={idx} className="p-3 bg-slate-950/40 rounded-xl border border-slate-800/80">
                    <span className="font-bold text-cyan-400 block mb-1">{res.algorithmName}</span>
                    <p className="text-slate-400 leading-relaxed text-[11px]">{res.explanation}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Scheduler;
