import type {
  AnalyticsData,
  AvailabilityData,
  Category,
  CheckoutSummary,
  Notification,
  Order,
  OrderStatus,
  Product,
  ProductFilters,
  Review,
  SellerBooking,
  SellerDashboard,
  SellerProfile,
  TimelineEvent,
  WalletData,
} from "@/shared/types";

export function mapOrderStatus(status: string): OrderStatus {
  return status.toLowerCase() as OrderStatus;
}

function instantToIso(value?: string | null): string | null {
  return value ?? null;
}

interface RawProductSummary {
  id: string;
  slug: string;
  productCode?: string;
  title: string;
  designer?: string;
  images?: string[];
  pricePerDay: number;
  deposit: number;
  currency?: string;
  rating?: number | null;
  reviewCount?: number;
  badges?: string[];
  includesTrial?: boolean;
  city?: string;
  audience?: "men" | "women" | "kids";
  garmentType?: string;
  trending?: boolean;
}

interface RawProductDetail extends RawProductSummary {
  description?: string;
  categoryId?: string;
  occasion?: string;
  variants?: Array<{ id: string; size: string; available: boolean }>;
  sellerId?: string;
  sellerName?: string;
  deliverablePincodes?: string[];
  trialDurationMinutes?: number;
  minRentalDays?: number;
  maxRentalDays?: number | null;
}

export function mapProductSummary(raw: RawProductSummary): Product {
  return {
    id: raw.id,
    slug: raw.slug,
    productCode: raw.productCode ?? "",
    title: raw.title,
    designer: raw.designer ?? "",
    description: "",
    categoryId: "",
    occasion: "",
    audience: raw.audience,
    garmentType: raw.garmentType,
    images: raw.images?.length ? raw.images : ["/placeholder-product.jpg"],
    pricePerDay: raw.pricePerDay,
    deposit: raw.deposit,
    currency: "INR",
    variants: [],
    rating: raw.rating ?? 0,
    reviewCount: raw.reviewCount ?? 0,
    badges: raw.badges ?? [],
    sellerId: "",
    sellerName: "",
    city: raw.city ?? "",
    deliverablePincodes: [],
    includesTrial: raw.includesTrial ?? false,
    trending: raw.trending ?? raw.badges?.includes("trending") ?? false,
    createdAt: new Date().toISOString(),
  };
}

export function mapProductDetail(raw: RawProductDetail): Product {
  return {
    ...mapProductSummary(raw),
    description: raw.description ?? "",
    categoryId: raw.categoryId ?? "",
    occasion: raw.occasion ?? "",
    variants: raw.variants ?? [],
    sellerId: raw.sellerId ?? "",
    sellerName: raw.sellerName ?? "",
    deliverablePincodes: raw.deliverablePincodes ?? [],
    includesTrial: raw.includesTrial ?? false,
    minRentalDays: raw.minRentalDays,
    maxRentalDays: raw.maxRentalDays,
  };
}

export function mapSellerProduct(raw: {
  id: string;
  productCode?: string;
  slug: string;
  title: string;
  pricePerDay: number;
  deposit: number;
  primaryImageUrl?: string | null;
  status?: string;
}): Product {
  return {
    ...mapProductSummary({
      id: raw.id,
      slug: raw.slug,
      productCode: raw.productCode,
      title: raw.title,
      pricePerDay: raw.pricePerDay,
      deposit: raw.deposit,
      images: raw.primaryImageUrl ? [raw.primaryImageUrl] : [],
    }),
    badges: raw.status ? [raw.status.toLowerCase()] : [],
  };
}

interface RawBookingSummary {
  id: string;
  rentalNumber?: string;
  bookingNumber?: string;
  orderNumber: string;
  status: string;
  productTitle: string;
  productImage?: string;
  variantSize?: string;
  rentalStartDate: string;
  rentalEndDate: string;
  totalAmount: number;
  currency?: string;
  createdAt: string;
}

interface RawBookingDetail extends RawBookingSummary {
  productId: string;
  rentalDays: number;
  rentalAmount: number;
  depositAmount: number;
  deliveryFee: number;
  discountAmount?: number;
  includesTrial: boolean;
  trialDurationMinutes: number;
  deliveryAddress?: {
    line1: string;
    line2?: string;
    city: string;
    state: string;
    pincode: string;
    phone?: string;
  } | null;
  timeline?: Array<{
    status: string;
    label: string;
    timestamp?: string | null;
    completed?: boolean | null;
    current?: boolean | null;
    pending?: boolean | null;
  }>;
}

