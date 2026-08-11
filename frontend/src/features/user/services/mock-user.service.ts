import userProfile from "@/mocks/data/user-profile.json";
import { delay } from "@/mocks/utils/delay";
import type { Address, Gender, User } from "@/shared/types";
import type {
  CreateAddressInput,
  UpdateAddressInput,
  UpdateProfileInput,
} from "./api-user.service";

type MockUserProfile = typeof userProfile & {
  alternatePhone?: string;
  alternateEmail?: string;
};

let addresses = [...userProfile.addresses] as Address[];

function mapUser(raw: MockUserProfile): User {
  return {
    ...raw,
    gender: raw.gender as Gender,
    alternatePhone: raw.alternatePhone,
    alternateEmail: raw.alternateEmail,
    roles: raw.roles.map((r) => r.toUpperCase() as User["roles"][number]),
    sellerProfile: raw.sellerProfile
      ? {
          ...raw.sellerProfile,
          verificationStatus: raw.sellerProfile.verificationStatus.toUpperCase() as "VERIFIED",
        }
      : undefined,
    addresses,
  };
}

export async function fetchUserProfile(): Promise<User> {
  await delay(150);
  return mapUser(userProfile as MockUserProfile);
}

export async function fetchUserAddresses(): Promise<Address[]> {
  await delay(150);
  return addresses;
}

export async function createAddress(input: CreateAddressInput): Promise<Address> {
  await delay(300);
  const address: Address = {
    id: `addr_${Date.now()}`,
    label: input.label,
    line1: input.line1,
    line2: input.line2,
    city: input.city,
    state: input.state,
    pincode: input.pincode,
    phone: input.phone,
    isDefault: input.isDefault ?? addresses.length === 0,
    serviceable: true,
  };
  if (address.isDefault) {
    addresses = addresses.map((a) => ({ ...a, isDefault: false }));
  }
  addresses = [...addresses, address];
  return address;
}

export async function updateAddress(id: string, input: UpdateAddressInput): Promise<Address> {
  await delay(300);
  const index = addresses.findIndex((a) => a.id === id);
  if (index === -1) throw new Error("Address not found");
  const updated = { ...addresses[index], ...input };
  if (input.isDefault) {
    addresses = addresses.map((a) => ({ ...a, isDefault: a.id === id }));
  } else {
    addresses[index] = updated;
  }
  return updated;
}

export async function deleteAddress(id: string): Promise<void> {
  await delay(300);
  addresses = addresses.filter((a) => a.id !== id);
}

export async function updateProfile(input: UpdateProfileInput): Promise<User> {
  await delay(300);
  Object.assign(userProfile, {
    firstName: input.firstName ?? userProfile.firstName,
    lastName: input.lastName ?? userProfile.lastName,
    gender: input.gender ?? userProfile.gender,
    displayName:
      input.firstName && input.lastName
        ? `${input.firstName} ${input.lastName.charAt(0)}.`
        : userProfile.displayName,
    alternatePhone: input.alternatePhone ?? userProfile.alternatePhone,
    alternateEmail: input.alternateEmail ?? userProfile.alternateEmail,
  });
  return mapUser(userProfile as MockUserProfile);
}

export async function deleteAccount(): Promise<void> {
  await delay(400);
  addresses = [];
}
