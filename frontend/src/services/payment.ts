import { api } from '@/lib/api';
import type { CreateVnpayResponse, Payment, VoucherValidation } from '@/types/payment';

export const paymentApi = {
  createVnpay: (orderId: string, amount: number, orderInfo: string) =>
    api.post<CreateVnpayResponse>('/payments/vnpay/create', { orderId, amount, orderInfo }),
  getPayment: (orderId: string) => api.get<Payment>(`/payments/${orderId}`),
};

export const voucherApi = {
  validate: (code: string, orderSubtotal: number) =>
    api.post<VoucherValidation>('/vouchers/validate', { code, orderSubtotal }),
};
