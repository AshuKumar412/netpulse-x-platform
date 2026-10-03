import api from './api';

/**
 * Phase 9 — Predictive Congestion Detection & ML Intelligence API service.
 * Consumes real historical telemetry, trains deterministic ML models, and evaluates prediction explainability.
 */
export const predictiveService = {
  /**
   * GET /api/predictions
   */
  getPredictions() {
    return api.get('/api/predictions').then((res) => res.data);
  },

  /**
   * GET /api/predictions/summary
   */
  getSummary() {
    return api.get('/api/predictions/summary').then((res) => res.data);
  },

  /**
   * GET /api/predictions/nodes/{nodeId}
   */
  getPredictionForNode(nodeId) {
    return api.get(`/api/predictions/nodes/${nodeId}`).then((res) => res.data);
  },

  /**
   * GET /api/predictions/nodes/{nodeId}/forecast
   */
  getForecastForNode(nodeId) {
    return api.get(`/api/predictions/nodes/${nodeId}/forecast`).then((res) => res.data);
  },

  /**
   * POST /api/predictions/run
   */
  runPredictions() {
    return api.post('/api/predictions/run').then((res) => res.data);
  },

  /**
   * POST /api/predictions/verify
   */
  verifyOutcomes() {
    return api.post('/api/predictions/verify').then((res) => res.data);
  },

  /**
   * GET /api/predictions/history
   */
  getHistory() {
    return api.get('/api/predictions/history').then((res) => res.data);
  },

  /**
   * GET /api/predictions/history/{nodeId}
   */
  getNodeHistory(nodeId) {
    return api.get(`/api/predictions/history/${nodeId}`).then((res) => res.data);
  },

  /**
   * POST /api/predictions/train
   */
  trainModel(data) {
    return api.post('/api/predictions/train', data).then((res) => res.data);
  },

  /**
   * GET /api/predictions/models
   */
  getModels() {
    return api.get('/api/predictions/models').then((res) => res.data);
  },

  /**
   * GET /api/predictions/models/{modelVersion}
   */
  getModelByVersion(modelVersion) {
    return api.get(`/api/predictions/models/${modelVersion}`).then((res) => res.data);
  },

  /**
   * GET /api/predictions/models/active/features
   */
  getActiveFeatureImportances() {
    return api.get('/api/predictions/models/active/features').then((res) => res.data);
  },

  /**
   * GET /api/predictions/config
   */
  getConfig() {
    return api.get('/api/predictions/config').then((res) => res.data);
  },

  /**
   * PUT /api/predictions/config
   */
  updateConfig(data) {
    return api.put('/api/predictions/config', data).then((res) => res.data);
  },
};

export default predictiveService;
