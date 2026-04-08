import client from '../../../shared/api/client';
import type { 
  ActivityMonthResponse, 
  ActivitySummaryResponse, 
  ActivityDetailResponse, 
  ActivityStatsResponse, 
  ActivityImportResponse 
} from '../types';

export const getMonths = () => 
  client.get<ActivityMonthResponse[]>('/activities/months');

export const getActivitiesByMonth = (month: string) => 
  client.get<ActivitySummaryResponse[]>('/activities', { params: { month } });

export const getActivityDetail = (activityId: number) => 
  client.get<ActivityDetailResponse>(`/activities/${activityId}`);

export const getMonthlyStats = (month: string) => 
  client.get<ActivityStatsResponse>('/activities/stats', { params: { month } });

export const importCsv = (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return client.post<ActivityImportResponse>('/activities/import', formData);
};

export const getAllOrders = () =>
  client.get<any[]>('/orders');

export const cancelOrderByUid = (orderUid: string, cancelReason: string) =>
  client.post<any>(`/orders/${encodeURIComponent(orderUid)}/cancel`, { cancelReason });

export const updateOrderShippingByUid = (
  orderUid: string,
  payload: { recipientName: string; address1: string; postalCode?: string; address2?: string }
) => client.patch<any>(`/orders/${encodeURIComponent(orderUid)}/shipping`, payload);

export const getAllBooks = () =>
  client.get<any[]>('/books');
