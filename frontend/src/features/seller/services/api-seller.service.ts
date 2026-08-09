import { apiFetch, apiFetchEnvelope } from "@/lib/api-client";
import { mapNotification } from "@/lib/api-mappers";
import type { CheckoutSummary } from "@/shared/types";
import {
  mapSellerAnalytics,
  mapSellerBooking,
  mapSellerBusinessProfile,
  mapSellerDashboard,
  mapSellerInventoryBlock,
  mapSellerListing,
  mapSellerListingDetail,
  mapSellerWallet,
} from "../lib/seller-mappers";
import type {
  AnalyticsData,
  ApiResponse,
  SellerBooking,
  SellerDashboard,
  WalletData,
} from "@/shared/types";
import type {
  SellerBusinessProfile,
  SellerInventoryBlock,
  SellerListing,
  SellerListingDetail,
} from "../types";
import type { CheckoutService, NotificationService } from "./buyer-aux.service";
import type { CreateSellerProductInput, SellerService } from "./seller.service";

class ApiSellerService implements SellerService {
  async getProfile() {
    const raw = await apiFetch<Parameters<typeof mapSellerBusinessProfile>[0]>("/seller/profile");
    return mapSellerBusinessProfile(raw);
  }

  async getDashboard() {
    const res = await apiFetchEnvelope<unknown>("/seller/dashboard");
    return {
      ...res,
      data: mapSellerDashboard(res.data as Parameters<typeof mapSellerDashboard>[0]),
    } satisfies ApiResponse<SellerDashboard>;
  }

  async listBookings() {
    const res = await apiFetchEnvelope<unknown[]>("/seller/bookings");
    return {
      ...res,
      data: res.data.map((item) => mapSellerBooking(item as Parameters<typeof mapSellerBooking>[0])),
    } satisfies ApiResponse<SellerBooking[]>;
  }

  async getBooking(id: string) {
    const res = await apiFetchEnvelope<unknown>(`/seller/bookings/${encodeURIComponent(id)}`);
    return {
      ...res,
      data: mapSellerBooking(res.data as Parameters<typeof mapSellerBooking>[0]),
    } satisfies ApiResponse<SellerBooking>;
  }

  async listProducts() {
    const res = await apiFetchEnvelope<unknown[]>("/seller/products");
    return {
      ...res,
      data: res.data.map((item) => mapSellerListing(item as Parameters<typeof mapSellerListing>[0])),
    } satisfies ApiResponse<SellerListing[]>;
  }

  async getProduct(id: string) {
    const res = await apiFetchEnvelope<unknown>(`/seller/products/${encodeURIComponent(id)}`);
    return {
      ...res,
      data: mapSellerListingDetail(res.data as Parameters<typeof mapSellerListingDetail>[0]),
    } satisfies ApiResponse<SellerListingDetail>;
  }

  async createProduct(input: CreateSellerProductInput) {
    const res = await apiFetchEnvelope<unknown>("/seller/products", {
      method: "POST",
      headers: { "Idempotency-Key": `create-product-${crypto.randomUUID()}` },
      body: JSON.stringify(input),
    });
    const raw = res.data as Parameters<typeof mapSellerListing>[0];
    return {
      ...res,
      data: mapSellerListing({
        ...raw,
        createdAt: raw.createdAt ?? new Date().toISOString(),
        primaryImageUrl: raw.primaryImageUrl ?? null,
      }),
    } satisfies ApiResponse<SellerListing>;
  }

  async listInventoryBlocks() {
    const res = await apiFetchEnvelope<unknown[]>("/seller/inventory/blocks");
    return {
      ...res,
      data: res.data.map((item) =>
        mapSellerInventoryBlock(item as Parameters<typeof mapSellerInventoryBlock>[0]),
      ),
    } satisfies ApiResponse<SellerInventoryBlock[]>;
  }

  async getWallet() {
    const res = await apiFetchEnvelope<unknown>("/seller/wallet");
    return {
      ...res,
      data: mapSellerWallet(res.data as WalletData),
    } satisfies ApiResponse<WalletData>;
  }

  async getAnalytics() {
    const res = await apiFetchEnvelope<unknown>("/seller/analytics");
    return {
      ...res,
      data: mapSellerAnalytics(res.data as AnalyticsData),
    } satisfies ApiResponse<AnalyticsData>;
  }
}

export const apiSellerService = new ApiSellerService();

class ApiNotificationService implements NotificationService {
  async list() {
    const res = await apiFetchEnvelope<unknown[]>("/notifications");
    return {
      ...res,
      data: res.data.map((item) => mapNotification(item as Parameters<typeof mapNotification>[0])),
    };
  }

  async markRead(id: string) {
    await apiFetch(`/notifications/${encodeURIComponent(id)}/read`, {
      method: "PATCH",
      body: JSON.stringify({ read: true }),
    });
  }

  async markAllRead() {
    return apiFetchEnvelope<{ markedCount: number }>("/notifications/read-all", {
      method: "POST",
      body: "{}",
    });
  }
}

class ApiCheckoutService implements CheckoutService {
  async calculate(input: {
    productId: string;
    variantId: string;
    rentalStartDate: string;
    rentalEndDate: string;
    pincode?: string;
    couponCode?: string;
  }) {
    return apiFetchEnvelope<CheckoutSummary>("/checkout/calculate", {
      method: "POST",
      body: JSON.stringify({
        productId: input.productId,
        variantId: input.variantId,
        rentalStartDate: input.rentalStartDate,
        rentalEndDate: input.rentalEndDate,
        pincode: input.pincode || undefined,
        couponCode: input.couponCode || undefined,
      }),
    });
  }

  async checkPincode(pincode: string) {
    const res = await apiFetchEnvelope<{
      pincode: string;
      serviceable: boolean;
      city?: string;
    }>(`/serviceability/pincodes/${encodeURIComponent(pincode)}`);
    return {
      ...res,
      data: {
        serviceable: res.data.serviceable,
        city: res.data.city,
      },
    };
  }
}

export const apiNotificationService = new ApiNotificationService();
export const apiCheckoutService = new ApiCheckoutService();
