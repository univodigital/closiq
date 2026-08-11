"use client";

import Link from "next/link";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { toast } from "sonner";
import { sellerService } from "@/features/seller/services";
import {
  createInventoryBlock,
  removeInventoryBlock,
} from "@/features/seller/services/seller-product-management.service";
import { PageHeader } from "@/shared/components/layout/Container";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { ROUTES } from "@/shared/constants/routes";
import { ApiError } from "@/lib/api-client";

export default function SellerInventoryPage() {
  const queryClient = useQueryClient();
  const [productId, setProductId] = useState("");
  const [variantId, setVariantId] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [removingId, setRemovingId] = useState<string | null>(null);

  const productsQuery = useQuery({
    queryKey: ["seller", "products"],
    queryFn: () => sellerService.listProducts(),
  });

  const blocksQuery = useQuery({
    queryKey: ["seller", "inventory", "blocks"],
    queryFn: () => sellerService.listInventoryBlocks(),
  });

  const products = productsQuery.data?.data ?? [];
  const blocks = blocksQuery.data?.data ?? [];

  const selectedProduct = useMemo(
    () => products.find((listing) => listing.id === productId),
    [products, productId],
  );

  const { data: productDetail } = useQuery({
    queryKey: ["seller", "products", productId, "variants-for-block"],
    queryFn: () => sellerService.getProduct(productId),
    enabled: !!productId,
  });

  const variants = productDetail?.data.variants ?? [];

  async function handleCreateBlock(event: React.FormEvent) {
    event.preventDefault();
    if (!productId || !variantId || !startDate || !endDate) {
      toast.error("Select listing, size, and dates");
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
      void queryClient.invalidateQueries({ queryKey: ["seller", "inventory", "blocks"] });
    } catch (error) {
      toast.error(error instanceof ApiError ? error.message : "Could not block dates");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleRemoveBlock(blockId: string) {
    if (!window.confirm("Remove this availability block?")) return;
    setRemovingId(blockId);
    try {
      await removeInventoryBlock(blockId);
      toast.success("Block removed");
      void queryClient.invalidateQueries({ queryKey: ["seller", "inventory", "blocks"] });
    } catch (error) {
      toast.error(error instanceof ApiError ? error.message : "Could not remove block");
    } finally {
      setRemovingId(null);
    }
  }

  return (
    <div className="space-y-8">
      <PageHeader
        title="Inventory calendar"
        description="Block dates for cleaning, repairs, or when a size is unavailable."
      />

      <Card>
        <CardContent className="space-y-4 p-6">
          <p className="label-caps text-muted-foreground">Block dates</p>
          <form onSubmit={(event) => void handleCreateBlock(event)} className="grid gap-4 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <label className="label-caps mb-2 block text-muted-foreground">Listing</label>
              <select
                required
                value={productId}
                onChange={(event) => {
                  setProductId(event.target.value);
                  setVariantId("");
                }}
                className="flex h-10 w-full rounded-sm border border-input bg-background px-3 py-2 text-sm"
              >
                <option value="">Select listing</option>
                {products.map((listing) => (
                  <option key={listing.id} value={listing.id}>
                    {listing.title} ({listing.status.toLowerCase()})
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="label-caps mb-2 block text-muted-foreground">Size</label>
              <select
                required
                value={variantId}
                disabled={!productId || variants.length === 0}
                onChange={(event) => setVariantId(event.target.value)}
                className="flex h-10 w-full rounded-sm border border-input bg-background px-3 py-2 text-sm disabled:opacity-60"
              >
                <option value="">Select size</option>
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
                placeholder="Maintenance"
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
            <div className="sm:col-span-2">
              <Button type="submit" variant="primary" size="sm" disabled={submitting || !productId}>
                {submitting ? "Saving…" : "Block dates"}
              </Button>
              {selectedProduct && (
                <Button asChild variant="link" size="sm" className="ml-3">
                  <Link href={ROUTES.seller.product(selectedProduct.id)}>Open listing</Link>
                </Button>
              )}
            </div>
          </form>
        </CardContent>
      </Card>

      {blocksQuery.isLoading ? (
        <p className="text-muted-foreground">Loading blocks…</p>
      ) : blocks.length === 0 ? (
        <p className="text-sm text-muted-foreground">No blocked dates yet.</p>
      ) : (
        <div className="space-y-3">
          {blocks.map((block) => (
            <Card key={block.id}>
              <CardContent className="flex flex-wrap items-start justify-between gap-3 p-4 text-sm">
                <div>
                  <Link href={ROUTES.seller.product(block.productId)} className="font-medium hover:underline">
                    {block.productTitle}
                  </Link>
                  <p className="text-muted-foreground">
                    {block.variantSize} · {block.startDate} → {block.endDate}
                  </p>
                  {block.reason && <p className="mt-1 text-muted-foreground">{block.reason}</p>}
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={removingId === block.id}
                  onClick={() => void handleRemoveBlock(block.id)}
                >
                  {removingId === block.id ? "Removing…" : "Remove"}
                </Button>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
