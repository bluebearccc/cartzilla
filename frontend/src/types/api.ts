// Backend response envelope (shared/common-web ApiResponse) + shared enums/types.

export interface ApiEnvelope<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export type Role = 'CUSTOMER' | 'STAFF' | 'ADMIN';

export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'SHIPPING' | 'DELIVERED' | 'CANCELLED';
export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED';
export type PaymentMethod = 'COD' | 'VNPAY';
export type VendorType = 'SUPPLIER' | 'BRAND' | 'MANUFACTURER';
export type DiscountType = 'PERCENTAGE' | 'FIXED_AMOUNT';
export type AudienceType = 'ALL_USERS' | 'NEW_CUSTOMER' | 'LOYAL_CUSTOMER' | 'SPECIFIC_USERS';

/** Normalized error thrown by the api helpers — carries the backend message. */
export class ApiError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}
