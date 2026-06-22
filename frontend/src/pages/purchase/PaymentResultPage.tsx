import { useMemo } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { paymentApi } from '@/services/payment';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { Spinner } from '@/components/ui/Spinner';
import { formatVnd, formatDateTime, shortCode } from '@/lib/format';

export function PaymentResultPage() {
  const [params] = useSearchParams();
  // Prefer explicit orderId; fall back to the one we stashed before redirecting.
  const orderId = params.get('orderId') || sessionStorage.getItem('cz_last_order') || '';
  // VNPay puts a response code in the return URL: "00" = success.
  const vnpCode = params.get('vnp_ResponseCode');

  const { data: payment, isLoading, refetch } = useQuery({
    queryKey: ['payment', orderId],
    queryFn: () => paymentApi.getPayment(orderId),
    enabled: !!orderId,
    refetchInterval: (q) => {
      const status = q.state.data?.status;
      return status === 'PAID' || status === 'FAILED' || status === 'REFUNDED' ? false : 3000;
    },
  });

  const state = useMemo<'success' | 'failed' | 'pending'>(() => {
    if (payment?.status === 'PAID') return 'success';
    if (payment?.status === 'FAILED') return 'failed';
    if (vnpCode && vnpCode !== '00') return 'failed';
    return 'pending';
  }, [payment, vnpCode]);

  if (!orderId) {
    return (
      <Centered>
        <Icon name="help" className="mb-3 text-[40px] text-ink-muted" />
        <h1 className="font-headline text-xl font-bold text-ink">Không tìm thấy thông tin thanh toán</h1>
        <Link to="/orders" className="mt-4"><Button>Xem đơn hàng</Button></Link>
      </Centered>
    );
  }

  if (isLoading) {
    return (
      <Centered>
        <Spinner className="h-10 w-10 text-brand" />
        <p className="mt-4 text-ink-secondary">Đang kiểm tra thanh toán...</p>
      </Centered>
    );
  }

  const variants = {
    success: { icon: 'check_circle', tone: 'bg-success-tint text-success', title: 'Thanh toán thành công' },
    failed: { icon: 'cancel', tone: 'bg-danger-tint text-danger', title: 'Thanh toán thất bại' },
    pending: { icon: 'schedule', tone: 'bg-warning-tint text-warning', title: 'Đang xác nhận thanh toán...' },
  }[state];

  return (
    <Centered>
      <div className={`mb-4 flex h-16 w-16 items-center justify-center rounded-full ${variants.tone}`}>
        <Icon name={variants.icon} className="text-[32px]" />
      </div>
      <h1 className="font-headline text-2xl font-bold text-ink">{variants.title}</h1>

      <div className="mt-6 w-full space-y-2 rounded-lg bg-page p-4 text-left text-sm">
        <Row label="Mã đơn" value={shortCode(orderId)} />
        {payment && <Row label="Số tiền" value={formatVnd(payment.amount)} />}
        {payment?.vnpayTxnRef && <Row label="Mã giao dịch" value={payment.vnpayTxnRef} />}
        {payment?.paidAt && <Row label="Thời gian" value={formatDateTime(payment.paidAt)} />}
      </div>

      {state === 'pending' && (
        <p className="mt-4 text-sm text-ink-secondary">Vui lòng chờ trong giây lát...</p>
      )}

      <div className="mt-6 flex w-full gap-3">
        {state === 'success' && (
          <>
            <Link to={`/orders/${orderId}`} className="flex-1"><Button fullWidth>Xem đơn hàng</Button></Link>
            <Link to="/products" className="flex-1"><Button variant="secondary" fullWidth>Tiếp tục mua sắm</Button></Link>
          </>
        )}
        {state === 'failed' && (
          <>
            <Link to="/cart" className="flex-1"><Button variant="secondary" fullWidth>Về giỏ hàng</Button></Link>
            <Link to={`/orders/${orderId}`} className="flex-1"><Button fullWidth>Xem đơn hàng</Button></Link>
          </>
        )}
        {state === 'pending' && (
          <Button fullWidth onClick={() => refetch()}>Tải lại</Button>
        )}
      </div>
    </Centered>
  );
}

function Centered({ children }: { children: React.ReactNode }) {
  return (
    <div className="mx-auto flex max-w-md flex-col items-center px-4 py-16 text-center">
      <div className="flex w-full flex-col items-center rounded-lg border border-border bg-white p-8 shadow-card">
        {children}
      </div>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between">
      <span className="text-ink-secondary">{label}</span>
      <span className="font-medium text-ink">{value}</span>
    </div>
  );
}
