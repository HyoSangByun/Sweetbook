export type OrderStatus = 'CREATED' | 'COMPLETED' | 'CANCELLED' | 'FAILED';

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

export interface CreateOrderRequest {
  items: OrderItem[];
  shipping: ShippingInfo;
  externalRef: string;
  externalUserId: string;
}

export interface OrderSummaryResponse {
  orderId: number;
  orderUid: string;
  externalRef: string;
  status: OrderStatus;
  remoteOrderStatusCode: string;
  remoteOrderStatusDisplay: string;
  remoteOrderedAt: string | null;
  createdAt: string;
}

export interface OrderDetailResponse extends OrderSummaryResponse {
  shipping: ShippingInfo;
  items: OrderItem[];
  lastErrorMessage: string | null;
}

export interface UpdateShippingRequest {
  shipping: ShippingInfo;
}
