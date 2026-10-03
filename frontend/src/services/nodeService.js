import api from './api';

export const nodeService = {
  async getAllNodes(status, search) {
    const params = {};
    if (status) params.status = status;
    if (search) params.search = search;
    const response = await api.get('/api/nodes', { params });
    return response.data;
  },

  async getNodeSummary() {
    const response = await api.get('/api/nodes/summary');
    return response.data;
  },

  async getNodeById(id) {
    const response = await api.get(`/api/nodes/${id}`);
    return response.data;
  },

  async createNode(nodeData) {
    const response = await api.post('/api/nodes', nodeData);
    return response.data;
  },

  async updateNode(id, nodeData) {
    const response = await api.put(`/api/nodes/${id}`, nodeData);
    return response.data;
  },

  async deleteNode(id) {
    const response = await api.delete(`/api/nodes/${id}`);
    return response.data;
  },
};
