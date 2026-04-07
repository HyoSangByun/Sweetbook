import { defineStore } from 'pinia';
import { orderApi } from '../api/orderApi';
import { OrderResponse, OrderRequest, ShippingUpdateRequest } from '../types';

export const useOrderStore = defineStore('order', {
  state: () => ({
    orders: [] as OrderResponse[],
    currentOrder: null as OrderResponse | null,
    loading: false,
    error: null as string | null,
  }),

  actions: {
    async fetchOrders(albumId: number) {
      this.loading = true;
      this.error = null;
      try {
        const response = await orderApi.getOrders(albumId);
        if (response.success) {
          this.orders = response.data;
        } else {
          this.error = response.error?.message || '주문 목록을 불러오는데 실패했습니다.';
        }
      } catch (err: any) {
        this.error = err.message || '네트워크 오류가 발생했습니다.';
      } finally {
        this.loading = false;
      }
    },

    async fetchOrderDetail(albumId: number, orderId: number) {
      this.loading = true;
      this.error = null;
      try {
        const response = await orderApi.getOrder(albumId, orderId);
        if (response.success) {
          this.currentOrder = response.data;
        } else {
          this.error = response.error?.message || '주문 상세 정보를 불러오는데 실패했습니다.';
        }
      } catch (err: any) {
        this.error = err.message || '네트워크 오류가 발생했습니다.';
      } finally {
        this.loading = false;
      }
    },

    async createOrder(albumId: number, data: OrderRequest) {
      this.loading = true;
      this.error = null;
      try {
        const response = await orderApi.createOrder(albumId, data);
        if (response.success) {
          await this.fetchOrders(albumId);
          return response.data;
        } else {
          this.error = response.error?.message || '주문 생성에 실패했습니다.';
          return null;
        }
      } catch (err: any) {
        this.error = err.message || '네트워크 오류가 발생했습니다.';
        return null;
      } finally {
        this.loading = false;
      }
    },

    async cancelOrder(albumId: number, orderId: number) {
      this.loading = true;
      this.error = null;
      try {
        const response = await orderApi.cancelOrder(albumId, orderId);
        if (response.success) {
          await this.fetchOrderDetail(albumId, orderId);
          await this.fetchOrders(albumId);
          return true;
        } else {
          this.error = response.error?.message || '주문 취소에 실패했습니다.';
          return false;
        }
      } catch (err: any) {
        this.error = err.message || '네트워크 오류가 발생했습니다.';
        return false;
      } finally {
        this.loading = false;
      }
    },

    async updateShipping(albumId: number, orderId: number, data: ShippingUpdateRequest) {
      this.loading = true;
      this.error = null;
      try {
        const response = await orderApi.updateShipping(albumId, orderId, data);
        if (response.success) {
          await this.fetchOrderDetail(albumId, orderId);
          return true;
        } else {
          this.error = response.error?.message || '배송지 수정에 실패했습니다.';
          return false;
        }
      } catch (err: any) {
        this.error = err.message || '네트워크 오류가 발생했습니다.';
        return false;
      } finally {
        this.loading = false;
      }
    },
  },
});
