import { api } from '@/lib/api';
import type { PageResponse, Category, ProductDetail, ProductSummary, Vendor } from '@/types/catalog';
import type {
  AdminUser,
  CategoryInput,
  CountByKey,
  ReportSummary,
  SpringPage,
  TopProduct,
  VendorInput,
  Voucher,
  VoucherInput,
  VoucherAllowedUser,
} from '@/types/admin';

interface AdminProductQuery {
  categoryId?: string;
  q?: string;
  sort?: string;
  page?: number;
  limit?: number;
}

function clean(o: Record<string, unknown>): Record<string, unknown> {
  const out: Record<string, unknown> = {};
  Object.entries(o).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') out[k] = v;
  });
  return out;
}

// Variant / image payloads for the product form.
export interface VariantPayload {
  sku: string;
  size?: string;
  color?: string;
  colorHex?: string;
  price: number;
  stock: number;
}
export interface ImagePayload {
  imageUrl: string;
  altText?: string;
  isPrimary: boolean;
  sortOrder: number;
}
export interface CreateProductPayload {
  categoryId: string;
  vendorId?: string | null;
  name: string;
  slug?: string;
  description?: string;
  basePrice: number;
  tags?: string;
  variants: VariantPayload[];
  images: ImagePayload[];
}

export const adminProductApi = {
  list: (q: AdminProductQuery = {}) =>
    api.get<PageResponse<ProductSummary>>('/admin/products', { params: clean({ ...q, limit: q.limit ?? 20 }) }),
  get: (id: string) => api.get<ProductDetail>(`/admin/products/${id}`),
  create: (body: CreateProductPayload) => api.post<ProductDetail>('/admin/products', body),
  update: (id: string, body: Record<string, unknown>) => api.put<ProductDetail>(`/admin/products/${id}`, body),
  remove: (id: string) => api.del<void>(`/admin/products/${id}`),
  addVariant: (id: string, body: VariantPayload) => api.post<ProductDetail>(`/admin/products/${id}/variants`, body),
  deleteVariant: (id: string, variantId: string) => api.del<ProductDetail>(`/admin/products/${id}/variants/${variantId}`),
  addImage: (id: string, body: ImagePayload) => api.post<ProductDetail>(`/admin/products/${id}/images`, body),
  setPrimaryImage: (id: string, imageId: string) => api.put<ProductDetail>(`/admin/products/${id}/images/${imageId}/primary`),
  deleteImage: (id: string, imageId: string) => api.del<ProductDetail>(`/admin/products/${id}/images/${imageId}`),
};

export const adminCategoryApi = {
  list: () => api.get<Category[]>('/admin/categories'),
  create: (body: CategoryInput) => api.post<Category>('/admin/categories', body),
  update: (id: string, body: CategoryInput) => api.put<Category>(`/admin/categories/${id}`, body),
  remove: (id: string) => api.del<void>(`/admin/categories/${id}`),
};

export const adminVendorApi = {
  list: () => api.get<Vendor[]>('/admin/vendors'),
  create: (body: VendorInput) => api.post<Vendor>('/admin/vendors', body),
  update: (id: string, body: VendorInput) => api.put<Vendor>(`/admin/vendors/${id}`, body),
  remove: (id: string) => api.del<void>(`/admin/vendors/${id}`),
};

export const adminVoucherApi = {
  list: () => api.get<Voucher[]>('/admin/vouchers'),
  get: (id: string) => api.get<Voucher>(`/admin/vouchers/${id}`),
  create: (body: VoucherInput) => api.post<Voucher>('/admin/vouchers', body),
  update: (id: string, body: VoucherInput) => api.put<Voucher>(`/admin/vouchers/${id}`, body),
  remove: (id: string) => api.del<void>(`/admin/vouchers/${id}`),
  listAllowedUsers: (id: string) => api.get<VoucherAllowedUser[]>(`/admin/vouchers/${id}/allowed-users`),
  addAllowedUser: (id: string, userId: string) => api.post<VoucherAllowedUser>(`/admin/vouchers/${id}/allowed-users`, { userId }),
  removeAllowedUser: (id: string, userId: string) => api.del<void>(`/admin/vouchers/${id}/allowed-users/${userId}`),
};

interface AdminUserQuery {
  q?: string;
  role?: string;
  active?: boolean;
  page?: number;
  limit?: number;
}

export const adminUserApi = {
  list: (q: AdminUserQuery = {}) =>
    api.get<SpringPage<AdminUser>>('/admin/users', { params: clean({ ...q, limit: q.limit ?? 20 }) }),
  updateRole: (id: string, role: string) => api.put<AdminUser>(`/admin/users/${id}/role`, { role }),
  updateStatus: (id: string, active: boolean) => api.put<AdminUser>(`/admin/users/${id}/status`, { active }),
};

export const adminReportApi = {
  summary: (fromDate?: string, toDate?: string) =>
    api.get<ReportSummary>('/admin/reports/summary', { params: clean({ fromDate, toDate }) }),
  orderStatus: (fromDate?: string, toDate?: string) =>
    api.get<CountByKey[]>('/admin/reports/order-status', { params: clean({ fromDate, toDate }) }),
  topProducts: (limit = 10, fromDate?: string, toDate?: string) =>
    api.get<TopProduct[]>('/admin/reports/top-products', { params: clean({ limit, fromDate, toDate }) }),
};
