import type { ApiResponse, CheckoutSummary, Notification } from "@/shared/types";

export interface NotificationService {
  list(): Promise<ApiResponse<Notification[]>>;
  markRead(id: string): Promise<void>;
  markAllRead(): Promise<ApiResponse<{ markedCount: number }>>;
}

export interface CheckoutService {
  calculate(input: {
    productId: string;
    variantId: string;
    rentalStartDate: string;
    rentalEndDate: string;
    pincode?: string;
    couponCode?: string;
  }): Promise<ApiResponse<CheckoutSummary>>;
  checkPincode(pincode: string): Promise<ApiResponse<{ serviceable: boolean; city?: string }>>;
}
