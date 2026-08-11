import type { ApiResponse, AvailabilityData, Order, ReturnScheduleResult, ShipmentTrackData, TrialRejectPreview } from "@/shared/types";

export interface CancelPreview {
  eligible: boolean;
  policyCode: string;
  policyLabel: string;
  originalAmount: number;
  refundAmount: number;
  nonRefundableAmount: number;
  rentalRefundAmount: number;
  depositRefundAmount: number;
  deliveryFeeNonRefundable: number;
  nonRefundableReason?: string;
  refundMethod: string;
  expectedRefundBusinessDays: number;
}

export interface OrderService {
  listOrders(): Promise<ApiResponse<Order[]>>;
  getOrder(id: string): Promise<ApiResponse<Order>>;
  getCancelPreview(id: string): Promise<ApiResponse<CancelPreview>>;
  getTrialRejectPreview(id: string): Promise<ApiResponse<TrialRejectPreview>>;
  scheduleReturn(id: string): Promise<ApiResponse<ReturnScheduleResult>>;
  trackReturnPickup(id: string): Promise<ApiResponse<ShipmentTrackData>>;
  cancelOrder(id: string, reason: string, comment?: string): Promise<ApiResponse<Order>>;
  downloadInvoice(id: string): Promise<void>;
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
