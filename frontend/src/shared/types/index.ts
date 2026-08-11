export interface ApiMeta {
  requestId?: string;
  timestamp?: string;
  pagination?: PagePagination;
  unreadCount?: number;
}

export interface PagePagination {
  type: "page";
  limit: number;
  nextPageToken: string | null;
  prevPageToken: string | null;
  hasMore: boolean;
  totalCount?: number | null;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  meta: ApiMeta;
}

export type UserRole = "CUSTOMER" | "SELLER" | "ADMIN";

export type Gender = "MALE" | "FEMALE" | "OTHER" | "PREFER_NOT_TO_SAY";

export interface Address {
  id: string;
  label: string;
  line1: string;
  line2?: string;
  city: string;
  state: string;
  pincode: string;
  phone: string;
  isDefault: boolean;
  serviceable: boolean;
}

export interface User {
  id: string;
  phone: string;
  phoneVerified: boolean;
  alternatePhone?: string;
  username?: string;
  usernameChangeAllowed?: boolean;
  pendingEmail?: string;
  email?: string;
  emailVerified?: boolean;
  alternateEmail?: string;
  firstName: string;
  lastName: string;
  gender: Gender;
  displayName: string;
  avatarUrl: string | null;
  roles: UserRole[];
  sellerProfile?: SellerProfile;
  addresses?: Address[];
  preferences?: UserPreferences;
  createdAt: string;
}

export interface SellerProfile {
  sellerId: string;
  businessName: string;
  verificationStatus: "PENDING" | "VERIFIED" | "REJECTED";
  city: string;
  listingCount: number;
  rating: number;
}

export interface UserPreferences {
  size?: string;
  occasions?: string[];
  notificationsEnabled?: boolean;
}

export type OrderStatus =
  | "pending_payment"
  | "confirmed"
  | "out_for_delivery"
  | "trial_ready"
  | "trial_rejected"
  | "rental_active"
  | "return_scheduled"
  | "returned"
  | "inspection"
  | "refund_pending"
  | "deposit_refunded"
  | "cancelled";

