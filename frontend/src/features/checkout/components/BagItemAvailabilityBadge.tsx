import { cn } from "@/lib/utils";
import type { BagAvailabilityStatus } from "@/features/checkout/utils/bag-availability";
import { availabilityStatusLabel } from "@/features/checkout/utils/bag-availability";

export function BagItemAvailabilityBadge({
  status,
  message,
  className,
}: {
  status: BagAvailabilityStatus;
  message?: string | null;
  className?: string;
}) {
  const label = availabilityStatusLabel(status);

  return (
    <div className={cn("mt-2 space-y-1", className)}>
      <p
        className={cn(
          "text-sm font-medium",
          status === "available" ? "text-success" : "text-destructive",
        )}
      >
        {status === "available" ? `${label} ✓` : label}
      </p>
      {message && status !== "available" && (
        <p className="text-sm text-muted-foreground">{message}</p>
      )}
    </div>
  );
}
