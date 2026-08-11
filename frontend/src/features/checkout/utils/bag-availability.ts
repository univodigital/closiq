import { availabilityService } from "@/features/orders/services";
import type { BagItem } from "@/features/checkout/bag/bag-store";
import { rentalDatesError, validateRentalDates } from "@/features/checkout/utils/rental-dates";
import type { BagLine } from "@/features/checkout/utils/bag-pricing";

export type BagAvailabilityStatus = "available" | "unavailable" | "invalid_dates" | "error";

export interface BagLineAvailability {
  slug: string;
  status: BagAvailabilityStatus;
  message: string | null;
  item: BagItem;
  productTitle: string;
}

/** Revalidate bag lines using the same availability API + rules as the product page. */
export async function validateBagLinesAvailability(lines: BagLine[]): Promise<BagLineAvailability[]> {
  return Promise.all(lines.map(validateBagLineAvailability));
}

export async function validateBagLineAvailability(line: BagLine): Promise<BagLineAvailability> {
  const { item, product, variantId } = line;
  const base = {
    slug: item.slug,
    item,
    productTitle: product.title,
  };

  const dateError = validateRentalDates(item.start, item.end);
  if (dateError) {
    return { ...base, status: "invalid_dates", message: dateError };
  }

  try {
    const res = await availabilityService.getAvailability(item.slug, variantId, {
      startDate: item.start,
      endDate: item.end,
    });
    const limits = {
      minRentalDays: product.minRentalDays,
      maxRentalDays: product.maxRentalDays,
    };
    const availabilityError = rentalDatesError(res.data, item.start, item.end, limits);
    if (availabilityError) {
      return {
        ...base,
        status: "unavailable",
        message: availabilityError.includes("aren't available")
          ? "This item is no longer available for your selected dates."
          : availabilityError,
      };
    }
    return { ...base, status: "available", message: null };
  } catch {
    return {
      ...base,
      status: "error",
      message: "Could not verify availability. Try again in a moment.",
    };
  }
}

export function hasBlockingAvailabilityIssues(results: BagLineAvailability[]): boolean {
  return results.some((r) => r.status !== "available");
}

export function unavailableItems(results: BagLineAvailability[]): BagLineAvailability[] {
  return results.filter((r) => r.status !== "available");
}

export function availabilityStatusLabel(status: BagAvailabilityStatus): string {
  switch (status) {
    case "available":
      return "Available";
    case "unavailable":
      return "Not available for selected dates";
    case "invalid_dates":
      return "Invalid rental dates";
    case "error":
      return "Could not verify availability";
  }
}
