export interface SellerBusinessProfile {
  sellerId: string;
  businessName: string;
  verificationStatus: "PENDING" | "VERIFIED" | "REJECTED" | "SUSPENDED";
  city: string;
  rating: number;
  listingCount: number;
}

export interface SellerListing {
  id: string;
  productCode: string;
  slug: string;
  title: string;
  status: string;
  pricePerDay: number;
  deposit: number;
  imageUrl: string | null;
  createdAt: string;
  publishedAt: string | null;
}

export interface SellerListingVariant {
  id: string;
  size: string;
  status: string;
  availableQuantity: number;
}

export interface SellerListingImage {
  id: string;
  url: string;
  sortOrder: number;
}

export interface SellerListingDetail extends SellerListing {
  description: string;
  city: string;
  imageUrls: string[];
  images: SellerListingImage[];
  variants: SellerListingVariant[];
  categoryId: string | null;
  occasion: string | null;
  audience: string | null;
  garmentType: string | null;
  minRentalDays: number;
  maxRentalDays: number | null;
  includesTrial: boolean;
}

export interface SellerInventoryBlock {
  id: string;
  productId: string;
  productTitle: string;
  variantId: string;
  variantSize: string;
  startDate: string;
  endDate: string;
  reason: string | null;
  status: string;
}

export interface SellerRejectReasonOption {
  code: string;
  label: string;
  requiresComment: boolean;
}

export interface SellerRejectPreview {
  refundAmount: number;
  expectedBusinessDays: number;
  refundMethod: string;
  currency: string;
}

export interface SellerBookingDetail {
  id: string;
  rentalNumber: string;
  orderNumber: string;
  status: string;
  productId: string;
  productTitle: string;
  productImage: string;
  variantSize: string;
  rentalStart: string;
  rentalEnd: string;
  rentalDays: number;
  currency: string;
  earnings: {
    rentalAmount: number;
    commission: number;
    netEarnings: number;
    depositHeld: number;
    creditedToWallet: boolean;
  };
  customer: {
    name: string | null;
    phoneMasked: string | null;
    deliveryPincode: string | null;
    deliveryCity: string | null;
  };
  prepBy: string | null;
  notes: string | null;
  customerNotes: string | null;
  prepChecklist: Array<{ item: string; done: boolean }>;
  acceptDeadlineAt: string | null;
  acceptanceExpired: boolean;
  canAccept: boolean;
  canReject: boolean;
  canMarkReady: boolean;
  acceptSlaHours: number;
  refundExpectedBusinessDays: number;
  rejectReasons: SellerRejectReasonOption[];
  rejectPreview: SellerRejectPreview | null;
}
