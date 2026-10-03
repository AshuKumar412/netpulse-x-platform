import api from './api';

export const authService = {
  async login(email, password) {
    const response = await api.post('/api/auth/login', { email, password });
    return response.data;
  },

  async register(name, email, password) {
    const response = await api.post('/api/auth/register', { name, email, password });
    return response.data;
  },

  async verifyEmail(email, otp) {
    const response = await api.post('/api/auth/verify-email', { email, otp });
    return response.data;
  },

  async resendOtp(email) {
    const response = await api.post('/api/auth/resend-otp', { email });
    return response.data;
  },

  async getCurrentUser() {
    const response = await api.get('/api/users/me');
    return response.data;
  },
};

export default authService;
