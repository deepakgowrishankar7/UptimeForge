/**
 * Authentication Module
 * Handles login, registration, OTP email verification, session persistence, and route protection.
 */

import { api } from './api.js';

export const auth = {
  async login(email, password) {
    const res = await api.post('/auth/login', { email, password });
    api.setTokens(res.accessToken, res.refreshToken);
    localStorage.setItem('pulsewatch_user', JSON.stringify(res.user));
    return res.user;
  },

  async sendRegistrationOtp(email, name) {
    return await api.post('/auth/register/send-otp', { email, name });
  },

  async register(name, email, password, otp) {
    const res = await api.post('/auth/register', { name, email, password, otp });
    api.setTokens(res.accessToken, res.refreshToken);
    localStorage.setItem('pulsewatch_user', JSON.stringify(res.user));
    return res.user;
  },

  async logout() {
    try {
      await api.post('/auth/logout', { refreshToken: api.refreshToken });
    } catch (e) {
      // Continue cleanup regardless of server response
    }
    api.clearTokens();
    window.location.href = '/login.html';
  },

  getUser() {
    const raw = localStorage.getItem('pulsewatch_user');
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  },

  isAuthenticated() {
    return !!api.accessToken && !!this.getUser();
  },

  isAdmin() {
    const user = this.getUser();
    return user && user.role === 'ADMIN';
  },

  requireAuth() {
    if (!this.isAuthenticated()) {
      window.location.href = '/login.html';
      return false;
    }
    return true;
  },

  requireAdmin() {
    if (!this.requireAuth()) return false;
    if (!this.isAdmin()) {
      window.location.href = '/dashboard.html';
      return false;
    }
    return true;
  }
};
