import client from '../../../shared/api/client';
import type { OrderRequest, OrderResponse, ShippingUpdateRequest } from '../types';

const unwrap = <T>(response: T | { data: T }) => {
  if (typeof response === 'object' && response !== null && 'data' in response) {
    return response.data;
  }
  return response;
};

export const orderApi = {
  createOrder: async (albumId: number, data: OrderRequest) => {
    const response = await client.post<OrderResponse>(`/albums/${albumId}/orders`, data);
    return unwrap<OrderResponse>(response as OrderResponse | { data: OrderResponse });
  },

  getOrders: async (albumId: number) => {
    const response = await client.get<OrderResponse[]>(`/albums/${albumId}/orders`);
    return unwrap<OrderResponse[]>(response as OrderResponse[] | { data: OrderResponse[] });
  },

  getOrder: async (albumId: number, orderId: number) => {
    const response = await client.get<OrderResponse>(`/albums/${albumId}/orders/${orderId}`);
    return unwrap<OrderResponse>(response as OrderResponse | { data: OrderResponse });
  },

  cancelOrder: async (albumId: number, orderId: number) => {
    const response = await client.post<OrderResponse>(`/albums/${albumId}/orders/${orderId}/cancel`);
    return unwrap<OrderResponse>(response as OrderResponse | { data: OrderResponse });
  },

  updateShipping: async (
    albumId: number,
    orderId: number,
    data: ShippingUpdateRequest
  ) => {
    const response = await client.patch<OrderResponse>(`/albums/${albumId}/orders/${orderId}/shipping`, data);
    return unwrap<OrderResponse>(response as OrderResponse | { data: OrderResponse });
  },
};

