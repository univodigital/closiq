"use client";

import {
  LISTING_AVAILABILITY_MESSAGES,
  type ListingDateAvailability,
} from "@/features/products/constants/listing-availability";
import { cn } from "@/lib/utils";

export function ProductListingAvailability({
  status,
  onChangeDates,
  className,
}: {
  status: ListingDateAvailability;
  onChangeDates?: () => void;
  className?: string;
}) {
  const isAvailable = status === "available";

  return (
    <div className={cn("space-y-1", className)}>
      <p
        className={cn(
          "text-sm font-medium",
          isAvailable ? "text-success" : "text-destructive",
        )}
      >
        {isAvailable
          ? `✓ ${LISTING_AVAILABILITY_MESSAGES.available}`
          : LISTING_AVAILABILITY_MESSAGES.unavailable}
      </p>
      {!isAvailable && onChangeDates && (
        <button
          type="button"
          onClick={(e) => {
            e.preventDefault();
            onChangeDates();
          }}
          className="text-sm text-accent underline-offset-2 hover:underline"
        >
          Change dates
        </button>
      )}
    </div>
  );
}