function rentalId(raw: { rentalNumber?: string; bookingNumber?: string }): string {
  return raw.rentalNumber ?? raw.bookingNumber ?? "";
}

export function mapBookingSummaryToOrder(raw: RawBookingSummary): Order {
  const rental = rentalId(raw);
  return {
    id: raw.id,
    orderNumber: raw.orderNumber,
    rentalNumber: rental,
    bookingId: rental,
    status: mapOrderStatus(raw.status),
    productId: "",
    productTitle: raw.productTitle,
    productImage: raw.productImage ?? "",
    variantSize: raw.variantSize ?? "",
    rentalStart: raw.rentalStartDate,
    rentalEnd: raw.rentalEndDate,
    rentalDays: 0,
    rentalAmount: 0,
    depositAmount: 0,
    deliveryFee: 0,
    totalPaid: raw.totalAmount,
    currency: "INR",
    deliveryAddress: {
      line1: "",
      city: "",
      state: "",
      pincode: "",
      phone: "",
    },
    includesTrial: false,
    trialDurationMinutes: 15,
    createdAt: raw.createdAt,
    timeline: [],
  };
}

export function mapBookingDetailToOrder(raw: RawBookingDetail): Order {
  const rental = rentalId(raw);
  const timeline: TimelineEvent[] = (raw.timeline ?? []).map((event) => ({
    status: event.status.toLowerCase(),
    label: event.label,
    timestamp: instantToIso(event.timestamp),
    completed: event.completed ?? false,
    current: event.current ?? false,
    pending: event.pending ?? false,
  }));

  return {
    id: raw.id,
    orderNumber: raw.orderNumber,
    rentalNumber: rental,
    bookingId: rental,
    status: mapOrderStatus(raw.status),
    productId: raw.productId,
    productTitle: raw.productTitle,
    productImage: raw.productImage ?? "",
    variantSize: raw.variantSize ?? "",
    rentalStart: raw.rentalStartDate,
    rentalEnd: raw.rentalEndDate,
    rentalDays: raw.rentalDays,
    rentalAmount: raw.rentalAmount,
    depositAmount: raw.depositAmount,
    deliveryFee: raw.deliveryFee,
    totalPaid: raw.totalAmount,
    currency: "INR",
    deliveryAddress: {
      line1: raw.deliveryAddress?.line1 ?? "",
      line2: raw.deliveryAddress?.line2,
      city: raw.deliveryAddress?.city ?? "",
      state: raw.deliveryAddress?.state ?? "",
      pincode: raw.deliveryAddress?.pincode ?? "",
      phone: raw.deliveryAddress?.phone ?? "",
    },
    includesTrial: raw.includesTrial,
    trialDurationMinutes: raw.trialDurationMinutes,
    createdAt: raw.createdAt,
    timeline,
  };
}

