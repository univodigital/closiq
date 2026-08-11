"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { ROUTES } from "@/shared/constants/routes";

const REASON_LABELS: Record<string, { title: string; message: string }> = {
  PAYMENT_FAILED: {
    title: "Payment failed",
    message: "We couldn't complete your payment. No successful charge was recorded.",
  },
  PAYMENT_CANCELLED: {
    title: "Payment cancelled",
    message: "You closed the payment window before completing checkout.",
  },
  CHECKOUT_EXPIRED: {
    title: "Checkout expired",
    message: "Your checkout session expired because the payment window ended.",
  },
  AVAILABILITY_FAILED: {
    title: "Items unavailable",
    message: "Some items in your bag are no longer available for the selected dates.",
  },
  PAYMENT_PENDING: {
    title: "Payment being verified",
    message: "Your payment is being verified. Please do not retry immediately.",
  },
};

export default function CheckoutFailedPage() {
  const searchParams = useSearchParams();
  const reason = searchParams.get("reason") ?? "PAYMENT_FAILED";
  const detail = searchParams.get("detail") ?? "";
  const batchId = searchParams.get("batchId") ?? "";

  const retryQuery = new URLSearchParams();
  for (const key of ["addressId", "pincode", "couponCode"]) {
    const value = searchParams.get(key);
    if (value) retryQuery.set(key, value);
  }
  const retryHref = retryQuery.toString()
    ? `${ROUTES.checkout.payment}?${retryQuery.toString()}`
    : ROUTES.checkout.payment;

  const copy = REASON_LABELS[reason] ?? REASON_LABELS.PAYMENT_FAILED;

  return (
    <div className="mx-auto max-w-lg px-4 py-16 text-center">
      <h1 className="font-heading text-3xl">{copy.title}</h1>
      <p className="mt-4 text-muted-foreground">{copy.message}</p>
      {detail && <p className="mt-2 text-sm text-muted-foreground">Reason: {detail}</p>}
      {batchId && (
        <p className="mt-2 text-xs text-muted-foreground">Reference: {batchId}</p>
      )}
      <p className="mt-4 text-sm text-muted-foreground">Your bag is still saved.</p>
      <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:justify-center">
        <Button asChild variant="rent">
          <Link href={retryHref}>Try again</Link>
        </Button>
        <Button asChild variant="outline">
          <Link href={ROUTES.checkout.bag}>Back to bag</Link>
        </Button>
      </div>
      <p className="mt-8 text-sm">
        <Link href={ROUTES.support} className="underline text-muted-foreground">
          Contact support
        </Link>
      </p>
    </div>
  );
}
