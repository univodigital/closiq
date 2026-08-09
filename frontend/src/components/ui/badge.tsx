import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const badgeVariants = cva(
  "inline-flex items-center rounded-full px-2.5 py-0.5 text-[10px] font-medium uppercase tracking-wider",
  {
    variants: {
      variant: {
        default: "bg-muted text-muted-foreground",
        trending: "bg-gold-light text-gold-deep",
        trial: "bg-gold-light text-gold-deep",
        lowStock: "bg-destructive/10 text-destructive",
        success: "bg-success/10 text-success",
      },
    },
    defaultVariants: { variant: "default" },
  },
);

export interface BadgeProps
  extends React.HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof badgeVariants> {}

export function Badge({ className, variant, ...props }: BadgeProps) {
  return <span className={cn(badgeVariants({ variant }), className)} {...props} />;
}

export function StatusBadge({ status }: { status: string }) {
  const map: Record<string, { label: string; className: string }> = {
    trial_ready: { label: "Trial ready", className: "bg-gold-light text-gold-deep" },
    out_for_delivery: { label: "In transit", className: "bg-muted text-muted-foreground" },
    rental_active: { label: "Rental active", className: "bg-success/10 text-success" },
    return_scheduled: { label: "Return scheduled", className: "bg-muted text-muted-foreground" },
    deposit_refunded: { label: "Completed", className: "bg-success/10 text-success" },
    confirmed: { label: "Confirmed", className: "bg-success/10 text-success" },
    cancelled: { label: "Cancelled", className: "bg-destructive/10 text-destructive" },
  };
  const item = map[status] ?? { label: status, className: "bg-muted text-muted-foreground" };
  return (
    <span className={cn("inline-flex rounded-full px-2.5 py-1 text-[10px] uppercase tracking-wider", item.className)}>
      {item.label}
    </span>
  );
}
