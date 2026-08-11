"use client";

import { formatCurrency } from "@/lib/format";
import type { Order, OrderPaymentSummary } from "@/shared/types";

export function PaymentReceipt({
  orderNumber,
  productTitle,
  payment,
}: {
  orderNumber: string;
  productTitle?: string;
  payment: OrderPaymentSummary;
}) {
  const rentalCharges = payment.rentalAmount + payment.deliveryFee;
  const isPaid = payment.status === "CAPTURED" || payment.status === "PAID";

  return (
    <div className="rounded-sm border border-border bg-card p-6 text-left">
      <p className="label-caps text-success">{isPaid ? "Payment successful" : "Payment summary"}</p>
      <h2 className="mt-2 font-heading text-2xl">Order #{orderNumber}</h2>
      {productTitle && <p className="mt-1 text-sm text-muted-foreground">{productTitle}</p>}

      <div className="mt-6 space-y-2 text-sm">
        <div className="flex justify-between">
          <span className="text-muted-foreground">Rental charges</span>
          <span>{formatCurrency(rentalCharges)}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-muted-foreground">Security deposit</span>
          <span>{formatCurrency(payment.depositAmount)}</span>
        </div>
        {payment.discountAmount > 0 && (
          <div className="flex justify-between text-success">
            <span>Discount</span>
            <span>-{formatCurrency(payment.discountAmount)}</span>
          </div>
        )}
        <div className="flex justify-between border-t border-border pt-3 font-medium">
          <span>Total paid</span>
          <span>{formatCurrency(payment.totalPaid)}</span>
        </div>
      </div>

      <div className="mt-6 space-y-1 text-sm text-muted-foreground">
        <p>
          Deposit: <span className="text-foreground">{formatCurrency(payment.depositAmount)}</span>
        </p>
        <p>
          Rental charges: <span className="text-foreground">{formatCurrency(rentalCharges)}</span>
        </p>
        {payment.method && (
          <p>
            Payment method: <span className="text-foreground">{payment.method}</span>
          </p>
        )}
        <p>
          Payment status:{" "}
          <span className="text-foreground">{isPaid ? "PAID" : payment.status}</span>
        </p>
      </div>
    </div>
  );
}

export function paymentSummaryFromOrder(order: Order): OrderPaymentSummary | null {
  if (order.paymentSummary) return order.paymentSummary;
  return {
    status: order.paymentPending ? "PENDING" : "PAID",
    rentalAmount: order.rentalAmount,
    depositAmount: order.depositAmount,
    deliveryFee: order.deliveryFee,
    discountAmount: order.discountAmount ?? 0,
    totalPaid: order.totalPaid,
    paymentPending: order.paymentPending ?? false,
    checkoutBatchId: order.checkoutBatchId ?? null,
  };
}
