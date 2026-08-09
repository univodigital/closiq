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
} from "../types";

export interface CreateSellerProductInput {
  title: string;
  description: string;
  categoryId: string;
  occasion: string;
  designer?: string;
  pricePerDay: number;
  deposit: number;
  city: string;
  variants: Array<{ size: string; quantity: number }>;
}

export interface SellerService {
  getProfile(): Promise<SellerBusinessProfile>;
  getDashboard(): Promise<ApiResponse<SellerDashboard>>;
  listBookings(): Promise<ApiResponse<SellerBooking[]>>;
  getBooking(id: string): Promise<ApiResponse<SellerBooking>>;
  listProducts(): Promise<ApiResponse<SellerListing[]>>;
  getProduct(id: string): Promise<ApiResponse<SellerListingDetail>>;
  createProduct(input: CreateSellerProductInput): Promise<ApiResponse<SellerListing>>;
  listInventoryBlocks(): Promise<ApiResponse<SellerInventoryBlock[]>>;
  getWallet(): Promise<ApiResponse<WalletData>>;
  getAnalytics(): Promise<ApiResponse<AnalyticsData>>;
}
