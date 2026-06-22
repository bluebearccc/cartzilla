import { api } from '@/lib/api';
import type { CheckoutRequestBody, OrderDetail, OrderSummary } from '@/types/order';

export const orderApi = {
  checkout: (body: CheckoutRequestBody) => api.post<string>('/orders/checkout', body),
  myOrders: () => api.get<OrderSummary[]>('/orders'),
  getOrder: (id: string) => api.get<OrderDetail>(`/orders/${id}`),
  cancel: (id: string, reason: string) =>
    api.post<OrderDetail>(`/orders/${id}/cancel`, { reason }),
};