export interface OrderPaymentSummary {
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

export interface OrderRefundDetails {
  refundAmount: number;
  depositRefundAmount: number;
  totalRefunded: number;
  status: string;
  refundMethod?: string;
  expectedBusinessDays?: number;
  expectedBy?: string | null;
  items: Array<{
    refundId: string;
    type: string;
    amount: number;
    status: string;
    initiatedAt?: string;
    processedAt?: string | null;
    expectedBy?: string | null;
  }>;
}

export interface OrderDepositSummary {
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
}

export interface OrderReturnPickup {
  shipmentId: string;
  returnReference: string;
  status: string;
  pickupDate: string;
  pickupWindow: string;
  pickupScheduledAt: string;
  pickedUpAt?: string | null;
  completedAt?: string | null;
  agentName?: string | null;
}

export interface ReturnScheduleResult {
  status: string;
  shipmentId: string;
  returnReference: string;
  pickupDate: string;
  pickupWindow: string;
  pickupScheduledAt: string;
  alreadyScheduled: boolean;
}

export interface ShipmentTrackData {
  shipmentId: string;
  status: string;
  trackingNumber: string;
  pickupScheduledAt?: string | null;
  pickupTimeSlot?: string | null;
  agentName?: string | null;
  agentPhone?: string | null;
  events: Array<{
    status: string;
    label: string;
    timestamp: string;
    location?: string | null;
  }>;
}

export interface TimelineEvent {
  status: string;
  label: string;
  description?: string | null;
  timestamp: string | null;
  completed: boolean;
  current?: boolean;
  pending?: boolean;
}

export interface OrderCancellationInfo {
  eligible: boolean;
  policyLabel: string;
}

export interface OrderTrialSession {
  startedAt: string;
  expiresAt: string;
  outcome: "PENDING" | "ACCEPTED" | "REJECTED" | "EXPIRED";
  active: boolean;
  expired: boolean;
  acceptedAt?: string | null;
  rejectedAt?: string | null;
}

export interface TrialRejectPreview {
  policyCode: string;
  policyLabel: string;
  rentalPaid: number;
  rentalRefundAmount: number;
  deliveryFeeNonRefundable: number;
  depositAmount: number;
  depositRefundAmount: number;
  depositRefundTiming: string;
  refundMethod: string;
  rentalRefundExpectedBusinessDays: number;
  depositRefundExpectedBusinessDaysMin: number;
  depositRefundExpectedBusinessDaysMax: number;
}

export interface Order {
  id: string;
  orderNumber: string;
  rentalNumber: string;
  /** @deprecated Use {@link rentalNumber} */
  bookingId: string;
  status: OrderStatus;
  productId: string;
  productTitle: string;
  productImage: string;
  variantSize: string;
  rentalStart: string;
  rentalEnd: string;
  rentalDays: number;
  rentalAmount: number;
  depositAmount: number;
  deliveryFee: number;
  discountAmount?: number;
  totalPaid: number;
  currency: "INR";
  paymentStatus?: string | null;
  paymentPending?: boolean;
  checkoutBatchId?: string | null;
  paymentSummary?: OrderPaymentSummary | null;
  refundDetails?: OrderRefundDetails | null;
  depositSummary?: OrderDepositSummary | null;
  invoiceAvailable?: boolean;
  depositRefundExpectedBusinessDays?: number;
  cancellation?: OrderCancellationInfo | null;
  deliveryAddress: Omit<Address, "id" | "label" | "isDefault" | "serviceable">;
  includesTrial: boolean;
  trialDurationMinutes: number;
  trialSession?: OrderTrialSession | null;
  returnPickup?: OrderReturnPickup | null;
  createdAt: string;
  timeline: TimelineEvent[];
}

export interface ProductVariant {
  id: string;
  size: string;
  available: boolean;
}

export interface Product {
  id: string;
  slug: string;
  productCode: string;
  title: string;
  designer: string;
  description: string;
  categoryId: string;
  occasion: string;
  audience?: "men" | "women" | "kids";
  garmentType?: string;
  images: string[];
  pricePerDay: number;
  deposit: number;
  currency: "INR";
  variants: ProductVariant[];
  rating: number;
  reviewCount: number;
  badges: string[];
  sellerId: string;
  sellerName: string;
  city: string;
  deliverablePincodes: string[];
  includesTrial: boolean;
  trending: boolean;
  minRentalDays?: number;
  maxRentalDays?: number | null;
  /** Set when listing request includes rental dates. */
  availableForDates?: boolean | null;
  createdAt: string;
}

export interface Category {
  id: string;
  slug: string;
  name: string;
  description: string;
  image: string;
  productCount: number;
  featured: boolean;
  sortOrder: number;
}

export interface Review {
  id: string;
  productId: string;
  rating: number;
  title?: string;
  body: string;
  comment?: string;
  authorName: string;
  customerDisplayName?: string;
  authorContext?: string;
  image?: string;
  photos?: string[];
  createdAt: string;
  verifiedRental?: boolean;
}

export interface Notification {
  id: string;
  type: string;
  title: string;
  body: string;
  read: boolean;
  deepLink: string;
  metadata?: Record<string, string>;
  createdAt: string;
}

export interface SellerBooking {
  id: string;
  rentalNumber: string;
  orderNumber: string;
  /** @deprecated Use {@link rentalNumber} */
  bookingId: string;
  /** @deprecated Use {@link orderNumber} */
  orderId: string | null;
  productId: string;
  productTitle: string;
  productImage: string;
  customerName: string | null;
  variantSize: string;
  status: string;
  rentalStart: string;
  rentalEnd: string;
  rentalDays: number;
  earnings: number;
  commission: number;
  currency: "INR";
  deliveryPincode: string | null;
  prepBy: string | null;
  notes: string | null;
  acceptDeadlineAt?: string | null;
  acceptanceExpired?: boolean;
  refundExpectedBusinessDays?: number;
}

export interface SellerDashboard {
  summary: {
    activeListings: number;
    pendingBookings: number;
    earningsThisMonth: number;
    currency: "INR";
  };
  tasks: Array<{
    type: string;
    bookingId: string;
    dueBy: string;
  }>;
  recentBookings: SellerBooking[];
}

export interface WalletData {
  sellerId: string;
  currency: "INR";
  availableBalance: number;
  pendingBalance: number;
  totalEarned: number;
  totalWithdrawn: number;
  minPayoutAmount?: number;
  payoutProviderConfigured?: boolean;
  transactions: WalletTransaction[];
  payoutMethods: PayoutMethod[];
}

export interface WalletTransaction {
  id: string;
  type: string;
  label: string;
  amount: number;
  status: string;
  createdAt: string;
}

export interface PayoutMethod {
  id: string;
  type: string;
  label: string;
  isDefault: boolean;
  verified?: boolean;
}

export interface AvailabilityData {
  productId: string;
  variantId: string;
  minRentalDays: number;
  maxRentalDays: number | null;
  bufferDaysAfterReturn: number;
  unavailableDates: string[];
  bookedRanges: Array<{ start: string; end: string; reason: string }>;
  blockedRanges: Array<{ start: string; end: string; reason: string }>;
  nextAvailableDate: string;
}

export interface AnalyticsData {
  period: string;
  views: number;
  uniqueVisitors: number;
  bookings: number;
  conversionRate: number;
  revenue: number;
  currency: "INR";
  topProducts: Array<{
    productId: string;
    title: string;
    views: number;
    bookings: number;
  }>;
}

export interface ProductFilters {
  occasions: Array<{ slug: string; name: string; count: number }>;
  sizes: Array<{ value: string; count: number }>;
  priceRange: { min: number; max: number };
  cities: Array<{ value: string; count: number }>;
}

export interface ProductListParams {
  occasion?: string;
  audience?: "men" | "women" | "kids";
  garmentType?: string;
  size?: string;
  minPrice?: number;
  maxPrice?: number;
  city?: string;
  featured?: boolean;
  trending?: boolean;
  sort?: string;
  limit?: number;
  pageToken?: string;
  q?: string;
  startDate?: string;
  endDate?: string;
}

export interface CheckoutSummary {
  rentalDays: number;
  lineItems: Array<{ type: string; label: string; amount: number }>;
  subtotal: number;
  discountAmount: number;
  totalAmount: number;
  depositAmount: number;
  payNowAmount: number;
  currency: "INR";
  serviceable: boolean;
}
