"use client";

import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { formatCurrency, formatDate } from "@/lib/format";
import { orderService } from "@/features/orders/services";
import type { Order } from "@/shared/types";
import { toast } from "sonner";

const RETURN_STEPS = [
  { key: "scheduled", label: "Pickup scheduled", match: ["CREATED"] },
  { key: "assigned", label: "Agent assigned", match: ["CREATED"], needsAgent: true },
  { key: "enroute", label: "Agent on the way", match: ["PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY"] },
  { key: "picked", label: "Item picked up", match: ["DELIVERED", "RETURNED_TO_SELLER"] },
];

function stepState(
  stepIndex: number,
  currentIndex: number,
): "completed" | "current" | "pending" {
  if (stepIndex < currentIndex) return "completed";
  if (stepIndex === currentIndex) return "current";
  return "pending";
}

function resolveCurrentStep(status: string, agentName?: string | null): number {
  if (status === "DELIVERED" || status === "RETURNED_TO_SELLER") return 3;
  if (status === "PICKED_UP" || status === "IN_TRANSIT" || status === "OUT_FOR_DELIVERY") return 2;
  if (agentName) return 1;
  return 0;
}

export function ReturnPickupPanel({ order, orderId }: { order: Order; orderId: string }) {
  const qc = useQueryClient();
  const [scheduling, setScheduling] = useState(false);
  const [showTrack, setShowTrack] = useState(false);

  const canSchedule = order.status === "rental_active";
  const pickup = order.returnPickup;

  const trackQuery = useQuery({
    queryKey: ["return-track", orderId],
    queryFn: () => orderService.trackReturnPickup(orderId),
    enabled: showTrack && !!pickup,
  });

  const handleSchedule = async () => {
    setScheduling(true);
    try {
      const res = await orderService.scheduleReturn(orderId);
      toast.success(
        res.data.alreadyScheduled ? "Return pickup already scheduled" : "Return pickup scheduled",
      );
      qc.invalidateQueries({ queryKey: ["order", orderId] });
    } catch {
      toast.error("Could not schedule return pickup");
    } finally {
      setScheduling(false);
    }
  };

  if (canSchedule && !pickup) {
    return (
      <div className="mt-8 rounded-sm border border-border p-6">
        <p className="label-caps text-muted-foreground">Return</p>
        <h2 className="mt-2 font-heading text-xl">Ready to return?</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          We&apos;ll assign the next available pickup slot based on your area and rental dates.
        </p>
        <Button variant="primary" className="mt-6" disabled={scheduling} onClick={handleSchedule}>
          Schedule return
        </Button>
      </div>
    );
  }

  if (!pickup) {
    return null;
  }

  const track = trackQuery.data?.data;
  const currentStep = resolveCurrentStep(track?.status ?? pickup.status, track?.agentName ?? pickup.agentName);

  return (
    <div className="mt-8 space-y-6">
      <div className="rounded-sm border border-border p-6">
        <p className="label-caps text-muted-foreground">Return pickup</p>
        <h2 className="mt-2 font-heading text-xl">
          {pickup.returnReference ? `Return ${pickup.returnReference}` : "Return scheduled"}
        </h2>
        <dl className="mt-4 space-y-2 text-sm">
          <div className="flex justify-between">
            <dt>Pickup date</dt>
            <dd>{formatDate(pickup.pickupDate)}</dd>
          </div>
          {pickup.pickupWindow && (
            <div className="flex justify-between">
              <dt>Pickup window</dt>
              <dd>{pickup.pickupWindow}</dd>
            </div>
          )}
        </dl>
        <Button variant="outline" className="mt-6" onClick={() => setShowTrack((v) => !v)}>
          {showTrack ? "Hide tracking" : "Track pickup"}
        </Button>
      </div>

      {showTrack && (
        <div className="rounded-sm border border-border p-6">
          <h3 className="label-caps text-muted-foreground">Pickup status</h3>
          <ol className="mt-6 space-y-4">
            {RETURN_STEPS.map((step, index) => {
              const state = stepState(index, currentStep);
              return (
                <li key={step.key} className="flex items-start gap-3 text-sm">
                  <span
                    className={
                      state === "completed"
                        ? "text-success"
                        : state === "current"
                          ? "text-accent"
                          : "text-muted-foreground"
                    }
                  >
                    {state === "completed" ? "✓" : state === "current" ? "●" : "○"}
                  </span>
                  <span className={state === "pending" ? "text-muted-foreground" : undefined}>{step.label}</span>
                </li>
              );
            })}
          </ol>
          {(track?.agentName ?? pickup.agentName) && (
            <p className="mt-4 text-xs text-muted-foreground">
              Agent: {track?.agentName ?? pickup.agentName}
            </p>
          )}
        </div>
      )}

      {order.depositSummary && (order.status === "returned" || order.status === "inspection") && (
        <div className="rounded-sm border border-border p-6">
          <h3 className="label-caps text-muted-foreground">Return status</h3>
          <ul className="mt-4 space-y-2 text-sm">
            <li>✓ Pickup completed</li>
            <li>✓ Item received</li>
            <li className="text-accent">● Inspection in progress</li>
            <li className="text-muted-foreground">○ Deposit refund</li>
          </ul>
        </div>
      )}

      {order.depositSummary &&
        (order.depositSummary.inspectionStatus === "COMPLETED" ||
          order.status === "deposit_refunded") && (
          <div className="rounded-sm border border-border p-6">
            <h3 className="label-caps text-muted-foreground">Deposit refund</h3>
            <dl className="mt-4 space-y-2 text-sm">
              <div className="flex justify-between">
                <dt>Deposit</dt>
                <dd>{formatCurrency(order.depositSummary.depositAmount)}</dd>
              </div>
              {(order.depositSummary.totalDeduction ?? 0) > 0 && (
                <>
                  {order.depositSummary.damageDeduction > 0 && (
                    <div className="flex justify-between text-muted-foreground">
                      <dt>Damage</dt>
                      <dd>-{formatCurrency(order.depositSummary.damageDeduction)}</dd>
                    </div>
                  )}
                  {order.depositSummary.lateFee > 0 && (
                    <div className="flex justify-between text-muted-foreground">
                      <dt>Late return</dt>
                      <dd>-{formatCurrency(order.depositSummary.lateFee)}</dd>
                    </div>
                  )}
                  {(order.depositSummary.cleaningFee ?? 0) > 0 && (
                    <div className="flex justify-between text-muted-foreground">
                      <dt>Cleaning</dt>
                      <dd>-{formatCurrency(order.depositSummary.cleaningFee ?? 0)}</dd>
                    </div>
                  )}
                </>
              )}
              {order.depositSummary.deductionReason && (
                <p className="text-xs text-muted-foreground">{order.depositSummary.deductionReason}</p>
              )}
              <div className="flex justify-between border-t border-border pt-2 font-medium">
                <dt>Refund</dt>
                <dd>{formatCurrency(order.depositSummary.refundAmount)}</dd>
              </div>
              {order.depositSummary.refundStatus && (
                <div className="flex justify-between text-muted-foreground">
                  <dt>Status</dt>
                  <dd>{order.depositSummary.refundStatus}</dd>
                </div>
              )}
              {order.depositSummary.expectedRefundWindow && (
                <p className="text-xs text-muted-foreground">
                  Expected: {order.depositSummary.expectedRefundWindow} · original payment method
                </p>
              )}
            </dl>
          </div>
        )}
    </div>
  );
}
