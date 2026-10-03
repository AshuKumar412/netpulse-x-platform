import api from './api';

/**
 * Phase 6 — Chaos Engineering API service.
 * All data originates from the backend simulation engine and state machine.
 */
export const chaosService = {
  /**
   * GET /api/chaos/status
   */
  getStatus() {
    return api.get('/api/chaos/status').then((res) => res.data);
  },

  /**
   * GET /api/chaos/experiments?page={page}&size={size}
   */
  getExperiments(page = 0, size = 20) {
    return api.get('/api/chaos/experiments', { params: { page, size } }).then((res) => res.data);
  },

  /**
   * GET /api/chaos/experiments/{id}
   */
  getExperimentById(id) {
    return api.get(`/api/chaos/experiments/${id}`).then((res) => res.data);
  },

  /**
   * GET /api/chaos/experiments/active
   */
  getActiveExperiments() {
    return api.get('/api/chaos/experiments/active').then((res) => res.data);
  },

  /**
   * POST /api/chaos/experiments
   */
  startExperiment(data) {
    return api.post('/api/chaos/experiments', data).then((res) => res.data);
  },

  /**
   * POST /api/chaos/experiments/{id}/stop
   */
  stopExperiment(id, reason = null) {
    const params = reason ? { reason } : {};
    return api.post(`/api/chaos/experiments/${id}/stop`, null, { params }).then((res) => res.data);
  },

  /**
   * POST /api/chaos/experiments/{id}/rollback
   */
  rollbackExperiment(id) {
    return api.post(`/api/chaos/experiments/${id}/rollback`).then((res) => res.data);
  },

  /**
   * GET /api/chaos/policies
   */
  getPolicies() {
    return api.get('/api/chaos/policies').then((res) => res.data);
  },
};

export default chaosService;
