"use client";

import { useEffect } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { ROUTES } from "@/shared/constants/routes";
import { useBag } from "@/providers/BagProvider";
import { orderService } from "@/features/orders/services";
import {
  PaymentReceipt,
  paymentSummaryFromOrder,
} from "@/features/checkout/components/PaymentReceipt";

export default function CheckoutSuccessPage() {
  const { clear } = useBag();
  const searchParams = useSearchParams();
  const orderRef = searchParams.get("order") ?? "";

  useEffect(() => {
    clear();
  }, [clear]);

  const orderQuery = useQuery({
    queryKey: ["checkout-success-order", orderRef],
    queryFn: () => orderService.getOrder(orderRef),
    enabled: !!orderRef,
  });

  const order = orderQuery.data?.data;
  const payment = order ? paymentSummaryFromOrder(order) : null;

  return (
    <div className="py-12">
      <div className="mx-auto max-w-lg space-y-6 text-center">
        {order && payment ? (
          <>
            <PaymentReceipt
              orderNumber={order.orderNumber}
              productTitle={order.productTitle}
              payment={payment}
            />
            <p className="text-sm text-muted-foreground">
              Your order has been confirmed. We&apos;ll notify you in the app when your piece is out for delivery.
            </p>
            <div className="flex flex-col gap-3 sm:flex-row sm:justify-center">
              <Button asChild variant="primary">
                <Link href={ROUTES.order(order.id)}>View order</Link>
              </Button>
              <Button asChild variant="outline">
                <Link href={ROUTES.home}>Continue shopping</Link>
              </Button>
            </div>
          </>
        ) : (
          <>
            <p className="label-caps text-success">Booking confirmed</p>
            <h1 className="font-heading text-3xl">You&apos;re all set</h1>
            <p className="text-sm text-muted-foreground">
              {orderRef
                ? "Loading your receipt…"
                : "Your rental is confirmed. Track it from your orders."}
            </p>
            <div className="flex flex-col gap-3 sm:flex-row sm:justify-center">
              <Button asChild variant="primary">
                <Link href={orderRef ? ROUTES.order(orderRef) : ROUTES.orders}>Track order</Link>
              </Button>
              <Button asChild variant="outline">
                <Link href={ROUTES.home}>Continue shopping</Link>
              </Button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