export function mapSellerBooking(raw: {
  id: string;
  rentalNumber?: string;
  bookingId?: string;
  orderNumber?: string;
  orderId?: string | null;
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
  currency?: string;
  deliveryPincode?: string | null;
  prepBy?: string | null;
  notes?: string | null;
}): SellerBooking {
  const rental = raw.rentalNumber ?? raw.bookingId ?? "";
  const orderNum = raw.orderNumber ?? raw.orderId ?? null;
  return {
    id: raw.id,
    rentalNumber: rental,
    orderNumber: orderNum ?? "",
    bookingId: rental,
    orderId: orderNum,
    productId: raw.productId,
    productTitle: raw.productTitle,
    productImage: raw.productImage ?? "",
    customerName: raw.customerName ?? null,
    variantSize: raw.variantSize ?? "",
    status: mapOrderStatus(raw.status),
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

export function mapCategory(raw: {
  id: string;
  slug: string;
  name: string;
  description?: string;
  image?: string;
  productCount: number;
  featured: boolean;
  sortOrder: number;
}): Category {
  return {
    id: raw.id,
    slug: raw.slug,
    name: raw.name,
    description: raw.description ?? "",
    image: raw.image ?? "",
    productCount: raw.productCount,
    featured: raw.featured,
    sortOrder: raw.sortOrder,
  };
}

export function mapReview(raw: {
  id: string;
  rating: number;
  comment?: string;
  customerDisplayName?: string;
  photos?: string[];
  createdAt: string;
  verifiedRental?: boolean;
}, productId: string): Review {
  return {
    id: raw.id,
    productId,
    rating: raw.rating,
    body: raw.comment ?? "",
    comment: raw.comment,
    authorName: raw.customerDisplayName ?? "Verified renter",
    customerDisplayName: raw.customerDisplayName,
    photos: raw.photos,
    createdAt: raw.createdAt,
    verifiedRental: raw.verifiedRental,
  };
}

export function mapFilters(raw: {
  occasions: Array<{ slug?: string; name?: string; value?: string; count: number }>;
  sizes: Array<{ slug?: string; name?: string; value?: string; count: number }>;
  priceRange: { min: number; max: number };
  cities: Array<{ slug?: string; name?: string; value?: string; count: number }>;
}): ProductFilters {
  return {
    occasions: raw.occasions.map((o) => ({
      slug: o.slug ?? o.value ?? "",
      name: o.name ?? o.value ?? "",
      count: o.count,
    })),
    sizes: raw.sizes.map((s) => ({
      value: s.value ?? s.slug ?? "",
      count: s.count,
    })),
    priceRange: raw.priceRange,
    cities: raw.cities.map((c) => ({
      value: c.value ?? c.slug ?? "",
      count: c.count,
    })),
  };
}

export function mapAvailability(raw: {
  productId: string;
  variantId: string;
  minRentalDays: number;
  maxRentalDays?: number | null;
  bufferDaysAfterReturn: number;
  unavailableDates: string[];
  bookedRanges: Array<{ start: string; end: string; reason: string }>;
  blockedRanges: Array<{ start: string; end: string; reason: string }>;
  nextAvailableDate?: string | null;
}): AvailabilityData {
  return {
    productId: raw.productId,
    variantId: raw.variantId,
    minRentalDays: raw.minRentalDays,
    maxRentalDays: raw.maxRentalDays ?? null,
    bufferDaysAfterReturn: raw.bufferDaysAfterReturn,
    unavailableDates: raw.unavailableDates,
    bookedRanges: raw.bookedRanges,
    blockedRanges: raw.blockedRanges,
    nextAvailableDate: raw.nextAvailableDate ?? new Date().toISOString().slice(0, 10),
  };
}

export function mapCheckoutSummary(raw: CheckoutSummary): CheckoutSummary {
  return raw;
}

export function mapNotification(raw: {
  id: string;
  type: string;
  title: string;
  body: string;
  read: boolean;
  deepLink: string;
  metadata?: Record<string, unknown>;
  createdAt: string;
}): Notification {
  const metadata: Record<string, string> = {};
  if (raw.metadata) {
    for (const [key, value] of Object.entries(raw.metadata)) {
      if (value != null) metadata[key] = String(value);
    }
  }
  return {
    id: raw.id,
    type: raw.type,
    title: raw.title,
    body: raw.body,
    read: raw.read,
    deepLink: raw.deepLink,
    metadata,
    createdAt: raw.createdAt,
  };
}

export function mapDashboard(raw: {
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

export function mapWallet(raw: WalletData): WalletData {
  return {
    ...raw,
    currency: "INR",
    transactions: raw.transactions.map((txn) => ({
      ...txn,
      createdAt: txn.createdAt,
    })),
  };
}

export function mapAnalytics(raw: AnalyticsData): AnalyticsData {
  return {
    ...raw,
    currency: "INR",
  };
}

export interface RawSellerProfile {
  sellerId: string;
  businessName: string;
  verificationStatus: string;
  city?: string;
  rating?: number | null;
  listingCount?: number;
}

export function mapSellerProfile(raw: RawSellerProfile): SellerProfile {
  return {
    sellerId: raw.sellerId,
    businessName: raw.businessName,
    verificationStatus: raw.verificationStatus as SellerProfile["verificationStatus"],
    city: raw.city ?? "",
    listingCount: raw.listingCount ?? 0,
    rating: raw.rating ?? 0,
  };
}
