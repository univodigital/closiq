import { apiFetch } from "@/lib/api-client";
import { checkoutIdempotencyKey } from "@/features/checkout/lib/checkout-idempotency";
import type { BagLine } from "@/features/checkout/utils/bag-pricing";

export interface PrepareCheckoutBatchResult {
  checkoutBatchId: string;
  totalAmount: number;
  discountAmount: number;
  currency: string;
  holdExpiresAt: string;
  bookings: Array<{
    bookingId: string;
    rentalNumber: string;
    checkoutSessionId: string;
    totalAmount: number;
  }>;
}

export interface RazorpayOrderResult {
  paymentId: string;
  razorpayOrderId: string;
  amount: number;
  amountInRupees: number;
  currency: string;
  keyId: string;
  bookingId: string;
  checkoutBatchId?: string;
  itemCount?: number;
  expiresAt?: string;
  stubEnabled?: boolean;
}

export interface VerifyPaymentResult {
  paymentId: string;
  rentalNumber: string;
  orderNumber: string;
  bookingStatus: string;
  status?: string;
  rentalAmount?: number;
  depositAmount?: number;
  deliveryFee?: number;
  discountAmount?: number;
  paymentMethod?: string;
  paidAmount?: number;
}

export async function prepareCheckoutBatch(
  lines: BagLine[],
  deliveryAddressId: string,
  couponCode?: string,
): Promise<PrepareCheckoutBatchResult> {
  return apiFetch<PrepareCheckoutBatchResult>("/checkout/batch/prepare", {
    method: "POST",
    headers: { "Idempotency-Key": checkoutIdempotencyKey("batch-prepare") },
    body: JSON.stringify({
      deliveryAddressId,
      couponCode: couponCode || undefined,
      items: lines.map((line) => ({
        productId: line.product.id,
        variantId: line.variantId,
        rentalStartDate: line.item.start,
        rentalEndDate: line.item.end,
      })),
    }),
  });
}

export async function createBatchRazorpayOrder(
  checkoutBatchId: string,
): Promise<RazorpayOrderResult> {
  return apiFetch<RazorpayOrderResult>("/payments/razorpay/orders/batch", {
    method: "POST",
    headers: { "Idempotency-Key": checkoutIdempotencyKey("razorpay-order") },
    body: JSON.stringify({ checkoutBatchId }),
  });
}

export async function verifyRazorpayPayment(input: {
  paymentId: string;
  razorpayOrderId: string;
  razorpayPaymentId: string;
  razorpaySignature: string;
}): Promise<VerifyPaymentResult> {
  return apiFetch<VerifyPaymentResult>("/payments/razorpay/verify", {
    method: "POST",
    headers: { "Idempotency-Key": checkoutIdempotencyKey("razorpay-verify") },
    body: JSON.stringify(input),
  });
}

export async function completeStubPayment(paymentId: string): Promise<VerifyPaymentResult> {
  return apiFetch<VerifyPaymentResult>("/payments/razorpay/stub/complete", {
    method: "POST",
    headers: { "Idempotency-Key": checkoutIdempotencyKey("stub-complete") },
    body: JSON.stringify({ paymentId }),
  });
}
