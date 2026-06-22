export interface CartItem {
  id: string;
  productId: string;
  sku: string;
  name: string;
  image: string | null;
  size: string | null;
  color: string | null;
  price: number;
  quantity: number;
  subtotal: number;
}

export interface Cart {
  items: CartItem[];
  totalQuantity: number;
  subtotal: number;
}
