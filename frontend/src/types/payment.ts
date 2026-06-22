import type { PaymentMethod, PaymentStatus } from './api';

export interface PaymentTransaction {
  id: string;
  type: string;
  status: string;
  amount: number;
  createdAt: string;
}

export interface Payment {
  id: string;
  orderId: string;
  userId: string | null;
  method: PaymentMethod | null;
  amount: number;
  status: PaymentStatus | null;
  vnpayTxnRef: string | null;
  paidAt: string | null;
  createdAt: string;
  transactions: PaymentTransaction[];
}

export interface CreateVnpayResponse {
  paymentUrl: string;
  txnRef: string;
  orderId: string;
}

export interface VoucherValidation {
  code: string;
  discountAmount: number;
  discountType: string;
  valid: boolean;
  message: string;
}
