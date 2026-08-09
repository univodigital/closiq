import { apiFetch, ApiError } from "@/lib/api-client";

export type SellerBusinessType =
  | "INDIVIDUAL"
  | "PROPRIETORSHIP"
  | "PARTNERSHIP"
  | "PRIVATE_LIMITED";

export type SellerApplicationStatus =
  | "DRAFT"
  | "PENDING"
  | "UNDER_REVIEW"
  | "VERIFIED"
  | "REJECTED"
  | "SUSPENDED";

export interface SubmitSellerApplicationInput {
  businessName: string;
  businessType: SellerBusinessType;
  city: string;
  description?: string;
  gstNumber?: string;
  panNumber: string;
}

export interface SellerApplicationDetail {
  applicationId: string;
  status: SellerApplicationStatus;
  businessName: string;
  submittedAt: string;
  reviewedAt?: string | null;
  rejectionReason?: string | null;
  documents: Array<{
    type: string;
    status: string;
    uploadedAt: string;
  }>;
}

export interface SellerApplicationSubmitResult {
  applicationId: string;
  status: SellerApplicationStatus;
  submittedAt: string;
}

export async function fetchMySellerApplication(): Promise<SellerApplicationDetail | null> {
  try {
    return await apiFetch<SellerApplicationDetail>("/seller/applications/me");
  } catch (err) {
    if (err instanceof ApiError && err.status === 404) {
      return null;
    }
    throw err;
  }
}

export async function submitSellerApplication(
  input: SubmitSellerApplicationInput,
): Promise<SellerApplicationSubmitResult> {
  return apiFetch<SellerApplicationSubmitResult>("/seller/applications", {
    method: "POST",
    body: JSON.stringify({
      businessName: input.businessName.trim(),
      businessType: input.businessType,
      city: input.city.trim(),
      description: input.description?.trim() || undefined,
      gstNumber: input.gstNumber?.trim().toUpperCase() || undefined,
      panNumber: input.panNumber.trim().toUpperCase(),
    }),
  });
}
