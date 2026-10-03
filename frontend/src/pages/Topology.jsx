import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { Link } from 'react-router-dom';
import {
  GitFork,
  Server,
  Play,
  Pause,
  Square,
  Plus,
  RefreshCw,
  Activity,
  Layers,
  Radio,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  Clock,
  Shield,
  Trash2,
  Edit3,
  Sliders,
  Maximize2,
  ZoomIn,
  ZoomOut,
  Info,
} from 'lucide-react';
import { topologyService } from '../services/topologyService';
import { simulationService } from '../services/simulationService';
import { nodeService } from '../services/nodeService';
import { telemetryService } from '../services/telemetryService';
import { healthService } from '../services/healthService';
import { heartbeatService } from '../services/heartbeatService';
import { useAuth } from '../context/AuthContext';
import { useWebSocket } from '../context/WebSocketContext';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { Modal } from '../components/common/Modal';
import { StatusBadge } from '../components/common/StatusBadge';
import { LoadingState } from '../components/common/LoadingState';
import { ErrorMessage } from '../components/common/ErrorMessage';

const INITIAL_LINK_FORM = {
  linkId: '',
  sourceNodeId: '',
  targetNodeId: '',
  linkType: 'DIRECT',
  bandwidth: 1000,
  weight: 1.0,
  enabled: true,
  status: 'ACTIVE',
};

