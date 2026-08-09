import sellerDashboard from "@/mocks/data/seller-dashboard.json";
import bookingsData from "@/mocks/data/bookings.json";
import walletData from "@/mocks/data/wallet.json";
import analyticsData from "@/mocks/data/analytics.json";
import notificationsData from "@/mocks/data/notifications.json";
import productsData from "@/mocks/data/products.json";
import userProfile from "@/mocks/data/user-profile.json";
import { delay } from "@/mocks/utils/delay";
import { MUMBAI_SERVICEABLE_PINCODES } from "@/shared/constants/routes";
import type {
  AnalyticsData,
  ApiResponse,
  CheckoutSummary,
  Notification,
  Product,
  SellerBooking,
  SellerDashboard,
  WalletData,
} from "@/shared/types";
import {
  mapSellerAnalytics,
  mapSellerBusinessProfile,
  mapSellerDashboard,
  mapSellerListing,
  mapSellerListingDetail,
} from "../lib/seller-mappers";
import type { SellerInventoryBlock, SellerListingDetail } from "../types";
import type { CheckoutService, NotificationService } from "./buyer-aux.service";

const bookings = bookingsData as SellerBooking[];
const products = productsData as Product[];

type RawNotification = {
  id: string;
  type: string;
  title: string;
  body: string;
  read: boolean;
  actionUrl: string;
  createdAt: string;
};

let notifications: Notification[] = (notificationsData as RawNotification[]).map((n) => ({
  id: n.id,
  type: n.type,
  title: n.title,
  body: n.body,
  read: n.read,
  deepLink: n.actionUrl,
  createdAt: n.createdAt,
}));

function wrap<T>(data: T): ApiResponse<T> {
  return {
    success: true,
    data,
    meta: { requestId: crypto.randomUUID(), timestamp: new Date().toISOString() },
  };
}

function sellerListings() {
  return products
    .filter((p) => p.sellerId === "seller_001")
    .map((p) =>
      mapSellerListing({
        id: p.id,
        productCode: p.productCode,
        slug: p.slug,
        title: p.title,
        status: "ACTIVE",
        pricePerDay: p.pricePerDay,
        deposit: p.deposit,
        primaryImageUrl: p.images[0] ?? null,
        createdAt: p.createdAt,
        publishedAt: p.createdAt,
      }),
    );
}

export class MockSellerService {
  async getProfile() {
    await delay(200);
    const raw = userProfile.sellerProfile!;
    return mapSellerBusinessProfile({
      sellerId: raw.sellerId,
      businessName: raw.businessName,
      verificationStatus: raw.verificationStatus.toUpperCase(),
      city: raw.city,
      rating: raw.rating,
      listingCount: raw.listingCount,
    });
  }

  async getDashboard() {
    await delay(350);
    const raw = sellerDashboard as {
      summary: {
        activeListings: number;
        activeBookings: number;
        totalEarnings: number;
      };
      upcomingTasks: Array<{ type: string; label: string; dueBy: string }>;
    };
    return wrap(
      mapSellerDashboard({
        summary: {
          activeListings: raw.summary.activeListings,
          pendingBookings: raw.summary.activeBookings,
          earningsThisMonth: raw.summary.totalEarnings,
          currency: "INR",
        },
        tasks: raw.upcomingTasks.map((t, i) => ({
          type: t.type.toUpperCase(),
          bookingId: bookings[i]?.id ?? "bkg_seller_001",
          dueBy: t.dueBy,
        })),
        recentBookings: bookings.filter((b) => b.status !== "blocked").slice(0, 3),
      }),
    );
  }

  async listBookings() {
    await delay(350);
    return wrap(bookings.filter((b) => b.status !== "blocked"));
  }

  async getBooking(id: string) {
    await delay(300);
    const booking = bookings.find((b) => b.id === id || b.bookingId === id);
    if (!booking) throw new Error("Booking not found");
    return wrap(booking);
  }

  async listProducts() {
    await delay(300);
    return wrap(sellerListings());
  }

