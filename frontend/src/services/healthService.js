import api from './api';

export const healthService = {
  getAllNodeHealth: async () => {
    const response = await api.get('/health/nodes');
    return response.data;
  },

  getNodeHealth: async (nodeId) => {
    const response = await api.get(`/health/nodes/${nodeId}`);
    return response.data;
  },
};

export default healthService;
