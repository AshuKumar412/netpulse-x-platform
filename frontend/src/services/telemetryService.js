import api from './api';

export const telemetryService = {
  getAllTelemetry: async () => {
    const response = await api.get('/telemetry');
    return response.data;
  },

  getNodeTelemetry: async (nodeId) => {
    const response = await api.get(`/telemetry/nodes/${nodeId}`);
    return response.data;
  },

  getNodeTelemetryHistory: async (nodeId, limit = 30) => {
    const response = await api.get(`/telemetry/nodes/${nodeId}/history`, {
      params: { limit },
    });
    return response.data;
  },
};

export default telemetryService;
