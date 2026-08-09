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

export interface SellerListingDetail extends SellerListing {
  description: string;
  city: string;
  imageUrls: string[];
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
