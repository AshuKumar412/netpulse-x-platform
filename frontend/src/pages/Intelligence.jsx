import React, { useState, useEffect, useCallback } from 'react';
import {
  BrainCircuit,
  Activity,
  Server,
  Zap,
  ShieldCheck,
  ShieldAlert,
  AlertTriangle,
  Play,
  RotateCcw,
  RefreshCw,
  Sliders,
  CheckCircle2,
  XCircle,
  Clock,
  Cpu,
  HelpCircle,
  BarChart2,
  Layers,
  Settings,
  Plus,
  Compass,
  ArrowRight,
  Sparkles,
} from 'lucide-react';
import { predictiveService } from '../services/predictiveService';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { LoadingState } from '../components/common/LoadingState';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { useWebSocket } from '../context/WebSocketContext';
import { useAuth } from '../context/AuthContext';

export const Intelligence = () => {
  const { user } = useAuth();
  const { predictionUpdate } = useWebSocket();

  // Primary State
  const [summary, setSummary] = useState(null);
  const [selectedNodeId, setSelectedNodeId] = useState(null);
  const [selectedForecast, setSelectedForecast] = useState(null);
  const [models, setModels] = useState([]);
  const [activeFeatureImportances, setActiveFeatureImportances] = useState([]);
  const [history, setHistory] = useState([]);
  const [verificationStats, setVerificationStats] = useState(null);
  const [config, setConfig] = useState({
    predictiveRoutingEnabled: false,
    predictiveRoutingPenaltyWeight: 0.25,
    predictiveFailoverWarningEnabled: true,
    predictionHorizonMinutes: 5,
  });

  // UI State
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);
  const [activeTab, setActiveTab] = useState('riskMap'); // 'riskMap', 'outcomes', 'models', 'config', 'history'
  const [showTrainModal, setShowTrainModal] = useState(false);

  // Train Form
  const [trainForm, setTrainForm] = useState({
    modelType: 'MULTINOMIAL_LOGISTIC_REGRESSION',
    testSplitRatio: 0.20,
    learningRate: 0.05,
    maxEpochs: 200,
    l2Regularization: 0.001,
    maxDepth: 6,
    randomSeed: 42,
    notes: 'Standard production training run on historical telemetry',
  });

  // Load All Predictive Data
  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [sumRes, modRes, featRes, histRes, cfgRes] = await Promise.all([
        predictiveService.getSummary().catch(() => null),
        predictiveService.getModels().catch(() => ({ data: [] })),
        predictiveService.getActiveFeatureImportances().catch(() => ({ data: [] })),
        predictiveService.getHistory().catch(() => ({ data: [] })),
        predictiveService.getConfig().catch(() => null),
      ]);

      const summaryData = sumRes?.data || sumRes;
      setSummary(summaryData);
      setModels(modRes?.data || []);
      setActiveFeatureImportances(featRes?.data || []);
      setHistory(histRes?.data || []);
      if (cfgRes?.data) setConfig(cfgRes.data);

      if (summaryData?.latestNodePredictions && summaryData.latestNodePredictions.length > 0) {
        const initialNode = summaryData.latestNodePredictions[0].nodeId;
        setSelectedNodeId(initialNode);
        loadForecast(initialNode);
      }
    } catch (err) {
      console.error('Failed to load predictive intelligence:', err);
      setError('Failed to fetch predictive state from backend.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Handle WebSocket prediction updates
  useEffect(() => {
    if (!predictionUpdate) return;
    setSummary((prev) => {
      if (!prev) return prev;
      const updatedList = prev.latestNodePredictions?.map((p) =>
        p.nodeId === predictionUpdate.nodeId ? predictionUpdate : p
      ) || [];
      return { ...prev, latestNodePredictions: updatedList };
    });

    if (predictionUpdate.nodeId === selectedNodeId) {
      loadForecast(selectedNodeId);
    }
  }, [predictionUpdate, selectedNodeId]);

  // Load Node Forecast
  const loadForecast = async (nodeId) => {
    try {
      const res = await predictiveService.getForecastForNode(nodeId);
      setSelectedForecast(res?.data || res);
    } catch (err) {
      console.error(`Failed to load forecast for ${nodeId}:`, err);
    }
  };

  // Run On-Demand Predictions
  const handleRunPredictions = async () => {
    try {
      setActionLoading(true);
      setError(null);
      const res = await predictiveService.runPredictions();
      setSuccessMessage('Predictive inference executed successfully across all nodes.');
      await loadData();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to run predictive inference.');
    } finally {
      setActionLoading(false);
    }
  };

  // Verify Past Outcomes
  const handleVerifyOutcomes = async () => {
    try {
      setActionLoading(true);
      setError(null);
      const res = await predictiveService.verifyOutcomes();
      setVerificationStats(res?.data || res);
      setSuccessMessage('Historical predictions evaluated against subsequent telemetry.');
      const histRes = await predictiveService.getHistory();
      setHistory(histRes?.data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to evaluate outcomes.');
    } finally {
      setActionLoading(false);
    }
  };

  // Train Model
  const handleTrainModel = async (e) => {
    e.preventDefault();
    try {
      setActionLoading(true);
      setShowTrainModal(false);
      setError(null);
      const res = await predictiveService.trainModel(trainForm);
      setSuccessMessage(res?.message || 'Model training completed successfully.');
      await loadData();
    } catch (err) {
      setError(err.response?.data?.message || 'Model training failed.');
    } finally {
      setActionLoading(false);
    }
  };

  // Update Config
  const handleSaveConfig = async (e) => {
    e.preventDefault();
    try {
      setActionLoading(true);
      await predictiveService.updateConfig(config);
      setSuccessMessage('Predictive configuration updated successfully.');
    } catch (err) {
      setError('Failed to update predictive configuration.');
    } finally {
      setActionLoading(false);
    }
  };

  // State Badge Helper
  const getStateBadge = (state) => {
    switch (state) {
      case 'CONGESTED':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-rose-500/20 text-rose-300 border border-rose-500/40 flex items-center gap-1"><ShieldAlert className="w-3.5 h-3.5 text-rose-400" /> CONGESTED</span>;
      case 'CONGESTION_LIKELY':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-orange-500/20 text-orange-300 border border-orange-500/40 flex items-center gap-1"><AlertTriangle className="w-3.5 h-3.5 text-orange-400" /> CONGESTION LIKELY</span>;
      case 'WARNING':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/40 flex items-center gap-1"><Activity className="w-3.5 h-3.5 text-amber-400" /> WARNING</span>;
      case 'INSUFFICIENT_DATA':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-slate-800 text-slate-400 border border-slate-700 flex items-center gap-1"><HelpCircle className="w-3.5 h-3.5" /> INSUFFICIENT DATA</span>;
      case 'MODEL_UNAVAILABLE':
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-violet-500/20 text-violet-300 border border-violet-500/40 flex items-center gap-1"><Sliders className="w-3.5 h-3.5" /> MODEL UNAVAILABLE</span>;
      default:
        return <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 flex items-center gap-1"><ShieldCheck className="w-3.5 h-3.5 text-emerald-400" /> NORMAL</span>;
    }
  };

  if (loading) {
    return <LoadingState message="Connecting to Predictive ML Engine & evaluating telemetry models..." />;
  }

  return (
    <div className="space-y-8 animate-fadeIn">
      {/* ── Header & Action Controls ── */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-3xl font-bold tracking-tight text-white flex items-center gap-3">
              <BrainCircuit className="w-8 h-8 text-violet-400 animate-pulse" />
              Predictive Congestion & ML Intelligence
            </h1>
            <span className="px-3 py-1 text-xs font-semibold uppercase tracking-wider rounded-full bg-violet-500/10 text-violet-400 border border-violet-500/30">
              Machine Learning Active
            </span>
          </div>
          <p className="text-slate-400 mt-1 text-sm">
            Telemetry-driven machine learning, 5-minute congestion forecasting, and Decision Explainability Engine.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            size="sm"
            disabled={actionLoading}
            onClick={handleVerifyOutcomes}
            className="flex items-center gap-2 border-slate-700 hover:border-slate-600 text-slate-300"
          >
            <CheckCircle2 className="w-4 h-4 text-emerald-400" />
            Verify Outcomes
          </Button>

          <Button
            variant="outline"
            size="sm"
            disabled={actionLoading}
            onClick={handleRunPredictions}
            className="flex items-center gap-2 border-slate-700 hover:border-violet-500/50"
          >
            <Play className="w-4 h-4 text-violet-400" />
            Run Predictions
          </Button>

          {user?.role === 'ADMIN' && (
            <Button
              variant="primary"
              size="sm"
              onClick={() => setShowTrainModal(true)}
              className="flex items-center gap-2 bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-500 hover:to-indigo-500 border-0"
            >
              <Plus className="w-4 h-4" />
              Train Model
            </Button>
          )}
        </div>
      </div>

      {/* ── Alerts & Notifications ── */}
      {error && <ErrorMessage message={error} />}
      {successMessage && (
        <div className="p-3.5 rounded-xl bg-emerald-950/40 border border-emerald-500/40 text-xs text-emerald-300 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-400" />
            <span>{successMessage}</span>
          </div>
          <button onClick={() => setSuccessMessage(null)} className="text-slate-400 hover:text-white">&times;</button>
        </div>
      )}

      {/* ── Predictive Overview Metric Cards ── */}
      {summary && (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
          <Card className="bg-slate-900/60 border-slate-800 p-3.5 flex flex-col justify-between">
            <span className="text-xs text-slate-400 font-medium">Nodes Monitored</span>
            <div className="flex items-baseline justify-between mt-1">
              <span className="text-xl font-bold text-white font-mono">{summary.totalNodesMonitored}</span>
              <Server className="w-4 h-4 text-emerald-400" />
            </div>
          </Card>

          <Card className="bg-slate-900/60 border-slate-800 p-3.5 flex flex-col justify-between">
            <span className="text-xs text-slate-400 font-medium">Congestion Likely</span>
            <div className="flex items-baseline justify-between mt-1">
              <span className="text-xl font-bold text-orange-400 font-mono">{summary.congestionLikelyNodes}</span>
              <AlertTriangle className="w-4 h-4 text-orange-400" />
            </div>
          </Card>

          <Card className="bg-slate-900/60 border-slate-800 p-3.5 flex flex-col justify-between">
            <span className="text-xs text-slate-400 font-medium">Severe Congestion</span>
            <div className="flex items-baseline justify-between mt-1">
              <span className="text-xl font-bold text-rose-400 font-mono">{summary.congestedNodes}</span>
              <ShieldAlert className="w-4 h-4 text-rose-400" />
            </div>
          </Card>

          <Card className="bg-slate-900/60 border-slate-800 p-3.5 flex flex-col justify-between">
            <span className="text-xs text-slate-400 font-medium">Active Model</span>
            <div className="flex items-baseline justify-between mt-1">
              <span className="text-sm font-bold text-violet-300 font-mono truncate">{summary.activeModelVersion}</span>
              <BrainCircuit className="w-4 h-4 text-violet-400" />
            </div>
          </Card>

          <Card className="bg-slate-900/60 border-slate-800 p-3.5 flex flex-col justify-between">
            <span className="text-xs text-slate-400 font-medium">Model Accuracy</span>
            <div className="flex items-baseline justify-between mt-1">
              <span className="text-xl font-bold text-cyan-400 font-mono">
                {summary.activeModelAccuracy ? `${(summary.activeModelAccuracy * 100).toFixed(1)}%` : '—'}
              </span>
              <Activity className="w-4 h-4 text-cyan-400" />
            </div>
          </Card>

          <Card className="bg-slate-900/60 border-slate-800 p-3.5 flex flex-col justify-between">
            <span className="text-xs text-slate-400 font-medium">Data Quality</span>
            <div className="flex items-baseline justify-between mt-1">
              <span className={`text-xs font-bold font-mono ${
                summary.overallDataQuality === 'SUFFICIENT' ? 'text-emerald-400' : 'text-amber-400'
              }`}>
                {summary.overallDataQuality}
              </span>
              <ShieldCheck className="w-4 h-4 text-emerald-400" />
            </div>
          </Card>
        </div>
      )}

      {/* ── Main Tab Navigation ── */}
      <div className="flex border-b border-slate-800 gap-6">
        {[
          { id: 'riskMap', label: 'Node Risk Map & Forecast', icon: BrainCircuit },
          { id: 'outcomes', label: 'Prediction vs Actual Outcomes', icon: BarChart2 },
          { id: 'models', label: 'Model Intelligence & Weights', icon: Layers },
          { id: 'config', label: 'Predictive Policy Integration', icon: Settings },
          { id: 'history', label: 'Prediction Archive', icon: Clock },
        ].map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`pb-3 text-sm font-medium flex items-center gap-2 transition border-b-2 ${
                isActive
                  ? 'border-violet-400 text-violet-400'
                  : 'border-transparent text-slate-400 hover:text-slate-200'
              }`}
            >
              <Icon className="w-4 h-4" />
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* ── TAB 1: Node Risk Map & Forecast ── */}
      {activeTab === 'riskMap' && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Left: Node Grid */}
          <div className="lg:col-span-7 space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-base font-semibold text-white flex items-center gap-2">
                <Server className="w-4 h-4 text-violet-400" />
                Network Nodes Congestion Map
              </h2>
              <span className="text-xs text-slate-400">Select a node to inspect forecast</span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {summary?.latestNodePredictions?.map((pred) => {
                const isSelected = selectedNodeId === pred.nodeId;
                return (
                  <div
                    key={pred.nodeId}
                    onClick={() => {
                      setSelectedNodeId(pred.nodeId);
                      loadForecast(pred.nodeId);
                    }}
                    className={`p-4 rounded-xl border transition cursor-pointer space-y-3 ${
                      isSelected
                        ? 'bg-slate-900 border-violet-500/70 shadow-lg shadow-violet-500/10'
                        : 'bg-slate-900/60 border-slate-800 hover:border-slate-700'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-mono text-sm font-bold text-white">{pred.nodeId}</span>
                      {getStateBadge(pred.predictedState)}
                    </div>

                    <div className="grid grid-cols-3 gap-2 text-xs font-mono pt-1">
                      <div>
                        <span className="text-[10px] text-slate-500 block">CPU LOAD</span>
                        <span className="text-white font-semibold">{pred.currentCpu?.toFixed(1)}%</span>
                      </div>
                      <div>
                        <span className="text-[10px] text-slate-500 block">LATENCY</span>
                        <span className="text-white font-semibold">{pred.currentLatency?.toFixed(1)}ms</span>
                      </div>
                      <div>
                        <span className="text-[10px] text-slate-500 block">CONFIDENCE</span>
                        <span className="text-violet-400 font-semibold">{(pred.confidence * 100).toFixed(0)}%</span>
                      </div>
                    </div>

                    <div className="pt-2 border-t border-slate-800/80 text-[11px] text-slate-400 flex items-center justify-between">
                      <span>Trend: {pred.cpuTrendPercentPerMin > 0 ? `+${pred.cpuTrendPercentPerMin}%/m` : `${pred.cpuTrendPercentPerMin}%/m`}</span>
                      <span className="text-violet-400 font-semibold flex items-center gap-1">
                        Inspect &rarr;
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Right: Selected Node 5-Minute Forecast & Explainability */}
          <div className="lg:col-span-5 space-y-4">
            <h2 className="text-base font-semibold text-white flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-violet-400" />
              5-Minute Horizon Forecast & Explainability
            </h2>

            {selectedForecast ? (
              <Card className="bg-slate-900/90 border-violet-500/30 p-5 space-y-5">
                <div className="flex items-center justify-between border-b border-slate-800 pb-3">
                  <div>
                    <span className="text-xs font-mono text-slate-400">TARGET NODE</span>
                    <h3 className="text-lg font-bold text-white font-mono">{selectedForecast.nodeId}</h3>
                  </div>
                  <div>{getStateBadge(selectedForecast.forecastState)}</div>
                </div>

                {/* Forecast Trajectory */}
                <div className="space-y-3">
                  <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                    5-Minute Projected Trajectory:
                  </span>
                  <div className="grid grid-cols-2 gap-3">
                    <div className="p-3 rounded-lg bg-slate-950 border border-slate-800">
                      <span className="text-[10px] font-mono text-slate-500 block">PROJECTED CPU</span>
                      <span className="text-base font-bold font-mono text-white">
                        {selectedForecast.projectedCpu?.toFixed(1)}%
                      </span>
                    </div>
                    <div className="p-3 rounded-lg bg-slate-950 border border-slate-800">
                      <span className="text-[10px] font-mono text-slate-500 block">PROJECTED LATENCY</span>
                      <span className="text-base font-bold font-mono text-white">
                        {selectedForecast.projectedLatency?.toFixed(1)} ms
                      </span>
                    </div>
                  </div>
                </div>

                {/* Risk Gauge Bar */}
                <div className="space-y-1.5">
                  <div className="flex justify-between text-xs font-mono">
                    <span className="text-slate-400">Congestion Risk Score:</span>
                    <span className="text-violet-400 font-bold">{selectedForecast.riskScore?.toFixed(0)} / 100</span>
                  </div>
                  <div className="w-full h-2 rounded-full bg-slate-800 overflow-hidden">
                    <div
                      className={`h-full transition-all duration-500 ${
                        selectedForecast.riskScore >= 75 ? 'bg-rose-500' : selectedForecast.riskScore >= 50 ? 'bg-amber-500' : 'bg-emerald-500'
                      }`}
                      style={{ width: `${selectedForecast.riskScore}%` }}
                    ></div>
                  </div>
                </div>

                {/* Explainability Rationale */}
                <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800/80 text-xs text-slate-300 space-y-1.5">
                  <div className="font-semibold text-violet-300 flex items-center gap-1.5">
                    <HelpCircle className="w-3.5 h-3.5 text-violet-400" />
                    Explainability Breakdown ("Why?"):
                  </div>
                  <p className="leading-relaxed font-sans">{selectedForecast.rationale}</p>
                </div>
              </Card>
            ) : (
              <div className="text-center py-16 text-slate-500 text-xs border border-dashed border-slate-800 rounded-xl">
                Select a node to inspect its predictive forecast and decision explainability trees.
              </div>
            )}
          </div>
        </div>
      )}

      {/* ── TAB 2: Prediction vs Actual Outcomes ── */}
      {activeTab === 'outcomes' && (
        <div className="space-y-6">
          <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <h3 className="text-base font-semibold text-white flex items-center gap-2">
                <BarChart2 className="w-4 h-4 text-violet-400" />
                Historical Prediction Outcome Verification
              </h3>
              <p className="text-xs text-slate-400 mt-0.5">
                Evaluates past 5-minute forecasts against subsequent real telemetry collected after the prediction horizon elapsed.
              </p>
            </div>
            <Button
              variant="outline"
              size="sm"
              disabled={actionLoading}
              onClick={handleVerifyOutcomes}
              className="border-violet-500/40 text-violet-300 hover:bg-violet-500/10"
            >
              Run Outcome Verification
            </Button>
          </div>

          {verificationStats && (
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
              <Card className="bg-slate-900/80 border-slate-800 p-4 space-y-1">
                <span className="text-xs text-slate-400">Total Evaluated</span>
                <span className="text-xl font-bold font-mono text-white block">{verificationStats.totalPredictionsEvaluated}</span>
              </Card>
              <Card className="bg-slate-900/80 border-slate-800 p-4 space-y-1">
                <span className="text-xs text-slate-400">Empirical Accuracy</span>
                <span className="text-xl font-bold font-mono text-emerald-400 block">
                  {(verificationStats.verificationAccuracy * 100).toFixed(1)}%
                </span>
              </Card>
              <Card className="bg-slate-900/80 border-slate-800 p-4 space-y-1">
                <span className="text-xs text-slate-400">Empirical Precision</span>
                <span className="text-xl font-bold font-mono text-cyan-400 block">
                  {(verificationStats.empiricalPrecision * 100).toFixed(1)}%
                </span>
              </Card>
              <Card className="bg-slate-900/80 border-slate-800 p-4 space-y-1">
                <span className="text-xs text-slate-400">Empirical Recall</span>
                <span className="text-xl font-bold font-mono text-violet-400 block">
                  {(verificationStats.empiricalRecall * 100).toFixed(1)}%
                </span>
              </Card>
            </div>
          )}

          {/* Verification Table */}
          <Card className="bg-slate-900/90 border-slate-800 p-5 space-y-4">
            <h4 className="text-sm font-semibold text-white">Recent Predictions & Verification Status</h4>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-300 font-mono">
                <thead className="bg-slate-950 text-slate-400 uppercase border-b border-slate-800">
                  <tr>
                    <th className="py-2.5 px-3">Node</th>
                    <th className="py-2.5 px-3">Timestamp</th>
                    <th className="py-2.5 px-3">Forecasted State</th>
                    <th className="py-2.5 px-3">Confidence</th>
                    <th className="py-2.5 px-3">Subsequent Actual State</th>
                    <th className="py-2.5 px-3">Outcome</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800">
                  {history.slice(0, 15).map((h) => {
                    const isCorrect = h.outcomeVerified && h.predictedState === h.actualState;
                    return (
                      <tr key={h.id} className="hover:bg-slate-800/40">
                        <td className="py-2.5 px-3 font-bold text-white">{h.nodeId}</td>
                        <td className="py-2.5 px-3 text-slate-500">{new Date(h.timestamp).toLocaleTimeString()}</td>
                        <td className="py-2.5 px-3">{h.predictedState}</td>
                        <td className="py-2.5 px-3 text-violet-400">{(h.confidence * 100).toFixed(0)}%</td>
                        <td className="py-2.5 px-3">{h.actualState || 'Pending horizon...'}</td>
                        <td className="py-2.5 px-3">
                          {h.outcomeVerified ? (
                            isCorrect ? (
                              <span className="text-emerald-400 font-semibold flex items-center gap-1">
                                <CheckCircle2 className="w-3.5 h-3.5" /> ACCURATE
                              </span>
                            ) : (
                              <span className="text-rose-400 font-semibold flex items-center gap-1">
                                <XCircle className="w-3.5 h-3.5" /> DIVERGED
                              </span>
                            )
                          ) : (
                            <span className="text-slate-500">Awaiting telemetry</span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </Card>
        </div>
      )}

      {/* ── TAB 3: Model Intelligence & Weights ── */}
      {activeTab === 'models' && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Left: Active Feature Importances */}
            <Card className="bg-slate-900/90 border-slate-800 p-5 space-y-4">
              <h3 className="text-base font-semibold text-white flex items-center gap-2">
                <Sliders className="w-4 h-4 text-violet-400" />
                Model-Derived Feature Importances
              </h3>
              <p className="text-xs text-slate-400">
                Mathematical weights computed directly from model parameters (Normalized L2 magnitude / MDI).
              </p>

              <div className="space-y-2.5">
                {activeFeatureImportances.map((f, idx) => (
                  <div key={idx} className="space-y-1">
                    <div className="flex justify-between text-xs font-mono">
                      <span className="text-slate-300">{f.featureName}</span>
                      <span className="text-violet-400 font-bold">{f.relativePercentage?.toFixed(1)}%</span>
                    </div>
                    <div className="w-full h-1.5 rounded-full bg-slate-950 overflow-hidden">
                      <div
                        className="h-full bg-gradient-to-r from-violet-500 to-indigo-500 rounded-full"
                        style={{ width: `${f.relativePercentage}%` }}
                      ></div>
                    </div>
                  </div>
                ))}
              </div>
            </Card>

            {/* Right: Versioned Models Catalog */}
            <Card className="bg-slate-900/90 border-slate-800 p-5 space-y-4">
              <h3 className="text-base font-semibold text-white flex items-center gap-2">
                <Layers className="w-4 h-4 text-cyan-400" />
                Versioned Models Catalog
              </h3>

              <div className="space-y-3">
                {models.map((m) => (
                  <div
                    key={m.id}
                    className="p-3.5 rounded-lg bg-slate-950 border border-slate-800 space-y-2 font-mono text-xs"
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-white">{m.modelVersion}</span>
                      <span className={`px-2 py-0.5 rounded text-[11px] ${
                        m.status === 'READY' ? 'bg-emerald-500/20 text-emerald-300' : 'bg-slate-800 text-slate-400'
                      }`}>
                        {m.status}
                      </span>
                    </div>
                    <div className="grid grid-cols-3 gap-2 text-slate-400 text-[11px]">
                      <div>TYPE: {m.modelType}</div>
                      <div>SAMPLES: {m.numberOfSamples}</div>
                      <div>ACCURACY: {m.accuracy ? `${(m.accuracy * 100).toFixed(1)}%` : '—'}</div>
                    </div>
                    <div className="text-[10px] text-slate-500">
                      TRAINED: {new Date(m.trainingStartedAt).toLocaleString()} by {m.createdBy}
                    </div>
                  </div>
                ))}
              </div>
            </Card>
          </div>
        </div>
      )}

      {/* ── TAB 4: Predictive Policy Integration ── */}
      {activeTab === 'config' && (
        <Card className="bg-slate-900/90 border-slate-800 p-6 max-w-2xl mx-auto space-y-6">
          <div>
            <h3 className="text-lg font-bold text-white flex items-center gap-2">
              <Compass className="w-5 h-5 text-violet-400" />
              Predictive Routing & Failover Integration
            </h3>
            <p className="text-xs text-slate-400 mt-1">
              Configure how ML forecasts influence candidate routing scoring and automated recovery advisories.
            </p>
          </div>

          <form onSubmit={handleSaveConfig} className="space-y-5">
            <div className="flex items-center justify-between p-3.5 rounded-xl bg-slate-950 border border-slate-800">
              <div>
                <label className="text-sm font-semibold text-white block">Predictive Traffic Routing</label>
                <span className="text-xs text-slate-400">
                  Applies congestion risk penalties to candidate scores during traffic routing decisions.
                </span>
              </div>
              <input
                type="checkbox"
                checked={config.predictiveRoutingEnabled}
                onChange={(e) => setConfig({ ...config, predictiveRoutingEnabled: e.target.checked })}
                className="w-4 h-4 rounded bg-slate-900 border-slate-700 text-violet-600 focus:ring-violet-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
                Predictive Penalty Weight ({config.predictiveRoutingPenaltyWeight})
              </label>
              <input
                type="range"
                min="0.0"
                max="1.0"
                step="0.05"
                value={config.predictiveRoutingPenaltyWeight}
                onChange={(e) => setConfig({ ...config, predictiveRoutingPenaltyWeight: parseFloat(e.target.value) })}
                className="w-full accent-violet-500"
              />
            </div>

            <div className="flex items-center justify-between p-3.5 rounded-xl bg-slate-950 border border-slate-800">
              <div>
                <label className="text-sm font-semibold text-white block">Predictive Failover Advisory Warnings</label>
                <span className="text-xs text-slate-400">
                  Emits early advisory alerts for nodes with severe impending congestion before hardware outages.
                </span>
              </div>
              <input
                type="checkbox"
                checked={config.predictiveFailoverWarningEnabled}
                onChange={(e) => setConfig({ ...config, predictiveFailoverWarningEnabled: e.target.checked })}
                className="w-4 h-4 rounded bg-slate-900 border-slate-700 text-violet-600 focus:ring-violet-500"
              />
            </div>

            {user?.role === 'ADMIN' && (
              <Button
                type="submit"
                variant="primary"
                size="sm"
                className="w-full bg-violet-600 hover:bg-violet-500 border-0"
              >
                Save Predictive Configuration
              </Button>
            )}
          </form>
        </Card>
      )}

      {/* ── TAB 5: Prediction Archive ── */}
      {activeTab === 'history' && (
        <Card className="bg-slate-900/90 border-slate-800 p-5 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-base font-semibold text-white flex items-center gap-2">
              <Clock className="w-4 h-4 text-violet-400" />
              Prediction Archive ({history.length} records)
            </h3>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-300 font-mono">
              <thead className="bg-slate-950 text-slate-400 uppercase border-b border-slate-800">
                <tr>
                  <th className="py-2.5 px-3">Prediction ID</th>
                  <th className="py-2.5 px-3">Node</th>
                  <th className="py-2.5 px-3">Timestamp</th>
                  <th className="py-2.5 px-3">State</th>
                  <th className="py-2.5 px-3">Confidence</th>
                  <th className="py-2.5 px-3">CPU</th>
                  <th className="py-2.5 px-3">Latency</th>
                  <th className="py-2.5 px-3">Quality</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {history.map((h) => (
                  <tr key={h.id} className="hover:bg-slate-800/40">
                    <td className="py-2.5 px-3 font-bold text-violet-400">{h.predictionId}</td>
                    <td className="py-2.5 px-3 text-white font-semibold">{h.nodeId}</td>
                    <td className="py-2.5 px-3 text-slate-500">{new Date(h.timestamp).toLocaleTimeString()}</td>
                    <td className="py-2.5 px-3">{h.predictedState}</td>
                    <td className="py-2.5 px-3 text-violet-300">{(h.confidence * 100).toFixed(0)}%</td>
                    <td className="py-2.5 px-3">{h.cpuUsage?.toFixed(1)}%</td>
                    <td className="py-2.5 px-3">{h.latency?.toFixed(1)} ms</td>
                    <td className="py-2.5 px-3">{h.dataQuality}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* ── Model Training Modal ── */}
      {showTrainModal && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4 animate-fadeIn">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto p-6 space-y-5 shadow-2xl">
            <div className="flex items-center justify-between border-b border-slate-800 pb-4">
              <div className="flex items-center gap-3">
                <BrainCircuit className="w-6 h-6 text-violet-400" />
                <h3 className="text-lg font-bold text-white">Train Predictive ML Model</h3>
              </div>
              <button
                onClick={() => setShowTrainModal(false)}
                className="text-slate-400 hover:text-white text-lg font-bold p-1"
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleTrainModel} className="space-y-4 text-xs font-sans">
              <div>
                <label className="block font-semibold text-slate-300 uppercase tracking-wider mb-1">
                  Model Algorithm
                </label>
                <select
                  value={trainForm.modelType}
                  onChange={(e) => setTrainForm({ ...trainForm, modelType: e.target.value })}
                  className="w-full px-3.5 py-2 rounded-lg bg-slate-950 border border-slate-800 text-white text-xs"
                >
                  <option value="MULTINOMIAL_LOGISTIC_REGRESSION">Multinomial Logistic Regression (Softmax + L2)</option>
                  <option value="DECISION_TREE_CLASSIFIER">Decision Tree Classifier (CART + Gini)</option>
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-slate-300 uppercase tracking-wider mb-1">
                    Test Split Ratio
                  </label>
                  <input
                    type="number"
                    step="0.05"
                    min="0.10"
                    max="0.50"
                    value={trainForm.testSplitRatio}
                    onChange={(e) => setTrainForm({ ...trainForm, testSplitRatio: parseFloat(e.target.value) })}
                    className="w-full px-3.5 py-2 rounded-lg bg-slate-950 border border-slate-800 text-white font-mono"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-slate-300 uppercase tracking-wider mb-1">
                    Random Seed
                  </label>
                  <input
                    type="number"
                    value={trainForm.randomSeed}
                    onChange={(e) => setTrainForm({ ...trainForm, randomSeed: parseInt(e.target.value) })}
                    className="w-full px-3.5 py-2 rounded-lg bg-slate-950 border border-slate-800 text-white font-mono"
                  />
                </div>
              </div>

              <div>
                <label className="block font-semibold text-slate-300 uppercase tracking-wider mb-1">
                  Notes
                </label>
                <textarea
                  rows={2}
                  value={trainForm.notes}
                  onChange={(e) => setTrainForm({ ...trainForm, notes: e.target.value })}
                  className="w-full px-3.5 py-2 rounded-lg bg-slate-950 border border-slate-800 text-white text-xs"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-800">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setShowTrainModal(false)}
                  className="border-slate-700 text-slate-300"
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  variant="primary"
                  size="sm"
                  className="bg-violet-600 hover:bg-violet-500 border-0 flex items-center gap-2"
                >
                  <Play className="w-4 h-4" /> Start Training Run
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Intelligence;
