import type { OrderStatus, PaymentMethod, PaymentStatus } from './api';

export interface OrderSummary {
  id: string;
  userId: string;
  status: OrderStatus;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  totalAmount: number;
  createdAt: string;
}

export interface OrderItem {
  id: string;
  productId: string;
  sku: string;
  name: string;
  image: string | null;
  size: string | null;
  color: string | null;
  unitPrice: number;
  quantity: number;
  subtotal: number;
}

export interface OrderStatusLog {
  id: string;
  fromStatus: OrderStatus | null;
  toStatus: OrderStatus;
  changedBy: string | null;
  note: string | null;
  createdAt: string;
}

export interface SagaState {
  currentStep: string;
  status: string;
  retryCount: number;
  errorMessage: string | null;
}

export interface OrderDetail {
  id: string;
  userId: string;
  status: OrderStatus;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  subtotal: number;
  discount: number;
  totalAmount: number;
  voucherCode: string | null;
  shippingAddress: string;
  cancelledReason: string | null;
  confirmedAt: string | null;
  createdAt: string;
  items: OrderItem[];
  statusLogs: OrderStatusLog[];
  saga: SagaState | null;
}

/** A shipping address snapshot is stored as a JSON string in the order. */
export interface ShippingSnapshot {
  fullName: string;
  phone: string;
  street: string;
  district: string;
  city: string;
}

export interface CheckoutLine {
  productId: string;
  sku: string;
  name: string;
  image: string | null;
  size: string | null;
  color: string | null;
  unitPrice: number;
  quantity: number;
}

export interface CheckoutRequestBody {
  userId: string;
  lines: CheckoutLine[];
  shippingAddress: string;
  paymentMethod: PaymentMethod;
  voucherCode?: string | null;
}
