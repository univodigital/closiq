export const LISTING_AVAILABILITY_MESSAGES = {
  available: "Available for your dates",
  unavailable: "Not available for your dates",
} as const;

export type ListingDateAvailability = "available" | "unavailable";
