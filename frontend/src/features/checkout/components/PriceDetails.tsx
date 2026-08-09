"use client";

import { formatCurrency } from "@/lib/format";
import type { CheckoutSummary } from "@/shared/types";

export function PriceDetails({
  pricing,
  title = "Price details",
}: {
  pricing?: CheckoutSummary;
  title?: string;
}) {
  if (!pricing) {
    return (
      <div className="rounded-sm border border-border p-5">
        <p className="label-caps text-muted-foreground">{title}</p>
        <p className="mt-3 text-sm text-muted-foreground">Calculating…</p>
      </div>
    );
  }

  return (
    <div className="rounded-sm border border-border p-5">
      <p className="label-caps text-muted-foreground">{title}</p>
      <dl className="mt-4 space-y-2.5 text-sm">
        {pricing.lineItems.map((item, index) => (
          <div key={`${item.type}-${item.label}-${index}`} className="flex justify-between gap-4">
            <dt className="text-muted-foreground">{item.label}</dt>
            <dd className={item.amount < 0 ? "font-mono text-success" : "font-mono"}>
              {item.amount < 0 ? "−" : ""}
              {formatCurrency(Math.abs(item.amount))}
            </dd>
          </div>
        ))}
        <div className="flex justify-between gap-4 border-t border-border pt-3 font-medium">
          <dt>Total amount</dt>
          <dd className="font-mono">{formatCurrency(pricing.payNowAmount)}</dd>
        </div>
        {pricing.discountAmount > 0 && (
          <p className="text-xs text-success">
            You save {formatCurrency(pricing.discountAmount)} on this rental
          </p>
        )}
      </dl>
      <p className="mt-3 text-xs text-muted-foreground">
        Includes refundable deposit · 15-minute home trial
      </p>
    </div>
  );
}
