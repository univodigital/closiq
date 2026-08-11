import { apiFetch } from "@/lib/api-client";

export interface AdminCategory {
  id: string;
  slug: string;
  name: string;
  description?: string | null;
  imageUrl?: string | null;
  status: "ACTIVE" | "DEPRECATED";
  featured: boolean;
  sortOrder: number;
  productCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAdminCategoryInput {
  name: string;
  description?: string;
  imageUrl?: string;
  featured?: boolean;
  sortOrder?: number;
}

export interface UpdateAdminCategoryInput {
  name?: string;
  description?: string;
  imageUrl?: string;
  status?: "ACTIVE" | "DEPRECATED";
  featured?: boolean;
  sortOrder?: number;
}

export async function fetchAdminCategories(): Promise<AdminCategory[]> {
  return apiFetch<AdminCategory[]>("/admin/categories");
}

export async function createAdminCategory(input: CreateAdminCategoryInput): Promise<AdminCategory> {
  return apiFetch<AdminCategory>("/admin/categories", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export async function updateAdminCategory(
  categoryId: string,
  input: UpdateAdminCategoryInput,
): Promise<AdminCategory> {
  return apiFetch<AdminCategory>(`/admin/categories/${encodeURIComponent(categoryId)}`, {
    method: "PATCH",
    body: JSON.stringify(input),
  });
}
