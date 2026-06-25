import type { AudienceType, DiscountType, Role, VendorType } from './api';

/** user-service paginated envelope (unified): items + totalItems. */
export interface SpringPage<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface AdminUser {
  id: string;
  email: string;
  fullName: string;
  phone: string | null;
  role: Role;
  emailVerified: boolean;
  active: boolean;
}

export interface Voucher {
  id: string;
  code: string;
  discountType: DiscountType;
  discountValue: number;
  maxDiscountAmount: number | null;
  minOrderAmount: number | null;
  minAccountAgeDays: number;
  perUserLimit: number;
  audienceType: AudienceType;
  firstOrderOnly: boolean;
  minCompletedOrders: number;
  minTotalSpent: number | null;
  maxUses: number;
  usedCount: number;
  active: boolean;
  startsAt: string | null;
  expiresAt: string | null;
}

export interface VoucherInput {
  code?: string;
  discountType: DiscountType;
  discountValue: number;
  maxDiscountAmount?: number | null;
  minOrderAmount?: number | null;
  maxUses: number;
  startsAt?: string | null;
  expiresAt?: string | null;
  minAccountAgeDays: number;
  perUserLimit: number;
  audienceType: AudienceType;
  firstOrderOnly: boolean;
  minCompletedOrders: number;
  minTotalSpent?: number | null;
  active?: boolean;
}

export interface VoucherAllowedUser {
  id?: string;
  voucherId?: string;
  userId: string;
  email?: string;
  fullName?: string;
}

export interface CategoryInput {
  name: string;
  slug?: string;
  parentId?: string | null;
  imageUrl?: string | null;
  sortOrder: number;
  active?: boolean;
}

export interface VendorInput {
  name: string;
  slug?: string;
  vendorType: VendorType;
  contactEmail?: string;
  phone?: string;
  website?: string;
  logoUrl?: string;
  active?: boolean;
}

// Reports
export interface CountByKey {
  key: string;
  count: number;
}
export interface MethodRevenue {
  method: string;
  count: number;
  revenue: number;
}
export interface ReportSummary {
  totalRevenue: number;
  totalOrders: number;
  ordersByStatus: CountByKey[];
  ordersByPaymentStatus: CountByKey[];
  revenueByMethod: MethodRevenue[];
}
export interface TopProduct {
  productId: string;
  name: string;
  sku: string;
  quantitySold: number;
  revenue: number;
}
