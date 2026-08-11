"use client";

import { useState } from "react";
import Link from "next/link";
import { useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { formatCurrency, formatDateRange } from "@/lib/format";
import { ROUTES } from "@/shared/constants/routes";
import type { Order, TrialRejectPreview } from "@/shared/types";
import { bookingService, orderService } from "@/features/orders/services";
import { TrialCountdown, isTrialExpired } from "./TrialCountdown";
import { toast } from "sonner";

type TrialPanelProps = {
  order: Order;
  orderId: string;
};

export function TrialPanel({ order, orderId }: TrialPanelProps) {
  const qc = useQueryClient();
  const [rejectOpen, setRejectOpen] = useState(false);
  const [rejectPreview, setRejectPreview] = useState<TrialRejectPreview | null>(null);
  const [rejectLoading, setRejectLoading] = useState(false);
  const [acceptLoading, setAcceptLoading] = useState(false);
  const [accepted, setAccepted] = useState(order.status === "rental_active");

  const trial = order.trialSession;
  const expired = trial
    ? isTrialExpired(trial.expiresAt, trial.expired) || trial.outcome === "EXPIRED"
    : false;
  const canDecide =
    order.status === "trial_ready" &&
    trial?.active &&
    !expired &&
    trial.outcome === "PENDING";

  const openReject = async () => {
    try {
      const preview = await orderService.getTrialRejectPreview(orderId);
      setRejectPreview(preview.data);
      setRejectOpen(true);
    } catch {
      toast.error("Could not load rejection details");
    }
  };

  const confirmReject = async () => {
    setRejectLoading(true);
    try {
      await bookingService.rejectTrial(orderId, "CHANGED_MIND");
      toast.success("Return initiated");
      setRejectOpen(false);
      qc.invalidateQueries({ queryKey: ["order", orderId] });
    } catch {
      toast.error("Could not reject outfit");
    } finally {
      setRejectLoading(false);
    }
  };

  const handleAccept = async () => {
    setAcceptLoading(true);
    try {
      await bookingService.acceptTrial(orderId);
      setAccepted(true);
      qc.invalidateQueries({ queryKey: ["order", orderId] });
    } catch {
      toast.error("Could not confirm rental");
    } finally {
      setAcceptLoading(false);
    }
  };

  if (accepted || order.status === "rental_active") {
    return (
      <div className="mt-8 rounded-sm border border-success/30 bg-success/5 p-6 text-center">
        <p className="label-caps text-success">Rental confirmed</p>
        <h2 className="mt-2 font-heading text-2xl">Your outfit has been accepted</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          Rental: {formatDateRange(order.rentalStart, order.rentalEnd)}
        </p>
        <dl className="mx-auto mt-4 max-w-xs space-y-1 text-sm">
          <div className="flex justify-between">
            <dt>Order</dt>
            <dd className="font-mono">{order.orderNumber}</dd>
          </div>
          <div className="flex justify-between">
            <dt>Rental amount</dt>
            <dd>{formatCurrency(order.rentalAmount)}</dd>
          </div>
          <div className="flex justify-between">
            <dt>Deposit</dt>
            <dd>{formatCurrency(order.depositAmount)}</dd>
          </div>
        </dl>
        <p className="mt-4 text-sm text-muted-foreground">Your rental is now active.</p>
        <Button asChild variant="outline" className="mt-6">
          <Link href={ROUTES.order(order.id)}>View order</Link>
        </Button>
      </div>
    );
  }

  if (order.status === "trial_rejected" || order.status === "refund_pending") {
    return (
      <div className="mt-8 rounded-sm border border-border p-6">
        <p className="label-caps text-muted-foreground">Return pickup</p>
        <h2 className="mt-2 font-heading text-xl">Return initiated</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          Your outfit will be picked up. Deposit refund follows return and inspection.
        </p>
        {order.refundDetails && (
          <dl className="mt-4 space-y-2 text-sm">
            <div className="flex justify-between">
              <dt>Rental refund</dt>
              <dd>{formatCurrency(order.refundDetails.refundAmount)}</dd>
            </div>
            <div className="flex justify-between text-muted-foreground">
              <dt>Status</dt>
              <dd>{order.refundDetails.status}</dd>
            </div>
          </dl>
        )}
        {order.depositSummary && (
          <p className="mt-3 text-xs text-muted-foreground">
            Deposit ({formatCurrency(order.depositSummary.depositAmount)}):{" "}
            {order.depositSummary.expectedRefundWindow ?? "refunded after inspection"}
          </p>
        )}
      </div>
    );
  }

  if (order.status !== "trial_ready" || !trial) {
    return null;
  }

  if (expired || trial.outcome === "EXPIRED") {
    return (
      <div className="mt-8 rounded-sm border border-border bg-muted/30 p-6">
        <p className="label-caps text-muted-foreground">Trial expired</p>
        <h2 className="mt-2 font-heading text-xl">Your trial window has expired</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          Accept and reject are no longer available. Please contact support if you need help.
        </p>
        <Button asChild variant="outline" className="mt-6">
          <Link href={ROUTES.support}>Contact support</Link>
        </Button>
      </div>
    );
  }

  return (
    <>
      <div className="mt-8 rounded-sm border border-accent/30 bg-gold-light p-6">
        <p className="label-caps text-accent">Agent has arrived</p>
        <h2 className="mt-2 font-heading text-xl">Your outfit is ready to try</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          Please try the outfit and decide whether to accept it.
        </p>
        <p className="mt-6 text-xs uppercase tracking-wider text-muted-foreground">
          Accept within {order.trialDurationMinutes} minutes
        </p>
        <div className="mt-2">
          <TrialCountdown expiresAt={trial.expiresAt} />
        </div>
        <div className="mt-6 flex flex-wrap gap-3">
          <Button variant="primary" disabled={!canDecide || acceptLoading} onClick={handleAccept}>
            Accept rental
          </Button>
          <Button variant="destructive" disabled={!canDecide} onClick={openReject}>
            Reject
          </Button>
        </div>
      </div>

      {rejectOpen && rejectPreview && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/40 p-4 sm:items-center">
          <div className="w-full max-w-md rounded-sm border border-border bg-card p-6 shadow-md">
            <h3 className="font-heading text-xl">Reject outfit?</h3>
            <p className="mt-2 text-sm text-muted-foreground">{rejectPreview.policyLabel}</p>
            <dl className="mt-4 space-y-2 text-sm">
              <div className="flex justify-between">
                <dt>Rental refund</dt>
                <dd>{formatCurrency(rejectPreview.rentalRefundAmount)}</dd>
              </div>
              <div className="flex justify-between">
                <dt>Deposit</dt>
                <dd>{formatCurrency(rejectPreview.depositAmount)}</dd>
              </div>
              <div className="flex justify-between text-muted-foreground">
                <dt>Deposit refund</dt>
                <dd>{rejectPreview.depositRefundTiming}</dd>
              </div>
              <p className="text-xs text-muted-foreground">
                Rental refund expected in {rejectPreview.rentalRefundExpectedBusinessDays} business days ·{" "}
                {rejectPreview.refundMethod.replace(/_/g, " ").toLowerCase()}
              </p>
            </dl>
            <div className="mt-6 flex gap-3">
              <Button variant="outline" className="flex-1" onClick={() => setRejectOpen(false)}>
                Keep outfit
              </Button>
              <Button
                variant="destructive"
                className="flex-1"
                disabled={rejectLoading}
                onClick={confirmReject}
              >
                Confirm rejection
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
