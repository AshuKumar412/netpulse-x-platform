import api from './api';

export const monitoringService = {
  getStatus: async () => {
    const response = await api.get('/monitoring/status');
    return response.data;
  },

  startMonitoring: async () => {
    const response = await api.post('/monitoring/start');
    return response.data;
  },

  stopMonitoring: async () => {
    const response = await api.post('/monitoring/stop');
    return response.data;
  },
};

export default monitoringService;
