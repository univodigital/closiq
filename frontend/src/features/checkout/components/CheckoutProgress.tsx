"use client";

import Link from "next/link";
import { cn } from "@/lib/utils";
import { ROUTES } from "@/shared/constants/routes";

export type CheckoutStep = "bag" | "address" | "payment";

const STEPS: { id: CheckoutStep; label: string; href: string }[] = [
  { id: "bag", label: "Bag", href: ROUTES.checkout.bag },
  { id: "address", label: "Address", href: ROUTES.checkout.address },
  { id: "payment", label: "Payment", href: ROUTES.checkout.payment },
];

export function CheckoutProgress({
  current,
  queryString,
}: {
  current: CheckoutStep;
  queryString: string;
}) {
  const currentIndex = STEPS.findIndex((s) => s.id === current);

  return (
    <nav
      aria-label="Checkout progress"
      className="flex min-w-0 shrink items-center justify-end gap-1.5 sm:gap-2 md:gap-4"
    >
      {STEPS.map((step, index) => {
        const done = index < currentIndex;
        const active = index === currentIndex;
        const href = `${step.href}${queryString ? `?${queryString}` : ""}`;
        const canNavigate = done;

        return (
          <div key={step.id} className="flex items-center gap-2 md:gap-4">
            {index > 0 && <span className="hidden h-px w-6 bg-border md:block md:w-10" aria-hidden />}
            {canNavigate ? (
              <Link
                href={href}
                className={cn(
                  "label-caps flex items-center gap-2 text-muted-foreground transition-colors hover:text-foreground",
                  done && "text-success",
                )}
              >
                <span className="flex h-6 w-6 items-center justify-center rounded-full border border-current text-xs">
                  ✓
                </span>
                {step.label}
              </Link>
            ) : (
              <span
                className={cn(
                  "label-caps flex items-center gap-2",
                  active ? "font-medium text-foreground" : "text-muted-foreground",
                )}
                aria-current={active ? "step" : undefined}
              >
                <span
                  className={cn(
                    "flex h-6 w-6 items-center justify-center rounded-full border text-xs",
                    active ? "border-accent bg-accent text-accent-foreground" : "border-border",
                  )}
                >
                  {index + 1}
                </span>
                {step.label}
              </span>
            )}
          </div>
        );
      })}
    </nav>
  );
}
