"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/providers/AuthProvider";
import { useBag } from "@/providers/BagProvider";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { formatCurrency } from "@/lib/format";
import { ROUTES } from "@/shared/constants/routes";
import { DESIGN_TOKENS } from "@/shared/constants/design-tokens";
import { cn } from "@/lib/utils";
import { ApiError } from "@/lib/api-client";
import { useCheckoutParams } from "@/features/checkout/hooks/useCheckoutParams";
import { CheckoutLayoutShell, CheckoutTwoColumn } from "@/features/checkout/components/CheckoutLayoutShell";
import { PriceDetails } from "@/features/checkout/components/PriceDetails";
import { HoldTimer } from "@/features/checkout/components/HoldTimer";
import { calculateBagPricing, loadBagLines } from "@/features/checkout/utils/bag-pricing";
import { earliestRentalStartDate, validateRentalDates } from "@/features/checkout/utils/rental-dates";
import {
  unavailableItems,
  validateBagLinesAvailability,
} from "@/features/checkout/utils/bag-availability";
import { isCompleteBagItem } from "@/features/checkout/bag/bag-store";
import { loadRazorpayScript, openRazorpayCheckout } from "@/features/checkout/lib/razorpay";
import {
  createBatchRazorpayOrder,
  prepareCheckoutBatch,
  verifyRazorpayPayment,
} from "@/features/checkout/services/payment.service";
import { resetCheckoutAttemptId } from "@/features/checkout/lib/checkout-idempotency";

const PAYMENT_METHODS = [
  { id: "razorpay", label: "Recommended", detail: "UPI, cards & net banking via Razorpay" },
] as const;

function buildFailedUrl(
  base: typeof ROUTES.checkout.failed,
  params: {
    reason: string;
    detail?: string;
    batchId?: string;
    addressId?: string;
    pincode?: string;
    couponCode?: string;
  },
) {
  const qs = new URLSearchParams();
  qs.set("reason", params.reason);
  if (params.detail) qs.set("detail", params.detail);
  if (params.batchId) qs.set("batchId", params.batchId);
  if (params.addressId) qs.set("addressId", params.addressId);
  if (params.pincode) qs.set("pincode", params.pincode);
  if (params.couponCode) qs.set("couponCode", params.couponCode);
  return `${base}?${qs.toString()}`;
}

