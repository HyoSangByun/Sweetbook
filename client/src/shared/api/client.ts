import axios from 'axios';
import type { ApiResponse } from '../types/common';

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request Interceptor: Add Authorization Header
client.interceptors.request.use((config) => {
  if (typeof FormData !== 'undefined' && config.data instanceof FormData && config.headers) {
    const headers = config.headers as any;
    if (typeof headers.delete === 'function') {
      headers.delete('Content-Type');
      headers.delete('content-type');
    } else {
      delete headers['Content-Type'];
      delete headers['content-type'];
    }
  }

  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response Interceptor: Unwrap data or handle error
client.interceptors.response.use(
  (response) => {
    const data = response.data;
    if (data && typeof data === 'object' && 'success' in data) {
      const apiResponse = data as ApiResponse<any>;
      if (apiResponse.success) {
        return apiResponse.data;
      }
      return Promise.reject(apiResponse.error || {
        code: 'UNKNOWN_ERROR',
        message: 'Request failed with success=false but no error provided'
      });
    }
    
    return Promise.reject({
      code: 'MALFORMED_RESPONSE',
      message: 'Server returned an invalid response envelope'
    });
  },
  (error) => {
    if (error.response?.data) {
      const data = error.response.data;
      if (data && typeof data === 'object' && 'error' in data) {
        const apiResponse = data as ApiResponse<any>;
        if (apiResponse.error) {
          return Promise.reject(apiResponse.error);
        }
      }
    }
    
    // Fallback for network errors or non-standard responses
    return Promise.reject({
      code: 'NETWORK_ERROR',
      message: error.message || 'Network error occurred',
      details: {
        status: error.response?.status,
        statusText: error.response?.statusText,
      }
    });
  }
);

export default client;
