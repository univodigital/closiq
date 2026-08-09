import { apiFetch, apiFetchEnvelope } from "@/lib/api-client";
import {
  mapAvailability,
  mapBookingDetailToOrder,
  mapBookingSummaryToOrder,
} from "@/lib/api-mappers";
import type { ApiResponse, AvailabilityData, Order } from "@/shared/types";
import type { AvailabilityService, BookingService, OrderService } from "./order.service";

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