  async getProduct(id: string) {
    await delay(300);
    const product = products.find((p) => p.id === id || p.slug === id);
    if (!product) throw new Error("Product not found");
    const detail: SellerListingDetail = mapSellerListingDetail({
      id: product.id,
      productCode: product.productCode,
      slug: product.slug,
      title: product.title,
      description: product.description,
      status: "ACTIVE",
      pricePerDay: product.pricePerDay,
      deposit: product.deposit,
      city: product.city,
      primaryImageUrl: product.images[0] ?? null,
      imageUrls: product.images,
      variants: product.variants.map((variant) => ({
        id: variant.id,
        size: variant.size,
        status: variant.available ? "ACTIVE" : "INACTIVE",
        availableQuantity: variant.available ? 1 : 0,
      })),
      categoryId: product.categoryId,
      occasion: product.occasion,
      audience: product.audience,
      garmentType: product.garmentType,
      minRentalDays: product.minRentalDays,
      maxRentalDays: product.maxRentalDays,
      includesTrial: product.includesTrial,
      createdAt: product.createdAt,
      publishedAt: product.createdAt,
    });
    return wrap(detail);
  }

  async listInventoryBlocks() {
    await delay(300);
    const blocked = bookings.filter((b) => b.status === "blocked");
    const blocks: SellerInventoryBlock[] = blocked.map((b) => ({
      id: b.id,
      productId: b.productId,
      productTitle: b.productTitle,
      variantId: "",
      variantSize: b.variantSize,
      startDate: b.rentalStart,
      endDate: b.rentalEnd,
      reason: "Blocked",
      status: "BLOCKED",
    }));
    return wrap(blocks);
  }

  async getWallet() {
    await delay(300);
    return wrap(walletData as WalletData);
  }

  async getAnalytics() {
    await delay(400);
    const raw = analyticsData as {
      period: string;
      metrics: { views: number; bookings: number; conversionRate: number; revenue: number };
      topProducts: AnalyticsData["topProducts"];
    };
    return wrap(
      mapSellerAnalytics({
        period: raw.period,
        views: raw.metrics.views,
        uniqueVisitors: Math.round(raw.metrics.views * 0.68),
        bookings: raw.metrics.bookings,
        conversionRate: raw.metrics.conversionRate / 100,
        revenue: raw.metrics.revenue,
        currency: "INR",
        topProducts: raw.topProducts,
      }),
    );
  }
}

export class MockNotificationService implements NotificationService {
  async list() {
    await delay(250);
    return {
      ...wrap(notifications),
      meta: {
        ...wrap(notifications).meta,
        unreadCount: notifications.filter((n) => !n.read).length,
      },
    };
  }

  async markRead(id: string) {
    await delay(150);
    notifications = notifications.map((n) => (n.id === id ? { ...n, read: true } : n));
  }

  async markAllRead() {
    await delay(200);
    const count = notifications.filter((n) => !n.read).length;
    notifications = notifications.map((n) => ({ ...n, read: true }));
    return wrap({ markedCount: count });
  }
}

export class MockCheckoutService implements CheckoutService {
  async calculate(input: {
    productId: string;
    variantId: string;
    rentalStartDate: string;
    rentalEndDate: string;
    pincode?: string;
    couponCode?: string;
  }) {
    await delay(400);
    const product = products.find((p) => p.id === input.productId || p.slug === input.productId);
    if (!product) throw new Error("Product not found");
    const start = new Date(input.rentalStartDate);
    const end = new Date(input.rentalEndDate);
    const rentalDays = Math.max(1, Math.ceil((end.getTime() - start.getTime()) / 86400000));
    const rentalAmount = product.pricePerDay * rentalDays;
    const discountAmount = input.couponCode === "FIRST500" ? 500 : 0;
    const totalAmount = rentalAmount + product.deposit - discountAmount;
    const summary: CheckoutSummary = {
      rentalDays,
      lineItems: [
        { type: "RENTAL", label: `Rental (${rentalDays} days × ₹${product.pricePerDay})`, amount: rentalAmount },
        { type: "DEPOSIT", label: "Refundable deposit", amount: product.deposit },
        { type: "DELIVERY", label: "Delivery", amount: 0 },
        ...(discountAmount ? [{ type: "DISCOUNT", label: "Coupon FIRST500", amount: -discountAmount }] : []),
      ],
      subtotal: rentalAmount + product.deposit,
      discountAmount,
      totalAmount,
      depositAmount: product.deposit,
      payNowAmount: totalAmount,
      currency: "INR",
      serviceable: input.pincode ? MUMBAI_SERVICEABLE_PINCODES.includes(input.pincode) : true,
    };
    return wrap(summary);
  }

  async checkPincode(pincode: string) {
    await delay(300);
    const serviceable = MUMBAI_SERVICEABLE_PINCODES.includes(pincode);
    return wrap({ serviceable, city: serviceable ? "Mumbai" : undefined });
  }
}

export const mockSellerService = new MockSellerService();
export const mockNotificationService = new MockNotificationService();
export const mockCheckoutService = new MockCheckoutService();
