import type { ApiResponse } from "@/shared/types";
import type {
  AdminDashboard,
  AdminProduct,
  AdminReview,
  AdminSellerApplication,
  AdminUser,
} from "./api-admin.service";

export interface AdminService {
  getDashboard(): Promise<ApiResponse<AdminDashboard>>;
  listUsers(params?: { status?: string; pageToken?: string; limit?: number }): Promise<ApiResponse<AdminUser[]>>;
  createUser(body: {
    phone: string;
    firstName: string;
    lastName: string;
    email?: string;
    roles?: string[];
  }): Promise<AdminUser>;
  updateUser(userId: string, body: { status?: string; roles?: string[] }): Promise<AdminUser>;
  deleteUser(userId: string): Promise<void>;
  listProducts(params?: { status?: string; pageToken?: string; limit?: number }): Promise<ApiResponse<AdminProduct[]>>;
  updateProduct(productId: string, body: { status: string }): Promise<AdminProduct>;
  deleteProduct(productId: string): Promise<void>;
  listReviews(params?: { status?: string; pageToken?: string; limit?: number }): Promise<ApiResponse<AdminReview[]>>;
  updateReview(reviewId: string, body: { status: string }): Promise<AdminReview>;
  deleteReview(reviewId: string): Promise<void>;
  listSellerApplications(status?: string): Promise<ApiResponse<AdminSellerApplication[]>>;
  approveSellerApplication(applicationId: string): Promise<AdminSellerApplication>;
  rejectSellerApplication(applicationId: string, reason: string): Promise<AdminSellerApplication>;
}
