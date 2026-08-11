import { apiFetch, apiFetchEnvelope } from "@/lib/api-client";

export interface OtpSessionResult {
  otpSessionId: string;
  phone?: string;
  expiresInSeconds: number;
  resendAvailableInSeconds: number;
}

export interface DeleteAccountPreview {
  activeBookings: number;
  canDelete: boolean;
  message: string;
}

export interface PresignedUploadData {
  uploadId: string;
  uploadUrl: string;
  method: string;
  headers?: Record<string, string>;
  formFields?: Record<string, string>;
  publicUrl?: string;
}

export async function fetchDeleteAccountPreview(): Promise<DeleteAccountPreview> {
  return apiFetch<DeleteAccountPreview>("/users/me/delete-preview");
}

export async function initiatePhoneChange(): Promise<OtpSessionResult> {
  return apiFetch<OtpSessionResult>("/users/me/phone-change/initiate", { method: "POST", body: "{}" });
}

export async function verifyOldPhoneOtp(otpSessionId: string, otp: string) {
  await apiFetch("/users/me/phone-change/verify-old", {
    method: "POST",
    body: JSON.stringify({ otpSessionId, otp }),
  });
}

export async function sendNewPhoneOtp(newPhone: string): Promise<OtpSessionResult> {
  return apiFetch<OtpSessionResult>("/users/me/phone-change/send-new", {
    method: "POST",
    body: JSON.stringify({ newPhone }),
  });
}

export async function completePhoneChange(otpSessionId: string, otp: string) {
  await apiFetch("/users/me/phone-change/complete", {
    method: "POST",
    body: JSON.stringify({ otpSessionId, otp }),
  });
}

export async function requestEmailChange(newEmail: string): Promise<OtpSessionResult> {
  return apiFetch<OtpSessionResult>("/users/me/email-change/request", {
    method: "POST",
    body: JSON.stringify({ newEmail }),
  });
}

export async function verifyEmailChange(otpSessionId: string, otp: string) {
  await apiFetch("/users/me/email-change/verify", {
    method: "POST",
    body: JSON.stringify({ otpSessionId, otp }),
  });
}

export async function changePassword(currentPassword: string, newPassword: string, confirmPassword: string) {
  await apiFetch("/users/me/password", {
    method: "POST",
    body: JSON.stringify({ currentPassword, newPassword, confirmPassword }),
  });
}

export async function changeUsername(username: string) {
  await apiFetch("/users/me/username", {
    method: "POST",
    body: JSON.stringify({ username }),
  });
}

export async function createAvatarUploadUrl(fileName: string, contentType: string, fileSize: number) {
  const res = await apiFetchEnvelope<PresignedUploadData>("/users/me/avatar/upload-url", {
    method: "POST",
    body: JSON.stringify({ fileName, contentType, fileSize }),
  });
  return res.data;
}

export async function confirmAvatarUpload(uploadId: string): Promise<string> {
  const res = await apiFetch<{ avatarUrl: string }>("/users/me/avatar/confirm", {
    method: "POST",
    body: JSON.stringify({ uploadId }),
  });
  return res.avatarUrl;
}

export async function removeAvatar() {
  await apiFetch<void>("/users/me/avatar", { method: "DELETE" });
}

export async function resendAccountOtp(otpSessionId: string): Promise<OtpSessionResult> {
  return apiFetch<OtpSessionResult>("/auth/resend-otp", {
    method: "POST",
    body: JSON.stringify({ otpSessionId }),
  });
}
