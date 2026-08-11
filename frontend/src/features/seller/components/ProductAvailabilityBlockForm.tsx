"use client";

import { useState } from "react";
import { toast } from "sonner";
import { createInventoryBlock } from "@/features/seller/services/seller-product-management.service";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ApiError } from "@/lib/api-client";
import type { SellerListingVariant } from "../types";

export function ProductAvailabilityBlockForm({
  productId,
  variants,
  onCreated,
}: {
  productId: string;
  variants: SellerListingVariant[];
  onCreated: () => void;
}) {
  const [variantId, setVariantId] = useState(variants[0]?.id ?? "");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!variantId || !startDate || !endDate) {
      toast.error("Select size and date range");
      return;
    }
    if (endDate < startDate) {
      toast.error("End date must be on or after start date");
      return;
    }

    setSubmitting(true);
    try {
      await createInventoryBlock({
        productId,
        variantId,
        startDate,
        endDate,
        reason: reason.trim() || undefined,
      });
      toast.success("Dates blocked");
      setStartDate("");
      setEndDate("");
      setReason("");
      onCreated();
    } catch (error) {
      toast.error(error instanceof ApiError ? error.message : "Could not block dates");
    } finally {
      setSubmitting(false);
    }
  }

  if (variants.length === 0) {
    return null;
  }

  return (
    <form onSubmit={(event) => void handleSubmit(event)} className="space-y-3">
      <p className="text-xs text-muted-foreground">
        Block dates when this size is unavailable (cleaning, repairs, personal use).
      </p>
      <div className="grid gap-3 sm:grid-cols-2">
        <div>
          <label className="label-caps mb-2 block text-muted-foreground">Size</label>
          <select
            required
            value={variantId}
            onChange={(event) => setVariantId(event.target.value)}
            className="flex h-10 w-full rounded-sm border border-input bg-background px-3 py-2 text-sm"
          >
            {variants.map((variant) => (
              <option key={variant.id} value={variant.id}>
                {variant.size}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="label-caps mb-2 block text-muted-foreground">Reason (optional)</label>
          <Input
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            placeholder="Dry cleaning"
            maxLength={200}
          />
        </div>
        <div>
          <label className="label-caps mb-2 block text-muted-foreground">From</label>
          <Input required type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} />
        </div>
        <div>
          <label className="label-caps mb-2 block text-muted-foreground">To</label>
          <Input required type="date" value={endDate} onChange={(event) => setEndDate(event.target.value)} />
        </div>
      </div>
      <Button type="submit" variant="outline" size="sm" disabled={submitting}>
        {submitting ? "Blocking…" : "Block dates"}
      </Button>
    </form>
  );
}
