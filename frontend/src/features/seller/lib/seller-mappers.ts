import type {
  AnalyticsData,
  SellerBooking,
  SellerDashboard,
  WalletData,
} from "@/shared/types";
import type {
  SellerBusinessProfile,
  SellerBookingDetail,
  SellerInventoryBlock,
  SellerListing,
  SellerListingDetail,
} from "../types";

export function mapSellerBusinessProfile(raw: {
  sellerId: string;
  businessName: string;
  verificationStatus: string;
  city?: string | null;
  rating?: number | null;
  listingCount?: number;
}): SellerBusinessProfile {
  return {
    sellerId: raw.sellerId,
    businessName: raw.businessName,
    verificationStatus: raw.verificationStatus as SellerBusinessProfile["verificationStatus"],
    city: raw.city ?? "",
    rating: raw.rating ?? 0,
    listingCount: raw.listingCount ?? 0,
  };
}

export function mapSellerListing(raw: {
  id: string;
  productCode?: string;
  slug: string;
  title: string;
  status: string;
  pricePerDay: number;
  deposit: number;
  primaryImageUrl?: string | null;
  createdAt: string;
  publishedAt?: string | null;
}): SellerListing {
  return {
    id: raw.id,
    productCode: raw.productCode ?? "",
    slug: raw.slug,
    title: raw.title,
    status: raw.status,
    pricePerDay: raw.pricePerDay,
    deposit: raw.deposit,
    imageUrl: raw.primaryImageUrl ?? null,
    createdAt: raw.createdAt,
    publishedAt: raw.publishedAt ?? null,
  };
}

export function mapSellerListingDetail(raw: {
  id: string;
  productCode?: string;
  slug: string;
  title: string;
  description?: string;
  status: string;
  pricePerDay: number;
  deposit: number;
  city?: string;
  primaryImageUrl?: string | null;
  imageUrls?: string[];
  images?: Array<{
    id: string;
    url: string;
    sortOrder: number;
  }>;
  variants?: Array<{
    id: string;
    size: string;
    status: string;
    availableQuantity: number;
  }>;
  categoryId?: string | null;
  occasion?: string | null;
  audience?: string | null;
  garmentType?: string | null;
  minRentalDays?: number;
  maxRentalDays?: number | null;
  includesTrial?: boolean;
  createdAt: string;
  publishedAt?: string | null;
}): SellerListingDetail {
  const listing = mapSellerListing({
    id: raw.id,
    productCode: raw.productCode,
    slug: raw.slug,
    title: raw.title,
    status: raw.status,
    pricePerDay: raw.pricePerDay,
    deposit: raw.deposit,
    primaryImageUrl: raw.primaryImageUrl ?? raw.imageUrls?.[0] ?? null,
    createdAt: raw.createdAt,
    publishedAt: raw.publishedAt,
  });

  return {
    ...listing,
    description: raw.description ?? "",
    city: raw.city ?? "",
    imageUrls: raw.imageUrls?.length
      ? raw.imageUrls
      : listing.imageUrl
        ? [listing.imageUrl]
        : [],
    images: (raw.images ?? []).map((image) => ({
      id: image.id,
      url: image.url,
      sortOrder: image.sortOrder,
    })),
    variants: (raw.variants ?? []).map((variant) => ({
      id: variant.id,
      size: variant.size,
      status: variant.status,
      availableQuantity: variant.availableQuantity,
    })),
    categoryId: raw.categoryId ?? null,
    occasion: raw.occasion ?? null,
    audience: raw.audience ?? null,
    garmentType: raw.garmentType ?? null,
    minRentalDays: raw.minRentalDays ?? 1,
    maxRentalDays: raw.maxRentalDays ?? null,
    includesTrial: raw.includesTrial ?? false,
  };
}

export function mapSellerInventoryBlock(raw: {
  id: string;
  productId: string;
  productTitle: string;
  variantId: string;
  variantSize: string;
  startDate: string;
  endDate: string;
  reason?: string | null;
  status: string;
}): SellerInventoryBlock {
  return {
    id: raw.id,
    productId: raw.productId,
    productTitle: raw.productTitle,
    variantId: raw.variantId,
    variantSize: raw.variantSize,
    startDate: raw.startDate,
    endDate: raw.endDate,
    reason: raw.reason ?? null,
    status: raw.status,
  };
}

export function mapSellerBooking(raw: {
  id: string;
  rentalNumber?: string;
  bookingNumber?: string;
  orderNumber?: string;
  productId: string;
  productTitle: string;
  productImage?: string;
  customerName?: string | null;
  variantSize?: string;
  status: string;
  rentalStart: string;
  rentalEnd: string;
  rentalDays: number;
  earnings: number;
  commission: number;
  deliveryPincode?: string | null;
  prepBy?: string | null;
  notes?: string | null;
  acceptDeadlineAt?: string | null;
  acceptanceExpired?: boolean;
  refundExpectedBusinessDays?: number;
}): SellerBooking {
  const rental = raw.rentalNumber ?? raw.bookingNumber ?? raw.id;
  return {
    id: raw.id,
    rentalNumber: rental,
    orderNumber: raw.orderNumber ?? "",
    orderId: raw.orderNumber ?? null,
    bookingId: rental,
    productId: raw.productId,
    productTitle: raw.productTitle,
    productImage: raw.productImage ?? "",
    customerName: raw.customerName ?? null,
    variantSize: raw.variantSize ?? "",
    status: raw.status.toLowerCase(),
    rentalStart: raw.rentalStart,
    rentalEnd: raw.rentalEnd,
    rentalDays: raw.rentalDays,
    earnings: raw.earnings,
    commission: raw.commission,
    currency: "INR",
    deliveryPincode: raw.deliveryPincode ?? null,
    prepBy: raw.prepBy ?? null,
    notes: raw.notes ?? null,
    acceptDeadlineAt: raw.acceptDeadlineAt ?? null,
    acceptanceExpired: raw.acceptanceExpired ?? false,
    refundExpectedBusinessDays: raw.refundExpectedBusinessDays,
  };
}

