import { apiFetch } from "@/lib/api-client";
import type { BagLine } from "@/features/checkout/utils/bag-pricing";

export interface CreateBookingResult {
  bookingId: string;
  rentalNumber: string;
  checkoutSessionId: string;
}

export interface RazorpayOrderResult {
  paymentId: string;
  razorpayOrderId: string;
  amount: number;
  amountInRupees: number;
  currency: string;
  keyId: string;
  bookingId: string;
}

export interface VerifyPaymentResult {
  paymentId: string;
  rentalNumber: string;
  orderNumber: string;
  bookingStatus: string;
}

function idempotencyKey(prefix: string) {
  return `${prefix}-${crypto.randomUUID()}`;
}

export async function createBookingHold(
  line: BagLine,
  idempotencyPrefix: string,
): Promise<CreateBookingResult> {
  const raw = await apiFetch<{
    bookingId: string;
    rentalNumber?: string;
    bookingNumber?: string;
    checkoutSessionId: string;
  }>("/bookings", {
    method: "POST",
    headers: { "Idempotency-Key": idempotencyKey(idempotencyPrefix) },
    body: JSON.stringify({
      productId: line.product.id,
      variantId: line.variantId,
      rentalStartDate: line.item.start,
      rentalEndDate: line.item.end,
    }),
  });

  return {
    bookingId: raw.bookingId,
    rentalNumber: raw.rentalNumber ?? raw.bookingNumber ?? raw.bookingId,
    checkoutSessionId: raw.checkoutSessionId,
  };
}

export async function initiateCheckoutSession(input: {
  bookingId: string;
  deliveryAddressId: string;
  couponCode?: string;
}): Promise<{ sessionId: string; bookingId: string }> {
  const raw = await apiFetch<{ sessionId: string; bookingId: string }>("/checkout/sessions", {
    method: "POST",
    headers: { "Idempotency-Key": idempotencyKey("checkout-session") },
    body: JSON.stringify(input),
  });
  return raw;
}

export async function createRazorpayOrder(input: {
  bookingId: string;
  checkoutSessionId: string;
}): Promise<RazorpayOrderResult> {
  return apiFetch<RazorpayOrderResult>("/payments/razorpay/orders", {
    method: "POST",
    headers: { "Idempotency-Key": idempotencyKey("razorpay-order") },
    body: JSON.stringify(input),
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
    headers: { "Idempotency-Key": idempotencyKey("razorpay-verify") },
    body: JSON.stringify(input),
  });
}

export async function prepareCheckoutBookings(
  lines: BagLine[],
  deliveryAddressId: string,
  couponCode?: string,
): Promise<CreateBookingResult[]> {
  const prepared: CreateBookingResult[] = [];

  for (let i = 0; i < lines.length; i++) {
    const booking = await createBookingHold(lines[i], `booking-${i}`);
    await initiateCheckoutSession({
      bookingId: booking.bookingId,
      deliveryAddressId,
      couponCode: i === 0 ? couponCode : undefined,
    });
    prepared.push(booking);
  }

  return prepared;
}
