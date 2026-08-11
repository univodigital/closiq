import type { PresignedUploadData } from "@/features/user/services/account-security.service";

const ALLOWED = new Set(["image/jpeg", "image/png", "image/webp"]);
const MAX_BYTES = 5 * 1024 * 1024;

export function validateAvatarFile(file: File): string | null {
  if (!ALLOWED.has(file.type)) return "Use JPEG, PNG, or WebP images";
  if (file.size > MAX_BYTES) return "Image must be 5 MB or smaller";
  return null;
}

export async function uploadAvatarBinary(instruction: PresignedUploadData, file: File) {
  if (instruction.method === "POST" && instruction.formFields) {
    const formData = new FormData();
    for (const [key, value] of Object.entries(instruction.formFields)) {
      formData.append(key, value);
    }
    formData.append("file", file);
    const response = await fetch(instruction.uploadUrl, { method: "POST", body: formData });
    if (!response.ok) throw new Error("Avatar upload failed");
    return;
  }
  if (instruction.method === "POST" && instruction.publicUrl) return;

  const headers: Record<string, string> = { ...(instruction.headers ?? {}) };
  if (!headers["Content-Type"]) headers["Content-Type"] = file.type;
  const response = await fetch(instruction.uploadUrl, {
    method: instruction.method || "PUT",
    headers,
    body: file,
  });
  if (!response.ok) throw new Error("Avatar upload failed");
}
