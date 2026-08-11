"use client";

import { use, useState } from "react";
import Image from "next/image";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { orderService } from "@/features/orders/services";
import { TrialPanel } from "@/features/orders/components/TrialPanel";
import { ReturnPickupPanel } from "@/features/orders/components/ReturnPickupPanel";
import type { CancelPreview } from "@/features/orders/services/order.service";
import { Container, PageHeader } from "@/shared/components/layout/Container";
import { StatusBadge } from "@/components/ui/badge";
import { OrderTimeline } from "@/shared/components/display/OrderTimeline";
import { Button } from "@/components/ui/button";
import { formatCurrency, formatDateRange } from "@/lib/format";
import { toast } from "sonner";

export default function OrderDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const qc = useQueryClient();
  const [cancelOpen, setCancelOpen] = useState(false);
  const [cancelPreview, setCancelPreview] = useState<CancelPreview | null>(null);
  const [cancelLoading, setCancelLoading] = useState(false);

  const { data, isLoading } = useQuery({
    queryKey: ["order", id],
    queryFn: () => orderService.getOrder(id),
    refetchInterval: (query) => {
      const order = query.state.data?.data;
      if (order?.status === "trial_ready" && order.trialSession?.active) {
        return 30_000;
      }
      return false;
    },
  });

  const order = data?.data;

  const openCancel = async () => {
    try {
      const preview = await orderService.getCancelPreview(id);
      setCancelPreview(preview.data);
      setCancelOpen(true);
    } catch {
      toast.error("Could not load cancellation details");
    }
  };

  const confirmCancel = async () => {
    setCancelLoading(true);
    try {
      await orderService.cancelOrder(id, "CHANGE_OF_PLANS");
      toast.success("Order cancelled");
      setCancelOpen(false);
      qc.invalidateQueries({ queryKey: ["order", id] });
    } catch {
      toast.error("Cancellation failed");
    } finally {
      setCancelLoading(false);
    }
  };

  const handleInvoice = async () => {
    try {
      await orderService.downloadInvoice(id);
    } catch {
      toast.error("Could not download invoice");
    }
  };

  if (isLoading || !order) {
    return (
      <Container narrow embedded>
        <PageHeader title="Order" />
        <p className="text-muted-foreground">Loading…</p>
      </Container>
    );
  }

  const payment = order.paymentSummary;
  const showTrialRefund =
    order.refundDetails &&
    (order.status === "trial_rejected" || order.status === "refund_pending");

  return (
    <Container narrow embedded>
      <PageHeader title={order.productTitle} breadcrumb={`Order ${order.orderNumber}`} />
      <div className="relative mb-6 aspect-video overflow-hidden rounded-sm bg-muted">
        <Image src={order.productImage} alt="" fill className="object-cover" sizes="768px" />
      </div>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <StatusBadge status={order.status} />
        <p className="font-mono text-sm">{formatCurrency(order.totalPaid)} paid</p>
      </div>
      <p className="text-sm text-muted-foreground">
        {formatDateRange(order.rentalStart, order.rentalEnd)} · Size {order.variantSize}
      </p>

      <TrialPanel order={order} orderId={id} />
      <ReturnPickupPanel order={order} orderId={id} />

      <section className="mt-10">
        <h2 className="label-caps mb-6 text-muted-foreground">Timeline</h2>
        <OrderTimeline events={order.timeline} />
      </section>

      <section className="mt-10 rounded-sm border border-border p-6">
        <h2 className="label-caps mb-4 text-muted-foreground">Payment</h2>
        <dl className="space-y-2 text-sm">
          <div className="flex justify-between">
            <dt>Rent</dt>
            <dd>{formatCurrency(payment?.rentalAmount ?? order.rentalAmount)}</dd>
          </div>
          <div className="flex justify-between">
            <dt>Deposit</dt>
            <dd>{formatCurrency(payment?.depositAmount ?? order.depositAmount)}</dd>
          </div>
          {(payment?.deliveryFee ?? order.deliveryFee) > 0 && (
            <div className="flex justify-between">
              <dt>Delivery</dt>
              <dd>{formatCurrency(payment?.deliveryFee ?? order.deliveryFee)}</dd>
            </div>
          )}
          <div className="flex justify-between border-t border-border pt-2 font-medium">
            <dt>Total</dt>
            <dd>{formatCurrency(payment?.totalPaid ?? order.totalPaid)}</dd>
          </div>
          <div className="flex justify-between text-muted-foreground">
            <dt>Payment status</dt>
            <dd>{payment?.status ?? order.paymentStatus ?? "—"}</dd>
          </div>
        </dl>
      </section>

      {order.depositSummary && (
        <section className="mt-6 rounded-sm border border-border p-6">
          <h2 className="label-caps mb-4 text-muted-foreground">Security deposit</h2>
          <dl className="space-y-2 text-sm">
            <div className="flex justify-between">
              <dt>Deposit</dt>
              <dd>{formatCurrency(order.depositSummary.depositAmount)}</dd>
            </div>
            {order.depositSummary.inspectionStatus && (
              <div className="flex justify-between">
                <dt>Inspection</dt>
                <dd className="capitalize">{order.depositSummary.inspectionStatus.toLowerCase()}</dd>
              </div>
            )}
            {order.depositSummary.damageDeduction > 0 && (
              <div className="flex justify-between">
                <dt>Damage deduction</dt>
                <dd>{formatCurrency(order.depositSummary.damageDeduction)}</dd>
              </div>
            )}
            {order.depositSummary.refundAmount > 0 && (
              <div className="flex justify-between">
                <dt>Refund amount</dt>
                <dd>{formatCurrency(order.depositSummary.refundAmount)}</dd>
              </div>
            )}
            {order.depositSummary.refundStatus && (
              <div className="flex justify-between text-muted-foreground">
                <dt>Refund status</dt>
                <dd>{order.depositSummary.refundStatus}</dd>
              </div>
            )}
            {order.depositSummary.expectedRefundWindow && (
              <p className="pt-2 text-xs text-muted-foreground">
                Expected refund: {order.depositSummary.expectedRefundWindow}
              </p>
            )}
          </dl>
        </section>
      )}

      {showTrialRefund && (
        <section className="mt-6 rounded-sm border border-border p-6">
          <h2 className="label-caps mb-4 text-muted-foreground">Rental refund</h2>
          <dl className="space-y-2 text-sm">
            <div className="flex justify-between">
              <dt>Refund</dt>
              <dd>{formatCurrency(order.refundDetails!.refundAmount)}</dd>
            </div>
            <div className="flex justify-between text-muted-foreground">
              <dt>Status</dt>
              <dd>{order.refundDetails!.status}</dd>
            </div>
            {order.refundDetails!.expectedBusinessDays != null && (
              <p className="text-xs text-muted-foreground">
                Expected in {order.refundDetails!.expectedBusinessDays} business days ·{" "}
                {order.refundDetails!.refundMethod?.replace(/_/g, " ").toLowerCase()}
              </p>
            )}
          </dl>
        </section>
      )}

      {order.refundDetails && order.status === "cancelled" && (
        <section className="mt-6 rounded-sm border border-border p-6">
          <h2 className="label-caps mb-4 text-muted-foreground">Refund</h2>
          <dl className="space-y-2 text-sm">
            <div className="flex justify-between">
              <dt>Refund</dt>
              <dd>{formatCurrency(order.refundDetails.refundAmount)}</dd>
            </div>
            <div className="flex justify-between text-muted-foreground">
              <dt>Status</dt>
              <dd>{order.refundDetails.status}</dd>
            </div>
            {order.refundDetails.expectedBusinessDays != null && (
              <p className="text-xs text-muted-foreground">
                Expected in {order.refundDetails.expectedBusinessDays} business days ·{" "}
                {order.refundDetails.refundMethod?.replace(/_/g, " ").toLowerCase()}
              </p>
            )}
          </dl>
        </section>
      )}

      <div className="mt-10 flex flex-wrap gap-3">
        {order.invoiceAvailable && (
          <Button variant="outline" onClick={handleInvoice}>
            Download invoice
          </Button>
        )}
        {order.cancellation?.eligible && (
          <Button variant="destructive" onClick={openCancel}>
            Cancel order
          </Button>
        )}
      </div>

      {cancelOpen && cancelPreview && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/40 p-4 sm:items-center">
          <div className="w-full max-w-md rounded-sm border border-border bg-card p-6 shadow-md">
            <h3 className="font-heading text-xl">Cancel order?</h3>
            <p className="mt-2 text-sm text-muted-foreground">{cancelPreview.policyLabel}</p>
            <dl className="mt-4 space-y-2 text-sm">
              <div className="flex justify-between">
                <dt>Original payment</dt>
                <dd>{formatCurrency(cancelPreview.originalAmount)}</dd>
              </div>
              <div className="flex justify-between font-medium text-success">
                <dt>Refund</dt>
                <dd>{formatCurrency(cancelPreview.refundAmount)}</dd>
              </div>
              {cancelPreview.nonRefundableAmount > 0 && (
                <>
                  <div className="flex justify-between">
                    <dt>Non-refundable</dt>
                    <dd>{formatCurrency(cancelPreview.nonRefundableAmount)}</dd>
                  </div>
                  {cancelPreview.nonRefundableReason && (
                    <p className="text-xs text-muted-foreground">{cancelPreview.nonRefundableReason}</p>
                  )}
                </>
              )}
              {cancelPreview.depositRefundAmount > 0 && (
                <div className="flex justify-between text-muted-foreground">
                  <dt>Deposit refund</dt>
                  <dd>{formatCurrency(cancelPreview.depositRefundAmount)}</dd>
                </div>
              )}
              <p className="text-xs text-muted-foreground">
                Refund method: original payment method · Expected{" "}
                {cancelPreview.expectedRefundBusinessDays} business days
              </p>
            </dl>
            <div className="mt-6 flex gap-3">
              <Button variant="outline" className="flex-1" onClick={() => setCancelOpen(false)}>
                Keep order
              </Button>
              <Button
                variant="destructive"
                className="flex-1"
                disabled={cancelLoading}
                onClick={confirmCancel}
              >
                Confirm cancellation
              </Button>
            </div>
          </div>
        </div>
      )}
    </Container>
  );
}
