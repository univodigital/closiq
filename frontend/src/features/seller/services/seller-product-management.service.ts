import { apiFetch, apiFetchEnvelope } from "@/lib/api-client";
import { uploadProductImage, abortProductImageUpload } from "../lib/product-image-upload";

export { uploadProductImage, abortProductImageUpload, validateProductImageFile } from "../lib/product-image-upload";

export interface UpdateSellerProductInput {
  title?: string;
  description?: string;
  categoryId?: string;
  designer?: string;
  pricePerDay?: number;
  deposit?: number;
  city?: string;
}

export interface SellerProductInventory {
  productId: string;
  variants: Array<{
    variantId: string;
    size: string;
    quantity: number;
    available: boolean;
    bookedDates: number;
  }>;
}

export interface BulkImportPreview {
  totalRows: number;
  validRows: number;
  errorRows: number;
  rows: Array<{
    rowNumber: number;
    valid: boolean;
    title: string;
    errors: string[];
  }>;
}

export interface BulkImportResult {
  totalRows: number;
  importedCount: number;
  failedCount: number;
  results: Array<{
    rowNumber: number;
    success: boolean;
    productId?: string;
    title: string;
    error?: string;
  }>;
}

export async function updateProduct(productId: string, input: UpdateSellerProductInput) {
  return apiFetch<{ id: string; title: string; status: string }>(
    `/seller/products/${encodeURIComponent(productId)}`,
    {
      method: "PATCH",
      body: JSON.stringify(input),
    },
  );
}

export async function archiveProduct(productId: string) {
  await apiFetch<void>(`/seller/products/${encodeURIComponent(productId)}`, {
    method: "DELETE",
  });
}

export async function unpublishProduct(productId: string) {
  return apiFetch<{ productId: string; status: string; publishedAt: string | null }>(
    `/seller/products/${encodeURIComponent(productId)}/unpublish`,
    { method: "POST", body: "{}" },
  );
}

export async function restoreProduct(productId: string) {
  return apiFetch<{ productId: string; status: string; publishedAt: string | null }>(
    `/seller/products/${encodeURIComponent(productId)}/restore`,
    { method: "POST", body: "{}" },
  );
}

export async function deleteProductImage(productId: string, imageId: string) {
  await apiFetch<void>(
    `/seller/products/${encodeURIComponent(productId)}/images/${encodeURIComponent(imageId)}`,
    { method: "DELETE" },
  );
}

export async function getProductInventory(productId: string): Promise<SellerProductInventory> {
  return apiFetch<SellerProductInventory>(
    `/seller/products/${encodeURIComponent(productId)}/inventory`,
  );
}

export async function updateProductInventory(
  productId: string,
  variants: Array<{ variantId: string; quantity: number }>,
): Promise<SellerProductInventory> {
  return apiFetch<SellerProductInventory>(
    `/seller/products/${encodeURIComponent(productId)}/inventory`,
    {
      method: "PATCH",
      body: JSON.stringify({ variants }),
    },
  );
}

export async function createInventoryBlock(input: {
  productId: string;
  variantId: string;
  startDate: string;
  endDate: string;
  reason?: string;
}) {
  return apiFetch<{
    id: string;
    productId: string;
    variantId: string;
    startDate: string;
    endDate: string;
    reason: string | null;
    status: string;
  }>("/seller/inventory/blocks", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export async function removeInventoryBlock(blockId: string) {
  await apiFetch<void>(`/seller/inventory/blocks/${encodeURIComponent(blockId)}`, {
    method: "DELETE",
  });
}

export async function duplicateProduct(productId: string) {
  return apiFetch<{ productId: string; slug: string; productCode: string; title: string; status: string }>(
    `/seller/products/${encodeURIComponent(productId)}/duplicate`,
    { method: "POST", body: "{}" },
  );
}

export async function previewSellerProduct(productId: string) {
  return apiFetch<unknown>(`/seller/products/${encodeURIComponent(productId)}/preview`);
}

export async function previewBulkImport(csvContent: string): Promise<BulkImportPreview> {
  return apiFetch<BulkImportPreview>("/seller/products/bulk/preview", {
    method: "POST",
    body: JSON.stringify({ csvContent }),
  });
}

export async function importBulkProducts(csvContent: string): Promise<BulkImportResult> {
  return apiFetch<BulkImportResult>("/seller/products/bulk/import", {
    method: "POST",
    body: JSON.stringify({ csvContent }),
  });
}
