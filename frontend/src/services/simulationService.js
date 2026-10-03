import api from './api';

export const simulationService = {
  getStatus: async () => {
    const response = await api.get('/api/simulation/status');
    return response.data;
  },

  startSimulation: async () => {
    const response = await api.post('/api/simulation/start');
    return response.data;
  },

  pauseSimulation: async () => {
    const response = await api.post('/api/simulation/pause');
    return response.data;
  },

  stopSimulation: async () => {
    const response = await api.post('/api/simulation/stop');
    return response.data;
  },
};
