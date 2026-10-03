import api from './api';

export const routingService = {
  decideRouting: async (strategy) => {
    const payload = strategy ? { strategy } : {};
    const response = await api.post('/routing/decide', payload);
    return response.data;
  },

  executeRoutingRequest: async (strategy) => {
    const payload = strategy ? { strategy } : {};
    const response = await api.post('/routing/request', payload);
    return response.data;
  },

  getStatus: async () => {
    const response = await api.get('/routing/status');
    return response.data;
  },

  getStrategy: async () => {
    const response = await api.get('/routing/strategy');
    return response.data;
  },

  updateStrategy: async (strategy) => {
    const response = await api.put('/routing/strategy', { strategy });
    return response.data;
  },

  getConfig: async () => {
    const response = await api.get('/routing/config');
    return response.data;
  },

  getCandidates: async () => {
    const response = await api.get('/routing/candidates');
    return response.data;
  },

  getHistory: async (limit = 30) => {
    const response = await api.get('/routing/history', {
      params: { limit },
    });
    return response.data;
  },
};

export default routingService;
