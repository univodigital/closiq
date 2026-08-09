import ordersData from "@/mocks/data/orders.json";
import availabilityData from "@/mocks/data/availability.json";
import productsData from "@/mocks/data/products.json";
import { delay } from "@/mocks/utils/delay";
import type { ApiResponse, AvailabilityData, Order, Product } from "@/shared/types";
import type { AvailabilityService, BookingService, OrderService } from "./order.service";

const orders = ordersData as Order[];
const products = productsData as Product[];
const availabilityMap = availabilityData as Record<
  string,
  {
    variantId: string;
    unavailableDates: string[];
    bookedRanges: Array<{ start: string; end: string; status: string }>;
    bufferDays: number;
    minRentalDays: number;
  }
>;

function resolveProductId(slugOrId: string): string {
  return products.find((p) => p.slug === slugOrId || p.id === slugOrId)?.id ?? slugOrId;
}

function wrap<T>(data: T): ApiResponse<T> {
  return {
    success: true,
    data,
    meta: { requestId: crypto.randomUUID(), timestamp: new Date().toISOString() },
  };
}

export class MockOrderService implements OrderService {
  async listOrders() {
    await delay(350);
    return wrap(orders);
  }

  async getOrder(id: string) {
    await delay(300);
    const order = orders.find((o) => o.id === id || o.orderNumber === id);
    if (!order) throw new Error("Order not found");
    return wrap(order);
  }
}

export class MockAvailabilityService implements AvailabilityService {
  async getAvailability(
    slugOrId: string,
    variantId: string,
    _options?: { startDate?: string; endDate?: string },
  ) {
    await delay(400);
    const productId = resolveProductId(slugOrId);
    const raw = availabilityMap[productId] ?? Object.values(availabilityMap)[0];
    const data: AvailabilityData = {
      productId,
      variantId,
      minRentalDays: raw.minRentalDays,
      maxRentalDays: 14,
      bufferDaysAfterReturn: raw.bufferDays,
      unavailableDates: raw.unavailableDates,
      bookedRanges: raw.bookedRanges.map((r) => ({
        start: r.start,
        end: r.end,
        reason: r.status === "blocked" ? "SELLER_BLOCKED" : "BOOKED",
      })),
      blockedRanges: raw.bookedRanges
        .filter((r) => r.status === "blocked")
        .map((r) => ({ start: r.start, end: r.end, reason: "SELLER_BLOCKED" })),
      nextAvailableDate: raw.unavailableDates[0]
        ? new Date(new Date(raw.unavailableDates.at(-1)!).getTime() + 86400000)
            .toISOString()
            .slice(0, 10)
        : new Date().toISOString().slice(0, 10),
    };
    return wrap(data);
  }
}

export class MockBookingService implements BookingService {
  async acceptTrial(orderId: string) {
    await delay(500);
    const order = orders.find((o) => o.id === orderId);
    if (!order) throw new Error("Order not found");
    return wrap({ ...order, status: "rental_active" as const });
  }

  async rejectTrial(orderId: string, _reason: string) {
    await delay(500);
    const order = orders.find((o) => o.id === orderId);
    if (!order) throw new Error("Order not found");
    return wrap({ ...order, status: "cancelled" as const });
  }
}

export const mockOrderService = new MockOrderService();
export const mockAvailabilityService = new MockAvailabilityService();
export const mockBookingService = new MockBookingService();
