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
  email?: string;
  emailVerified?: boolean;
  alternateEmail?: string;
  firstName: string;
  lastName: string;
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
  | "confirmed"
  | "out_for_delivery"
  | "trial_ready"
  | "rental_active"
  | "return_scheduled"
  | "returned"
  | "deposit_refunded"
  | "cancelled";

export interface TimelineEvent {
  status: string;
  label: string;
  timestamp: string | null;
  completed: boolean;
  current?: boolean;
  pending?: boolean;
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
  totalPaid: number;
  currency: "INR";
  deliveryAddress: Omit<Address, "id" | "label" | "isDefault" | "serviceable">;
  includesTrial: boolean;
  trialDurationMinutes: number;
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
}

export interface AvailabilityData {
  productId: string;
  variantId: string;
  minRentalDays: number;
  maxRentalDays: number;
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
