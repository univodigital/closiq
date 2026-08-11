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
  SellerBookingDetail,
} from "../types";

export interface CreateSellerProductInput {
  title: string;
  description: string;
  categoryId: string;
  occasion: string;
  audience: "men" | "women" | "kids";
  garmentType: string;
  designer?: string;
  pricePerDay: number;
  deposit: number;
  city: string;
  variants: Array<{ size: string; quantity: number }>;
}

export interface PresignedUploadData {
  uploadId: string;
  uploadUrl: string;
  method: string;
  headers?: Record<string, string>;
  formFields?: Record<string, string>;
  publicUrl?: string;
}

export interface ProductImageAttachData {
  imageId: string;
  url: string;
  sortOrder: number;
  alt?: string;
}

export interface SellerService {
  getProfile(): Promise<SellerBusinessProfile>;
  getDashboard(): Promise<ApiResponse<SellerDashboard>>;
  listBookings(): Promise<ApiResponse<SellerBooking[]>>;
  getBooking(id: string): Promise<ApiResponse<SellerBookingDetail>>;
  listProducts(params?: { status?: string }): Promise<ApiResponse<SellerListing[]>>;
  getProduct(id: string): Promise<ApiResponse<SellerListingDetail>>;
  createProduct(input: CreateSellerProductInput): Promise<ApiResponse<SellerListing>>;
  requestProductImageUpload(
    productId: string,
    input: { contentType: string; fileName: string },
  ): Promise<ApiResponse<PresignedUploadData>>;
  confirmProductImage(
    productId: string,
    input: { uploadId: string; sortOrder: number; alt?: string },
  ): Promise<ApiResponse<ProductImageAttachData>>;
  publishProduct(productId: string): Promise<ApiResponse<{ status: string; publishedAt: string }>>;
  listInventoryBlocks(): Promise<ApiResponse<SellerInventoryBlock[]>>;
  getWallet(): Promise<ApiResponse<WalletData>>;
  getAnalytics(): Promise<ApiResponse<AnalyticsData>>;
}
