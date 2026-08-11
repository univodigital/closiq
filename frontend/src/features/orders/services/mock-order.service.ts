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

  async getCancelPreview(id: string) {
    await delay(200);
    const order = orders.find((o) => o.id === id || o.orderNumber === id);
    if (!order) throw new Error("Order not found");
    return wrap({
      eligible: order.status === "confirmed",
      policyCode: "PRE_DISPATCH",
      policyLabel: "Cancel before dispatch: Full refund",
      originalAmount: order.totalPaid,
      refundAmount: order.totalPaid,
      nonRefundableAmount: 0,
      rentalRefundAmount: order.rentalAmount,
      depositRefundAmount: order.depositAmount,
      deliveryFeeNonRefundable: 0,
      refundMethod: "ORIGINAL_PAYMENT_METHOD",
      expectedRefundBusinessDays: 5,
    });
  }

  async cancelOrder(id: string, _reason: string, _comment?: string) {
    await delay(400);
    const order = orders.find((o) => o.id === id || o.orderNumber === id);
    if (!order) throw new Error("Order not found");
    return wrap({ ...order, status: "cancelled" as const });
  }

  async getTrialRejectPreview(_id: string) {
    await delay(200);
    return wrap({
      policyCode: "TRIAL_REJECT",
      policyLabel: "Reject during home trial — no rental charge",
      rentalPaid: 3000,
      rentalRefundAmount: 3000,
      deliveryFeeNonRefundable: 0,
      depositAmount: 2000,
      depositRefundAmount: 0,
      depositRefundTiming: "5–7 business days after inspection",
      refundMethod: "ORIGINAL_PAYMENT_METHOD",
      rentalRefundExpectedBusinessDays: 5,
      depositRefundExpectedBusinessDaysMin: 5,
      depositRefundExpectedBusinessDaysMax: 7,
    });
  }

  async scheduleReturn(id: string) {
    await delay(400);
    const order = orders.find((o) => o.id === id || o.orderNumber === id);
    if (!order) throw new Error("Order not found");
    return wrap({
      status: "return_scheduled",
      shipmentId: "ship_mock_return",
      returnReference: "RET-MOCK-001",
      pickupDate: order.rentalEnd,
      pickupWindow: "10:00 – 14:00",
      pickupScheduledAt: new Date().toISOString(),
      alreadyScheduled: order.status === "return_scheduled",
    });
  }

  async trackReturnPickup(_id: string) {
    await delay(300);
    return wrap({
      shipmentId: "ship_mock_return",
      status: "CREATED",
      trackingNumber: "RET-MOCK-001",
      pickupScheduledAt: new Date().toISOString(),
      pickupTimeSlot: "10:00-14:00",
      agentName: null,
      agentPhone: null,
      events: [],
    });
  }

  async downloadInvoice(_id: string) {
    await delay(200);
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
    return wrap({ ...order, status: "trial_rejected" as const });
  }
}

export const mockOrderService = new MockOrderService();
export const mockAvailabilityService = new MockAvailabilityService();
export const mockBookingService = new MockBookingService();
