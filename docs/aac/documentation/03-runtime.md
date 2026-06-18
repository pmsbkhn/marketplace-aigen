# Runtime — luồng quan trọng (Dynamic views)

## Đặt hàng (happy path)

Checkout điều phối **đồng bộ** (REST) theo thứ tự price → reserve → split → order → escrow,
rồi gateway trả payment URL.

![Checkout Saga](embed:CheckoutSaga)

## Bù trừ saga (compensation)

Nếu một bước lỗi, Checkout chạy compensation **ngược thứ tự** (huỷ order mới nhất trước,
nhả stock). Init-escrow không có API huỷ → thiết kế đặt escrow ở bước cuối.

![Checkout Compensation](embed:CheckoutCompensation)

## Sau thanh toán (choreography qua Kafka)

Webhook PAID → PaymentReceived lan toả: Order chuyển TO_SHIP, Notification báo merchant;
khi đơn COMPLETED → OrderCompleted kích hoạt đối soát/payout và trừ kho.

![Post-payment Flow](embed:PostPaymentFlow)

> Hợp đồng wire của mọi event: [`/contracts`](../../../contracts).
