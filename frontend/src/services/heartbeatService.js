import api from './api';

export const heartbeatService = {
  getAllHeartbeatStatuses: async () => {
    const response = await api.get('/heartbeat/status');
    return response.data;
  },

  getNodeHeartbeatStatus: async (nodeId) => {
    const response = await api.get(`/heartbeat/nodes/${nodeId}`);
    return response.data;
  },

  toggleHeartbeatSuppression: async (nodeId, suppress = true) => {
    const response = await api.post(`/heartbeat/nodes/${nodeId}/suppress`, null, {
      params: { suppress },
    });
    return response.data;
  },
};

export default heartbeatService;
