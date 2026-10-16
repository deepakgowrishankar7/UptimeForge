/**
 * PulseWatch API Client
 * Manages JWT tokens, automatic refresh, error handling, and requests.
 */

const API_BASE = '/api/v1';

class ApiClient {
  constructor() {
    this.accessToken = localStorage.getItem('pulsewatch_access_token');
    this.refreshToken = localStorage.getItem('pulsewatch_refresh_token');
    this.isRefreshing = false;
    this.refreshSubscribers = [];
  }

  setTokens(accessToken, refreshToken) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    if (accessToken) localStorage.setItem('pulsewatch_access_token', accessToken);
    else localStorage.removeItem('pulsewatch_access_token');

    if (refreshToken) localStorage.setItem('pulsewatch_refresh_token', refreshToken);
    else localStorage.removeItem('pulsewatch_refresh_token');
  }

  clearTokens() {
    this.setTokens(null, null);
    localStorage.removeItem('pulsewatch_user');
  }

  getHeaders(isJson = true) {
    const headers = {};
    if (isJson) headers['Content-Type'] = 'application/json';
    if (this.accessToken) {
      headers['Authorization'] = `Bearer ${this.accessToken}`;
    }
    return headers;
  }

  async request(endpoint, options = {}) {
    const url = endpoint.startsWith('http') ? endpoint : `${API_BASE}${endpoint}`;
    options.headers = {
      ...this.getHeaders(options.body && !(options.body instanceof FormData)),
      ...(options.headers || {})
    };

    let response = await fetch(url, options);

    // If 401 Unauthorized, try refreshing access token once
    if (response.status === 401 && this.refreshToken && !options._retry && !endpoint.includes('/auth/login') && !endpoint.includes('/auth/refresh')) {
      options._retry = true;
      const refreshed = await this.tryRefreshToken();
      if (refreshed) {
        options.headers['Authorization'] = `Bearer ${this.accessToken}`;
        return fetch(url, options).then(res => this.handleResponse(res));
      } else {
        this.clearTokens();
        window.location.href = '/login.html';
        throw new Error('Session expired. Please log in again.');
      }
    }

    return this.handleResponse(response);
  }

  async tryRefreshToken() {
    if (this.isRefreshing) {
      return new Promise(resolve => this.refreshSubscribers.push(resolve));
    }

    this.isRefreshing = true;
    try {
      const res = await fetch(`${API_BASE}/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: this.refreshToken })
      });
      const data = await res.json();
      if (res.ok && data.success && data.data && data.data.accessToken) {
        this.setTokens(data.data.accessToken, data.data.refreshToken);
        this.refreshSubscribers.forEach(cb => cb(true));
        this.refreshSubscribers = [];
        return true;
      }
    } catch (e) {
      console.error('Failed to refresh token', e);
    } finally {
      this.isRefreshing = false;
    }

    this.refreshSubscribers.forEach(cb => cb(false));
    this.refreshSubscribers = [];
    return false;
  }

  async handleResponse(response) {
    const contentType = response.headers.get('content-type');
    let data;
    if (contentType && contentType.includes('application/json')) {
      data = await response.json();
    } else {
      data = await response.text();
    }

    if (!response.ok) {
      let errorMessage = 'Request failed';
      if (data && typeof data === 'object') {
        errorMessage = data.message || data.error || (data.validationErrors ? Object.values(data.validationErrors).join(', ') : 'Error');
      } else if (typeof data === 'string') {
        errorMessage = data;
      }
      const err = new Error(errorMessage);
      err.status = response.status;
      err.data = data;
      throw err;
    }

    return data && data.data !== undefined ? data.data : data;
  }

  get(endpoint, params = {}) {
    const query = new URLSearchParams(params).toString();
    const url = query ? `${endpoint}?${query}` : endpoint;
    return this.request(url, { method: 'GET' });
  }

  post(endpoint, body = {}) {
    return this.request(endpoint, {
      method: 'POST',
      body: JSON.stringify(body)
    });
  }

  put(endpoint, body = {}) {
    return this.request(endpoint, {
      method: 'PUT',
      body: JSON.stringify(body)
    });
  }

  patch(endpoint, body = {}) {
    return this.request(endpoint, {
      method: 'PATCH',
      body: JSON.stringify(body)
    });
  }

  del(endpoint) {
    return this.request(endpoint, { method: 'DELETE' });
  }
}

export const api = new ApiClient();
