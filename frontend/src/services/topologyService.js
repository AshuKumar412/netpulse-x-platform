import api from './api';

export const topologyService = {
  getTopology: async () => {
    const response = await api.get('/api/topology');
    return response.data;
  },

  getTopologyNodes: async () => {
    const response = await api.get('/api/topology/nodes');
    return response.data;
  },

  getTopologyLinks: async () => {
    const response = await api.get('/api/topology/links');
    return response.data;
  },

  getLinkById: async (id) => {
    const response = await api.get(`/api/topology/links/${id}`);
    return response.data;
  },

  createLink: async (linkData) => {
    const response = await api.post('/api/topology/links', linkData);
    return response.data;
  },

  updateLink: async (id, linkData) => {
    const response = await api.put(`/api/topology/links/${id}`, linkData);
    return response.data;
  },

  deleteLink: async (id) => {
    const response = await api.delete(`/api/topology/links/${id}`);
    return response.data;
  },

  getTopologySummary: async () => {
    const response = await api.get('/api/topology/summary');
    return response.data;
  },
};
