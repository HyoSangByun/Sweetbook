import client from '../../../shared/api/client';
import type { 
  CreateOrderRequest, 
  OrderSummaryResponse, 
  OrderDetailResponse, 
  UpdateShippingRequest 
} from '../types';

export const createOrder = (albumId: number, data: CreateOrderRequest) => 
  client.post<OrderDetailResponse>(`/albums/${albumId}/orders`, data);

export const listOrders = (albumId: number) => 
  client.get<OrderSummaryResponse[]>(`/albums/${albumId}/orders`);

export const getOrder = (albumId: number, orderId: number) => 
  client.get<OrderDetailResponse>(`/albums/${albumId}/orders/${orderId}`);

export const cancelOrder = (albumId: number, orderId: number, reason: string) => 
  client.post<OrderDetailResponse>(`/albums/${albumId}/orders/${orderId}/cancel`, { reason });

export const updateShipping = (albumId: number, orderId: number, data: UpdateShippingRequest) => 
  client.patch<OrderDetailResponse>(`/albums/${albumId}/orders/${orderId}/shipping`, data);
