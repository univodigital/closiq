import { apiFetchEnvelope } from "@/lib/api-client";
import type { ProductImageAttachData } from "../services/seller.service";

const ALLOWED_IMAGE_TYPES = new Set(["image/jpeg", "image/png", "image/webp"]);
const MAX_IMAGE_BYTES = 10 * 1024 * 1024;

export function validateProductImageFile(file: File): string | null {
  if (!ALLOWED_IMAGE_TYPES.has(file.type)) {
    return "Use JPEG, PNG, or WebP images";
  }
  if (file.size > MAX_IMAGE_BYTES) {
    return "Each image must be 10 MB or smaller";
  }
  return null;
}

export async function uploadProductImage(
  productId: string,
  file: File,
  sortOrder: number,
): Promise<ProductImageAttachData> {
  const validationError = validateProductImageFile(file);
  if (validationError) {
    throw new Error(validationError);
  }

  const formData = new FormData();
  formData.append("file", file);
  formData.append("sortOrder", String(sortOrder));
  formData.append("alt", file.name.replace(/\.[^.]+$/, ""));

  const confirmed = await apiFetchEnvelope<ProductImageAttachData>(
    `/seller/products/${encodeURIComponent(productId)}/images/upload`,
    {
      method: "POST",
      body: formData,
    },
  );
  return confirmed.data;
}

/** @deprecated Legacy two-step direct-to-Cloudinary flow; kept for compatibility. */
export async function abortProductImageUpload(_productId: string, _uploadId: string): Promise<void> {
  // No-op: server-side upload handles cleanup internally.
}
