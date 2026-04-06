export type OrderStatus = 'REQUESTED' | 'CREATED' | 'COMPLETED' | 'CANCELLED' | 'FAILED';

export interface OrderItem {
  bookUid: string;
  quantity: number;
}

export interface ShippingInfo {
  receiverName: string;
  phoneNumber: string;
  zipCode: string;
  address: string;
  addressDetail: string;
}

export interface OrderPayload {
  items: OrderItem[];
  shipping: ShippingInfo;
  externalRef: string;
  externalUserId: string;
}

export type CreateOrderRequest = OrderPayload;

export interface OrderSummaryResponse {
  orderId: number;
  orderUid: string;
  externalRef: string;
  status: OrderStatus;
  remoteOrderStatusCode: number;
  remoteOrderStatusDisplay: string;
  remoteOrderedAt: string | null;
  lastErrorMessage: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface OrderDetailResponse extends OrderSummaryResponse {
  payload: OrderPayload;
}

export interface UpdateShippingRequest {
  shipping: ShippingInfo;
}
