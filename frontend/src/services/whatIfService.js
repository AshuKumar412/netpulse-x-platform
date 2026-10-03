import api from './api';

/**
 * Phase 8 — What-If Infrastructure Simulator & Decision Explainability API service.
 * All simulations run read-only against live snapshots without mutating operational state.
 */
export const whatIfService = {
  /**
   * GET /api/what-if/snapshot
   */
  getSnapshot() {
    return api.get('/api/what-if/snapshot').then((res) => res.data);
  },

  /**
   * GET /api/what-if/templates
   */
  getTemplates() {
    return api.get('/api/what-if/templates').then((res) => res.data);
  },

  /**
   * POST /api/what-if/scenarios
   */
  createScenario(data) {
    return api.post('/api/what-if/scenarios', data).then((res) => res.data);
  },

  /**
   * GET /api/what-if/scenarios
   */
  getScenarios() {
    return api.get('/api/what-if/scenarios').then((res) => res.data);
  },

  /**
   * GET /api/what-if/scenarios/{id}
   */
  getScenarioById(id) {
    return api.get(`/api/what-if/scenarios/${id}`).then((res) => res.data);
  },

  /**
   * POST /api/what-if/scenarios/{id}/run
   */
  runSimulation(id) {
    return api.post(`/api/what-if/scenarios/${id}/run`).then((res) => res.data);
  },

  /**
   * POST /api/what-if/scenarios/{id}/cancel
   */
  cancelSimulation(id) {
    return api.post(`/api/what-if/scenarios/${id}/cancel`).then((res) => res.data);
  },

  /**
   * GET /api/what-if/scenarios/{id}/timeline
   */
  getTimeline(id) {
    return api.get(`/api/what-if/scenarios/${id}/timeline`).then((res) => res.data);
  },

  /**
   * GET /api/what-if/scenarios/{id}/decisions
   */
  getDecisions(id) {
    return api.get(`/api/what-if/scenarios/${id}/decisions`).then((res) => res.data);
  },

  /**
   * GET /api/what-if/scenarios/{id}/comparison
   */
  getComparison(id) {
    return api.get(`/api/what-if/scenarios/${id}/comparison`).then((res) => res.data);
  },

  /**
   * POST /api/what-if/compare/routing
   */
  compareRouting(data) {
    return api.post('/api/what-if/compare/routing', data).then((res) => res.data);
  },

  /**
   * POST /api/what-if/compare/scheduler
   */
  compareScheduler(data) {
    return api.post('/api/what-if/compare/scheduler', data).then((res) => res.data);
  },

  /**
   * GET /api/what-if/scenarios/{id}/export?format={format}
   */
  exportReport(id, format = 'json') {
    return api.get(`/api/what-if/scenarios/${id}/export`, {
      params: { format },
      responseType: 'blob',
    });
  },
};
