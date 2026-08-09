import { apiFetch, apiFetchEnvelope } from "@/lib/api-client";
import { mapSellerProfile, type RawSellerProfile } from "@/lib/api-mappers";
import type { Address, Gender, User } from "@/shared/types";

export interface CreateAddressInput {
  label: string;
  line1: string;
  line2?: string;
  city: string;
  state: string;
  pincode: string;
  phone: string;
  isDefault?: boolean;
}

export type UpdateAddressInput = Partial<CreateAddressInput>;

export interface UpdateProfileInput {
  firstName?: string;
  lastName?: string;
  gender?: Gender;
  email?: string;
  alternatePhone?: string;
  alternateEmail?: string;
}

export async function fetchUserAddresses(): Promise<Address[]> {
  const res = await apiFetchEnvelope<Address[]>("/users/me/addresses");
  return res.data;
}

export async function fetchUserProfile(): Promise<User> {
  const raw = await apiFetch<UserProfileResponse>("/users/me");
  return mapProfileResponse(raw);
}

export async function createAddress(input: CreateAddressInput): Promise<Address> {
  return apiFetch<Address>("/users/me/addresses", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export async function updateAddress(id: string, input: UpdateAddressInput): Promise<Address> {
  return apiFetch<Address>(`/users/me/addresses/${id}`, {
    method: "PATCH",
    body: JSON.stringify(input),
  });
}

export async function deleteAddress(id: string): Promise<void> {
  await apiFetch<void>(`/users/me/addresses/${id}`, { method: "DELETE" });
}

export async function updateProfile(input: UpdateProfileInput): Promise<User> {
  const raw = await apiFetch<UserProfileResponse>("/users/me", {
    method: "PATCH",
    body: JSON.stringify(input),
  });
  return mapProfileResponse(raw);
}

export async function deleteAccount(): Promise<void> {
  await apiFetch<void>("/users/me", { method: "DELETE" });
}

interface UserProfileResponse {
  id: string;
  phone: string;
  phoneVerified: boolean;
  alternatePhone?: string;
  email?: string;
  emailVerified?: boolean;
  alternateEmail?: string;
  firstName: string;
  lastName: string;
  gender: Gender;
  displayName: string;
  avatarUrl?: string | null;
  roles: string[];
  createdAt: string;
  sellerProfile?: RawSellerProfile;
}

function mapProfileResponse(raw: UserProfileResponse): User {
  return {
    id: raw.id,
    phone: raw.phone,
    phoneVerified: raw.phoneVerified,
    alternatePhone: raw.alternatePhone,
    email: raw.email,
    emailVerified: raw.emailVerified,
    alternateEmail: raw.alternateEmail,
    firstName: raw.firstName,
    lastName: raw.lastName,
    gender: raw.gender,
    displayName: raw.displayName,
    avatarUrl: raw.avatarUrl ?? null,
    roles: raw.roles as User["roles"],
    sellerProfile: raw.sellerProfile ? mapSellerProfile(raw.sellerProfile) : undefined,
    createdAt: raw.createdAt,
  };
}
