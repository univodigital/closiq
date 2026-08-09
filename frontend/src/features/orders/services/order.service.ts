import type { ApiResponse, AvailabilityData, Order } from "@/shared/types";

export interface OrderService {
  listOrders(): Promise<ApiResponse<Order[]>>;
  getOrder(id: string): Promise<ApiResponse<Order>>;
}

export interface AvailabilityService {
  getAvailability(
    slugOrId: string,
    variantId: string,
    options?: { startDate?: string; endDate?: string },
  ): Promise<ApiResponse<AvailabilityData>>;
}

export interface BookingService {
  acceptTrial(orderId: string): Promise<ApiResponse<Order>>;
  rejectTrial(orderId: string, reason: string): Promise<ApiResponse<Order>>;
}
