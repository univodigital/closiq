"use client";

import { earliestRentalStartDate } from "@/features/checkout/utils/rental-dates";
import { cn } from "@/lib/utils";

type RentalDateFieldsProps = {
  start: string;
  end: string;
  onStartChange: (value: string) => void;
  onEndChange: (value: string) => void;
  minStart?: string;
  error?: string | null;
  required?: boolean;
  className?: string;
};

export function RentalDateFields({
  start,
  end,
  onStartChange,
  onEndChange,
  minStart,
  error,
  required = true,
  className,
}: RentalDateFieldsProps) {
  const today = minStart ?? earliestRentalStartDate();
  const minEnd = start || today;

  return (
    <div className={className}>
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="label-caps mb-2 block text-muted-foreground">
            Delivery{required ? " *" : ""}
          </label>
          <input
            type="date"
            required={required}
            min={today}
            value={start}
            onChange={(e) => onStartChange(e.target.value)}
            className={cn(
              "h-11 w-full rounded-sm border bg-background px-3 text-sm",
              error && !start ? "border-destructive" : "border-border",
            )}
          />
        </div>
        <div>
          <label className="label-caps mb-2 block text-muted-foreground">
            Return{required ? " *" : ""}
          </label>
          <input
            type="date"
            required={required}
            min={minEnd}
            value={end}
            onChange={(e) => onEndChange(e.target.value)}
            className={cn(
              "h-11 w-full rounded-sm border bg-background px-3 text-sm",
              error && !end ? "border-destructive" : "border-border",
            )}
          />
        </div>
      </div>
      {error && <p className="mt-2 text-sm text-destructive">{error}</p>}
    </div>
  );
}
