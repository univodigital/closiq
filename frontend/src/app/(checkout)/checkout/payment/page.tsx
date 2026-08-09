"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/providers/AuthProvider";
import { useBag } from "@/providers/BagProvider";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { ROUTES } from "@/shared/constants/routes";
import { toast } from "sonner";
import { cn } from "@/lib/utils";
import { ApiError } from "@/lib/api-client";
import { useCheckoutParams } from "@/features/checkout/hooks/useCheckoutParams";
import { CheckoutLayoutShell, CheckoutTwoColumn } from "@/features/checkout/components/CheckoutLayoutShell";
import { PriceDetails } from "@/features/checkout/components/PriceDetails";
import { calculateBagPricing, loadBagLines } from "@/features/checkout/utils/bag-pricing";
import { earliestRentalStartDate, validateRentalDates } from "@/features/checkout/utils/rental-dates";
import { isCompleteBagItem } from "@/features/checkout/bag/bag-store";
import { loadRazorpayScript, openRazorpayCheckout } from "@/features/checkout/lib/razorpay";
import {
  createRazorpayOrder,
  prepareCheckoutBookings,
  verifyRazorpayPayment,
} from "@/features/checkout/services/payment.service";

const PAYMENT_METHODS = [
  { id: "razorpay", label: "Recommended", detail: "UPI, cards & net banking via Razorpay" },
] as const;

export default function CheckoutPaymentPage() {
  const router = useRouter();
  const { user, isAuthenticated } = useAuth();
  const { items: bagItems } = useBag();
  const { addressId, pincode, fullQuery, couponCode } = useCheckoutParams();
  const [method, setMethod] = useState<(typeof PAYMENT_METHODS)[number]["id"]>("razorpay");
  const [processing, setProcessing] = useState(false);

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
      toast.error("Sign in to complete checkout.");
      router.push(`${ROUTES.login}?returnUrl=${encodeURIComponent(window.location.pathname + window.location.search)}`);
      return;
    }

    if (!addressId) {
      toast.error("Select a delivery address before paying.");
      return;
    }

    for (const item of completeItems) {
      const dateError = validateRentalDates(item.start, item.end);
      if (dateError) {
        toast.error(dateError);
        return;
      }
      if (item.start < earliestRentalStartDate()) {
        toast.error(`Update bag dates — delivery must be on or after ${earliestRentalStartDate()}.`);
        router.push(ROUTES.checkout.bag);
        return;
      }
    }

    setProcessing(true);
    try {
      const scriptLoaded = await loadRazorpayScript();
      if (!scriptLoaded) {
        toast.error("Could not load Razorpay checkout. Check your connection and try again.");
        return;
      }

      const lines = await loadBagLines(completeItems);
      if (!lines.length) {
        toast.error("Could not load bag items for checkout.");
        return;
      }

      const bookings = await prepareCheckoutBookings(lines, addressId, couponCode || undefined);
      let lastOrderNumber = "";

      for (let i = 0; i < bookings.length; i++) {
        const booking = bookings[i];
        const order = await createRazorpayOrder({
          bookingId: booking.bookingId,
          checkoutSessionId: booking.checkoutSessionId,
        });

        const keyId =
          order.keyId || process.env.NEXT_PUBLIC_RAZORPAY_KEY_ID || "";
        if (!keyId) {
          toast.error("Razorpay key is not configured.");
          return;
        }

        const itemLabel =
          bookings.length > 1 ? `Rental payment (${i + 1} of ${bookings.length})` : "Rental payment";

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
          theme: { color: "#1a1a1a" },
        });

        const verified = await verifyRazorpayPayment({
          paymentId: order.paymentId,
          razorpayOrderId: paymentResponse.razorpay_order_id,
          razorpayPaymentId: paymentResponse.razorpay_payment_id,
          razorpaySignature: paymentResponse.razorpay_signature,
        });

        lastOrderNumber = verified.orderNumber || verified.rentalNumber;
      }

      toast.success("Payment successful");
      router.push(
        `${ROUTES.checkout.success}?order=${encodeURIComponent(lastOrderNumber)}`,
      );
    } catch (error) {
      if (error instanceof ApiError) {
        if (error.status === 403 || error.status === 401) {
          toast.error("Session expired. Sign in again to continue checkout.");
        } else if (error.status === 409) {
          toast.error(error.message || "Selected dates are no longer available. Update your bag and try again.");
        } else {
          toast.error(error.message);
        }
      } else if (error instanceof Error && error.message === "Payment cancelled") {
        toast.message("Payment cancelled");
      } else if (error instanceof Error) {
        toast.error(error.message || "Payment failed. Please try again.");
      } else {
        toast.error("Payment failed. Please try again.");
      }
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
            : "Select a delivery address before payment."}
        </p>
        <Button asChild variant="outline" className="mt-4">
          <Link href={!completeItems.length ? ROUTES.checkout.bag : `${ROUTES.checkout.address}?${fullQuery()}`}>
            {!completeItems.length ? "Back to bag" : "Back to address"}
          </Link>
        </Button>
      </CheckoutLayoutShell>
    );
  }

  return (
    <CheckoutLayoutShell step="payment" queryString={fullQuery()}>
      <CheckoutTwoColumn
        main={
          <div className="space-y-6">
            <h1 className="font-heading text-3xl">Payment</h1>

            <p className="text-sm text-muted-foreground">
              Paying for {completeItems.length}{" "}
              {completeItems.length === 1 ? "rental" : "rentals"} in your bag.
            </p>

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
                  Razorpay checkout opens for UPI, credit/debit cards, and net banking.
                </p>
                {process.env.NODE_ENV === "development" && (
                  <div className="rounded-sm border border-border bg-muted/30 p-3 text-xs text-muted-foreground">
                    <p className="font-medium text-foreground">Razorpay sandbox — use these if cards fail</p>
                    <p className="mt-2">
                      The Visa test card is sometimes flagged as international on test accounts. If you see
                      &quot;International cards are not supported&quot;, use one of these instead:
                    </p>
                    <ul className="mt-2 list-inside list-disc space-y-1">
                      <li>
                        <strong>Netbanking (most reliable):</strong> pick any bank → click{" "}
                        <strong>Success</strong> on the mock page
                      </li>
                      <li>
                        <strong>UPI:</strong> <code className="text-foreground">success@razorpay</code>
                      </li>
                      <li>
                        <strong>Mastercard (domestic):</strong> 5267 3181 8797 5449 · CVV 123 · 12/26
                      </li>
                      <li>
                        <strong>Visa (domestic):</strong> 4111 1111 1111 1111 · CVV 123 · 12/26 — then click{" "}
                        <strong>Success</strong> on the mock page if shown
                      </li>
                    </ul>
                  </div>
                )}
                <Button variant="gold" size="lg" onClick={handlePay} disabled={processing || !addressId}>
                  {processing ? "Processing…" : "Pay now"}
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
