import client from '@/shared/api/client';
import { ApiResponse } from '@/shared/types/common';
import { OrderRequest, OrderResponse, ShippingUpdateRequest } from '../types';

export const orderApi = {
  /**
   * 주문 생성
   */
  createOrder: async (albumId: number, data: OrderRequest) => {
    const response = await client.post<ApiResponse<OrderResponse>>(
      `/albums/${albumId}/orders`,
      data
    );
    return response.data;
  },

  /**
   * 주문 목록 조회
   */
  getOrders: async (albumId: number) => {
    const response = await client.get<ApiResponse<OrderResponse[]>>(
      `/albums/${albumId}/orders`
    );
    return response.data;
  },

  /**
   * 주문 상세 조회
   */
  getOrder: async (albumId: number, orderId: number) => {
    const response = await client.get<ApiResponse<OrderResponse>>(
      `/albums/${albumId}/orders/${orderId}`
    );
    return response.data;
  },

  /**
   * 주문 취소
   */
  cancelOrder: async (albumId: number, orderId: number) => {
    const response = await client.post<ApiResponse<OrderResponse>>(
      `/albums/${albumId}/orders/${orderId}/cancel`
    );
    return response.data;
  },

  /**
   * 배송지 수정
   */
  updateShipping: async (
    albumId: number,
    orderId: number,
    data: ShippingUpdateRequest
  ) => {
    const response = await client.patch<ApiResponse<OrderResponse>>(
      `/albums/${albumId}/orders/${orderId}/shipping`,
      data
    );
    return response.data;
  },
};
