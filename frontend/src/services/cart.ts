import { api } from '@/lib/api';
import type { Cart, CartItem } from '@/types/cart';

export const cartApi = {
  view: () => api.get<Cart>('/orders/cart/items'),
  add: (sku: string, quantity: number) =>
    api.post<CartItem>('/orders/cart/items', { sku, quantity }),
  updateQuantity: (itemId: string, quantity: number) =>
    api.put<void>(`/orders/cart/items/${itemId}`, { quantity }),
  remove: (itemId: string) => api.del<void>(`/orders/cart/items/${itemId}`),
};
