"use client";

import Image from "next/image";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { toast } from "sonner";
import { AcceptCountdown } from "@/features/seller/components/AcceptCountdown";
import {
  acceptSellerBooking,
  markSellerBookingReadyForPickup,
  rejectSellerBooking,
} from "@/features/seller/services/seller-booking-management.service";
import { sellerService } from "@/features/seller/services";
import { fetchUserAddresses } from "@/features/user/services/api-user.service";
import { PageHeader } from "@/shared/components/layout/Container";
import { StatusBadge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { ApiError } from "@/lib/api-client";
import { formatCurrency, formatDateRange } from "@/lib/format";
import { ROUTES } from "@/shared/constants/routes";

const PICKUP_SLOTS = ["10:00-14:00", "14:00-18:00", "18:00-21:00"];

function defaultPrepBy(rentalStart: string): string {
  const start = new Date(rentalStart);
  start.setDate(start.getDate() - 1);
  start.setHours(12, 0, 0, 0);
  if (start.getTime() < Date.now()) {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    tomorrow.setHours(12, 0, 0, 0);
    return tomorrow.toISOString();
  }
  return start.toISOString();
}

export default function SellerBookingDetailPage() {
  const params = useParams<{ id: string }>();
  const bookingId = params.id;
  const queryClient = useQueryClient();

  const [accepting, setAccepting] = useState(false);
  const [rejecting, setRejecting] = useState(false);
  const [markingReady, setMarkingReady] = useState(false);
  const [showRejectForm, setShowRejectForm] = useState(false);
  const [rejectReason, setRejectReason] = useState("");
  const [rejectComment, setRejectComment] = useState("");
  const [pickupAddressId, setPickupAddressId] = useState("");
  const [pickupTimeSlot, setPickupTimeSlot] = useState(PICKUP_SLOTS[0]);
  const [handoffNotes, setHandoffNotes] = useState("");

  const { data, isLoading, error } = useQuery({
    queryKey: ["seller", "bookings", bookingId],
    queryFn: () => sellerService.getBooking(bookingId),
    enabled: !!bookingId,
    refetchInterval: (query) => {
      const booking = query.state.data?.data;
      if (booking?.canAccept && booking.acceptDeadlineAt) return 30_000;
      return false;
    },
  });

  const { data: addresses = [] } = useQuery({
    queryKey: ["user", "addresses"],
    queryFn: fetchUserAddresses,
    enabled: !!data?.data?.canMarkReady,
  });

  const booking = data?.data;

  const selectedReason = useMemo(
    () => booking?.rejectReasons.find((r) => r.code === rejectReason),
    [booking?.rejectReasons, rejectReason],
  );

  async function refreshBooking() {
    await queryClient.invalidateQueries({ queryKey: ["seller", "bookings", bookingId] });
    await queryClient.invalidateQueries({ queryKey: ["seller", "bookings"] });
  }

  async function handleAccept() {
    if (!booking) return;
    setAccepting(true);
    try {
      await acceptSellerBooking(booking.id, {
        estimatedPrepBy: defaultPrepBy(booking.rentalStart),
      });
      toast.success("Order accepted");
      await refreshBooking();
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Could not accept order");
    } finally {
      setAccepting(false);
    }
  }

  async function handleReject() {
    if (!booking || !rejectReason) {
      toast.error("Select a rejection reason");
      return;
    }
    if (selectedReason?.requiresComment && !rejectComment.trim()) {
      toast.error("Please provide details for this reason");
      return;
    }

    setRejecting(true);
    try {
      await rejectSellerBooking(booking.id, {
        reason: rejectReason,
        comment: rejectComment.trim() || undefined,
      });
      toast.success("Order rejected — customer refund initiated");
      setShowRejectForm(false);
      await refreshBooking();
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Could not reject order");
    } finally {
      setRejecting(false);
    }
  }

  async function handleMarkReady() {
    if (!booking) return;
    if (!pickupAddressId) {
      toast.error("Select a pickup address");
      return;
    }

    setMarkingReady(true);
    try {
      await markSellerBookingReadyForPickup(booking.id, {
        pickupAddressId,
        pickupTimeSlot,
        handoffNotes: handoffNotes.trim() || undefined,
      });
      toast.success("Pickup scheduled");
      await refreshBooking();
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Could not schedule pickup");
    } finally {
      setMarkingReady(false);
    }
  }

  if (isLoading) {
    return <p className="text-muted-foreground">Loading…</p>;
  }

  if (error || !booking) {
    return (
      <div>
        <PageHeader title="Booking" />
        <p className="text-destructive">Could not load booking.</p>
        <Link href={ROUTES.seller.bookings} className="mt-4 inline-block text-sm underline">
          Back to bookings
        </Link>
      </div>
    );
  }

  const isPendingAcceptance = booking.canAccept || booking.canReject;

  return (
    <div className="space-y-8">
      <div>
        <Link href={ROUTES.seller.bookings} className="text-sm text-muted-foreground hover:underline">
          ← Bookings
        </Link>
        <PageHeader
          title={isPendingAcceptance ? "New rental request" : `Order ${booking.rentalNumber}`}
          description={booking.orderNumber ? `#${booking.orderNumber}` : undefined}
        />
      </div>

      <div className="flex flex-col gap-6 lg:flex-row">
        <div className="relative h-48 w-full shrink-0 overflow-hidden rounded-sm bg-muted lg:h-64 lg:w-48">
          {booking.productImage ? (
            <Image src={booking.productImage} alt="" fill className="object-cover" sizes="192px" />
          ) : null}
        </div>

        <div className="min-w-0 flex-1 space-y-4">
          <div className="flex flex-wrap items-center gap-3">
            <h2 className="text-xl font-medium">{booking.productTitle}</h2>
            <StatusBadge status={booking.status} />
          </div>

          <dl className="grid gap-2 text-sm sm:grid-cols-2">
            <div>
              <dt className="text-muted-foreground">Customer</dt>
              <dd>{booking.customer.name ?? "Verified customer"}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Size</dt>
              <dd>{booking.variantSize || "—"}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Rental dates</dt>
              <dd>{formatDateRange(booking.rentalStart, booking.rentalEnd)}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Delivery</dt>
              <dd>
                {booking.customer.deliveryCity ?? "—"}
                {booking.customer.deliveryPincode ? ` · ${booking.customer.deliveryPincode}` : ""}
              </dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Rental amount</dt>
              <dd className="font-mono">{formatCurrency(booking.earnings.rentalAmount)}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Your earning</dt>
              <dd className="font-mono text-success">
                {formatCurrency(booking.earnings.netEarnings)}
                {booking.earnings.creditedToWallet ? " · credited" : " · after completion"}
              </dd>
            </div>
          </dl>

          {isPendingAcceptance && booking.acceptDeadlineAt ? (
            <Card className="border-warning/40 bg-warning/5">
              <CardContent className="space-y-4 p-5">
                <div>
                  <p className="label-caps text-muted-foreground">Accept within</p>
                  <p className="mt-1 font-mono text-2xl">
                    <AcceptCountdown
                      deadlineAt={booking.acceptDeadlineAt}
                      expired={booking.acceptanceExpired}
                    />
                  </p>
                  <p className="mt-1 text-xs text-muted-foreground">
                    {booking.acceptSlaHours}-hour acceptance window
                  </p>
                </div>
                {!booking.acceptanceExpired ? (
                  <div className="flex flex-wrap gap-3">
                    <Button onClick={handleAccept} disabled={accepting || !booking.canAccept}>
                      {accepting ? "Accepting…" : "Accept"}
                    </Button>
                    <Button
                      variant="outline"
                      onClick={() => setShowRejectForm(true)}
                      disabled={!booking.canReject}
                    >
                      Reject
                    </Button>
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground">
                    Acceptance window expired. This order can no longer be accepted or rejected.
                  </p>
                )}
              </CardContent>
            </Card>
          ) : null}

          {showRejectForm && booking.rejectPreview ? (
            <Card>
              <CardContent className="space-y-4 p-5">
                <h3 className="font-medium">Reject order</h3>
                <div className="space-y-2">
                  <label htmlFor="reject-reason" className="text-sm text-muted-foreground">
                    Reason
                  </label>
                  <select
                    id="reject-reason"
                    className="w-full rounded-sm border border-border bg-background px-3 py-2 text-sm"
                    value={rejectReason}
                    onChange={(e) => setRejectReason(e.target.value)}
                  >
                    <option value="">Select reason</option>
                    {booking.rejectReasons.map((reason) => (
                      <option key={reason.code} value={reason.code}>
                        {reason.label}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="space-y-2">
                  <label htmlFor="reject-comment" className="text-sm text-muted-foreground">
                    {selectedReason?.requiresComment ? "Details (required)" : "Additional details (optional)"}
                  </label>
                  <Input
                    id="reject-comment"
                    value={rejectComment}
                    onChange={(e) => setRejectComment(e.target.value)}
                    placeholder="Tell the customer what happened"
                  />
                </div>
                <div className="rounded-sm border border-border bg-muted/30 p-3 text-sm">
                  <p>
                    Refund to customer:{" "}
                    <span className="font-mono">{formatCurrency(booking.rejectPreview.refundAmount)}</span>
                  </p>
                  <p className="text-muted-foreground">
                    Expected within {booking.rejectPreview.expectedBusinessDays} business days via{" "}
                    {booking.rejectPreview.refundMethod.toLowerCase()}
                  </p>
                </div>
                <div className="flex flex-wrap gap-3">
                  <Button
                    variant="destructive"
                    onClick={handleReject}
                    disabled={rejecting || !rejectReason}
                  >
                    {rejecting ? "Rejecting…" : "Confirm rejection"}
                  </Button>
                  <Button variant="outline" onClick={() => setShowRejectForm(false)} disabled={rejecting}>
                    Cancel
                  </Button>
                </div>
              </CardContent>
            </Card>
          ) : null}

          {booking.canMarkReady || booking.prepChecklist.length > 0 ? (
            <Card>
              <CardContent className="space-y-4 p-5">
                <h3 className="font-medium">Prepare this order</h3>
                <ul className="space-y-2">
                  {booking.prepChecklist.map((item) => (
                    <li key={item.item} className="flex items-center gap-2 text-sm">
                      <span aria-hidden>{item.done ? "✓" : "□"}</span>
                      <span className={item.done ? "text-muted-foreground line-through" : undefined}>
                        {item.item}
                      </span>
                    </li>
                  ))}
                </ul>

                {booking.canMarkReady ? (
                  <div className="space-y-3 border-t border-border pt-4">
                    <p className="text-sm text-muted-foreground">Schedule courier pickup</p>
                    <div className="space-y-2">
                      <label htmlFor="pickup-address" className="text-sm text-muted-foreground">
                        Pickup address
                      </label>
                      <select
                        id="pickup-address"
                        className="w-full rounded-sm border border-border bg-background px-3 py-2 text-sm"
                        value={pickupAddressId}
                        onChange={(e) => setPickupAddressId(e.target.value)}
                      >
                        <option value="">Select address</option>
                        {addresses.map((address) => (
                          <option key={address.id} value={address.id}>
                            {address.label} — {address.line1}, {address.city}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="space-y-2">
                      <label htmlFor="pickup-slot" className="text-sm text-muted-foreground">
                        Pickup window
                      </label>
                      <select
                        id="pickup-slot"
                        className="w-full rounded-sm border border-border bg-background px-3 py-2 text-sm"
                        value={pickupTimeSlot}
                        onChange={(e) => setPickupTimeSlot(e.target.value)}
                      >
                        {PICKUP_SLOTS.map((slot) => (
                          <option key={slot} value={slot}>
                            {slot}
                          </option>
                        ))}
                      </select>
                    </div>
                    <Input
                      value={handoffNotes}
                      onChange={(e) => setHandoffNotes(e.target.value)}
                      placeholder="Handoff notes (optional)"
                    />
                    <Button onClick={handleMarkReady} disabled={markingReady}>
                      {markingReady ? "Scheduling…" : "Mark ready for pickup"}
                    </Button>
                  </div>
                ) : null}
              </CardContent>
            </Card>
          ) : null}

          {booking.customerNotes ? (
            <Card>
              <CardContent className="p-5">
                <h3 className="label-caps text-muted-foreground">Customer notes</h3>
                <p className="mt-2 text-sm">{booking.customerNotes}</p>
              </CardContent>
            </Card>
          ) : null}
        </div>
      </div>
    </div>
  );
}
