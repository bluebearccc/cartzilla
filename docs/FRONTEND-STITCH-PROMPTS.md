# Cartzilla — Bộ prompt Google Stitch CHI TIẾT (toàn bộ màn hình)

> Dùng [Google Stitch](https://stitch.withgoogle.com) sinh UI cho **toàn bộ frontend Cartzilla**
> (React 18 + TypeScript + Vite + Tailwind), bám **SRS §7** và đúng dữ liệu/endpoint backend
> (gateway `http://localhost:8080`, envelope `ApiResponse`, JWT Bearer).
>
> Mỗi mục bên dưới là **1 màn hình = 1 prompt** chi tiết tối đa: layout, từng component, label
> tiếng Việt, mọi trạng thái (loading / empty / error / success), validation theo business rule,
> responsive. **Quy trình:** dán **Prompt 0** trước → dán prompt từng màn hình → generate từng cái.

## Mục lục

- **Prompt 0** — Design System & component library (dán đầu mỗi session)
- Storefront: [1 Home](#1-home) · [2 Product List](#2-product-list) · [3 Product Detail](#3-product-detail)
- Auth: [4 Login](#4-login) · [5 Register](#5-register) · [6 Verify Email](#6-verify-email) · [7 OAuth Callback](#7-oauth-callback) · [8 Forgot Password](#8-forgot-password) · [9 Reset Password](#9-reset-password)
- Khách: [10 Profile](#10-profile) · [11 Addresses](#11-addresses) · [12 Notifications](#12-notifications)
- Mua hàng: [13 Cart](#13-cart) · [14 Checkout](#14-checkout) · [15 Payment](#15-payment) · [16 Payment Result](#16-payment-result)
- Đơn hàng: [17 Orders List](#17-orders-list) · [18 Order Detail](#18-order-detail)
- Staff: [19 Staff Orders](#19-staff-orders) · [20 Staff Order Detail](#20-staff-order-detail)
- Admin: [21 Products](#21-admin-products) · [22 Product Form](#22-admin-product-form) · [23 Categories](#23-admin-categories) · [24 Vendors](#24-admin-vendors) · [25 Vouchers](#25-admin-vouchers) · [26 Users](#26-admin-users) · [27 Reports](#27-admin-reports)
- [Sau khi Stitch — nối backend](#sau-khi-stitch--nối-backend)

---

## Prompt 0 — Design System (dán đầu MỖI session)

```
You are the lead product designer for "Cartzilla", a modern Vietnamese fashion e-commerce web app.
Design desktop-first but fully responsive (breakpoints: mobile 360, tablet 768, desktop 1280).
Tech target for handoff: React + TypeScript + Tailwind CSS. Output clean, production-grade UI.

DESIGN TOKENS
- Surfaces: page bg #F8FAFC, card bg #FFFFFF, border #E2E8F0, divider #EEF2F6.
- Text: primary #0F172A, secondary #475569, muted #94A3B8.
- Brand: primary indigo #4F46E5 (hover #4338CA), primary-tint #EEF2FF.
- Semantic: success #16A34A/tint #DCFCE7, warning #F59E0B/tint #FEF3C7,
  danger #DC2626/tint #FEE2E2, info #0EA5E9/tint #E0F2FE.
- Radius: inputs/buttons 10px, cards 16px, pills 999px. Shadow: card = 0 1px 2px rgba(15,23,42,.06)
  + 0 8px 24px rgba(15,23,42,.04). Spacing scale 4/8/12/16/24/32/48.
- Typography: "Inter" (UI) + "Manrope" (headings). H1 32/40 bold, H2 24/32, H3 18/28,
  body 14–16/24, caption 12/16. Prices use tabular numerals.

CURRENCY & FORMAT
- All prices in Vietnamese Dong formatted "1.250.000 ₫" (dot thousands separator, ₫ suffix).
- Dates "dd/MM/yyyy HH:mm". Relative time in Vietnamese ("2 giờ trước", "Hôm qua").

COMPONENT LIBRARY (reuse across screens)
- Buttons: primary (filled indigo), secondary (outline), ghost (text), danger; sizes sm/md/lg;
  with optional leading icon; disabled + loading (spinner) states.
- Inputs: text/email/password/number/textarea/select/date with label, helper text, and error state
  (red border + message). Password field has show/hide toggle. Required fields marked with *.
- Status chips (pill, tint bg + colored text), used everywhere with this exact mapping:
  Đơn: PENDING="Chờ xử lý" warning · CONFIRMED="Đã xác nhận" info · SHIPPING="Đang giao" indigo ·
  DELIVERED="Đã giao" success · CANCELLED="Đã hủy" danger.
  Thanh toán: PENDING="Chờ thanh toán" warning · PAID="Đã thanh toán" success ·
  FAILED="Thất bại" danger · REFUNDED="Đã hoàn tiền" muted.
- Cards, data tables (sticky header, zebra optional, row hover, sortable headers, pagination footer),
  modals/drawers (overlay + focus trap), toasts (top-right, auto-dismiss), tabs, breadcrumbs,
  skeleton loaders, empty states (icon + title + subtitle + CTA), inline form validation.

GLOBAL CHROME
- Storefront header (sticky): Cartzilla wordmark logo (left), centered search input with placeholder
  "Tìm sản phẩm...", right cluster = nav links (Trang chủ, Sản phẩm), notifications bell with red
  unread dot, cart icon with item-count badge, user avatar dropdown (Hồ sơ, Đơn hàng, Thông báo,
  Đăng xuất) OR "Đăng nhập"/"Đăng ký" buttons when guest.
- Storefront footer: 4 columns (Về Cartzilla, Hỗ trợ, Chính sách, Kết nối) + newsletter input +
  copyright + payment/VNPay badges.
- Dashboard chrome (staff/admin): fixed left sidebar (logo, grouped nav with icons, active item
  highlighted with indigo tint + left accent bar) + top bar (page title, breadcrumb, user menu).

Always include loading, empty, and error states where relevant. ALL visible copy in Vietnamese.
From now on, design ONLY the single screen I describe in each next message.
```

---

## 1. Home
`/` · Guest/Customer · F03

```
Design the Cartzilla HOME page ("/") using the storefront header + footer.
Sections, top to bottom:

1) HERO carousel (full-bleed, 16:6): 3 slides of fashion campaigns. Each slide = background image,
   left-aligned overlay with eyelash label ("Bộ sưu tập mới"), H1 headline, one-line subtext,
   primary button "Mua ngay". Dots + prev/next arrows; auto-rotate.

2) CATEGORY shortcuts: horizontal row of 6–8 circular image tiles + label below
   (Áo, Quần, Đầm, Giày, Túi, Phụ kiện...). Scrollable on mobile.

3) "SẢN PHẨM NỔI BẬT" section: section header (title left + "Xem tất cả →" link right) and a
   responsive PRODUCT CARD grid (4 cols desktop / 3 tablet / 2 mobile, 8 items).
   PRODUCT CARD (reused everywhere): square image; top-left badge "Mới"/"-20%" if applicable;
   a wishlist heart top-right; below image: vendor/brand name (12px muted), product name
   (2 lines max), price in ₫ (bold) + optional struck original price; a row of small color swatch
   dots; on hover the card lifts and shows a full-width "Thêm vào giỏ" button. Out-of-stock cards
   show a "Hết hàng" overlay and disabled button.

4) PROMO band: two side-by-side promotional banners (rounded, image + text + button).

5) "DANH MỤC PHỔ BIẾN": 3–4 large category blocks linking to filtered product list.

6) "THƯƠNG HIỆU": grayscale vendor/brand logo strip.

7) Trust band: 4 mini features with icon (Miễn phí vận chuyển, Đổi trả 7 ngày, Thanh toán VNPay,
   Hỗ trợ 24/7).

Include a loading skeleton for the featured grid. Fully responsive.
```

---

## 2. Product List
`/products` · Guest/Customer · F03 · `GET /api/products?category=&size=&color=&vendor=&minPrice=&maxPrice=&inStock=&sort=&page=`

```
Design the Cartzilla PRODUCT LISTING page ("/products"). Storefront header + footer.
Top: breadcrumb (Trang chủ / Sản phẩm) + H1 "Sản phẩm" + result count text ("Hiển thị 24 / 312 sản phẩm").

LAYOUT: left FILTER sidebar (sticky, ~280px) + right RESULTS area.

FILTER SIDEBAR (each group is a collapsible accordion with a header + clear link):
- "Danh mục": checkbox tree (parent → children), counts in parentheses.
- "Kích cỡ": chip toggles S, M, L, XL, XXL (multi-select, selected = indigo fill).
- "Màu sắc": grid of color swatch chips (circle in colorHex + name tooltip; selected = ring).
- "Thương hiệu": searchable checkbox list of vendors.
- "Khoảng giá": dual-handle range slider + two ₫ number inputs (min/max) + "Áp dụng".
- "Tình trạng": toggle "Chỉ hiển thị còn hàng".
- Footer of sidebar: "Xóa tất cả bộ lọc".
On mobile the sidebar becomes a "Bộ lọc" button opening a bottom-sheet drawer; active filters
show as removable chips above the grid.

RESULTS TOOLBAR (sticky under header): active-filter chips (removable) on the left; on the right a
sort dropdown ("Sắp xếp: Mới nhất / Giá thấp → cao / Giá cao → thấp / Nổi bật") and a grid/list view toggle.

RESULTS GRID: PRODUCT CARDS (same component as Home), 3–4 cols responsive. Bottom = pagination
(prev, numbered pages, next) + page-size note.

STATES:
- Loading: skeleton card grid (8–12 placeholders).
- Empty: centered illustration + "Không tìm thấy sản phẩm phù hợp" + "Thử xóa bớt bộ lọc" button.
- Error: inline message + "Thử lại".
```

---

## 3. Product Detail
`/products/:id` · Guest/Customer · F04 · `GET /api/products/{id}`

```
Design the Cartzilla PRODUCT DETAIL page ("/products/:id"). Storefront header + footer.
Breadcrumb: Trang chủ / {Danh mục} / {Tên sản phẩm}.

TOP — two columns (desktop), stacked on mobile:
LEFT (gallery): large primary image (zoom on hover) + vertical/horizontal thumbnail strip; the image
flagged "primary" is selected by default. Support 1–6 images.
RIGHT (buy box):
- vendor/brand name (link, muted) + a small "Còn hàng"/"Hết hàng" chip.
- H1 product name. Star rating + review count (static ok).
- Price block: large ₫ price (selected variant), optional struck original + "-20%".
- VARIANT selectors:
  • "Kích cỡ": size chips (S/M/L/XL); unavailable sizes greyed/struck.
  • "Màu sắc": labeled hex swatch chips; selecting updates image + price + SKU.
- "SKU: TSN-001-M-WHT" (updates with selection) and stock line "Còn 12 sản phẩm".
- Quantity stepper (− n +), capped at stock.
- Actions: primary "Thêm vào giỏ" (full width) + secondary "Mua ngay"; a wishlist heart.
- Mini trust row: Giao nhanh · Đổi trả 7 ngày · Thanh toán COD/VNPay.

BELOW: tabbed panel "Mô tả" | "Thông số kỹ thuật" (table) | "Đánh giá".
Then "SẢN PHẨM LIÊN QUAN": product card row (4 items).

STATES: must-select-variant validation ("Vui lòng chọn kích cỡ và màu"); when chosen variant is
out of stock disable both buttons and show "Hết hàng"; loading = skeleton (image block + text lines);
not-found = "Sản phẩm không tồn tại hoặc đã ẩn" + button "Về danh sách".
```

---

## 4. Login
`/login` · Guest · F01 · `POST /api/users/login` · `GET /api/oauth/google/authorize`

```
Design the Cartzilla LOGIN page ("/login"). Split layout: LEFT 45% brand panel (fashion image,
Cartzilla logo, tagline "Thời trang cho mọi phong cách"); RIGHT 55% centered form card.

FORM "Đăng nhập":
- Email field (label "Email", placeholder "ban@email.com").
- Password field (label "Mật khẩu") with show/hide toggle.
- Row: checkbox "Ghi nhớ đăng nhập" (left) + link "Quên mật khẩu?" (right).
- Primary button full-width "Đăng nhập" (loading state while submitting).
- Divider "hoặc".
- Google button (outline, Google icon) "Đăng nhập với Google".
- Bottom text: "Chưa có tài khoản? Đăng ký" (link).

STATES:
- Field validation: "Email không hợp lệ", "Vui lòng nhập mật khẩu".
- Top form-level error alert (danger tint): "Email hoặc mật khẩu không đúng" (401).
- Special blocking alert (warning): "Tài khoản chưa xác minh email. Vui lòng kiểm tra hộp thư"
  with a "Gửi lại email xác minh" link (when login blocked by unverified email).
- Disabled-account error (danger): "Tài khoản đã bị khóa".
Mobile: single column, brand panel becomes a slim header.
```

---

## 5. Register
`/register` · Guest · F01 · `POST /api/users/register`

```
Design the Cartzilla REGISTER page ("/register"). Same split layout as login.

FORM "Tạo tài khoản":
- "Họ và tên".
- "Email".
- "Mật khẩu" with show/hide + a live password-strength meter (Yếu/Trung bình/Mạnh) and a rule hint
  "Tối thiểu 8 ký tự, gồm chữ và số".
- "Xác nhận mật khẩu".
- Checkbox "Tôi đồng ý với Điều khoản & Chính sách bảo mật" (required).
- Primary full-width "Đăng ký".
- Bottom: "Đã có tài khoản? Đăng nhập".

STATES:
- Inline errors: "Email đã tồn tại" (409), "Mật khẩu xác nhận không khớp",
  "Bạn cần đồng ý điều khoản".
- SUCCESS state (replace form): green check illustration + "Đăng ký thành công!" +
  "Chúng tôi đã gửi email xác minh tới {email}. Vui lòng kiểm tra hộp thư để kích hoạt tài khoản." +
  buttons "Mở Gmail" + "Gửi lại email".
```

---

## 6. Verify Email
`/verify-email` · Guest/Customer · F01 · `POST /api/users/verify-email`, `POST /api/users/resend-verification`

```
Design the Cartzilla EMAIL VERIFICATION result page ("/verify-email?token=..."). Centered single card
on a soft background, no storefront nav (minimal logo top).
Three variants of the same card (design all three):
1) VERIFYING: spinner + "Đang xác minh email...".
2) SUCCESS: large green check, "Xác minh thành công!", subtext "Tài khoản của bạn đã được kích hoạt.",
   primary button "Đăng nhập ngay".
3) FAILED/EXPIRED: red cross, "Liên kết không hợp lệ hoặc đã hết hạn", subtext, an email input +
   button "Gửi lại email xác minh", and a small success toast pattern "Đã gửi lại email".
```

---

## 7. OAuth Callback
`/oauth/callback` · Guest/Customer · F15 · `GET /api/oauth/google/callback`

```
Design the Cartzilla OAUTH CALLBACK page ("/oauth/callback"). Minimal centered screen (logo +
spinner) shown while exchanging the Google code for tokens: "Đang đăng nhập với Google...".
Design also the ERROR variant: "Đăng nhập Google thất bại" + reason line + button "Thử lại" +
link "Quay lại đăng nhập". Keep it very lightweight (transient screen).
```

---

## 8. Forgot Password
`/forgot-password` · Guest · UC-02 A1 · `POST /api/users/forgot-password`

```
Design the Cartzilla FORGOT PASSWORD page ("/forgot-password"). Centered card, minimal chrome.
- Title "Quên mật khẩu", subtitle "Nhập email để nhận liên kết đặt lại mật khẩu.".
- Email input. Primary button "Gửi liên kết". Link "← Quay lại đăng nhập".
- SUCCESS state (replace form): mail icon + "Đã gửi email!" + "Nếu email tồn tại, bạn sẽ nhận được
  liên kết đặt lại trong vài phút. Liên kết có hiệu lực 30 phút." + button "Mở email".
- Error/inline: "Email không hợp lệ".
```

---

## 9. Reset Password
`/reset-password` · Guest · UC-02 A1 · `POST /api/users/reset-password`

```
Design the Cartzilla RESET PASSWORD page ("/reset-password?token=..."). Centered card.
- Title "Đặt lại mật khẩu".
- "Mật khẩu mới" (show/hide + strength meter + rule hint) and "Xác nhận mật khẩu mới".
- Primary button "Đặt lại mật khẩu".
STATES:
- Invalid/expired token banner (danger): "Liên kết đặt lại đã hết hạn" + link "Yêu cầu liên kết mới".
- Mismatch error "Mật khẩu xác nhận không khớp".
- SUCCESS: green check + "Đổi mật khẩu thành công!" + button "Đăng nhập".
```

---

## 10. Profile
`/profile` · Customer · F02 · `GET/PUT /api/users/me`, password change

```
Design the Cartzilla PROFILE page ("/profile"). Storefront header + a LEFT ACCOUNT MENU
(vertical nav: Hồ sơ [active], Địa chỉ, Đơn hàng, Thông báo, Đổi mật khẩu, Đăng xuất) + RIGHT content.

RIGHT content = two cards:
CARD 1 "Thông tin cá nhân":
- Avatar with "Đổi ảnh".
- "Họ và tên" (editable).
- "Email" (read-only/disabled) with a chip next to it: "Đã xác minh" (success) or
  "Chưa xác minh" (warning) + link "Gửi lại email" when unverified.
- "Số điện thoại" (editable).
- Buttons: primary "Lưu thay đổi" + ghost "Hủy". Show success toast "Đã cập nhật hồ sơ".

CARD 2 "Đổi mật khẩu":
- "Mật khẩu hiện tại", "Mật khẩu mới" (strength meter), "Xác nhận mật khẩu mới".
- Primary "Cập nhật mật khẩu". Note line: "Đổi mật khẩu sẽ đăng xuất các thiết bị khác."
- Errors: "Mật khẩu hiện tại không đúng", "Xác nhận không khớp".
Responsive: account menu collapses to a horizontal tab bar / dropdown on mobile.
```

---

## 11. Addresses
`/addresses` · Customer · F02 · `GET/POST/PUT/DELETE /api/users/me/addresses`

```
Design the Cartzilla ADDRESSES page ("/addresses"). Same account-menu layout (Địa chỉ active).
- Header row: H2 "Địa chỉ giao hàng" + primary button "+ Thêm địa chỉ".
- GRID of address cards (2 cols). Each card: full name (bold) + phone, full address
  (street, district, city), a "Mặc định" badge on the default one, and a footer with actions:
  "Sửa", "Đặt làm mặc định" (hidden if already default), "Xóa".
- ADD/EDIT modal form: "Họ và tên", "Số điện thoại", "Địa chỉ (đường/số nhà)",
  "Quận/Huyện", "Tỉnh/Thành phố" (selects), toggle "Đặt làm địa chỉ mặc định". Buttons "Lưu" / "Hủy".
STATES & RULES:
- First address is auto-default. Setting B as default visually unsets A (only one "Mặc định").
- Deleting the default address while others exist → confirm dialog: "Hãy chọn địa chỉ mặc định khác
  trước khi xóa" (block).
- Delete confirm dialog: "Xóa địa chỉ này?".
- Empty state: "Bạn chưa có địa chỉ nào" + "Thêm địa chỉ".
```

---

## 12. Notifications
`/notifications` · Customer · F12 · `GET /api/notifications?page=&size=`, `PUT /api/notifications/{id}/read`

```
Design the Cartzilla NOTIFICATIONS page ("/notifications"). Account-menu layout (Thông báo active).
- Header: H2 "Thông báo" + unread count badge (e.g. "3 chưa đọc") + action "Đánh dấu tất cả đã đọc".
- Optional filter tabs: "Tất cả" / "Chưa đọc".
- LIST of notification rows. Each row: a type icon in a tinted circle
  (ORDER_CONFIRMED=check/success, ORDER_SHIPPED=truck/indigo, ORDER_CANCELLED=x/danger),
  title (bold), message (1–2 lines, muted), relative time right-aligned ("2 giờ trước"),
  and an unread blue dot for UNREAD items (unread rows have a faint indigo tint background).
  Clicking a row marks it read (dot disappears, bg clears) and can deep-link to the related order.
- Pagination or "Tải thêm" button at the bottom.
STATES: empty = bell illustration + "Bạn chưa có thông báo nào"; loading = 4 skeleton rows.
```

---

## 13. Cart
`/cart` · Customer · F05 · `GET/POST/PUT/DELETE /api/orders/cart/items`

```
Design the Cartzilla CART page ("/cart"). Storefront header + footer. Breadcrumb + H1 "Giỏ hàng".
Two columns (desktop), stacked on mobile.

LEFT — cart items list (card with a header row: "Sản phẩm | Đơn giá | Số lượng | Thành tiền"):
Each LINE ITEM: product thumbnail, name (link) + variant line "Size M · Trắng" + SKU (muted),
unit price ₫, a quantity stepper (− n +), line subtotal ₫, and a trash/remove icon.
Setting quantity to 0 removes the row. Footer: "Tiếp tục mua sắm" link (left) + "Xóa giỏ hàng" (right).

RIGHT — sticky "TÓM TẮT ĐƠN HÀNG" card:
- Voucher row: input "Nhập mã giảm giá" + button "Áp dụng"; on success show applied chip
  "WELCOME20 ✓ −150.000 ₫ Gỡ"; on error show "Mã không hợp lệ".
- Tạm tính (subtotal) ₫.
- Giảm giá (discount) −₫ (if any).
- Phí vận chuyển: "Miễn phí".
- Divider, then Tổng cộng (total, bold, large) ₫.
- Primary full-width "Tiến hành thanh toán".
- Small note: "Đã bao gồm VAT nếu có."

STATES: EMPTY CART = illustration + "Giỏ hàng của bạn đang trống" + "Khám phá sản phẩm" button;
quantity capped at available stock with hint "Chỉ còn 5 sản phẩm"; loading skeleton rows.
```

---

## 14. Checkout
`/checkout` · Customer · F06/F14 · `POST /api/orders/checkout`, voucher `POST /api/vouchers/validate`

```
Design the Cartzilla CHECKOUT page ("/checkout"). Storefront header (simplified, no search) + footer.
H1 "Thanh toán". Two columns: LEFT steps (2/3 width) + RIGHT sticky order summary (1/3).

LEFT — three stacked sections (numbered):
SECTION 1 "Địa chỉ giao hàng":
- Selectable address cards (radio). The default one preselected and labeled "Mặc định".
  Each card shows name, phone, full address. Link "+ Thêm địa chỉ mới" (opens modal).
- Validation if none selected: "Vui lòng chọn địa chỉ giao hàng".
SECTION 2 "Phương thức thanh toán":
- Radio cards: "COD — Thanh toán khi nhận hàng" (cash icon) and
  "VNPay — Thẻ/ATM/QR" (VNPay logo). Selected card highlighted indigo.
SECTION 3 "Mã giảm giá":
- Input + "Áp dụng". On valid: green line "Áp dụng WELCOME20: −150.000 ₫".
  On invalid show the precise error returned by rules, e.g.:
  "Voucher đã hết lượt sử dụng", "Voucher đã hết hạn",
  "Tài khoản chưa đủ tuổi tối thiểu (cần 30 ngày)", "Bạn không thuộc nhóm áp dụng voucher",
  "Chưa đạt giá trị đơn tối thiểu".
- Optional "Ghi chú đơn hàng" textarea.

RIGHT — "ĐƠN HÀNG CỦA BẠN" summary:
- Compact item list (thumbnail, name, "x2", line total ₫) — snapshot of cart.
- Tạm tính, Giảm giá, Phí vận chuyển (Miễn phí), Tổng cộng (bold) ₫.
- Primary full-width "Đặt hàng". Disabled (with reason tooltip) when no address or empty cart.
- Trust line: "Thông tin của bạn được bảo mật."

STATES: empty cart → redirect notice "Giỏ hàng trống, không thể thanh toán";
submitting → button spinner "Đang tạo đơn..."; after success → for COD go to order detail,
for VNPay go to the Payment screen.
```

---

## 15. Payment
`/checkout/payment` · Customer · F08/F13 · `POST /api/payments/vnpay/create`

```
Design the Cartzilla VNPAY PAYMENT redirect screen ("/checkout/payment"). Centered card, minimal chrome.
- Header: VNPay logo + "Thanh toán qua VNPay".
- Order summary mini: mã đơn, số tiền (bold ₫), phương thức.
- Big primary button "Thanh toán ngay" (this triggers redirect to VNPay) + a spinner state
  "Đang chuyển tới cổng VNPay...".
- Secondary "Hủy và quay lại giỏ hàng".
- A countdown note "Phiên thanh toán hết hạn sau 15:00".
STATES: error creating payment → danger alert "Không tạo được phiên thanh toán, vui lòng thử lại".
Keep it clean and reassuring (security badges).
```

---

## 16. Payment Result
`/payment/result` · Customer · F08/F13 · reads VNPay return / `GET /api/payments/{orderId}`

```
Design the Cartzilla PAYMENT RESULT screen ("/payment/result"). Centered card. Design THREE variants:
1) SUCCESS: large green check, "Thanh toán thành công", order code, amount ₫, payment time,
   transaction id; buttons primary "Xem đơn hàng" + ghost "Tiếp tục mua sắm".
2) FAILED: large red cross, "Thanh toán thất bại", reason ("Giao dịch bị hủy / Số tiền không khớp"),
   order code; buttons "Thử lại thanh toán" + "Về giỏ hàng".
3) PENDING/PROCESSING: clock icon, "Đang xác nhận thanh toán...", subtext "Vui lòng chờ trong giây lát",
   a "Tải lại" button.
Show a small order summary card (items + total) beneath the status in all variants.
```

---

## 17. Orders List
`/orders` · Customer · F09 · `GET /api/orders`

```
Design the Cartzilla CUSTOMER ORDERS list ("/orders"). Account-menu layout (Đơn hàng active).
- H2 "Đơn hàng của tôi".
- STATUS FILTER tab bar: "Tất cả | Chờ xử lý | Đã xác nhận | Đang giao | Đã giao | Đã hủy"
  (each tab shows a count).
- LIST of order cards (not a dense table — friendly cards). Each ORDER CARD:
  • Header row: "Đơn #A1B2C3" + ngày đặt + status chip (right) + payment status chip.
  • Body: a horizontal thumbnail strip of the order's items (first 3 + "+2") with names/qty.
  • Footer row: "Tổng: 1.250.000 ₫" (bold) + buttons "Xem chi tiết" and, if status=PENDING,
    "Hủy đơn".
- Pagination at bottom.
STATES: empty = box illustration + "Bạn chưa có đơn hàng nào" + "Mua sắm ngay";
loading = 3 skeleton cards.
```

---

## 18. Order Detail
`/orders/:id` · Customer · F09 · `GET /api/orders/{id}`, cancel `POST /api/orders/{id}/cancel`

```
Design the Cartzilla CUSTOMER ORDER DETAIL ("/orders/:id"). Account-menu layout.
Breadcrumb "Đơn hàng / #A1B2C3". Header: order code + status chip + ngày đặt;
right side a "Hủy đơn" button (only when status=PENDING).

1) STATUS TIMELINE (horizontal stepper): Đặt hàng → Đã xác nhận → Đang giao → Đã giao
   (completed steps in indigo with check, current pulsing, future muted). If CANCELLED show a
   red "Đã hủy" terminal state with the cancel reason.

2) Two-column content:
LEFT:
- "Sản phẩm" card: item rows (thumbnail, name, "Size M · Trắng", SKU, đơn giá ₫, số lượng,
  thành tiền ₫) — these are immutable snapshots.
- "Lịch sử trạng thái" (order status logs): timeline list of {trạng thái, thời gian, ghi chú}.
RIGHT (sticky):
- "Địa chỉ giao hàng" card (snapshot: tên, SĐT, địa chỉ đầy đủ).
- "Thanh toán" card: phương thức (COD/VNPay), trạng thái thanh toán chip, mã giao dịch nếu có.
- "Tóm tắt" card: tạm tính, giảm giá (+ mã voucher), tổng cộng (bold) ₫.

CANCEL flow: clicking "Hủy đơn" opens a modal requiring a "Lý do hủy" textarea (required) +
confirm "Xác nhận hủy". Error if empty: "Vui lòng nhập lý do hủy".
STATES: not found = "Không tìm thấy đơn hàng".
```

---

## 19. Staff Orders
`/staff/orders` · Staff/Admin · F10 · `GET /api/staff/orders?status=&paymentMethod=&fromDate=&toDate=&page=&limit=`

```
Design the Cartzilla STAFF ORDER MANAGEMENT list ("/staff/orders") in the DASHBOARD layout
(left sidebar nav: Đơn hàng [active], plus admin links if ADMIN). Top bar: title "Quản lý đơn hàng".

TOOLBAR (filter bar): search by order code; "Trạng thái" select; "Phương thức thanh toán" select
(COD/VNPay); a date-range picker "Từ ngày – Đến ngày"; a "Xóa lọc" link; result count on the right.

DATA TABLE (sticky header, sortable, row hover) columns:
Mã đơn | Khách hàng (userId short) | Ngày tạo | Phương thức | Trạng thái (chip) |
Thanh toán (chip) | Tổng tiền ₫ | Thao tác ("Xem").
Footer: pagination (page size selector "20/trang", prev/next, page numbers).

STATES: empty = "Không có đơn hàng khớp bộ lọc"; loading = shimmer table rows;
status chips use the global mapping. Optional bulk-select column for future actions.
```

---

## 20. Staff Order Detail
`/staff/orders/:id` · Staff/Admin · F10 · `GET /api/staff/orders/{id}`, `PUT /api/staff/orders/{id}/status`

```
Design the Cartzilla STAFF ORDER DETAIL ("/staff/orders/:id") in the dashboard layout.
Breadcrumb "Đơn hàng / #A1B2C3". Header: order code + current status chip + payment chip +
created time.

Three-column-ish content:
LEFT (main):
- "Sản phẩm" table (item snapshots: ảnh, tên, size/màu, SKU, đơn giá, SL, thành tiền).
- "Lịch sử trạng thái" (OrderStatusLog) vertical timeline: {trạng thái cũ→mới, người thực hiện,
  thời gian, ghi chú} — append-only.
RIGHT (sidebar cards):
- "Khách hàng": userId, (email if available).
- "Địa chỉ giao hàng" snapshot.
- "Thanh toán": phương thức + trạng thái + mã giao dịch.
- "Tóm tắt tiền": tạm tính / giảm / tổng.

ACTION PANEL "Cập nhật trạng thái" (prominent card):
- A state-machine control showing the next valid transitions only:
  PENDING → [Xác nhận] or [Hủy]; CONFIRMED → [Giao hàng] or [Hủy];
  SHIPPING → [Đã giao]; DELIVERED/CANCELLED = terminal (panel shows "Đơn đã kết thúc").
- Choosing "Hủy" reveals a required "Lý do hủy" textarea.
- Primary button "Cập nhật". Invalid transitions are not shown/disabled.
- Note for COD: "Khi chuyển 'Đã giao', thanh toán COD sẽ tự động chuyển 'Đã thanh toán'."
- Success toast "Đã cập nhật trạng thái đơn"; error "Chuyển trạng thái không hợp lệ".
```

---

## 21. Admin Products
`/admin/products` · Admin · F11 · `GET/DELETE /api/admin/products`

```
Design the Cartzilla ADMIN PRODUCTS list ("/admin/products") in dashboard layout.
Top bar: title "Sản phẩm" + primary button "+ Thêm sản phẩm".
Toolbar: search by name/SKU; filter by "Danh mục", "Thương hiệu", "Trạng thái" (Active/Ẩn);
"Xóa lọc".
DATA TABLE columns: Ảnh (thumb) | Tên sản phẩm (+ slug muted) | Danh mục | Thương hiệu |
Giá (range nếu nhiều variant) ₫ | Tồn kho (tổng) | Trạng thái (Active/Ẩn chip) |
Thao tác (Sửa, Ẩn/Hiện, Xóa-soft với confirm).
Footer: pagination.
STATES: empty "Chưa có sản phẩm" + CTA; row action "Xóa" opens confirm "Ẩn sản phẩm này khỏi cửa hàng?".
Show a "không bán được" warning icon on rows missing an active variant or image (per sellable rule).
```

---

## 22. Admin Product Form
`/admin/products/new` (+ edit `/admin/products/:id/edit`) · Admin · F11/F16 · `POST/PUT /api/admin/products`, variants & images endpoints

```
Design the Cartzilla ADMIN PRODUCT FORM ("/admin/products/new") in dashboard layout. Two columns.
Breadcrumb "Sản phẩm / Thêm mới". Sticky footer action bar: "Lưu nháp", primary "Lưu & xuất bản", "Hủy".

LEFT (main form, grouped cards):
CARD "Thông tin cơ bản": Tên sản phẩm; Slug (auto-generate from name, editable, with "trùng slug" error);
Danh mục (select — chỉ category active); Thương hiệu/Vendor (select — chỉ vendor active, optional);
Mô tả (rich text editor).
CARD "Biến thể (Variants)": an editable table, each row = SKU (uppercase, "trùng SKU" error),
Kích cỡ, Màu (text), Mã màu (hex color picker), Giá ₫, Tồn kho (≥0); a "+ Thêm biến thể" button;
delete row icon. At least one variant required to publish.
CARD "Hình ảnh": a drag-drop uploader + a thumbnail grid; each thumbnail has a radio "Ảnh chính"
(exactly one primary) and a remove button; reorder by drag. At least one image required to publish.

RIGHT (sidebar):
CARD "Trạng thái": toggle "Đang bán / Ẩn".
CARD "Xem trước": a live mini product-card preview.

VALIDATION SUMMARY: a banner listing publish blockers, e.g. "Cần ít nhất 1 biến thể",
"Cần ít nhất 1 ảnh", "Slug đã tồn tại", "SKU TSN-001 đã tồn tại".
Design both the EMPTY create state and an EDIT state (fields prefilled).
```

---

## 23. Admin Categories
`/admin/categories` · Admin · F11 · `GET/POST/PUT/DELETE /api/admin/categories`

```
Design the Cartzilla ADMIN CATEGORIES page ("/admin/categories") in dashboard layout. Two columns.
LEFT — a CATEGORY TREE (parent → children, expandable rows). Each node: name, slug (muted),
product count, status dot (Active/Ẩn), inline actions (Sửa, Thêm danh mục con, Ẩn/Hiện). A
"+ Thêm danh mục gốc" button at the top. Drag to reorder/re-parent.
RIGHT — a CREATE/EDIT form card: "Tên danh mục", "Slug" (auto, editable), "Danh mục cha" (select,
optional = gốc), toggle "Active". Buttons "Lưu" / "Hủy".
RULES & STATES:
- Deactivating a category that still has active products → block with toast
  "Không thể ẩn danh mục đang có sản phẩm đang bán".
- Duplicate slug error inline.
- Empty tree state "Chưa có danh mục".
```

---

## 24. Admin Vendors
`/admin/vendors` · Admin · F16 · `GET/POST/PUT/DELETE /api/admin/vendors`

```
Design the Cartzilla ADMIN VENDORS page ("/admin/vendors") in dashboard layout.
Top bar: title "Nhà cung cấp / Thương hiệu" + "+ Thêm vendor".
Toolbar: search; filter by "Loại" (SUPPLIER/BRAND/MANUFACTURER) and "Trạng thái".
DATA TABLE: Logo/Tên | Loại (chip: Nhà cung cấp / Thương hiệu / Nhà sản xuất) | Slug |
Số sản phẩm | Trạng thái | Thao tác (Sửa, Ẩn/Hiện, Xóa-soft).
CREATE/EDIT modal: "Tên", "Slug" (auto), "Loại" (select 3 options), "Mô tả", "Logo URL",
toggle "Active". "Lưu"/"Hủy".
RULES: inactive vendor cannot be assigned to new products (note in form). Duplicate slug error.
Empty state + pagination.
```

---

## 25. Admin Vouchers
`/admin/vouchers` · Admin · F14 · `POST/PUT/DELETE /api/admin/vouchers`, allowed-users sub-resource

```
Design the Cartzilla ADMIN VOUCHERS page ("/admin/vouchers") in dashboard layout.
Top bar: title "Mã giảm giá" + "+ Tạo voucher".
Toolbar: search by code; filter by trạng thái (Đang chạy/Hết hạn/Tạm dừng) and audience.
DATA TABLE columns: Code (uppercase, monospace) | Loại giảm (Phần trăm / Số tiền chip) |
Giá trị (20% hoặc 100.000 ₫) | Đã dùng / Giới hạn (usedCount/maxUses, with a tiny progress bar) |
Hiệu lực (validFrom – validUntil) | Đối tượng (chip) | Trạng thái | Thao tác (Sửa, Tạm dừng, Xóa).

CREATE/EDIT drawer (right side panel), grouped:
- "Mã & loại": Code (auto-uppercase, "đã tồn tại" error; locked when voucher already has usages),
  Loại giảm (radio PERCENTAGE/FIXED_AMOUNT), Giá trị (suffix % or ₫;
  rule: PERCENTAGE 0<value≤100, FIXED_AMOUNT >0).
- "Điều kiện": Giá trị đơn tối thiểu (₫), Giảm tối đa (₫, required when PERCENTAGE),
  Tuổi tài khoản tối thiểu (số ngày, ≥0).
- "Giới hạn dùng": Tổng lượt (maxUses), Mỗi người (perUserLimit).
- "Thời gian": Từ ngày / Đến ngày (datetime).
- "Đối tượng áp dụng": select audience (ALL_USERS / NEW_CUSTOMER / LOYAL_CUSTOMER / SPECIFIC_USERS);
  when SPECIFIC_USERS show an "Người dùng được phép" picker (search + chips list, add/remove).
- Footer: "Lưu" / "Hủy". Show a live summary line "Giảm 20% tối đa 150.000 ₫ cho đơn từ 500.000 ₫".
STATES: empty list; per-row usage progress; warning when editing a code that already has usages
("Không thể đổi code đã phát sinh lượt dùng").
```

---

## 26. Admin Users
`/admin/users` · Admin · F17 · `GET/PUT /api/admin/users/{id}/role`, `/status`

```
Design the Cartzilla ADMIN USERS page ("/admin/users") in dashboard layout.
Top bar: title "Người dùng". Toolbar: search by tên/email; filter by Role (CUSTOMER/STAFF/ADMIN)
and Trạng thái (Hoạt động/Đã khóa).
DATA TABLE columns: Avatar + Họ tên | Email (+ "đã xác minh"/"chưa xác minh" mini chip) |
Vai trò (role chip) | Trạng thái (Hoạt động success / Đã khóa danger) | Ngày tạo |
Thao tác: a "Vai trò" dropdown (CUSTOMER/STAFF/ADMIN) + a "Khóa/Mở khóa" toggle + "..." menu.
RULES & STATES:
- Changing role or status opens a confirm dialog ("Đổi vai trò {tên} thành STAFF?",
  "Khóa tài khoản này? Người dùng sẽ không thể đăng nhập/checkout.").
- Guard note: an admin cannot demote themselves if they are the last admin
  ("Không thể tự hạ quyền admin cuối cùng").
- Pagination + empty state.
```

---

## 27. Admin Reports
`/admin/reports` · Admin · F18 · `GET /api/admin/reports/summary|order-status|top-products`

```
Design the Cartzilla ADMIN REPORTS dashboard ("/admin/reports") in dashboard layout — analytics style.
Top bar: title "Báo cáo" + a DATE-RANGE picker ("Từ ngày" – "Đến ngày") + quick presets
("Hôm nay", "7 ngày", "30 ngày", "Tháng này") + "Áp dụng".

ROW 1 — KPI cards (4): "Tổng doanh thu" (big ₫ + small trend), "Tổng đơn hàng",
"Đơn đã giao", "Tỷ lệ hủy (%)". Each card: label, big number, small caption, an accent icon.

ROW 2 — charts (2 columns):
- "Đơn theo trạng thái" — donut chart with legend (Chờ xử lý / Đã xác nhận / Đang giao / Đã giao /
  Đã hủy) using the status colors; center shows total orders.
- "Đơn theo trạng thái thanh toán" — horizontal bar chart (Chờ / Đã thanh toán / Thất bại / Hoàn tiền).

ROW 3 — "Doanh thu theo phương thức": a compact bar/stat comparing COD vs VNPay (count + revenue ₫).

ROW 4 — "Top sản phẩm bán chạy": a ranked TABLE: # | Tên sản phẩm | SKU | Số lượng bán |
Doanh thu ₫, top 10, with a tiny bar in the quantity column.

STATES: empty-range "Không có dữ liệu trong khoảng thời gian này"; loading skeleton for cards + charts.
Clean cards, soft shadows, tabular numerals, Vietnamese labels. Export button "Xuất CSV" (top-right).
```

---

## Sau khi Stitch — nối backend

Stitch sinh **UI**; phần nối API làm trong React khi port code (Export → Figma/Code).

- **Base URL:** mọi request qua API Gateway `http://localhost:8080`, prefix `/api/**`.
- **Envelope:** response luôn là `{ success, message, data, timestamp }` → đọc `res.data.data`.
  Lỗi nghiệp vụ trả HTTP 422 với `message` tiếng Việt → hiển thị thẳng vào alert/toast.
- **Auth:** lưu access token (localStorage), gắn `Authorization: Bearer <token>`; tự refresh qua
  `POST /api/users/refresh-token` khi 401; gateway inject `X-User-Id`/`X-User-Role` xuống service.
- **Format ₫:** `new Intl.NumberFormat('vi-VN').format(value) + ' ₫'`.

| Màn hình | Endpoint chính |
|---|---|
| 1–3 Storefront | `GET /api/products?...`, `GET /api/products/{id}`, `GET /api/categories`, `GET /api/vendors` |
| 4–9 Auth | `POST /api/users/register\|login\|verify-email\|resend-verification\|refresh-token\|logout\|forgot-password\|reset-password`, `GET /api/oauth/google/authorize\|callback` |
| 10–11 Profile/Address | `GET\|PUT /api/users/me`, `PUT /api/users/me/password`, `GET\|POST\|PUT\|DELETE /api/users/me/addresses` (+ `/{id}/default`) |
| 12 Notifications | `GET /api/notifications`, `PUT /api/notifications/{id}/read` |
| 13 Cart | `GET\|POST\|PUT\|DELETE /api/orders/cart/items` |
| 14 Checkout | `POST /api/orders/checkout`, preview voucher `POST /api/vouchers/validate` |
| 15–16 Payment | `POST /api/payments/vnpay/create`, `GET /api/payments/vnpay/callback`, `GET /api/payments/{orderId}` |
| 17–18 Orders | `GET /api/orders`, `GET /api/orders/{id}`, `POST /api/orders/{id}/cancel` |
| 19–20 Staff | `GET /api/staff/orders`, `GET /api/staff/orders/{id}`, `PUT /api/staff/orders/{id}/status` |
| 21–22 Admin catalog | `/api/admin/products` (+ `/{id}/variants`, `/{id}/images`, `/{id}/images/{imageId}/primary`) |
| 23–24 Category/Vendor | `/api/admin/categories`, `/api/admin/vendors` |
| 25 Voucher | `POST/PUT/DELETE /api/admin/vouchers` (+ `/{id}/allowed-users`) |
| 26 Users | `GET /api/admin/users/{id}`, `PUT /api/admin/users/{id}/role`, `/status` |
| 27 Reports | `GET /api/admin/reports/summary\|order-status\|top-products?fromDate=&toDate=` |

**Stack gợi ý:** Vite + React Router (routes đúng SRS §7), Axios instance (interceptor gắn token +
unwrap envelope + bắt 401 refresh), TanStack Query (cache/loading/error), Tailwind (khớp Prompt 0),
React Hook Form + Zod (validation form), Recharts (biểu đồ màn 27).
```