export const Topology = () => {
  const [topology, setTopology] = useState(null);
  const [simulationStatus, setSimulationStatus] = useState(null);
  const [allNodes, setAllNodes] = useState([]);
  const [telemetryMap, setTelemetryMap] = useState({});
  const [healthMap, setHealthMap] = useState({});
  const [heartbeatMap, setHeartbeatMap] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Inspector & Selection
  const [selectedNode, setSelectedNode] = useState(null);
  const [selectedLink, setSelectedLink] = useState(null);

  // Modals
  const [isCreateLinkOpen, setIsCreateLinkOpen] = useState(false);
  const [isEditLinkOpen, setIsEditLinkOpen] = useState(false);
  const [isDeleteLinkOpen, setIsDeleteLinkOpen] = useState(false);
  const [linkFormData, setLinkFormData] = useState(INITIAL_LINK_FORM);
  const [formErrors, setFormErrors] = useState({});
  const [actionLoading, setActionLoading] = useState(false);

  // Canvas View Controls
  const [zoom, setZoom] = useState(1);
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState(false);
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 });
  const [nodePositions, setNodePositions] = useState({});
  const [draggingNode, setDraggingNode] = useState(null);

  const { isOperator, isAdmin } = useAuth();
  const { topologyUpdate, simulationUpdate, telemetryUpdate, healthUpdate, heartbeatUpdate } = useWebSocket();

  // Fetch initial topology and simulation data
  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [topoRes, simRes, nodesRes, telemRes, healthRes, hbRes] = await Promise.all([
        topologyService.getTopology(),
        simulationService.getStatus(),
        nodeService.getAllNodes(),
        telemetryService.getAllTelemetry().catch(() => ({ data: [] })),
        healthService.getAllNodeHealth().catch(() => ({ data: [] })),
        heartbeatService.getAllHeartbeatStatuses().catch(() => ({ data: [] })),
      ]);

      if (topoRes.success && topoRes.data) {
        setTopology(topoRes.data);
      }
      if (simRes.success && simRes.data) {
        setSimulationStatus(simRes.data);
      }
      if (nodesRes.success && nodesRes.data) {
        setAllNodes(nodesRes.data);
      }
      if (telemRes?.data) {
        const tMap = {};
        telemRes.data.forEach((t) => { tMap[t.nodeId] = t; });
        setTelemetryMap(tMap);
      }
      if (healthRes?.data) {
        const hMap = {};
        healthRes.data.forEach((h) => { hMap[h.nodeId] = h; });
        setHealthMap(hMap);
      }
      if (hbRes?.data) {
        const hbMap = {};
        hbRes.data.forEach((hb) => { hbMap[hb.nodeId] = hb; });
        setHeartbeatMap(hbMap);
      }
    } catch (err) {
      setError(err.message || 'Failed to load network topology');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // WebSocket Live Updates
  useEffect(() => {
    if (topologyUpdate) {
      setTopology(topologyUpdate);
    }
  }, [topologyUpdate]);

  useEffect(() => {
    if (simulationUpdate) {
      setSimulationStatus(simulationUpdate);
    }
  }, [simulationUpdate]);

  useEffect(() => {
    if (telemetryUpdate && Array.isArray(telemetryUpdate)) {
      const tMap = { ...telemetryMap };
      telemetryUpdate.forEach((t) => { tMap[t.nodeId] = t; });
      setTelemetryMap(tMap);
    }
  }, [telemetryUpdate]);

  useEffect(() => {
    if (healthUpdate && Array.isArray(healthUpdate)) {
      const hMap = { ...healthMap };
      healthUpdate.forEach((h) => { hMap[h.nodeId] = h; });
      setHealthMap(hMap);
    }
  }, [healthUpdate]);

  useEffect(() => {
    if (heartbeatUpdate && Array.isArray(heartbeatUpdate)) {
      const hbMap = { ...heartbeatMap };
      heartbeatUpdate.forEach((hb) => { hbMap[hb.nodeId] = hb; });
      setHeartbeatMap(hbMap);
    }
  }, [heartbeatUpdate]);

  // Compute node layout coordinates
  useEffect(() => {
    if (!topology || !topology.nodes || topology.nodes.length === 0) return;

    const width = 800;
    const height = 500;
    const radius = Math.min(width, height) / 2 - 80;
    const centerX = width / 2;
    const centerY = height / 2;

    const positions = {};
    const count = topology.nodes.length;

    topology.nodes.forEach((node, index) => {
      if (nodePositions[node.nodeId]) {
        positions[node.nodeId] = nodePositions[node.nodeId];
      } else {
        if (count === 1) {
          positions[node.nodeId] = { x: centerX, y: centerY };
        } else {
          const angle = (index / count) * 2 * Math.PI - Math.PI / 2;
          positions[node.nodeId] = {
            x: centerX + radius * Math.cos(angle),
            y: centerY + radius * Math.sin(angle),
          };
        }
      }
    });

    setNodePositions(positions);
  }, [topology?.nodes]);

  // Simulation Controls
  const handleStartSim = async () => {
    try {
      setActionLoading(true);
      const res = await simulationService.startSimulation();
      if (res.success && res.data) setSimulationStatus(res.data);
    } catch (err) {
      setError(err.message || 'Failed to start simulation');
    } finally {
      setActionLoading(false);
    }
  };

  const handlePauseSim = async () => {
    try {
      setActionLoading(true);
      const res = await simulationService.pauseSimulation();
      if (res.success && res.data) setSimulationStatus(res.data);
    } catch (err) {
      setError(err.message || 'Failed to pause simulation');
    } finally {
      setActionLoading(false);
    }
  };

  const handleStopSim = async () => {
    try {
      setActionLoading(true);
      const res = await simulationService.stopSimulation();
      if (res.success && res.data) setSimulationStatus(res.data);
    } catch (err) {
      setError(err.message || 'Failed to stop simulation');
    } finally {
      setActionLoading(false);
    }
  };

  // Link Management Handlers
  const handleCreateLinkOpen = () => {
    setLinkFormData({
      ...INITIAL_LINK_FORM,
      sourceNodeId: allNodes[0]?.nodeId || '',
      targetNodeId: allNodes[1]?.nodeId || '',
    });
    setFormErrors({});
    setIsCreateLinkOpen(true);
  };

  const handleCreateLinkSubmit = async (e) => {
    e.preventDefault();
    if (!linkFormData.sourceNodeId || !linkFormData.targetNodeId) {
      setFormErrors({ general: 'Source and Target nodes are required' });
      return;
    }
    if (linkFormData.sourceNodeId === linkFormData.targetNodeId) {
      setFormErrors({ targetNodeId: 'Target node cannot be the same as Source node' });
      return;
    }

    try {
      setActionLoading(true);
      await topologyService.createLink({
        ...linkFormData,
        bandwidth: Number(linkFormData.bandwidth),
        weight: Number(linkFormData.weight),
      });
      setIsCreateLinkOpen(false);
      fetchData();
    } catch (err) {
      setFormErrors({ general: err.message || 'Failed to create link' });
    } finally {
      setActionLoading(false);
    }
  };

  const handleEditLinkOpen = (link) => {
    setSelectedLink(link);
    setLinkFormData({
      linkId: link.linkId,
      sourceNodeId: link.sourceNodeId,
      targetNodeId: link.targetNodeId,
      linkType: link.linkType,
      bandwidth: link.bandwidth,
      weight: link.weight,
      enabled: link.enabled,
      status: link.status,
    });
    setFormErrors({});
    setIsEditLinkOpen(true);
  };

  const handleEditLinkSubmit = async (e) => {
    e.preventDefault();
    if (!selectedLink) return;

    try {
      setActionLoading(true);
      await topologyService.updateLink(selectedLink.id, {
        linkType: linkFormData.linkType,
        bandwidth: Number(linkFormData.bandwidth),
        weight: Number(linkFormData.weight),
        enabled: linkFormData.enabled,
        status: linkFormData.status,
      });
      setIsEditLinkOpen(false);
      fetchData();
    } catch (err) {
      setFormErrors({ general: err.message || 'Failed to update link' });
    } finally {
      setActionLoading(false);
    }
  };

  const handleDeleteLinkOpen = (link) => {
    setSelectedLink(link);
    setIsDeleteLinkOpen(true);
  };

  const handleDeleteLinkSubmit = async () => {
    if (!selectedLink) return;
    try {
      setActionLoading(true);
      await topologyService.deleteLink(selectedLink.id);
      setIsDeleteLinkOpen(false);
      setSelectedLink(null);
      fetchData();
    } catch (err) {
      setError(err.message || 'Failed to delete link');
    } finally {
      setActionLoading(false);
    }
  };

  // Node Dragging on Canvas
  const handleNodeMouseDown = (e, nodeId) => {
    e.stopPropagation();
    setDraggingNode(nodeId);
  };

  const handleSvgMouseMove = (e) => {
    if (draggingNode && nodePositions[draggingNode]) {
      const svg = e.currentTarget.getBoundingClientRect();
      const x = (e.clientX - svg.left - pan.x) / zoom;
      const y = (e.clientY - svg.top - pan.y) / zoom;
      setNodePositions((prev) => ({
        ...prev,
        [draggingNode]: { x, y },
      }));
    } else if (isDragging) {
      setPan({
        x: e.clientX - dragStart.x,
        y: e.clientY - dragStart.y,
      });
    }
  };

  const handleSvgMouseUp = () => {
    setDraggingNode(null);
    setIsDragging(false);
  };

  const handleSvgMouseDown = (e) => {
    if (e.target.tagName === 'svg' || e.target.tagName === 'rect') {
      setIsDragging(true);
      setDragStart({ x: e.clientX - pan.x, y: e.clientY - pan.y });
      setSelectedNode(null);
      setSelectedLink(null);
    }
  };

  // Node status colors
  const getNodeColor = (status) => {
    switch (status) {
      case 'HEALTHY':
        return { fill: '#10b981', stroke: '#059669', bg: 'bg-emerald-500/20', text: 'text-emerald-400' };
      case 'WARNING':
        return { fill: '#f59e0b', stroke: '#d97706', bg: 'bg-amber-500/20', text: 'text-amber-400' };
      case 'CONGESTED':
        return { fill: '#f97316', stroke: '#ea580c', bg: 'bg-orange-500/20', text: 'text-orange-400' };
      case 'FAILED':
        return { fill: '#f43f5e', stroke: '#e11d48', bg: 'bg-rose-500/20', text: 'text-rose-400' };
      case 'OFFLINE':
      default:
        return { fill: '#64748b', stroke: '#475569', bg: 'bg-slate-500/20', text: 'text-slate-400' };
    }
  };

  // Link status styles
  const getLinkStyle = (link) => {
    if (!link.enabled || link.status === 'OFFLINE') {
      return { stroke: '#475569', strokeWidth: 1.5, strokeDasharray: '4,4', opacity: 0.4 };
    }
    switch (link.status) {
      case 'ACTIVE':
        return { stroke: '#0284c7', strokeWidth: 2.5, strokeDasharray: 'none', opacity: 0.9 };
      case 'DEGRADED':
        return { stroke: '#f59e0b', strokeWidth: 2, strokeDasharray: '6,4', opacity: 0.8 };
      case 'FAILED':
        return { stroke: '#f43f5e', strokeWidth: 2, strokeDasharray: '4,4', opacity: 0.9 };
      default:
        return { stroke: '#64748b', strokeWidth: 1.5, strokeDasharray: 'none', opacity: 0.5 };
    }
  };

  if (loading) {
    return <LoadingState message="Calculating network topology graph..." />;
  }

  if (error) {
    return <ErrorMessage message={error} onRetry={fetchData} />;
  }

  const nodes = topology?.nodes || [];
  const links = topology?.links || [];
  const summary = topology?.summary || {};
  const hasNodes = nodes.length > 0;

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2.5">
            <GitFork className="w-6 h-6 text-sky-400" />
            Network Topology Visualizer
          </h1>
          <p className="text-xs text-slate-400 font-mono mt-1">
            Dynamic Graph Representation &bull; Active Connections &bull; Deterministic Engine
          </p>
        </div>

        <div className="flex items-center flex-wrap gap-2.5">
          {/* Simulation Control Bar */}
          <div className="flex items-center bg-[#111622] border border-[#1e2638] rounded-lg p-1">
            <div className="px-2.5 py-1 flex items-center gap-1.5 border-r border-[#1e2638]">
              <span className={`w-2 h-2 rounded-full ${
                simulationStatus?.status === 'RUNNING' ? 'bg-emerald-400 animate-pulse' :
                simulationStatus?.status === 'PAUSED' ? 'bg-amber-400' : 'bg-slate-500'
              }`} />
              <span className="text-[11px] font-mono font-bold text-slate-300">
                {simulationStatus?.status || 'STOPPED'}
              </span>
              {simulationStatus?.status === 'RUNNING' && (
                <span className="text-[10px] font-mono text-slate-400 ml-1">
                  t:{simulationStatus.tickCount}
                </span>
              )}
            </div>

            {isOperator && (
              <div className="flex items-center gap-1 px-1">
                {simulationStatus?.status !== 'RUNNING' ? (
                  <button
                    onClick={handleStartSim}
                    disabled={actionLoading}
                    title="Start Topology Simulation"
                    className="p-1.5 rounded hover:bg-emerald-500/20 text-emerald-400 transition-colors"
                  >
                    <Play className="w-3.5 h-3.5 fill-current" />
                  </button>
                ) : (
                  <button
                    onClick={handlePauseSim}
                    disabled={actionLoading}
                    title="Pause Simulation"
                    className="p-1.5 rounded hover:bg-amber-500/20 text-amber-400 transition-colors"
                  >
                    <Pause className="w-3.5 h-3.5 fill-current" />
                  </button>
                )}
                <button
                  onClick={handleStopSim}
                  disabled={actionLoading || simulationStatus?.status === 'STOPPED'}
                  title="Stop Simulation"
                  className="p-1.5 rounded hover:bg-rose-500/20 text-rose-400 transition-colors disabled:opacity-30"
                >
                  <Square className="w-3.5 h-3.5 fill-current" />
                </button>
              </div>
            )}
          </div>

          <Button variant="outline" size="sm" onClick={fetchData} icon={RefreshCw}>
            Reload
          </Button>

          {isOperator && hasNodes && (
            <Button variant="primary" size="sm" onClick={handleCreateLinkOpen} icon={Plus}>
              Establish Link
            </Button>
          )}
        </div>
      </div>

      {/* Graph Theory Summary Bar */}
      <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-6 gap-3">
        <div className="p-3 rounded-xl bg-[#111622] border border-[#1e2638]">
          <span className="text-[10px] font-mono font-semibold uppercase tracking-wider text-slate-400">Nodes (Act/Tot)</span>
          <p className="text-lg font-bold font-mono text-sky-400 mt-0.5">
            {summary.activeNodes ?? 0} <span className="text-xs text-slate-500">/ {summary.totalNodes ?? 0}</span>
          </p>
        </div>

        <div className="p-3 rounded-xl bg-[#111622] border border-[#1e2638]">
          <span className="text-[10px] font-mono font-semibold uppercase tracking-wider text-slate-400">Links (Act/Tot)</span>
          <p className="text-lg font-bold font-mono text-emerald-400 mt-0.5">
            {summary.activeLinks ?? 0} <span className="text-xs text-slate-500">/ {summary.totalLinks ?? 0}</span>
          </p>
        </div>

        <div className="p-3 rounded-xl bg-[#111622] border border-[#1e2638]">
          <span className="text-[10px] font-mono font-semibold uppercase tracking-wider text-slate-400">Degraded / Failed</span>
          <p className="text-lg font-bold font-mono text-amber-400 mt-0.5">
            {summary.degradedNodes ?? 0} <span className="text-xs text-slate-500">/</span> <span className="text-rose-400">{summary.failedNodes ?? 0}</span>
          </p>
        </div>

        <div className="p-3 rounded-xl bg-[#111622] border border-[#1e2638]">
          <span className="text-[10px] font-mono font-semibold uppercase tracking-wider text-slate-400">Components</span>
          <p className="text-lg font-bold font-mono text-white mt-0.5">
            {summary.connectedComponents ?? 0}
          </p>
        </div>

        <div className="p-3 rounded-xl bg-[#111622] border border-[#1e2638]">
          <span className="text-[10px] font-mono font-semibold uppercase tracking-wider text-slate-400">Disconnected</span>
          <p className="text-lg font-bold font-mono text-slate-300 mt-0.5">
            {summary.disconnectedNodes ?? 0}
          </p>
        </div>

        <div className="p-3 rounded-xl bg-[#111622] border border-[#1e2638]">
          <span className="text-[10px] font-mono font-semibold uppercase tracking-wider text-slate-400">Bandwidth</span>
          <p className="text-lg font-bold font-mono text-sky-300 mt-0.5">
            {(summary.totalBandwidth ?? 0).toLocaleString()} <span className="text-[10px] text-slate-500">Mbps</span>
          </p>
        </div>
      </div>

      {/* Main Visualizer Container */}
      {!hasNodes ? (
        <Card className="py-20 text-center">
          <Server className="w-14 h-14 text-slate-600 mx-auto mb-3" />
          <h4 className="text-lg font-semibold text-slate-200">No network nodes configured.</h4>
          <p className="text-xs text-slate-400 mt-1 max-w-md mx-auto">
            The cluster topology currently contains zero registered endpoints. Provision nodes in Node Management to begin constructing the graph.
          </p>
          {isOperator && (
            <div className="mt-5">
              <Link to="/nodes">
                <Button variant="primary" size="sm" icon={Plus}>
                  Provision First Node
                </Button>
              </Link>
            </div>
          )}
        </Card>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          {/* Interactive Topology Graph Canvas */}
          <div className="lg:col-span-3 bg-[#0a0d14] border border-[#1e2638] rounded-2xl overflow-hidden relative shadow-2xl flex flex-col min-h-[550px]">
            {/* Canvas Toolbar */}
            <div className="absolute top-4 left-4 z-10 flex items-center gap-2 bg-[#111622]/90 backdrop-blur border border-[#1e2638] rounded-lg p-1">
              <button
                onClick={() => setZoom((z) => Math.min(z + 0.2, 2.5))}
                title="Zoom In"
                className="p-1.5 rounded hover:bg-slate-800 text-slate-300 transition-colors"
              >
                <ZoomIn className="w-4 h-4" />
              </button>
              <button
                onClick={() => setZoom((z) => Math.max(z - 0.2, 0.5))}
                title="Zoom Out"
                className="p-1.5 rounded hover:bg-slate-800 text-slate-300 transition-colors"
              >
                <ZoomOut className="w-4 h-4" />
              </button>
              <button
                onClick={() => { setZoom(1); setPan({ x: 0, y: 0 }); }}
                title="Reset View"
                className="p-1.5 rounded hover:bg-slate-800 text-slate-300 transition-colors"
              >
                <Maximize2 className="w-4 h-4" />
              </button>
            </div>

            {/* Canvas Legend */}
            <div className="absolute bottom-4 left-4 z-10 hidden sm:flex items-center gap-3 bg-[#111622]/90 backdrop-blur border border-[#1e2638] rounded-lg px-3 py-1.5 text-[11px] font-mono">
              <div className="flex items-center gap-1.5"><span className="w-2.5 h-2.5 rounded-full bg-emerald-500" /><span className="text-slate-300">Healthy</span></div>
              <div className="flex items-center gap-1.5"><span className="w-2.5 h-2.5 rounded-full bg-amber-500" /><span className="text-slate-300">Warning</span></div>
              <div className="flex items-center gap-1.5"><span className="w-2.5 h-2.5 rounded-full bg-orange-500" /><span className="text-slate-300">Congested</span></div>
              <div className="flex items-center gap-1.5"><span className="w-2.5 h-2.5 rounded-full bg-rose-500" /><span className="text-slate-300">Failed</span></div>
            </div>

            {/* SVG Interactive Canvas */}
            <svg
              className="w-full h-[550px] cursor-grab active:cursor-grabbing select-none"
              onMouseDown={handleSvgMouseDown}
              onMouseMove={handleSvgMouseMove}
              onMouseUp={handleSvgMouseUp}
            >
              <defs>
                <pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse">
                  <path d="M 40 0 L 0 0 0 40" fill="none" stroke="#1e2638" strokeWidth="0.5" strokeOpacity="0.4" />
                </pattern>
                <marker id="arrow" viewBox="0 0 10 10" refX="22" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
                  <path d="M 0 0 L 10 5 L 0 10 z" fill="#0284c7" />
                </marker>
              </defs>

              <rect width="100%" height="100%" fill="url(#grid)" />

              <g transform={`translate(${pan.x}, ${pan.y}) scale(${zoom})`}>
                {/* Links / Edges */}
                {links.map((link) => {
                  const srcPos = nodePositions[link.sourceNodeId];
                  const tgtPos = nodePositions[link.targetNodeId];
                  if (!srcPos || !tgtPos) return null;

                  const isSelected = selectedLink?.id === link.id;
                  const style = getLinkStyle(link);
                  const midX = (srcPos.x + tgtPos.x) / 2;
                  const midY = (srcPos.y + tgtPos.y) / 2;

                  return (
                    <g key={link.id} className="cursor-pointer" onClick={(e) => { e.stopPropagation(); setSelectedLink(link); setSelectedNode(null); }}>
                      <line
                        x1={srcPos.x}
                        y1={srcPos.y}
                        x2={tgtPos.x}
                        y2={tgtPos.y}
                        stroke={isSelected ? '#38bdf8' : style.stroke}
                        strokeWidth={isSelected ? style.strokeWidth + 2 : style.strokeWidth}
                        strokeDasharray={style.strokeDasharray}
                        opacity={style.opacity}
                      />
                      {/* Interactive click area for thin lines */}
                      <line
                        x1={srcPos.x}
                        y1={srcPos.y}
                        x2={tgtPos.x}
                        y2={tgtPos.y}
                        stroke="transparent"
                        strokeWidth={14}
                      />
                      {/* Bandwidth & Metric Badge on Link */}
                      <g transform={`translate(${midX}, ${midY})`}>
                        <rect
                          x="-22"
                          y="-9"
                          width="44"
                          height="18"
                          rx="4"
                          fill="#0f172a"
                          stroke={isSelected ? '#38bdf8' : '#334155'}
                          strokeWidth="1"
                        />
                        <text
                          textAnchor="middle"
                          dy="3.5"
                          className="text-[9px] font-mono fill-slate-300 font-bold pointer-events-none"
                        >
                          {link.bandwidth >= 1000 ? `${link.bandwidth / 1000}G` : `${link.bandwidth}M`}
                        </text>
                      </g>
                    </g>
                  );
                })}

                {/* Nodes */}
                {nodes.map((node) => {
                  const pos = nodePositions[node.nodeId];
                  if (!pos) return null;

                  const isSelected = selectedNode?.id === node.id;
                  const color = getNodeColor(node.status);

                  return (
                    <g
                      key={node.id}
                      transform={`translate(${pos.x}, ${pos.y})`}
                      className="cursor-pointer"
                      onMouseDown={(e) => handleNodeMouseDown(e, node.nodeId)}
                      onClick={(e) => { e.stopPropagation(); setSelectedNode(node); setSelectedLink(null); }}
                    >
                      {/* Selection Glow */}
                      {isSelected && (
                        <circle r="30" fill="none" stroke="#38bdf8" strokeWidth="2.5" strokeDasharray="4,4" className="animate-spin-slow" />
                      )}

                      {/* Main Node Circle */}
                      <circle
                        r="20"
                        fill="#111622"
                        stroke={color.stroke}
                        strokeWidth={isSelected ? 3 : 2}
                        className="transition-all hover:scale-110"
                      />

                      {/* Inner Status Dot */}
                      <circle r="6" fill={color.fill} />

                      {/* Node Label */}
                      <text
                        y="34"
                        textAnchor="middle"
                        className="text-[11px] font-mono font-bold fill-slate-200 pointer-events-none tracking-tight"
                      >
                        {node.nodeId}
                      </text>
                      <text
                        y="46"
                        textAnchor="middle"
                        className="text-[9px] font-sans fill-slate-400 pointer-events-none"
                      >
                        {node.name}
                      </text>
                    </g>
                  );
                })}
              </g>
            </svg>
          </div>

          {/* Side Inspector Panel */}
          <div className="space-y-4">
            {selectedNode ? (() => {
              const telem = telemetryMap[selectedNode.nodeId];
              const health = healthMap[selectedNode.nodeId];
              const hb = heartbeatMap[selectedNode.nodeId];
              const liveness = hb?.liveness || 'UNREACHABLE';
              const healthStatus = health?.healthStatus || (selectedNode.status === 'OFFLINE' ? 'OFFLINE' : 'HEALTHY');

              return (
                <Card title="Node Telemetry & Inspector" subtitle="Live runtime telemetry and node configuration">
                  <div className="space-y-4">
                    {/* Node Header */}
                    <div className="flex items-center justify-between pb-3 border-b border-[#1e2638]">
                      <div>
                        <h4 className="text-sm font-bold text-white font-mono">{selectedNode.nodeId}</h4>
                        <p className="text-xs text-slate-400">{selectedNode.name}</p>
                      </div>
                      <span className={`text-[10px] font-mono px-2 py-0.5 rounded font-bold ${
                        healthStatus === 'HEALTHY' ? 'bg-emerald-500/20 text-emerald-400' :
                        healthStatus === 'WARNING' ? 'bg-amber-500/20 text-amber-400' :
                        healthStatus === 'DEGRADED' ? 'bg-orange-500/20 text-orange-400' :
                        healthStatus === 'FAILED' ? 'bg-rose-500/20 text-rose-400' :
                        'bg-slate-700/40 text-slate-400'
                      }`}>
                        {healthStatus}
                      </span>
                    </div>

                    {/* Liveness & Heartbeat */}
                    <div className="p-2.5 rounded-lg bg-slate-950 border border-slate-800 space-y-1.5 text-xs font-mono">
                      <div className="flex justify-between items-center">
                        <span className="text-slate-400">Heartbeat:</span>
                        <span className={`text-[10px] px-1.5 py-0.5 rounded font-bold ${
                          liveness === 'ALIVE' ? 'bg-emerald-500/20 text-emerald-400' :
                          liveness === 'SUSPECTED' ? 'bg-amber-500/20 text-amber-400' :
                          'bg-rose-500/20 text-rose-400'
                        }`}>
                          {liveness} {hb?.ageSeconds !== null && hb?.ageSeconds !== undefined ? `(${hb.ageSeconds}s ago)` : ''}
                        </span>
                      </div>
                      <div className="text-[10px] text-slate-400 truncate">
                        {health?.reason || 'Optimal operating conditions'}
                      </div>
                    </div>

                    {/* Phase 3: Live Runtime Telemetry Measurements */}
                    <div>
                      <p className="text-[10px] font-mono font-bold uppercase tracking-wider text-sky-400 mb-2">
                        [LIVE RUNTIME TELEMETRY]
                      </p>
                      {telem ? (
                        <div className="grid grid-cols-2 gap-2 text-xs font-mono">
                          <div className="p-2 rounded bg-slate-900/60 border border-slate-800">
                            <span className="text-[10px] text-slate-400 block">CPU USAGE</span>
                            <span className="font-bold text-sky-400">{telem.cpuUsage}%</span>
                          </div>
                          <div className="p-2 rounded bg-slate-900/60 border border-slate-800">
                            <span className="text-[10px] text-slate-400 block">MEMORY</span>
                            <span className="font-bold text-purple-400">{telem.memoryUsage}%</span>
                          </div>
                          <div className="p-2 rounded bg-slate-900/60 border border-slate-800">
                            <span className="text-[10px] text-slate-400 block">LATENCY</span>
                            <span className="font-bold text-emerald-400">{telem.latency} ms</span>
                          </div>
                          <div className="p-2 rounded bg-slate-900/60 border border-slate-800">
                            <span className="text-[10px] text-slate-400 block">PACKET LOSS</span>
                            <span className="font-bold text-amber-400">{telem.packetLoss}%</span>
                          </div>
                          <div className="p-2 rounded bg-slate-900/60 border border-slate-800">
                            <span className="text-[10px] text-slate-400 block">ACTIVE CONNS</span>
                            <span className="font-bold text-cyan-400">{telem.activeConnections}</span>
                          </div>
                          <div className="p-2 rounded bg-slate-900/60 border border-slate-800">
                            <span className="text-[10px] text-slate-400 block">ERROR RATE</span>
                            <span className="font-bold text-rose-400">{telem.errorRate}%</span>
                          </div>
                        </div>
                      ) : (
                        <div className="p-3 rounded bg-slate-950 border border-slate-800 text-[11px] text-slate-400 font-mono text-center">
                          Waiting for telemetry...
                        </div>
                      )}
                    </div>

                    {/* Phase 2: Static Topology Configuration */}
                    <div>
                      <p className="text-[10px] font-mono font-bold uppercase tracking-wider text-slate-400 mb-2">
                        [TOPOLOGY CONFIGURATION]
                      </p>
                      <div className="space-y-1.5 text-xs font-mono">
                        <div className="flex justify-between py-1 border-b border-slate-800/60">
                          <span className="text-slate-400">Endpoint:</span>
                          <span className="text-slate-200">{selectedNode.host}:{selectedNode.port}</span>
                        </div>
                        <div className="flex justify-between py-1 border-b border-slate-800/60">
                          <span className="text-slate-400">Configured Capacity:</span>
                          <span className="text-sky-400">{selectedNode.capacity.toLocaleString()} req/s</span>
                        </div>
                        <div className="flex justify-between py-1 border-b border-slate-800/60">
                          <span className="text-slate-400">Connected Links:</span>
                          <span className="text-emerald-400">{selectedNode.degree ?? 0} links</span>
                        </div>
                      </div>
                    </div>

                    <div className="pt-2">
                      <Link to="/nodes">
                        <Button variant="outline" size="sm" className="w-full text-xs">
                          Manage in Node Console
                        </Button>
                      </Link>
                    </div>
                  </div>
                </Card>
              );
            })() : selectedLink ? (
              <Card title="Link Inspector" subtitle="Connection properties and routing metrics">
                <div className="space-y-4">
                  <div className="flex items-center justify-between pb-3 border-b border-[#1e2638]">
                    <div>
                      <h4 className="text-xs font-bold text-white font-mono">{selectedLink.linkId}</h4>
                      <p className="text-[11px] text-slate-400 font-mono mt-0.5">
                        {selectedLink.sourceNodeId} &rarr; {selectedLink.targetNodeId}
                      </p>
                    </div>
                    <span className={`text-[10px] font-mono px-2 py-0.5 rounded font-bold ${
                      selectedLink.status === 'ACTIVE' ? 'bg-emerald-500/20 text-emerald-400' :
                      selectedLink.status === 'DEGRADED' ? 'bg-amber-500/20 text-amber-400' :
                      'bg-rose-500/20 text-rose-400'
                    }`}>
                      {selectedLink.status}
                    </span>
                  </div>

                  <div className="space-y-2 text-xs">
                    <div className="flex justify-between py-1 border-b border-slate-800/60">
                      <span className="text-slate-400">Link Type:</span>
                      <span className="font-mono text-slate-200">{selectedLink.linkType}</span>
                    </div>
                    <div className="flex justify-between py-1 border-b border-slate-800/60">
                      <span className="text-slate-400">Bandwidth:</span>
                      <span className="font-mono text-sky-400">{selectedLink.bandwidth.toLocaleString()} Mbps</span>
                    </div>
                    <div className="flex justify-between py-1 border-b border-slate-800/60">
                      <span className="text-slate-400">Routing Weight:</span>
                      <span className="font-mono text-slate-200">{selectedLink.weight}</span>
                    </div>
                    <div className="flex justify-between py-1 border-b border-slate-800/60">
                      <span className="text-slate-400">Operational State:</span>
                      <span className="font-mono text-slate-200">{selectedLink.enabled ? 'ENABLED' : 'DISABLED'}</span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 pt-2">
                    {isOperator && (
                      <Button
                        variant="outline"
                        size="sm"
                        className="flex-1 text-xs"
                        onClick={() => handleEditLinkOpen(selectedLink)}
                        icon={Edit3}
                      >
                        Edit Link
                      </Button>
                    )}
                    {isAdmin && (
                      <Button
                        variant="danger"
                        size="sm"
                        onClick={() => handleDeleteLinkOpen(selectedLink)}
                        icon={Trash2}
                      />
                    )}
                  </div>
                </div>
              </Card>
            ) : (
              <Card title="Topology Inspector" subtitle="Select an element to inspect details">
                <div className="py-8 text-center text-slate-400">
                  <Sliders className="w-8 h-8 mx-auto mb-2 text-slate-600" />
                  <p className="text-xs">Click on any network node or connection link in the canvas to inspect real-time properties.</p>
                </div>
              </Card>
            )}

            {/* Quick Links List Card */}
            <Card title="Network Links" subtitle={`${links.length} total connections established`}>
              {links.length === 0 ? (
                <div className="py-6 text-center text-xs text-slate-500">
                  No active connections. Establish links to connect nodes.
                </div>
              ) : (
                <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
                  {links.map((link) => (
                    <div
                      key={link.id}
                      onClick={() => { setSelectedLink(link); setSelectedNode(null); }}
                      className={`p-2 rounded-lg border text-xs cursor-pointer transition-colors flex items-center justify-between ${
                        selectedLink?.id === link.id
                          ? 'bg-sky-500/10 border-sky-500/40 text-sky-300'
                          : 'bg-slate-900/60 border-slate-800 hover:border-slate-700 text-slate-300'
                      }`}
                    >
                      <div className="truncate font-mono">
                        <span className="text-slate-400">{link.sourceNodeId}</span> &harr; <span>{link.targetNodeId}</span>
                      </div>
                      <span className="text-[10px] font-mono text-slate-400 shrink-0 ml-2">
                        {link.bandwidth}M
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </Card>
          </div>
        </div>
      )}

      {/* Establish Link Modal */}
      <Modal
        isOpen={isCreateLinkOpen}
        onClose={() => setIsCreateLinkOpen(false)}
        title="Establish Network Link"
        subtitle="Create a new directional connection between topology endpoints"
        maxWidth="max-w-lg"
      >
        <form onSubmit={handleCreateLinkSubmit} className="space-y-4">
          {formErrors.general && (
            <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/30 text-xs text-rose-300">
              {formErrors.general}
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400">
                Source Node
              </label>
              <select
                value={linkFormData.sourceNodeId}
                onChange={(e) => setLinkFormData({ ...linkFormData, sourceNodeId: e.target.value })}
                className="w-full rounded-lg border border-slate-800 bg-[#111622] px-3.5 py-2.5 text-sm text-slate-100 focus:outline-none focus:ring-2 focus:ring-sky-500/30 font-mono"
                required
              >
                {allNodes.map((n) => (
                  <option key={n.nodeId} value={n.nodeId}>
                    {n.nodeId} ({n.name})
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400">
                Target Node
              </label>
              <select
                value={linkFormData.targetNodeId}
                onChange={(e) => setLinkFormData({ ...linkFormData, targetNodeId: e.target.value })}
                className="w-full rounded-lg border border-slate-800 bg-[#111622] px-3.5 py-2.5 text-sm text-slate-100 focus:outline-none focus:ring-2 focus:ring-sky-500/30 font-mono"
                required
              >
                {allNodes
                  .filter((n) => n.nodeId !== linkFormData.sourceNodeId)
                  .map((n) => (
                    <option key={n.nodeId} value={n.nodeId}>
                      {n.nodeId} ({n.name})
                    </option>
                  ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400">
                Link Type
              </label>
              <select
                value={linkFormData.linkType}
                onChange={(e) => setLinkFormData({ ...linkFormData, linkType: e.target.value })}
                className="w-full rounded-lg border border-slate-800 bg-[#111622] px-3.5 py-2.5 text-sm text-slate-100 focus:outline-none focus:ring-2 focus:ring-sky-500/30"
              >
                <option value="DIRECT">DIRECT</option>
                <option value="BACKBONE">BACKBONE</option>
                <option value="EDGE">EDGE</option>
                <option value="CROSS_REGION">CROSS_REGION</option>
              </select>
            </div>

            <Input
              id="bandwidth"
              label="Bandwidth (Mbps)"
              type="number"
              placeholder="1000"
              value={linkFormData.bandwidth}
              onChange={(e) => setLinkFormData({ ...linkFormData, bandwidth: e.target.value })}
              required
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              id="weight"
              label="Routing Weight / Cost"
              type="number"
              step="0.1"
              placeholder="1.0"
              value={linkFormData.weight}
              onChange={(e) => setLinkFormData({ ...linkFormData, weight: e.target.value })}
              required
            />

            <div className="space-y-1.5">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400">
                Initial Status
              </label>
              <select
                value={linkFormData.status}
                onChange={(e) => setLinkFormData({ ...linkFormData, status: e.target.value })}
                className="w-full rounded-lg border border-slate-800 bg-[#111622] px-3.5 py-2.5 text-sm text-slate-100 focus:outline-none focus:ring-2 focus:ring-sky-500/30"
              >
                <option value="ACTIVE">ACTIVE</option>
                <option value="DEGRADED">DEGRADED</option>
                <option value="FAILED">FAILED</option>
                <option value="OFFLINE">OFFLINE</option>
              </select>
            </div>
          </div>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-[#1e2638]">
            <Button variant="outline" size="sm" onClick={() => setIsCreateLinkOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" size="sm" loading={actionLoading}>
              Establish Connection
            </Button>
          </div>
        </form>
      </Modal>

      {/* Edit Link Modal */}
      <Modal
        isOpen={isEditLinkOpen}
        onClose={() => setIsEditLinkOpen(false)}
        title={`Edit Link: ${selectedLink?.linkId}`}
        subtitle="Update bandwidth capacity, routing cost, or operational state"
        maxWidth="max-w-lg"
      >
        <form onSubmit={handleEditLinkSubmit} className="space-y-4">
          {formErrors.general && (
            <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/30 text-xs text-rose-300">
              {formErrors.general}
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400">
                Link Type
              </label>
              <select
                value={linkFormData.linkType}
                onChange={(e) => setLinkFormData({ ...linkFormData, linkType: e.target.value })}
                className="w-full rounded-lg border border-slate-800 bg-[#111622] px-3.5 py-2.5 text-sm text-slate-100 focus:outline-none focus:ring-2 focus:ring-sky-500/30"
              >
                <option value="DIRECT">DIRECT</option>
                <option value="BACKBONE">BACKBONE</option>
                <option value="EDGE">EDGE</option>
                <option value="CROSS_REGION">CROSS_REGION</option>
              </select>
            </div>

            <Input
              id="edit-bandwidth"
              label="Bandwidth (Mbps)"
              type="number"
              value={linkFormData.bandwidth}
              onChange={(e) => setLinkFormData({ ...linkFormData, bandwidth: e.target.value })}
              required
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              id="edit-weight"
              label="Routing Weight / Cost"
              type="number"
              step="0.1"
              value={linkFormData.weight}
              onChange={(e) => setLinkFormData({ ...linkFormData, weight: e.target.value })}
              required
            />

            <div className="space-y-1.5">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400">
                Operational Status
              </label>
              <select
                value={linkFormData.status}
                onChange={(e) => setLinkFormData({ ...linkFormData, status: e.target.value })}
                className="w-full rounded-lg border border-slate-800 bg-[#111622] px-3.5 py-2.5 text-sm text-slate-100 focus:outline-none focus:ring-2 focus:ring-sky-500/30"
              >
                <option value="ACTIVE">ACTIVE</option>
                <option value="DEGRADED">DEGRADED</option>
                <option value="FAILED">FAILED</option>
                <option value="OFFLINE">OFFLINE</option>
              </select>
            </div>
          </div>

          <div className="flex items-center gap-2 pt-2">
            <input
              type="checkbox"
              id="link-enabled"
              checked={linkFormData.enabled}
              onChange={(e) => setLinkFormData({ ...linkFormData, enabled: e.target.checked })}
              className="rounded border-slate-800 bg-[#111622] text-sky-500 focus:ring-sky-500/30"
            />
            <label htmlFor="link-enabled" className="text-xs text-slate-300 font-medium cursor-pointer">
              Enable connection in active graph topology
            </label>
          </div>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-[#1e2638]">
            <Button variant="outline" size="sm" onClick={() => setIsEditLinkOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" size="sm" loading={actionLoading}>
              Save Changes
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Link Modal */}
      <Modal
        isOpen={isDeleteLinkOpen}
        onClose={() => setIsDeleteLinkOpen(false)}
        title="Confirm Link Decommission"
        subtitle="This action removes the connection between nodes from the topology"
      >
        <div className="space-y-4">
          <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 flex items-start gap-3">
            <AlertTriangle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
            <div className="text-xs text-rose-200">
              Are you sure you want to delete connection <strong className="font-mono text-rose-300">{selectedLink?.linkId}</strong> ({selectedLink?.sourceNodeId} &rarr; {selectedLink?.targetNodeId})?
            </div>
          </div>

          <div className="flex items-center justify-end gap-3 pt-2">
            <Button variant="outline" size="sm" onClick={() => setIsDeleteLinkOpen(false)}>
              Cancel
            </Button>
            <Button variant="danger" size="sm" onClick={handleDeleteLinkSubmit} loading={actionLoading}>
              Decommission Link
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
