import { apiFetch, apiFetchEnvelope } from "@/lib/api-client";
import { mapSellerBookingDetail } from "../lib/seller-mappers";
import type { SellerBookingDetail } from "../types";

export async function fetchSellerBookingDetail(id: string): Promise<SellerBookingDetail> {
  const res = await apiFetchEnvelope<unknown>(`/seller/bookings/${encodeURIComponent(id)}`);
  return mapSellerBookingDetail(res.data as Parameters<typeof mapSellerBookingDetail>[0]);
}

export async function fetchSellerRejectPreview(bookingId: string) {
  const res = await apiFetchEnvelope<{
    refundAmount: number;
    expectedBusinessDays: number;
    refundMethod: string;
    currency: string;
  }>(`/seller/bookings/${encodeURIComponent(bookingId)}/reject-preview`);
  return res.data;
}

export async function acceptSellerBooking(
  bookingId: string,
  input: { estimatedPrepBy: string; notes?: string },
) {
  return apiFetchEnvelope<{ status: string }>(
    `/seller/bookings/${encodeURIComponent(bookingId)}/accept`,
    {
      method: "POST",
      body: JSON.stringify({
        estimatedPrepBy: input.estimatedPrepBy,
        notes: input.notes || undefined,
      }),
    },
  );
}

export async function rejectSellerBooking(
  bookingId: string,
  input: { reason: string; comment?: string },
) {
  return apiFetchEnvelope<{ status: string }>(
    `/seller/bookings/${encodeURIComponent(bookingId)}/reject`,
    {
      method: "POST",
      body: JSON.stringify({
        reason: input.reason,
        comment: input.comment || undefined,
      }),
    },
  );
}

export async function markSellerBookingReadyForPickup(
  bookingId: string,
  input: {
    pickupAddressId: string;
    pickupTimeSlot: string;
    handoffNotes?: string;
  },
) {
  return apiFetchEnvelope<{ status: string; shipmentId: string; pickupScheduledAt: string }>(
    `/seller/bookings/${encodeURIComponent(bookingId)}/ready-for-pickup`,
    {
      method: "POST",
      body: JSON.stringify(input),
    },
  );
}

export async function requestSellerPayout(input: {
  amount: number;
  payoutMethodId: string;
  idempotencyKey?: string;
}) {
  const headers: Record<string, string> = {};
  if (input.idempotencyKey) {
    headers["Idempotency-Key"] = input.idempotencyKey;
  }

  return apiFetchEnvelope<{ payoutId: string; status: string; amount: number }>(
    "/seller/wallet/payouts",
    {
      method: "POST",
      headers,
      body: JSON.stringify({
        amount: input.amount,
        payoutMethodId: input.payoutMethodId,
      }),
    },
  );
}
