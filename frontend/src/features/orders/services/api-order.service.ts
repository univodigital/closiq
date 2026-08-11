import { apiFetch, apiFetchEnvelope } from "@/lib/api-client";
import { getAccessToken } from "@/lib/auth-token";
import {
  mapAvailability,
  mapBookingDetailToOrder,
  mapBookingSummaryToOrder,
} from "@/lib/api-mappers";
import type { ApiResponse, AvailabilityData, Order, ReturnScheduleResult, ShipmentTrackData, TrialRejectPreview } from "@/shared/types";
import type { AvailabilityService, BookingService, CancelPreview, OrderService } from "./order.service";

class ApiOrderService implements OrderService {
  async listOrders() {
    const res = await apiFetchEnvelope<unknown[]>("/bookings");
    return {
      ...res,
      data: res.data.map((item) =>
        mapBookingSummaryToOrder(item as Parameters<typeof mapBookingSummaryToOrder>[0]),
      ),
    } satisfies ApiResponse<Order[]>;
  }

  async getOrder(id: string) {
    const res = await apiFetchEnvelope<unknown>(`/bookings/${encodeURIComponent(id)}`);
    return {
      ...res,
      data: mapBookingDetailToOrder(res.data as Parameters<typeof mapBookingDetailToOrder>[0]),
    } satisfies ApiResponse<Order>;
  }

  async getCancelPreview(id: string) {
    const res = await apiFetchEnvelope<CancelPreview>(
      `/bookings/${encodeURIComponent(id)}/cancel-preview`,
    );
    return res satisfies ApiResponse<CancelPreview>;
  }

  async cancelOrder(id: string, reason: string, comment?: string) {
    const res = await apiFetchEnvelope<unknown>(`/bookings/${encodeURIComponent(id)}/cancel`, {
      method: "POST",
      body: JSON.stringify({ reason, comment: comment ?? "" }),
    });
    return {
      ...res,
      data: mapBookingDetailToOrder(res.data as Parameters<typeof mapBookingDetailToOrder>[0]),
    } satisfies ApiResponse<Order>;
  }

  async getTrialRejectPreview(id: string) {
    const res = await apiFetchEnvelope<TrialRejectPreview>(
      `/bookings/${encodeURIComponent(id)}/trial/reject-preview`,
    );
    return res satisfies ApiResponse<TrialRejectPreview>;
  }

  async scheduleReturn(id: string) {
    const res = await apiFetchEnvelope<ReturnScheduleResult>(
      `/bookings/${encodeURIComponent(id)}/return-request`,
      { method: "POST", body: "{}" },
    );
    return res satisfies ApiResponse<ReturnScheduleResult>;
  }

  async trackReturnPickup(id: string) {
    const res = await apiFetchEnvelope<{
      shipmentId: string;
      trackingNumber: string;
      status: string;
      pickupScheduledAt?: string | null;
      pickupTimeSlot?: string | null;
      agentName?: string | null;
      agentPhone?: string | null;
      events?: Array<{ status: string; label: string; timestamp: string; location?: string | null }>;
    }>(`/shipments/${encodeURIComponent(id)}/track?type=RETURN`);
    return {
      ...res,
      data: {
        shipmentId: String(res.data.shipmentId),
        status: res.data.status,
        trackingNumber: res.data.trackingNumber,
        pickupScheduledAt: res.data.pickupScheduledAt ?? null,
        pickupTimeSlot: res.data.pickupTimeSlot ?? null,
        agentName: res.data.agentName ?? null,
        agentPhone: res.data.agentPhone ?? null,
        events: res.data.events ?? [],
      },
    } satisfies ApiResponse<ShipmentTrackData>;
  }

  async downloadInvoice(id: string) {
    const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8081/api/v1";
    const token = getAccessToken();
    const response = await fetch(`${API_BASE}/bookings/${encodeURIComponent(id)}/invoice`, {
      headers: {
        Accept: "text/html",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      credentials: "include",
    });
    if (!response.ok) throw new Error("Could not download invoice");
    const html = await response.text();
    const blob = new Blob([html], { type: "text/html" });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `invoice-${id}.html`;
    anchor.click();
    URL.revokeObjectURL(url);
  }
}

class ApiAvailabilityService implements AvailabilityService {
  async getAvailability(
    slugOrId: string,
    variantId: string,
    options?: { startDate?: string; endDate?: string },
  ) {
    const qs = new URLSearchParams({ variantId });
    if (options?.startDate) qs.set("startDate", options.startDate);
    if (options?.endDate) qs.set("endDate", options.endDate);
    const res = await apiFetchEnvelope<unknown>(
      `/products/${encodeURIComponent(slugOrId)}/availability?${qs.toString()}`,
    );
    return {
      ...res,
      data: mapAvailability(res.data as Parameters<typeof mapAvailability>[0]),
    } satisfies ApiResponse<AvailabilityData>;
  }
}

class ApiBookingService implements BookingService {
  async acceptTrial(bookingId: string) {
    await apiFetch(`/bookings/${encodeURIComponent(bookingId)}/trial/accept`, {
      method: "POST",
      body: "{}",
    });
    return apiOrderService.getOrder(bookingId);
  }

  async rejectTrial(bookingId: string, reason: string) {
    await apiFetch(`/bookings/${encodeURIComponent(bookingId)}/trial/reject`, {
      method: "POST",
      body: JSON.stringify({ reason }),
    });
    return apiOrderService.getOrder(bookingId);
  }
}

export const apiOrderService = new ApiOrderService();
export const apiAvailabilityService = new ApiAvailabilityService();
export const apiBookingService = new ApiBookingService();
