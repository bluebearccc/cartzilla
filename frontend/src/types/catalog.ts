import type { VendorType } from './api';

/** product-service paginated envelope: { items, page, size, totalItems, totalPages }. */
export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface ProductSummary {
  id: string;
  name: string;
  slug: string;
  basePrice: number;
  primaryImage: string | null;
  categoryId: string | null;
  vendorId: string | null;
  tags: string | null;
  active: boolean;
  featured: boolean;
  inStock: boolean;
  totalStock?: number;
}

export interface ProductVariant {
  id: string;
  sku: string;
  size: string | null;
  color: string | null;
  colorHex: string | null;
  price: number;
  stock: number;
}

export interface ProductImage {
  id: string;
  imageUrl: string;
  altText: string | null;
  isPrimary: boolean;
  sortOrder: number;
}

export interface ProductDetail {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  basePrice: number;
  categoryId: string | null;
  vendorId: string | null;
  tags: string | null;
  active: boolean;
  featured: boolean;
  sellable: boolean;
  variants: ProductVariant[];
  images: ProductImage[];
  createdAt: string;
}

export interface Category {
  id: string;
  parentId: string | null;
  name: string;
  slug: string;
  imageUrl: string | null;
  active: boolean;
  sortOrder: number;
  children: Category[];
}

export interface Vendor {
  id: string;
  name: string;
  slug: string;
  vendorType: VendorType;
  contactEmail: string | null;
  phone: string | null;
  website: string | null;
  logoUrl: string | null;
  active: boolean;
}

export interface ProductQuery {
  category?: string;
  categoryId?: string;
  q?: string;
  size?: string;
  color?: string;
  vendorId?: string;
  minPrice?: number;
  maxPrice?: number;
  inStock?: boolean;
  featured?: boolean;
  sort?: 'newest' | 'price_asc' | 'price_desc' | 'featured';
  page?: number;
  limit?: number;
}
