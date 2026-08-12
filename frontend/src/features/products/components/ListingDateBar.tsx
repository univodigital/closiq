"use client";

import { useRentalDates } from "@/providers/RentalDatesProvider";
import { RentalDateFields } from "@/features/checkout/components/RentalDateFields";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

/** Shared date picker for product listing surfaces. */
export function ListingDateBar({ className }: { className?: string }) {
  const { dates, setDates, clearDates, dateError } = useRentalDates();

  return (
    <div className={cn("min-w-0 overflow-hidden", className)} id="listing-date-bar">
      <p className="label-caps mb-3 text-muted-foreground">Your rental dates</p>
      <RentalDateFields
        start={dates?.start ?? ""}
        end={dates?.end ?? ""}
        onStartChange={(start) => setDates(start, dates?.end && start <= dates.end ? dates.end : "")}
        onEndChange={(end) => dates?.start && setDates(dates.start, end)}
        error={dateError}
      />
      {dates && (
        <Button type="button" variant="ghost" size="sm" className="mt-2" onClick={clearDates}>
          Clear dates
        </Button>
      )}
      <p className="mt-2 text-xs text-muted-foreground">
        Select dates to see which pieces are available for your rental period.
      </p>
    </div>
  );
}