export function mapSellerBookingDetail(raw: {
  id: string;
  rentalNumber?: string;
  orderNumber?: string;
  status: string;
  productId: string;
  productTitle: string;
  productImage?: string;
  variantSize?: string;
  rentalStart: string;
  rentalEnd: string;
  rentalDays: number;
  currency?: string;
  earnings: {
    rentalAmount: number;
    commission: number;
    netEarnings: number;
    depositHeld: number;
    creditedToWallet: boolean;
  };
  customer?: {
    name?: string | null;
    phoneMasked?: string | null;
    deliveryPincode?: string | null;
    deliveryCity?: string | null;
  };
  prepBy?: string | null;
  notes?: string | null;
  customerNotes?: string | null;
  prepChecklist?: Array<{ item: string; done: boolean }>;
  acceptDeadlineAt?: string | null;
  acceptanceExpired?: boolean;
  canAccept?: boolean;
  canReject?: boolean;
  canMarkReady?: boolean;
  acceptSlaHours?: number;
  refundExpectedBusinessDays?: number;
  rejectReasons?: Array<{ code: string; label: string; requiresComment: boolean }>;
  rejectPreview?: {
    refundAmount: number;
    expectedBusinessDays: number;
    refundMethod: string;
    currency: string;
  } | null;
}): SellerBookingDetail {
  return {
    id: raw.id,
    rentalNumber: raw.rentalNumber ?? raw.id,
    orderNumber: raw.orderNumber ?? "",
    status: raw.status.toLowerCase(),
    productId: raw.productId,
    productTitle: raw.productTitle,
    productImage: raw.productImage ?? "",
    variantSize: raw.variantSize ?? "",
    rentalStart: raw.rentalStart,
    rentalEnd: raw.rentalEnd,
    rentalDays: raw.rentalDays,
    currency: raw.currency ?? "INR",
    earnings: raw.earnings,
    customer: {
      name: raw.customer?.name ?? null,
      phoneMasked: raw.customer?.phoneMasked ?? null,
      deliveryPincode: raw.customer?.deliveryPincode ?? null,
      deliveryCity: raw.customer?.deliveryCity ?? null,
    },
    prepBy: raw.prepBy ?? null,
    notes: raw.notes ?? null,
    customerNotes: raw.customerNotes ?? null,
    prepChecklist: raw.prepChecklist ?? [],
    acceptDeadlineAt: raw.acceptDeadlineAt ?? null,
    acceptanceExpired: raw.acceptanceExpired ?? false,
    canAccept: raw.canAccept ?? false,
    canReject: raw.canReject ?? false,
    canMarkReady: raw.canMarkReady ?? false,
    acceptSlaHours: raw.acceptSlaHours ?? 24,
    refundExpectedBusinessDays: raw.refundExpectedBusinessDays ?? 5,
    rejectReasons: raw.rejectReasons ?? [],
    rejectPreview: raw.rejectPreview ?? null,
  };
}

export function mapSellerDashboard(raw: {
  summary: {
    activeListings: number;
    pendingBookings: number;
    earningsThisMonth: number;
    currency: string;
  };
  tasks: Array<{ type: string; bookingId: string; dueBy: string }>;
  recentBookings: Array<Parameters<typeof mapSellerBooking>[0]>;
}): SellerDashboard {
  return {
    summary: {
      activeListings: raw.summary.activeListings,
      pendingBookings: raw.summary.pendingBookings,
      earningsThisMonth: raw.summary.earningsThisMonth,
      currency: "INR",
    },
    tasks: raw.tasks.map((task) => ({
      type: task.type,
      bookingId: task.bookingId,
      dueBy: task.dueBy,
    })),
    recentBookings: raw.recentBookings.map(mapSellerBooking),
  };
}

export function mapSellerWallet(raw: WalletData): WalletData {
  return {
    ...raw,
    currency: "INR",
    minPayoutAmount: raw.minPayoutAmount ?? 500,
    payoutProviderConfigured: raw.payoutProviderConfigured ?? false,
    transactions: raw.transactions.map((txn) => ({
      ...txn,
      label: txn.label ?? txn.type,
    })),
    payoutMethods: (raw.payoutMethods ?? []).map((method) => ({
      ...method,
      verified: method.verified ?? false,
    })),
  };
}

export function mapSellerAnalytics(raw: AnalyticsData): AnalyticsData {
  return {
    ...raw,
    currency: "INR",
  };
}
