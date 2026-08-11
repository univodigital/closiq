import type {
  AnalyticsData,
  AvailabilityData,
  Category,
  CheckoutSummary,
  Notification,
  Order,
  OrderStatus,
  OrderTrialSession,
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
  const normalized = status.toLowerCase();
  if (normalized === "pending_payment") return "pending_payment";
  if (normalized === "seller_accepted" || normalized === "preparing") return "confirmed";
  if (normalized === "return_in_transit") return "return_scheduled";
  if (normalized === "trial_rejected") return "trial_rejected";
  if (normalized === "refund_pending") return "refund_pending";
  if (normalized === "completed") return "deposit_refunded";
  return normalized as OrderStatus;
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
  availableForDates?: boolean | null;
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
    availableForDates: raw.availableForDates ?? null,
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
  paymentStatus?: string | null;
  paymentPending?: boolean;
  checkoutBatchId?: string | null;
}

interface RawPaymentSummary {
  paymentId?: string;
  status: string;
  method?: string;
  rentalAmount: number;
  depositAmount: number;
  deliveryFee: number;
  discountAmount: number;
  totalPaid: number;
  paidAt?: string | null;
  checkoutBatchId?: string | null;
  paymentPending: boolean;
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
  trialInfo?: {
    startedAt: string;
    expiresAt: string;
    outcome: string;
    active: boolean;
    expired: boolean;
    acceptedAt?: string | null;
    rejectedAt?: string | null;
  } | null;
  paymentSummary?: RawPaymentSummary | null;
  refundDetails?: {
    refundAmount: number;
    depositRefundAmount: number;
    totalRefunded: number;
    status: string;
    refundMethod?: string;
    expectedBusinessDays?: number;
    expectedBy?: string | null;
    items?: Array<{
      refundId: string;
      type: string;
      amount: number;
      status: string;
      initiatedAt?: string;
      processedAt?: string | null;
      expectedBy?: string | null;
    }>;
  } | null;
  depositSummary?: {
    depositStatus: string;
    inspectionStatus?: string | null;
    depositAmount: number;
    damageDeduction: number;
    lateFee: number;
    cleaningFee?: number;
    totalDeduction?: number;
    deductionReason?: string | null;
    refundAmount: number;
    refundStatus?: string | null;
    refundMethod?: string;
    expectedRefundWindow?: string;
  } | null;
  returnPickup?: {
    shipmentId: string;
    returnReference: string;
    status: string;
    pickupDate: string;
    pickupWindow: string;
    pickupScheduledAt: string;
    pickedUpAt?: string | null;
    completedAt?: string | null;
    agentName?: string | null;
  } | null;
  invoiceAvailable?: boolean;
  depositRefundExpectedBusinessDays?: number;
  cancellation?: { eligible: boolean; policyLabel: string } | null;
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
    description?: string | null;
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
    paymentStatus: raw.paymentStatus ?? null,
    paymentPending: raw.paymentPending ?? raw.status === "PENDING_PAYMENT",
    checkoutBatchId: raw.checkoutBatchId ?? null,
  };
}

export function mapBookingDetailToOrder(raw: RawBookingDetail): Order {
  const rental = rentalId(raw);
  const timeline: TimelineEvent[] = (raw.timeline ?? []).map((event) => ({
    status: event.status.toLowerCase(),
    label: event.label,
    description: event.description ?? null,
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
    discountAmount: raw.discountAmount ?? 0,
    totalPaid: raw.totalAmount,
    currency: "INR",
    paymentStatus: raw.paymentSummary?.status ?? null,
    paymentPending: raw.paymentSummary?.paymentPending ?? raw.status === "PENDING_PAYMENT",
    checkoutBatchId: raw.paymentSummary?.checkoutBatchId ?? null,
    paymentSummary: raw.paymentSummary
      ? {
          paymentId: raw.paymentSummary.paymentId,
          status: raw.paymentSummary.status,
          method: raw.paymentSummary.method,
          rentalAmount: raw.paymentSummary.rentalAmount,
          depositAmount: raw.paymentSummary.depositAmount,
          deliveryFee: raw.paymentSummary.deliveryFee,
          discountAmount: raw.paymentSummary.discountAmount,
          totalPaid: raw.paymentSummary.totalPaid,
          paidAt: raw.paymentSummary.paidAt ?? null,
          checkoutBatchId: raw.paymentSummary.checkoutBatchId ?? null,
          paymentPending: raw.paymentSummary.paymentPending,
        }
      : null,
    refundDetails: raw.refundDetails
      ? {
          refundAmount: raw.refundDetails.refundAmount,
          depositRefundAmount: raw.refundDetails.depositRefundAmount,
          totalRefunded: raw.refundDetails.totalRefunded,
          status: raw.refundDetails.status,
          refundMethod: raw.refundDetails.refundMethod,
          expectedBusinessDays: raw.refundDetails.expectedBusinessDays,
          expectedBy: raw.refundDetails.expectedBy ?? null,
          items: raw.refundDetails.items ?? [],
        }
      : null,
    depositSummary: raw.depositSummary
      ? {
          depositStatus: raw.depositSummary.depositStatus,
          inspectionStatus: raw.depositSummary.inspectionStatus ?? null,
          depositAmount: raw.depositSummary.depositAmount,
          damageDeduction: raw.depositSummary.damageDeduction,
          lateFee: raw.depositSummary.lateFee,
          cleaningFee: raw.depositSummary.cleaningFee ?? 0,
          totalDeduction: raw.depositSummary.totalDeduction ?? 0,
          deductionReason: raw.depositSummary.deductionReason ?? null,
          refundAmount: raw.depositSummary.refundAmount,
          refundStatus: raw.depositSummary.refundStatus ?? null,
          refundMethod: raw.depositSummary.refundMethod,
          expectedRefundWindow: raw.depositSummary.expectedRefundWindow,
        }
      : null,
    returnPickup: raw.returnPickup
      ? {
          shipmentId: raw.returnPickup.shipmentId,
          returnReference: raw.returnPickup.returnReference,
          status: raw.returnPickup.status,
          pickupDate: raw.returnPickup.pickupDate,
          pickupWindow: raw.returnPickup.pickupWindow,
          pickupScheduledAt: raw.returnPickup.pickupScheduledAt,
          pickedUpAt: raw.returnPickup.pickedUpAt ?? null,
          completedAt: raw.returnPickup.completedAt ?? null,
          agentName: raw.returnPickup.agentName ?? null,
        }
      : null,
    invoiceAvailable: raw.invoiceAvailable ?? false,
    depositRefundExpectedBusinessDays: raw.depositRefundExpectedBusinessDays,
    cancellation: raw.cancellation
      ? { eligible: raw.cancellation.eligible, policyLabel: raw.cancellation.policyLabel }
      : null,
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
    trialSession: raw.trialInfo
      ? {
          startedAt: raw.trialInfo.startedAt,
          expiresAt: raw.trialInfo.expiresAt,
          outcome: raw.trialInfo.outcome as OrderTrialSession["outcome"],
          active: raw.trialInfo.active,
          expired: raw.trialInfo.expired,
          acceptedAt: raw.trialInfo.acceptedAt ?? null,
          rejectedAt: raw.trialInfo.rejectedAt ?? null,
        }
      : null,
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
