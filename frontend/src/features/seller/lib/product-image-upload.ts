import { apiFetchEnvelope } from "@/lib/api-client";
import type { PresignedUploadData, ProductImageAttachData } from "../services/seller.service";

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

async function uploadBinary(
  instruction: PresignedUploadData,
  file: File,
): Promise<void> {
  if (instruction.method === "POST" && instruction.formFields) {
    const formData = new FormData();
    for (const [key, value] of Object.entries(instruction.formFields)) {
      formData.append(key, value);
    }
    formData.append("file", file);
    const response = await fetch(instruction.uploadUrl, { method: "POST", body: formData });
    if (!response.ok) {
      throw new Error("Image upload failed");
    }
    return;
  }

  // Storage stub mode: backend returns a predetermined public URL without a real upload target.
  if (instruction.method === "POST" && instruction.publicUrl) {
    return;
  }

  const headers: Record<string, string> = { ...(instruction.headers ?? {}) };
  if (!headers["Content-Type"]) {
    headers["Content-Type"] = file.type || "image/jpeg";
  }

  const response = await fetch(instruction.uploadUrl, {
    method: instruction.method || "PUT",
    headers,
    body: file,
  });
  if (!response.ok) {
    throw new Error("Image upload failed");
  }
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

  const presigned = await apiFetchEnvelope<PresignedUploadData>(
    `/seller/products/${encodeURIComponent(productId)}/images/upload-url`,
    {
      method: "POST",
      body: JSON.stringify({
        contentType: file.type || "image/jpeg",
        fileName: file.name,
      }),
    },
  );

  await uploadBinary(presigned.data, file);

  const confirmed = await apiFetchEnvelope<ProductImageAttachData>(
    `/seller/products/${encodeURIComponent(productId)}/images`,
    {
      method: "POST",
      body: JSON.stringify({
        uploadId: presigned.data.uploadId,
        sortOrder,
        alt: file.name.replace(/\.[^.]+$/, ""),
      }),
    },
  );

  return confirmed.data;
}
