"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import {
  getProductInventory,
  updateProductInventory,
} from "@/features/seller/services/seller-product-management.service";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ApiError } from "@/lib/api-client";
import type { SellerListingVariant } from "../types";

export function ProductStockEditor({
  productId,
  variants,
  onUpdated,
}: {
  productId: string;
  variants: SellerListingVariant[];
  onUpdated: () => void;
}) {
  const [quantities, setQuantities] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const variantKey = variants.map((v) => `${v.id}:${v.availableQuantity}`).join("|");

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      try {
        const inventory = await getProductInventory(productId);
        if (cancelled) return;
        const next: Record<string, string> = {};
        for (const row of inventory.variants) {
          next[row.variantId] = String(row.quantity);
        }
        setQuantities(next);
      } catch {
        if (!cancelled) {
          const fallback: Record<string, string> = {};
          for (const variant of variants) {
            fallback[variant.id] = String(variant.availableQuantity);
          }
          setQuantities(fallback);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    void load();
    return () => {
      cancelled = true;
    };
  }, [productId, variantKey, variants]);

  async function handleSave() {
    setSaving(true);
    try {
      await updateProductInventory(
        productId,
        variants.map((variant) => ({
          variantId: variant.id,
          quantity: Math.max(0, Number(quantities[variant.id]) || 0),
        })),
      );
      toast.success("Stock updated");
      onUpdated();
    } catch (error) {
      toast.error(error instanceof ApiError ? error.message : "Could not update stock");
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return <p className="text-sm text-muted-foreground">Loading stock…</p>;
  }

  return (
    <div className="space-y-3">
      {variants.map((variant) => (
        <div key={variant.id} className="flex items-center gap-3">
          <span className="w-12 text-sm font-medium">{variant.size}</span>
          <Input
            type="number"
            min={0}
            className="max-w-[120px]"
            value={quantities[variant.id] ?? "0"}
            onChange={(event) =>
              setQuantities((current) => ({
                ...current,
                [variant.id]: event.target.value,
              }))
            }
          />
          <span className="text-xs text-muted-foreground">units available</span>
        </div>
      ))}
      <Button type="button" variant="outline" size="sm" disabled={saving} onClick={() => void handleSave()}>
        {saving ? "Saving…" : "Save stock"}
      </Button>
    </div>
  );
}
