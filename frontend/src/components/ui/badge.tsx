import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const badgeVariants = cva(
  "inline-flex items-center rounded-full px-2.5 py-0.5 text-[10px] font-medium uppercase tracking-wider",
  {
    variants: {
      variant: {
        default: "bg-muted text-muted-foreground",
        trending: "bg-champagne-muted text-navy-dark",
        trial: "bg-gold-light text-accent",
        rent: "bg-gold-light text-accent",
        buy: "bg-muted text-primary",
        lowStock: "bg-error-muted text-destructive",
        success: "bg-success-muted text-success",
        warning: "bg-warning-muted text-navy-dark",
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
    pending_payment: { label: "Payment pending", className: "bg-warning-muted text-navy-dark" },
    payment_failed: { label: "Payment failed", className: "bg-error-muted text-destructive" },
    refund_pending: { label: "Refund processing", className: "bg-warning-muted text-navy-dark" },
    partially_refunded: { label: "Partially refunded", className: "bg-muted text-muted-foreground" },
    refunded: { label: "Refunded", className: "bg-success-muted text-success" },
    inspection: { label: "Inspection", className: "bg-muted text-muted-foreground" },
    trial_ready: { label: "Trial ready", className: "bg-gold-light text-accent" },
    trial_rejected: { label: "Trial rejected", className: "bg-muted text-muted-foreground" },
    out_for_delivery: { label: "In transit", className: "bg-muted text-muted-foreground" },
    rental_active: { label: "Rental active", className: "bg-success-muted text-success" },
    return_scheduled: { label: "Return scheduled", className: "bg-muted text-muted-foreground" },
    deposit_refunded: { label: "Completed", className: "bg-success-muted text-success" },
    confirmed: { label: "Confirmed", className: "bg-success-muted text-success" },
    cancelled: { label: "Cancelled", className: "bg-error-muted text-destructive" },
    draft: { label: "Draft", className: "bg-muted text-muted-foreground" },
    active: { label: "Live", className: "bg-success-muted text-success" },
    archived: { label: "Archived", className: "bg-muted text-muted-foreground" },
    pending: { label: "Pending review", className: "bg-warning-muted text-navy-dark" },
    under_review: { label: "Under review", className: "bg-warning-muted text-navy-dark" },
    verified: { label: "Approved", className: "bg-success-muted text-success" },
    rejected: { label: "Rejected", className: "bg-error-muted text-destructive" },
    suspended: { label: "Suspended", className: "bg-error-muted text-destructive" },
  };
  const item = map[status] ?? { label: status, className: "bg-muted text-muted-foreground" };
  return (
    <span className={cn("inline-flex rounded-full px-2.5 py-1 text-[10px] uppercase tracking-wider", item.className)}>
      {item.label}
    </span>
  );
}