export default function CheckoutPaymentPage() {
  const router = useRouter();
  const { user, isAuthenticated } = useAuth();
  const { items: bagItems } = useBag();
  const { addressId, pincode, fullQuery, couponCode } = useCheckoutParams();
  const [method, setMethod] = useState<(typeof PAYMENT_METHODS)[number]["id"]>("razorpay");
  const [processing, setProcessing] = useState(false);
  const [holdExpiresAt, setHoldExpiresAt] = useState<string | null>(null);
  const [holdExpired, setHoldExpired] = useState(false);

  const completeItems = bagItems.filter(isCompleteBagItem);
  const bagKey = JSON.stringify(completeItems);

  const pricing = useQuery({
    queryKey: ["bag-pricing", bagKey, pincode, couponCode],
    queryFn: async () => {
      const lines = await loadBagLines(completeItems);
      return calculateBagPricing(lines, {
        pincode: pincode || undefined,
        couponCode: couponCode || undefined,
      });
    },
    enabled: completeItems.length > 0 && !!pincode,
  });

  const handlePay = async () => {
    if (!isAuthenticated) {
      router.push(
        `${ROUTES.login}?returnUrl=${encodeURIComponent(window.location.pathname + window.location.search)}`,
      );
      return;
    }

    if (!addressId) {
      router.push(`${ROUTES.checkout.address}?${fullQuery()}`);
      return;
    }

    for (const item of completeItems) {
      const dateError = validateRentalDates(item.start, item.end);
      if (dateError) {
        router.push(
          buildFailedUrl(ROUTES.checkout.failed, {
            reason: "AVAILABILITY_FAILED",
            detail: dateError,
            addressId,
            pincode,
            couponCode,
          }),
        );
        return;
      }
      if (item.start < earliestRentalStartDate()) {
        router.push(ROUTES.checkout.bag);
        return;
      }
    }

    if (holdExpired) {
      resetCheckoutAttemptId();
      router.push(
        buildFailedUrl(ROUTES.checkout.failed, {
          reason: "CHECKOUT_EXPIRED",
          addressId,
          pincode,
          couponCode,
        }),
      );
      return;
    }

    setProcessing(true);
    let batchId: string | undefined;

    try {
      const scriptLoaded = await loadRazorpayScript();
      if (!scriptLoaded) {
        router.push(
          buildFailedUrl(ROUTES.checkout.failed, {
            reason: "PAYMENT_FAILED",
            detail: "Could not load Razorpay checkout.",
            addressId,
            pincode,
            couponCode,
          }),
        );
        return;
      }

      const lines = await loadBagLines(completeItems);
      if (!lines.length) {
        router.push(ROUTES.checkout.bag);
        return;
      }

      const availability = await validateBagLinesAvailability(lines);
      const blocked = unavailableItems(availability);
      if (blocked.length > 0) {
        router.push(
          buildFailedUrl(ROUTES.checkout.failed, {
            reason: "AVAILABILITY_FAILED",
            detail: blocked.map((b) => b.productTitle).join(", "),
            addressId,
            pincode,
            couponCode,
          }),
        );
        return;
      }

      const batch = await prepareCheckoutBatch(lines, addressId, couponCode || undefined);
      batchId = batch.checkoutBatchId;
      setHoldExpiresAt(batch.holdExpiresAt);

      if (new Date(batch.holdExpiresAt).getTime() <= Date.now()) {
        router.push(
          buildFailedUrl(ROUTES.checkout.failed, {
            reason: "CHECKOUT_EXPIRED",
            batchId,
            addressId,
            pincode,
            couponCode,
          }),
        );
        return;
      }

      const order = await createBatchRazorpayOrder(batch.checkoutBatchId);

      const keyId = order.keyId || process.env.NEXT_PUBLIC_RAZORPAY_KEY_ID || "";
      if (!keyId) {
        router.push(
          buildFailedUrl(ROUTES.checkout.failed, {
            reason: "PAYMENT_FAILED",
            detail: "Razorpay is not configured.",
            batchId,
            addressId,
            pincode,
            couponCode,
          }),
        );
        return;
      }

      const itemLabel =
        (order.itemCount ?? lines.length) > 1
          ? `Rental payment (${order.itemCount ?? lines.length} items)`
          : "Rental payment";

      const paymentResponse = await openRazorpayCheckout({
        key: keyId,
        name: "Closiq",
        description: itemLabel,
        order_id: order.razorpayOrderId,
        prefill: {
          name: user?.displayName,
          email: user?.email,
          contact: user?.phone,
        },
        theme: { color: DESIGN_TOKENS.navy },
      });

      const verified = await verifyRazorpayPayment({
        paymentId: order.paymentId,
        razorpayOrderId: paymentResponse.razorpay_order_id,
        razorpayPaymentId: paymentResponse.razorpay_payment_id,
        razorpaySignature: paymentResponse.razorpay_signature,
      });

      resetCheckoutAttemptId();
      router.push(
        `${ROUTES.checkout.success}?order=${encodeURIComponent(verified.orderNumber || verified.rentalNumber)}`,
      );
    } catch (error) {
      if (error instanceof ApiError) {
        if (error.status === 403 || error.status === 401) {
          router.push(
            `${ROUTES.login}?returnUrl=${encodeURIComponent(window.location.pathname + window.location.search)}`,
          );
          return;
        }
        if (error.status === 409) {
          router.push(
            buildFailedUrl(ROUTES.checkout.failed, {
              reason: error.message.toLowerCase().includes("expired")
                ? "CHECKOUT_EXPIRED"
                : "AVAILABILITY_FAILED",
              detail: error.message,
              batchId,
              addressId,
              pincode,
              couponCode,
            }),
          );
          return;
        }
        router.push(
          buildFailedUrl(ROUTES.checkout.failed, {
            reason: "PAYMENT_FAILED",
            detail: error.message,
            batchId,
            addressId,
            pincode,
            couponCode,
          }),
        );
        return;
      }
      if (error instanceof Error && error.message === "Payment cancelled") {
        router.push(
          buildFailedUrl(ROUTES.checkout.failed, {
            reason: "PAYMENT_CANCELLED",
            batchId,
            addressId,
            pincode,
            couponCode,
          }),
        );
        return;
      }
      router.push(
        buildFailedUrl(ROUTES.checkout.failed, {
          reason: "PAYMENT_FAILED",
          detail: error instanceof Error ? error.message : undefined,
          batchId,
          addressId,
          pincode,
          couponCode,
        }),
      );
    } finally {
      setProcessing(false);
    }
  };

  if (!completeItems.length || !pincode) {
    return (
      <CheckoutLayoutShell step="payment" queryString={fullQuery()}>
        <p className="text-muted-foreground">
          {!completeItems.length
            ? "Your bag is empty. Select rental items before payment."
            : "Select a delivery address or pincode before payment."}
        </p>
        <Button asChild variant="outline" className="mt-4">
          <Link
            href={
              !completeItems.length
                ? ROUTES.checkout.bag
                : `${ROUTES.checkout.address}?${fullQuery()}`
            }
          >
            {!completeItems.length ? "Back to bag" : "Back to address"}
          </Link>
        </Button>
      </CheckoutLayoutShell>
    );
  }

  const payLabel =
    pricing.data?.payNowAmount != null
      ? `Pay ${formatCurrency(pricing.data.payNowAmount)}`
      : "Pay now";

  return (
    <CheckoutLayoutShell step="payment" queryString={fullQuery()}>
      <CheckoutTwoColumn
        main={
          <div className="space-y-6">
            <h1 className="font-heading text-3xl">Payment</h1>

            <p className="text-sm text-muted-foreground">
              Paying for {completeItems.length}{" "}
              {completeItems.length === 1 ? "rental" : "rentals"} in one checkout.
            </p>

            {holdExpiresAt && (
              <HoldTimer
                expiresAt={holdExpiresAt}
                onExpired={() => setHoldExpired(true)}
              />
            )}

            <div>
              <p className="label-caps text-muted-foreground">Choose payment mode</p>
              <div className="mt-3 space-y-2">
                {PAYMENT_METHODS.map((m) => (
                  <label
                    key={m.id}
                    className={cn(
                      "flex cursor-pointer items-start gap-3 rounded-sm border p-4",
                      method === m.id ? "border-accent bg-muted/20" : "border-border",
                    )}
                  >
                    <input
                      type="radio"
                      name="payment-method"
                      checked={method === m.id}
                      onChange={() => setMethod(m.id)}
                      className="mt-1"
                    />
                    <span>
                      <span className="block font-medium">{m.label}</span>
                      <span className="mt-0.5 block text-sm text-muted-foreground">{m.detail}</span>
                    </span>
                  </label>
                ))}
              </div>
            </div>

            <Card>
              <CardContent className="space-y-4 p-6">
                <p className="label-caps text-muted-foreground">Payment details</p>
                <p className="text-sm text-muted-foreground">
                  One Razorpay checkout for all bag items. Your card or UPI is charged once for the combined total.
                </p>
                {!isAuthenticated && (
                  <p className="text-sm text-muted-foreground">
                    Sign in when you continue — your bag and checkout details are preserved.
                  </p>
                )}
                <Button
                  variant="rent"
                  size="lg"
                  onClick={handlePay}
                  disabled={processing || holdExpired || (!isAuthenticated ? false : !addressId)}
                >
                  {processing ? "Processing…" : isAuthenticated ? payLabel : "Sign in to pay"}
                </Button>
              </CardContent>
            </Card>
          </div>
        }
        sidebar={
          <PriceDetails pricing={pricing.data} title="Price details" />
        }
      />
    </CheckoutLayoutShell>
  );
}
