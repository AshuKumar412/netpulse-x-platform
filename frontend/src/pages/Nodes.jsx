import React, { useState, useEffect, useCallback } from 'react';
import {
  Server,
  Plus,
  Search,
  Filter,
  Edit2,
  Trash2,
  RefreshCw,
  AlertTriangle,
  Check,
} from 'lucide-react';
import { nodeService } from '../services/nodeService';
import { useAuth } from '../context/AuthContext';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { Card } from '../components/common/Card';
import { Modal } from '../components/common/Modal';
import { StatusBadge } from '../components/common/StatusBadge';
import { LoadingState } from '../components/common/LoadingState';
import { ErrorMessage } from '../components/common/ErrorMessage';

const INITIAL_FORM_STATE = {
  nodeId: '',
  name: '',
  host: '',
  port: 8080,
  capacity: 5000,
  status: 'HEALTHY',
};

export const Nodes = () => {
  const [nodes, setNodes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filters & Search
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  // Modals state
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [selectedNode, setSelectedNode] = useState(null);

  // Form State
  const [formData, setFormData] = useState(INITIAL_FORM_STATE);
  const [formErrors, setFormErrors] = useState({});
  const [submitLoading, setSubmitLoading] = useState(false);

  const { isOperator, isAdmin } = useAuth();

  const fetchNodes = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await nodeService.getAllNodes(statusFilter || null, search || null);
      if (res.success && res.data) {
        setNodes(res.data);
      }
    } catch (err) {
      setError(err.message || 'Failed to fetch network nodes');
    } finally {
      setLoading(false);
    }
  }, [statusFilter, search]);

  useEffect(() => {
    fetchNodes();
  }, [fetchNodes]);

  const validateForm = (isEdit = false) => {
    const errors = {};
    if (!isEdit && !formData.nodeId.trim()) {
      errors.nodeId = 'Node ID is required (e.g. node-us-east-1)';
    } else if (!isEdit && !/^[a-zA-Z0-9_-]{3,64}$/.test(formData.nodeId)) {
      errors.nodeId = 'Node ID must be 3-64 alphanumeric characters, hyphens or underscores';
    }

    if (!formData.name.trim()) {
      errors.name = 'Node name is required';
    }
    if (!formData.host.trim()) {
      errors.host = 'Host is required (IP or hostname)';
    }
    if (!formData.port || formData.port < 1 || formData.port > 65535) {
      errors.port = 'Port must be between 1 and 65535';
    }
    if (!formData.capacity || formData.capacity < 1) {
      errors.capacity = 'Capacity must be a positive integer';
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleCreateOpen = () => {
    setFormData(INITIAL_FORM_STATE);
    setFormErrors({});
    setIsCreateModalOpen(true);
  };

  const handleEditOpen = (node) => {
    setSelectedNode(node);
    setFormData({
      nodeId: node.nodeId,
      name: node.name,
      host: node.host,
      port: node.port,
      capacity: node.capacity,
      status: node.status,
    });
    setFormErrors({});
    setIsEditModalOpen(true);
  };

  const handleDeleteOpen = (node) => {
    setSelectedNode(node);
    setIsDeleteModalOpen(true);
  };

  const handleCreateSubmit = async (e) => {
    e.preventDefault();
    if (!validateForm(false)) return;

    setSubmitLoading(true);
    try {
      await nodeService.createNode(formData);
      setIsCreateModalOpen(false);
      fetchNodes();
    } catch (err) {
      setFormErrors({ general: err.message });
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    if (!validateForm(true)) return;

    setSubmitLoading(true);
    try {
      await nodeService.updateNode(selectedNode.id, {
        name: formData.name,
        host: formData.host,
        port: Number(formData.port),
        capacity: Number(formData.capacity),
        status: formData.status,
      });
      setIsEditModalOpen(false);
      fetchNodes();
    } catch (err) {
      setFormErrors({ general: err.message });
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleDeleteSubmit = async () => {
    if (!selectedNode) return;
    setSubmitLoading(true);
    try {
      await nodeService.deleteNode(selectedNode.id);
      setIsDeleteModalOpen(false);
      setSelectedNode(null);
      fetchNodes();
    } catch (err) {
      setError(err.message || 'Failed to delete node');
    } finally {
      setSubmitLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Network Node Infrastructure</h1>
          <p className="text-xs text-slate-400 font-mono mt-1">
            Registered cluster endpoints, capacity limits, and operational statuses
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            size="sm"
            onClick={fetchNodes}
            icon={RefreshCw}
          >
            Reload
          </Button>

          {isOperator && (
            <Button
              variant="primary"
              size="sm"
              onClick={handleCreateOpen}
              icon={Plus}
            >
              Provision Node
            </Button>
          )}
        </div>
      </div>

      {/* Search & Filter Bar */}
      <Card className="p-4">
        <div className="flex flex-col sm:flex-row gap-4 items-center justify-between">
          <div className="w-full sm:w-80">
            <Input
              id="search"
              placeholder="Search by Node ID, name, or host..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              icon={Search}
            />
          </div>

          <div className="flex items-center gap-3 w-full sm:w-auto">
            <Filter className="w-4 h-4 text-slate-400 shrink-0" />
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="w-full sm:w-48 rounded-lg border border-slate-800 bg-[#111622] px-3 py-2.5 text-xs text-slate-200 focus:outline-none focus:ring-2 focus:ring-sky-500/30"
            >
              <option value="">All Statuses</option>
              <option value="HEALTHY">HEALTHY</option>
              <option value="WARNING">WARNING</option>
              <option value="CONGESTED">CONGESTED</option>
              <option value="FAILED">FAILED</option>
              <option value="OFFLINE">OFFLINE</option>
            </select>
          </div>
        </div>
      </Card>

      {/* Nodes Table / Content */}
      {loading ? (
        <LoadingState message="Loading registered network nodes..." />
      ) : error ? (
        <ErrorMessage message={error} onRetry={fetchNodes} />
      ) : nodes.length === 0 ? (
        <Card className="py-16 text-center">
          <Server className="w-12 h-12 text-slate-600 mx-auto mb-3" />
          <h4 className="text-base font-semibold text-slate-300">No network nodes available.</h4>
          <p className="text-xs text-slate-400 mt-1 max-w-sm mx-auto">
            {search || statusFilter
              ? 'No nodes match the selected search and filter criteria.'
              : 'The cluster topology currently has no registered endpoints.'}
          </p>
          {isOperator && !search && !statusFilter && (
            <div className="mt-4">
              <Button variant="primary" size="sm" onClick={handleCreateOpen} icon={Plus}>
                Provision Node
              </Button>
            </div>
          )}
        </Card>
      ) : (
        <div className="bg-[#111622] border border-[#1e2638] rounded-xl overflow-hidden shadow-lg">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-300">
              <thead className="bg-slate-900/80 text-[11px] uppercase tracking-wider text-slate-400 font-mono border-b border-[#1e2638]">
                <tr>
                  <th className="px-6 py-3.5">Node ID</th>
                  <th className="px-6 py-3.5">Name</th>
                  <th className="px-6 py-3.5">Endpoint (Host:Port)</th>
                  <th className="px-6 py-3.5">Capacity</th>
                  <th className="px-6 py-3.5">Status</th>
                  <th className="px-6 py-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#1e2638]">
                {nodes.map((node) => (
                  <tr key={node.id} className="hover:bg-slate-800/30 transition-colors font-sans">
                    <td className="px-6 py-4 font-mono text-xs font-semibold text-sky-400">
                      {node.nodeId}
                    </td>
                    <td className="px-6 py-4 font-medium text-slate-200">
                      {node.name}
                    </td>
                    <td className="px-6 py-4 font-mono text-xs text-slate-400">
                      {node.host}:{node.port}
                    </td>
                    <td className="px-6 py-4 font-mono text-xs text-slate-300">
                      {node.capacity.toLocaleString()} <span className="text-[10px] text-slate-500">req/s</span>
                    </td>
                    <td className="px-6 py-4">
                      <StatusBadge status={node.status} />
                    </td>
                    <td className="px-6 py-4 text-right">
                      <div className="inline-flex items-center gap-1.5">
                        {isOperator && (
                          <button
                            onClick={() => handleEditOpen(node)}
                            className="p-1.5 rounded-lg text-slate-400 hover:text-sky-400 hover:bg-slate-800/80 transition-colors"
                            title="Edit Node"
                          >
                            <Edit2 className="w-4 h-4" />
                          </button>
                        )}
                        {isAdmin && (
                          <button
                            onClick={() => handleDeleteOpen(node)}
                            className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
                            title="Delete Node (Admin Only)"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Provision Node Modal */}
      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        title="Provision Network Node"
        subtitle="Register a new network node in the cluster topology"
        maxWidth="max-w-lg"
      >
        <form onSubmit={handleCreateSubmit} className="space-y-4">
          {formErrors.general && (
            <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/30 text-xs text-rose-300">
              {formErrors.general}
            </div>
          )}

          <Input
            id="create-nodeId"
            label="Node Identifier (Node ID)"
            placeholder="node-us-east-1"
            value={formData.nodeId}
            onChange={(e) => setFormData({ ...formData, nodeId: e.target.value })}
            error={formErrors.nodeId}
            helperText="Unique identifier across cluster topology"
            required
          />

          <Input
            id="create-name"
            label="Node Name"
            placeholder="Edge Router 01"
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            error={formErrors.name}
            required
          />

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              id="create-host"
              label="Host / IP"
              placeholder="192.168.1.10"
              value={formData.host}
              onChange={(e) => setFormData({ ...formData, host: e.target.value })}
              error={formErrors.host}
              required
            />

            <Input
              id="create-port"
              label="Port"
              type="number"
              placeholder="8080"
              value={formData.port}
              onChange={(e) => setFormData({ ...formData, port: e.target.value })}
              error={formErrors.port}
              required
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              id="create-capacity"
              label="Capacity (req/sec)"
              type="number"
              placeholder="5000"
              value={formData.capacity}
              onChange={(e) => setFormData({ ...formData, capacity: e.target.value })}
              error={formErrors.capacity}
              required
            />

            <div className="space-y-1.5">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400">
                Initial Status
              </label>
              <select
                value={formData.status}
                onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                className="w-full rounded-lg border border-slate-800 bg-[#111622] px-3.5 py-2.5 text-sm text-slate-100 focus:outline-none focus:ring-2 focus:ring-sky-500/30"
              >
                <option value="HEALTHY">HEALTHY</option>
                <option value="WARNING">WARNING</option>
                <option value="CONGESTED">CONGESTED</option>
                <option value="FAILED">FAILED</option>
                <option value="OFFLINE">OFFLINE</option>
              </select>
            </div>
          </div>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-[#1e2638]">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsCreateModalOpen(false)}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              loading={submitLoading}
            >
              Provision Node
            </Button>
          </div>
        </form>
      </Modal>

      {/* Edit Node Modal */}
      <Modal
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        title={`Edit Node: ${selectedNode?.nodeId}`}
        subtitle="Update configuration parameters and operational state"
        maxWidth="max-w-lg"
      >
        <form onSubmit={handleEditSubmit} className="space-y-4">
          {formErrors.general && (
            <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/30 text-xs text-rose-300">
              {formErrors.general}
            </div>
          )}

          <Input
            id="edit-name"
            label="Node Name"
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            error={formErrors.name}
            required
          />

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              id="edit-host"
              label="Host / IP"
              value={formData.host}
              onChange={(e) => setFormData({ ...formData, host: e.target.value })}
              error={formErrors.host}
              required
            />

            <Input
              id="edit-port"
              label="Port"
              type="number"
              value={formData.port}
              onChange={(e) => setFormData({ ...formData, port: e.target.value })}
              error={formErrors.port}
              required
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              id="edit-capacity"
              label="Capacity (req/sec)"
              type="number"
              value={formData.capacity}
              onChange={(e) => setFormData({ ...formData, capacity: e.target.value })}
              error={formErrors.capacity}
              required
            />

            <div className="space-y-1.5">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400">
                Operational Status
              </label>
              <select
                value={formData.status}
                onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                className="w-full rounded-lg border border-slate-800 bg-[#111622] px-3.5 py-2.5 text-sm text-slate-100 focus:outline-none focus:ring-2 focus:ring-sky-500/30"
              >
                <option value="HEALTHY">HEALTHY</option>
                <option value="WARNING">WARNING</option>
                <option value="CONGESTED">CONGESTED</option>
                <option value="FAILED">FAILED</option>
                <option value="OFFLINE">OFFLINE</option>
              </select>
            </div>
          </div>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-[#1e2638]">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsEditModalOpen(false)}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              loading={submitLoading}
            >
              Save Changes
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={isDeleteModalOpen}
        onClose={() => setIsDeleteModalOpen(false)}
        title="Confirm Node Decommission"
        subtitle="This action removes the node configuration from PostgreSQL"
      >
        <div className="space-y-4">
          <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 flex items-start gap-3">
            <AlertTriangle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
            <div className="text-xs text-rose-200">
              Are you sure you want to delete node <strong className="font-mono text-rose-300">{selectedNode?.nodeId}</strong> ({selectedNode?.name})? This action cannot be undone.
            </div>
          </div>

          <div className="flex items-center justify-end gap-3 pt-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsDeleteModalOpen(false)}
            >
              Cancel
            </Button>
            <Button
              variant="danger"
              size="sm"
              onClick={handleDeleteSubmit}
              loading={submitLoading}
            >
              Decommission Node
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
