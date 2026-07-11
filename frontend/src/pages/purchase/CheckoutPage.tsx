import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { CART_QUERY_KEY, useCart } from '@/store/cart';
import { userApi } from '@/services/user';
import { orderApi } from '@/services/order';
import { voucherApi } from '@/services/payment';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Icon } from '@/components/ui/Icon';
import { EmptyState, InlineMessage } from '@/components/ui/States';
import { useToast } from '@/components/ui/Toast';
import { getUserId } from '@/lib/auth-storage';
import { formatVnd } from '@/lib/format';
import { ApiError, type PaymentMethod } from '@/types/api';
import type { VoucherValidation } from '@/types/payment';
import { cn } from '@/lib/cn';

export function CheckoutPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const qc = useQueryClient();
  const { cart, isLoading } = useCart();
  const { data: addresses } = useQuery({ queryKey: ['addresses'], queryFn: userApi.listAddresses });

  const [addressId, setAddressId] = useState<string | null>(null);
  const [method, setMethod] = useState<PaymentMethod>('COD');
  const [voucherCode, setVoucherCode] = useState('');
  const [voucher, setVoucher] = useState<VoucherValidation | null>(null);
  const [voucherError, setVoucherError] = useState<string | null>(null);
  const [applying, setApplying] = useState(false);
  const [placing, setPlacing] = useState(false);

  useEffect(() => {
    if (addresses?.length && !addressId) {
      setAddressId((addresses.find((a) => a.defaultAddress) ?? addresses[0]).id);
    }
  }, [addresses, addressId]);

  const subtotal = cart?.subtotal ?? 0;
  const discount = voucher?.valid ? voucher.discountAmount : 0;
  const total = Math.max(0, subtotal - discount);
  const items = cart?.items ?? [];

  const applyVoucher = async () => {
    if (!voucherCode.trim()) return;
    setApplying(true);
    setVoucherError(null);
    try {
      const res = await voucherApi.validate(voucherCode.trim(), subtotal);
      if (res.valid) {
        setVoucher(res);
        toast.success(`Áp dụng ${res.code}: -${formatVnd(res.discountAmount)}`);
      } else {
        setVoucher(null);
        setVoucherError(res.message || 'Mã giảm giá không hợp lệ');
      }
    } catch (e) {
      setVoucher(null);
      setVoucherError(e instanceof ApiError ? e.message : 'Mã giảm giá không hợp lệ');
    } finally {
      setApplying(false);
    }
  };

  const placeOrder = async () => {
    const userId = getUserId();
    const address = addresses?.find((a) => a.id === addressId);
    if (!address) {
      toast.error('Vui lòng chọn địa chỉ giao hàng');
      return;
    }
    if (!userId || !items.length) return;
    setPlacing(true);
    try {
      const orderId = await orderApi.checkout({
        userId,
        lines: items.map((it) => ({
          productId: it.productId,
          sku: it.sku,
          name: it.name,
          image: it.image,
          size: it.size,
          color: it.color,
          unitPrice: it.price,
          quantity: it.quantity,
        })),
        shippingAddress: JSON.stringify({
          fullName: address.fullName,
          phone: address.phone,
          street: address.street,
          district: address.district,
          city: address.city,
        }),
        paymentMethod: method,
        voucherCode: voucher?.valid ? voucher.code : null,
      });
      // Server đã xóa các dòng giỏ vừa đặt — refetch để badge/cart cập nhật ngay.
      qc.invalidateQueries({ queryKey: CART_QUERY_KEY });
      if (method === 'VNPAY') {
        navigate(`/checkout/payment?orderId=${orderId}`);
      } else {
        toast.success('Đặt hàng thành công');
        navigate(`/orders/${orderId}`);
      }
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : 'Đặt hàng thất bại');
    } finally {
      setPlacing(false);
    }
  };

  if (!isLoading && !items.length) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6">
        <EmptyState
          icon="shopping_cart_off"
          title="Giỏ hàng trống, không thể thanh toán"
          action={{ label: 'Mua sắm ngay', onClick: () => navigate('/products') }}
        />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6">
      <h1 className="mb-6 font-headline text-2xl font-bold text-ink">Thanh toán</h1>
      <div className="grid gap-6 lg:grid-cols-[1fr_380px]">
        <div className="space-y-6">
          {/* Address */}
          <Section number={1} title="Địa chỉ giao hàng">
            {!addresses?.length ? (
              <InlineMessage tone="warning">
                Bạn chưa có địa chỉ.{' '}
                <button onClick={() => navigate('/addresses')} className="font-medium underline">Thêm địa chỉ</button>
              </InlineMessage>
            ) : (
              <div className="space-y-3">
                {addresses.map((a) => (
                  <label
                    key={a.id}
                    className={cn(
                      'flex cursor-pointer items-start gap-3 rounded-lg border p-4',
                      addressId === a.id ? 'border-brand bg-brand-tint/40' : 'border-border',
                    )}
                  >
                    <input type="radio" name="address" checked={addressId === a.id} onChange={() => setAddressId(a.id)} className="mt-1 h-4 w-4 text-brand" />
                    <div>
                      <p className="font-medium text-ink">
                        {a.fullName} · {a.phone}
                        {a.defaultAddress && <span className="ml-2 rounded-full bg-brand-tint px-2 py-0.5 text-xs text-brand">Mặc định</span>}
                      </p>
                      <p className="text-sm text-ink-secondary">{a.street}, {a.district}, {a.city}</p>
                    </div>
                  </label>
                ))}
                <button onClick={() => navigate('/addresses')} className="text-sm font-medium text-brand hover:underline">
                  + Thêm địa chỉ mới
                </button>
              </div>
            )}
          </Section>

          {/* Payment method */}
          <Section number={2} title="Phương thức thanh toán">
            <div className="grid gap-3 sm:grid-cols-2">
              {([
                { v: 'COD', label: 'COD', desc: 'Thanh toán khi nhận hàng', icon: 'payments' },
                { v: 'VNPAY', label: 'VNPay', desc: 'Thẻ / ATM / QR', icon: 'qr_code_2' },
              ] as const).map((m) => (
                <label
                  key={m.v}
                  className={cn(
                    'flex cursor-pointer items-center gap-3 rounded-lg border p-4',
                    method === m.v ? 'border-brand bg-brand-tint/40' : 'border-border',
                  )}
                >
                  <input type="radio" name="method" checked={method === m.v} onChange={() => setMethod(m.v)} className="h-4 w-4 text-brand" />
                  <Icon name={m.icon} className="text-[24px] text-brand" />
                  <div>
                    <p className="font-medium text-ink">{m.label}</p>
                    <p className="text-xs text-ink-muted">{m.desc}</p>
                  </div>
                </label>
              ))}
            </div>
          </Section>

          {/* Voucher */}
          <Section number={3} title="Mã giảm giá">
            <div className="flex gap-2">
              <Input placeholder="Nhập mã giảm giá" value={voucherCode} onChange={(e) => setVoucherCode(e.target.value.toUpperCase())} />
              <Button variant="secondary" onClick={applyVoucher} loading={applying}>Áp dụng</Button>
            </div>
            {voucher?.valid && (
              <p className="mt-2 text-sm text-success">
                Áp dụng {voucher.code}: −{formatVnd(voucher.discountAmount)}
              </p>
            )}
            {voucherError && <p className="mt-2 text-sm text-danger">{voucherError}</p>}
          </Section>
        </div>

        {/* Summary */}
        <div className="h-fit rounded-lg border border-border bg-white p-5 lg:sticky lg:top-20">
          <h2 className="mb-4 font-headline text-lg font-bold text-ink">Đơn hàng của bạn</h2>
          <div className="max-h-60 space-y-3 overflow-y-auto">
            {items.map((it) => (
              <div key={it.id} className="flex items-center gap-3">
                <div className="relative">
                  <img src={it.image || ''} alt="" className="h-12 w-12 rounded object-cover" />
                  <span className="absolute -right-1 -top-1 flex h-5 min-w-5 items-center justify-center rounded-full bg-ink px-1 text-[10px] text-white">
                    {it.quantity}
                  </span>
                </div>
                <p className="line-clamp-1 flex-1 text-sm text-ink">{it.name}</p>
                <p className="text-sm tabular-nums text-ink">{formatVnd(it.subtotal)}</p>
              </div>
            ))}
          </div>
          <div className="my-4 space-y-2 border-t border-divider pt-4 text-sm">
            <div className="flex justify-between"><span className="text-ink-secondary">Tạm tính</span><span className="tabular-nums">{formatVnd(subtotal)}</span></div>
            {discount > 0 && <div className="flex justify-between text-success"><span>Giảm giá</span><span className="tabular-nums">−{formatVnd(discount)}</span></div>}
            <div className="flex justify-between"><span className="text-ink-secondary">Phí vận chuyển</span><span className="text-success">Miễn phí</span></div>
          </div>
          <div className="flex items-center justify-between border-t border-divider pt-4">
            <span className="font-semibold text-ink">Tổng cộng</span>
            <span className="font-headline text-xl font-bold tabular-nums text-brand">{formatVnd(total)}</span>
          </div>
          <Button fullWidth size="lg" className="mt-5" loading={placing} disabled={!addressId || !items.length} onClick={placeOrder}>
            Đặt hàng
          </Button>
          <p className="mt-2 flex items-center justify-center gap-1 text-center text-xs text-ink-muted">
            <Icon name="lock" className="text-[14px]" /> Thông tin của bạn được bảo mật.
          </p>
        </div>
      </div>
    </div>
  );
}

function Section({ number, title, children }: { number: number; title: string; children: React.ReactNode }) {
  return (
    <section className="rounded-lg border border-border bg-white p-5">
      <h2 className="mb-4 flex items-center gap-2 font-headline text-lg font-bold text-ink">
        <span className="flex h-7 w-7 items-center justify-center rounded-full bg-brand text-sm text-white">{number}</span>
        {title}
      </h2>
      {children}
    </section>
  );
}
