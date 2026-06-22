// Cart state via TanStack Query. Cart is server-side (F05) and only for logged-in
// customers, so we only fetch when authenticated. Exposes count for the header
// badge plus add/update/remove helpers that invalidate the cached cart.

import { createContext, useContext, type ReactNode } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { cartApi } from '@/services/cart';
import { useAuth } from '@/store/auth';
import type { Cart } from '@/types/cart';

interface CartContextValue {
  cart: Cart | undefined;
  count: number;
  isLoading: boolean;
  refetch: () => void;
  addItem: (sku: string, quantity: number) => Promise<unknown>;
  updateItem: (itemId: string, quantity: number) => Promise<unknown>;
  removeItem: (itemId: string) => Promise<unknown>;
}

const CartContext = createContext<CartContextValue | null>(null);

export const CART_QUERY_KEY = ['cart'];

export function CartProvider({ children }: { children: ReactNode }) {
  const { isAuthenticated, hasRole } = useAuth();
  const qc = useQueryClient();
  // Only customers have a cart; staff/admin sessions skip it.
  const enabled = isAuthenticated && hasRole('CUSTOMER');

  const query = useQuery({
    queryKey: CART_QUERY_KEY,
    queryFn: cartApi.view,
    enabled,
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: CART_QUERY_KEY });

  const addMut = useMutation({
    mutationFn: ({ sku, quantity }: { sku: string; quantity: number }) =>
      cartApi.add(sku, quantity),
    onSuccess: invalidate,
  });
  const updateMut = useMutation({
    mutationFn: ({ itemId, quantity }: { itemId: string; quantity: number }) =>
      cartApi.updateQuantity(itemId, quantity),
    onSuccess: invalidate,
  });
  const removeMut = useMutation({
    mutationFn: (itemId: string) => cartApi.remove(itemId),
    onSuccess: invalidate,
  });

  const value: CartContextValue = {
    cart: query.data,
    count: query.data?.totalQuantity ?? 0,
    isLoading: query.isLoading,
    refetch: () => query.refetch(),
    addItem: (sku, quantity) => addMut.mutateAsync({ sku, quantity }),
    updateItem: (itemId, quantity) => updateMut.mutateAsync({ itemId, quantity }),
    removeItem: (itemId) => removeMut.mutateAsync(itemId),
  };

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart(): CartContextValue {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart must be used within <CartProvider>');
  return ctx;
}
