import axios from 'axios';
import type { AxiosInstance, AxiosRequestConfig } from 'axios';
import type { ApiResponse } from '../types/common';

type HttpClient = Omit<
  AxiosInstance,
  'request' | 'get' | 'delete' | 'head' | 'options' | 'post' | 'put' | 'patch'
> & {
  request<T = unknown, D = unknown>(config: AxiosRequestConfig<D>): Promise<T>;
  get<T = unknown, D = unknown>(url: string, config?: AxiosRequestConfig<D>): Promise<T>;
  delete<T = unknown, D = unknown>(url: string, config?: AxiosRequestConfig<D>): Promise<T>;
  head<T = unknown, D = unknown>(url: string, config?: AxiosRequestConfig<D>): Promise<T>;
  options<T = unknown, D = unknown>(url: string, config?: AxiosRequestConfig<D>): Promise<T>;
  post<T = unknown, D = unknown>(url: string, data?: D, config?: AxiosRequestConfig<D>): Promise<T>;
  put<T = unknown, D = unknown>(url: string, data?: D, config?: AxiosRequestConfig<D>): Promise<T>;
  patch<T = unknown, D = unknown>(url: string, data?: D, config?: AxiosRequestConfig<D>): Promise<T>;
};

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request Interceptor
axiosClient.interceptors.request.use((config) => {
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

  return config;
});

// Response Interceptor: Unwrap data or handle error
axiosClient.interceptors.response.use(
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

const client = axiosClient as unknown as HttpClient;

export default client;
