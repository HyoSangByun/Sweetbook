import { defineStore } from 'pinia';
import { orderApi } from '../api/orderApi';
import type { OrderRequest, OrderResponse, ShippingUpdateRequest } from '../types';
import { toErrorMessage } from '../../../shared/utils/errorMessage';

export const useOrderStore = defineStore('order', {
  state: () => ({
    orders: [] as OrderResponse[],
    currentOrder: null as OrderResponse | null,

    isFetchingList: false,
    isFetchingDetail: false,
    isCreating: false,
    isCancelling: false,
    isUpdatingShipping: false,

    listError: null as string | null,
    detailError: null as string | null,
    createError: null as string | null,
    cancelError: null as string | null,
    updateShippingError: null as string | null,
  }),

  actions: {
    async fetchOrders(albumId: number) {
      this.isFetchingList = true;
      this.listError = null;

      try {
        this.orders = await orderApi.getOrders(albumId);
      } catch (error) {
        this.listError = toErrorMessage(error, '주문 목록을 불러오지 못했습니다.');
      } finally {
        this.isFetchingList = false;
      }
    },

    async fetchOrderDetail(albumId: number, orderId: number) {
      this.isFetchingDetail = true;
      this.detailError = null;

      try {
        this.currentOrder = await orderApi.getOrder(albumId, orderId);
      } catch (error) {
        this.detailError = toErrorMessage(error, '주문 상세 정보를 불러오지 못했습니다.');
      } finally {
        this.isFetchingDetail = false;
      }
    },

    async createOrder(albumId: number, data: OrderRequest) {
      if (this.isCreating) return null;

      this.isCreating = true;
      this.createError = null;

      try {
        const createdOrder = await orderApi.createOrder(albumId, data);

        await Promise.all([
          this.fetchOrders(albumId),
          this.fetchOrderDetail(albumId, createdOrder.orderId),
        ]);

        return createdOrder;
      } catch (error) {
        this.createError = toErrorMessage(error, '주문 생성에 실패했습니다.');
        return null;
      } finally {
        this.isCreating = false;
      }
    },

    async cancelOrder(albumId: number, orderId: number) {
      if (this.isCancelling) return false;

      this.isCancelling = true;
      this.cancelError = null;

      try {
        await orderApi.cancelOrder(albumId, orderId);

        await Promise.all([
          this.fetchOrderDetail(albumId, orderId),
          this.fetchOrders(albumId),
        ]);

        return true;
      } catch (error) {
        this.cancelError = toErrorMessage(error, '주문 취소에 실패했습니다.');
        return false;
      } finally {
        this.isCancelling = false;
      }
    },

    async updateShipping(albumId: number, orderId: number, data: ShippingUpdateRequest) {
      if (this.isUpdatingShipping) return false;

      this.isUpdatingShipping = true;
      this.updateShippingError = null;

      try {
        await orderApi.updateShipping(albumId, orderId, data);

        await Promise.all([
          this.fetchOrderDetail(albumId, orderId),
          this.fetchOrders(albumId),
        ]);

        return true;
      } catch (error) {
        this.updateShippingError = toErrorMessage(error, '배송지 수정에 실패했습니다.');
        return false;
      } finally {
        this.isUpdatingShipping = false;
      }
    },
  },
});
