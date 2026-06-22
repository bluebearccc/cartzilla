import { api } from '@/lib/api';
import type {
  Category,
  PageResponse,
  ProductDetail,
  ProductQuery,
  ProductSummary,
  Vendor,
} from '@/types/catalog';

function cleanParams(q: ProductQuery): Record<string, unknown> {
  const out: Record<string, unknown> = {};
  Object.entries(q).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') out[k] = v;
  });
  return out;
}

export const catalogApi = {
  listProducts: (q: ProductQuery = {}) =>
    api.get<PageResponse<ProductSummary>>('/products', { params: cleanParams(q) }),

  getProduct: (id: string) => api.get<ProductDetail>(`/products/${id}`),

  getProductBySlug: (slug: string) => api.get<ProductDetail>(`/products/slug/${slug}`),

  getCategories: () => api.get<Category[]>('/categories'),

  getVendors: () => api.get<Vendor[]>('/vendors'),
};
