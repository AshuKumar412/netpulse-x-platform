import api from './api';

/**
 * Phase 7 — OS Process Management & CPU Scheduling API service.
 * All simulation data, queues, and benchmarks originate directly from the backend.
 */
export const schedulerService = {
  /**
   * GET /api/scheduler/status
   */
  getStatus() {
    return api.get('/api/scheduler/status').then((res) => res.data);
  },

  /**
   * POST /api/scheduler/start
   */
  start() {
    return api.post('/api/scheduler/start').then((res) => res.data);
  },

  /**
   * POST /api/scheduler/pause
   */
  pause() {
    return api.post('/api/scheduler/pause').then((res) => res.data);
  },

  /**
   * POST /api/scheduler/resume
   */
  resume() {
    return api.post('/api/scheduler/resume').then((res) => res.data);
  },

  /**
   * POST /api/scheduler/stop
   */
  stop() {
    return api.post('/api/scheduler/stop').then((res) => res.data);
  },

  /**
   * POST /api/scheduler/reset
   */
  reset() {
    return api.post('/api/scheduler/reset').then((res) => res.data);
  },

  /**
   * POST /api/scheduler/step
   */
  step() {
    return api.post('/api/scheduler/step').then((res) => res.data);
  },

  /**
   * POST /api/scheduler/processes
   */
  createProcess(data) {
    return api.post('/api/scheduler/processes', data).then((res) => res.data);
  },

  /**
   * POST /api/scheduler/processes/batch
   */
  batchCreateProcesses(data) {
    return api.post('/api/scheduler/processes/batch', data).then((res) => res.data);
  },

  /**
   * GET /api/scheduler/processes
   */
  getProcesses(state = null, nodeId = null) {
    const params = {};
    if (state) params.state = state;
    if (nodeId) params.nodeId = nodeId;
    return api.get('/api/scheduler/processes', { params }).then((res) => res.data);
  },

  /**
   * GET /api/scheduler/processes/{processId}
   */
  getProcess(processId) {
    return api.get(`/api/scheduler/processes/${processId}`).then((res) => res.data);
  },

  /**
   * DELETE /api/scheduler/processes/{processId}
   */
  terminateProcess(processId) {
    return api.delete(`/api/scheduler/processes/${processId}`).then((res) => res.data);
  },

  /**
   * GET /api/scheduler/policy
   */
  getPolicy() {
    return api.get('/api/scheduler/policy').then((res) => res.data);
  },

  /**
   * PUT /api/scheduler/policy
   */
  updatePolicy(policy) {
    return api.put('/api/scheduler/policy', policy).then((res) => res.data);
  },

  /**
   * POST /api/scheduler/policy/algorithm?algorithm={algorithm}
   */
  switchAlgorithm(algorithm) {
    return api.post('/api/scheduler/policy/algorithm', null, { params: { algorithm } }).then((res) => res.data);
  },

  /**
   * GET /api/scheduler/context-switches?limit={limit}
   */
  getContextSwitches(limit = 20) {
    return api.get('/api/scheduler/context-switches', { params: { limit } }).then((res) => res.data);
  },

  /**
   * POST /api/scheduler/benchmark
   */
  runBenchmark(customWorkload = []) {
    return api.post('/api/scheduler/benchmark', customWorkload).then((res) => res.data);
  },
};
