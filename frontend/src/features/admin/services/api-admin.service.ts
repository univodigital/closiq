import { apiFetch, apiFetchEnvelope } from "@/lib/api-client";
import type { ApiResponse } from "@/shared/types";
import type { AdminService } from "./admin.service";

export interface AdminDashboard {
  totalUsers: number;
  activeUsers: number;
  suspendedUsers: number;
  totalProducts: number;
  activeProducts: number;
  totalReviews: number;
  publishedReviews: number;
  pendingSellerApplications: number;
}

export interface AdminUser {
  id: string;
  userCode: string;
  phone: string;
  email?: string;
  firstName: string;
  lastName: string;
  displayName: string;
  status: string;
  roles: string[];
  createdAt: string;
}

export interface AdminProduct {
  id: string;
  productCode: string;
  slug: string;
  title: string;
  status: string;
  sellerBusinessName?: string;
  primaryImageUrl?: string;
  pricePerDay: number;
  createdAt: string;
  publishedAt?: string;
}

export interface AdminReview {
  id: string;
  authorDisplayName: string;
  productTitle: string;
  productRating: number;
  sellerRating?: number;
  title?: string;
  body?: string;
  status: string;
  createdAt: string;
  publishedAt?: string;
}

export interface AdminSellerApplication {
  applicationId: string;
  userId: string;
  applicantName: string;
  applicantPhone: string;
  businessName: string;
  businessType: string;
  city: string;
  status: string;
  submittedAt: string;
  reviewedAt?: string | null;
  rejectionReason?: string | null;
}

class ApiAdminService implements AdminService {
  async getDashboard() {
    const res = await apiFetchEnvelope<AdminDashboard>("/admin/dashboard");
    return res satisfies ApiResponse<AdminDashboard>;
  }

  async listUsers(params?: { status?: string; pageToken?: string; limit?: number }) {
    const query = new URLSearchParams();
    if (params?.status) query.set("status", params.status);
    if (params?.pageToken) query.set("pageToken", params.pageToken);
    if (params?.limit) query.set("limit", String(params.limit));
    const qs = query.toString();
    const res = await apiFetchEnvelope<AdminUser[]>(`/admin/users${qs ? `?${qs}` : ""}`);
    return res satisfies ApiResponse<AdminUser[]>;
  }

  async createUser(body: {
    phone: string;
    firstName: string;
    lastName: string;
    email?: string;
    roles?: string[];
  }) {
    const data = await apiFetch<AdminUser & { phoneVerified?: boolean }>("/admin/users", {
      method: "POST",
      body: JSON.stringify(body),
    });
    return data;
  }

  async updateUser(userId: string, body: { status?: string; roles?: string[] }) {
    return apiFetch<AdminUser>(`/admin/users/${encodeURIComponent(userId)}`, {
      method: "PATCH",
      body: JSON.stringify(body),
    });
  }

  async deleteUser(userId: string) {
    await apiFetch<void>(`/admin/users/${encodeURIComponent(userId)}`, { method: "DELETE" });
  }

  async listProducts(params?: { status?: string; pageToken?: string; limit?: number }) {
    const query = new URLSearchParams();
    if (params?.status) query.set("status", params.status);
    if (params?.pageToken) query.set("pageToken", params.pageToken);
    if (params?.limit) query.set("limit", String(params.limit));
    const qs = query.toString();
    const res = await apiFetchEnvelope<AdminProduct[]>(`/admin/products${qs ? `?${qs}` : ""}`);
    return res satisfies ApiResponse<AdminProduct[]>;
  }

  async updateProduct(productId: string, body: { status: string }) {
    return apiFetch<AdminProduct>(`/admin/products/${encodeURIComponent(productId)}`, {
      method: "PATCH",
      body: JSON.stringify(body),
    });
  }

  async deleteProduct(productId: string) {
    await apiFetch<void>(`/admin/products/${encodeURIComponent(productId)}`, { method: "DELETE" });
  }

  async listReviews(params?: { status?: string; pageToken?: string; limit?: number }) {
    const query = new URLSearchParams();
    if (params?.status) query.set("status", params.status);
    if (params?.pageToken) query.set("pageToken", params.pageToken);
    if (params?.limit) query.set("limit", String(params.limit));
    const qs = query.toString();
    const res = await apiFetchEnvelope<AdminReview[]>(`/admin/reviews${qs ? `?${qs}` : ""}`);
    return res satisfies ApiResponse<AdminReview[]>;
  }

  async updateReview(reviewId: string, body: { status: string }) {
    return apiFetch<AdminReview>(`/admin/reviews/${encodeURIComponent(reviewId)}`, {
      method: "PATCH",
      body: JSON.stringify(body),
    });
  }

  async deleteReview(reviewId: string) {
    await apiFetch<void>(`/admin/reviews/${encodeURIComponent(reviewId)}`, { method: "DELETE" });
  }

  async listSellerApplications(status?: string) {
    const qs = status ? `?status=${encodeURIComponent(status)}` : "";
    const res = await apiFetchEnvelope<AdminSellerApplication[]>(`/admin/seller-applications${qs}`);
    return res satisfies ApiResponse<AdminSellerApplication[]>;
  }

  async approveSellerApplication(applicationId: string) {
    return apiFetch<AdminSellerApplication>(
      `/admin/seller-applications/${encodeURIComponent(applicationId)}/approve`,
      { method: "POST", body: "{}" },
    );
  }

  async rejectSellerApplication(applicationId: string, reason: string) {
    return apiFetch<AdminSellerApplication>(
      `/admin/seller-applications/${encodeURIComponent(applicationId)}/reject`,
      { method: "POST", body: JSON.stringify({ reason }) },
    );
  }
}

export const apiAdminService = new ApiAdminService();
