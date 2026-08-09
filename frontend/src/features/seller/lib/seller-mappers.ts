import type {
  AnalyticsData,
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
}): SellerBooking {
  const rental = raw.rentalNumber ?? raw.bookingNumber ?? raw.id;
  return {
    id: raw.id,
    rentalNumber: rental,
    orderNumber: raw.orderNumber ?? "",
    orderId: raw.orderNumber,
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
    transactions: raw.transactions.map((txn) => ({
      ...txn,
      label: txn.label ?? txn.type,
    })),
  };
}

export function mapSellerAnalytics(raw: AnalyticsData): AnalyticsData {
  return {
    ...raw,
    currency: "INR",
  };
}
