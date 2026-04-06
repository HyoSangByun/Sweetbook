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
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response Interceptor: Unwrap data or handle error
client.interceptors.response.use(
  (response) => {
    const apiResponse = response.data as ApiResponse<any>;
    if (apiResponse.success) {
      return apiResponse.data;
    }
    return Promise.reject(apiResponse.error);
  },
  (error) => {
    if (error.response?.data) {
      const apiResponse = error.response.data as ApiResponse<any>;
      if (apiResponse.error) {
        return Promise.reject(apiResponse.error);
      }
    }
    
    // Fallback for network errors or non-standard responses
    return Promise.reject({
      code: 'NETWORK_ERROR',
      message: error.message || 'Network error occurred',
      details: error
    });
  }
);

export default client;
