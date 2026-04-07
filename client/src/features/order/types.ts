export type OrderStatus = 'CREATED' | 'COMPLETED' | 'CANCELLED' | 'FAILED';

export interface OrderItem {
  bookUid: string;
  quantity: number;
  unitPrice: number;
}

export interface ShippingAddress {
  recipientName: string;
  phoneNumber: string;
  postalCode: string;
  address: string;
  addressDetail: string;
}

export interface OrderRequest {
  items: OrderItem[];
  shipping: ShippingAddress;
  externalRef: string;
  externalUserId: string;
}

export interface OrderResponse {
  orderId: number;
  orderUid: string;
  externalRef: string;
  status: OrderStatus;
  lastErrorMessage: string | null;
  remoteOrderStatusCode: string;
  remoteOrderStatusDisplay: string;
  remoteOrderedAt: string | null;
  createdAt: string;
}

export interface ShippingUpdateRequest {
  recipientName?: string;
  phoneNumber?: string;
  postalCode?: string;
  address?: string;
  addressDetail?: string;
}
