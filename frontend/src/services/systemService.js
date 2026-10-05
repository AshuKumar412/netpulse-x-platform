import api from './api';

export const systemService = {
  getSystemProfile: async () => {
    const response = await api.get('/api/system/profile');
    return response.data;
  },

  getLiveMetrics: async () => {
    const response = await api.get('/api/system/metrics');
    return response.data;
  },

  getSystemIdentity: async () => {
    const response = await api.get('/api/system/identity');
    return response.data;
  },
};

export default systemService;
