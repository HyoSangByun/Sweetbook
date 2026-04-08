export type OrderStatus = 'CREATED' | 'COMPLETED' | 'CANCELLED' | 'FAILED';

export interface OrderItem {
  bookUid: string;
  quantity: number;
}

export interface ShippingAddress {
  recipientName: string;
  recipientPhone: string;
  postalCode: string;
  address1: string;
  address2: string;
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
  remoteOrderStatusCode: number | null;
  remoteOrderStatusDisplay: string;
  remoteOrderedAt: string | null;
  createdAt: string;
  payload?: {
    shipping?: {
      recipientName?: string;
      recipientPhone?: string;
      postalCode?: string;
      address1?: string;
      address2?: string;
      memo?: string;
    };
  };
}

export interface ShippingUpdateRequest {
  recipientName?: string;
  phoneNumber?: string;
  postalCode?: string;
  address?: string;
  addressDetail?: string;
}
