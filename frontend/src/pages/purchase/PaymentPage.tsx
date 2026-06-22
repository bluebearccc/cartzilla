import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { paymentApi } from '@/services/payment';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { InlineMessage } from '@/components/ui/States';
import { formatVnd } from '@/lib/format';
import { shortCode } from '@/lib/format';
import { ApiError } from '@/types/api';

export function PaymentPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const orderId = params.get('orderId') ?? '';
  const amount = Number(params.get('amount') ?? 0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const pay = async () => {
    if (!orderId) return;
    setLoading(true);
    setError(null);
    try {
      const res = await paymentApi.createVnpay(orderId, amount, `Thanh toan don hang ${shortCode(orderId)}`);
      sessionStorage.setItem('cz_last_order', orderId);
      window.location.assign(res.paymentUrl);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Không tạo được phiên thanh toán, vui lòng thử lại.');
      setLoading(false);
    }
  };

  return (
    <div className="mx-auto flex max-w-md flex-col items-center px-4 py-16">
      <div className="w-full rounded-lg border border-border bg-white p-8 text-center shadow-card">
        <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-info-tint text-info">
          <Icon name="account_balance" className="text-[28px]" />
        </div>
        <h1 className="font-headline text-xl font-bold text-ink">Thanh toán qua VNPay</h1>
        <div className="mt-6 space-y-2 rounded-lg bg-page p-4 text-left text-sm">
          <div className="flex justify-between"><span className="text-ink-secondary">Mã đơn</span><span className="font-medium text-ink">{shortCode(orderId)}</span></div>
          <div className="flex justify-between"><span className="text-ink-secondary">Phương thức</span><span className="font-medium text-ink">VNPay</span></div>
          <div className="flex justify-between"><span className="text-ink-secondary">Số tiền</span><span className="font-headline text-lg font-bold tabular-nums text-brand">{formatVnd(amount)}</span></div>
        </div>
        {error && <div className="mt-4"><InlineMessage>{error}</InlineMessage></div>}
        <Button fullWidth size="lg" className="mt-6" loading={loading} onClick={pay}>
          {loading ? 'Đang chuyển tới cổng VNPay...' : 'Thanh toán ngay'}
        </Button>
        <button onClick={() => navigate('/cart')} className="mt-3 text-sm text-ink-muted hover:text-ink">
          Hủy và quay lại giỏ hàng
        </button>
        <p className="mt-4 flex items-center justify-center gap-1 text-xs text-ink-muted">
          <Icon name="verified_user" className="text-[14px]" /> Giao dịch được bảo mật bởi VNPay.
        </p>
      </div>
    </div>
  );
}
