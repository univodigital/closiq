"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { ProductImageUpload } from "@/features/seller/components/ProductImageUpload";
import { listingImagesFromProduct } from "@/features/seller/lib/listing-images";
import { ProductAvailabilityBlockForm } from "@/features/seller/components/ProductAvailabilityBlockForm";
import { ProductStockEditor } from "@/features/seller/components/ProductStockEditor";
import {
  archiveProduct,
  duplicateProduct,
  restoreProduct,
  unpublishProduct,
} from "@/features/seller/services/seller-product-management.service";
import { sellerService } from "@/features/seller/services";
import { PageHeader } from "@/shared/components/layout/Container";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { StatusBadge } from "@/components/ui/badge";
import { formatCurrency } from "@/lib/format";
import { ROUTES } from "@/shared/constants/routes";
import { ApiError } from "@/lib/api-client";

export default function SellerProductDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const queryClient = useQueryClient();
  const productId = params.id;
  const [publishing, setPublishing] = useState(false);
  const [duplicating, setDuplicating] = useState(false);
  const [unpublishing, setUnpublishing] = useState(false);
  const [archiving, setArchiving] = useState(false);
  const [restoring, setRestoring] = useState(false);

  useEffect(() => {
    if (productId === "new") {
      router.replace(ROUTES.seller.productNew);
    }
  }, [productId, router]);

  const { data, isLoading, error } = useQuery({
    queryKey: ["seller", "products", productId],
    queryFn: () => sellerService.getProduct(productId),
    enabled: !!productId && productId !== "new",
  });

  const blocksQuery = useQuery({
    queryKey: ["seller", "inventory", "blocks"],
    queryFn: () => sellerService.listInventoryBlocks(),
  });

  const product = data?.data;
  const isDraft = product?.status === "DRAFT";
  const isActive = product?.status === "ACTIVE";
  const isArchived = product?.status === "ARCHIVED";
  const productBlocks =
    blocksQuery.data?.data.filter((block) => block.productId === product?.id) ?? [];

  async function handlePublish() {
    if (!product) return;

    if (product.imageUrls.length < 1) {
      toast.error("Add at least one photo before publishing");
      return;
    }

    setPublishing(true);
    try {
      await sellerService.publishProduct(product.id);
      toast.success("Listing published");
      await refreshProduct();
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Could not publish listing");
    } finally {
      setPublishing(false);
    }
  }

  function refreshProduct() {
    void queryClient.invalidateQueries({ queryKey: ["seller", "products", productId] });
    void queryClient.invalidateQueries({ queryKey: ["seller", "products"] });
  }

  function refreshBlocks() {
    void queryClient.invalidateQueries({ queryKey: ["seller", "inventory", "blocks"] });
  }

  async function handleDuplicate() {
    if (!product) return;
    setDuplicating(true);
    try {
      const duplicate = await duplicateProduct(product.id);
      toast.success("Draft duplicate created");
      router.push(ROUTES.seller.product(duplicate.productId));
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Could not duplicate listing");
    } finally {
      setDuplicating(false);
    }
  }

  async function handleUnpublish() {
    if (!product) return;
    if (!window.confirm("Unpublish this listing? It will be hidden from customers until you publish again.")) {
      return;
    }
    setUnpublishing(true);
    try {
      await unpublishProduct(product.id);
      toast.success("Listing unpublished");
      await refreshProduct();
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Could not unpublish listing");
    } finally {
      setUnpublishing(false);
    }
  }

  async function handleArchive() {
    if (!product) return;
    if (
      !window.confirm(
        "Archive this listing? You can restore it later from the Archived tab.",
      )
    ) {
      return;
    }
    setArchiving(true);
    try {
      await archiveProduct(product.id);
      toast.success("Listing archived");
      await refreshProduct();
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Could not archive listing");
    } finally {
      setArchiving(false);
    }
  }

  async function handleRestore() {
    if (!product) return;
    setRestoring(true);
    try {
      await restoreProduct(product.id);
      toast.success("Listing restored to draft");
      await refreshProduct();
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Could not restore listing");
    } finally {
      setRestoring(false);
    }
  }

  if (isLoading) {
    return <p className="text-muted-foreground">Loading listing…</p>;
  }

  if (error || !product) {
    return (
      <div>
        <PageHeader title="Listing not found" />
        <Button asChild variant="outline" size="sm">
          <Link href={ROUTES.seller.products}>Back to listings</Link>
        </Button>
      </div>
    );
  }

  return (
    <div>
      <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
        <PageHeader title={product.title} description={product.productCode} />
        <div className="flex flex-wrap gap-2">
          {!isArchived && (
            <Button asChild variant="outline" size="sm">
              <Link href={ROUTES.seller.productEdit(product.id)}>Edit details</Link>
            </Button>
          )}
          <Button asChild variant="outline" size="sm">
            <Link href={ROUTES.seller.productPreview(product.id)}>Preview as customer</Link>
          </Button>
          <Button variant="outline" size="sm" disabled={duplicating} onClick={() => void handleDuplicate()}>
            {duplicating ? "Duplicating…" : "Duplicate"}
          </Button>
          {isDraft && (
            <Button
              variant="primary"
              size="sm"
              disabled={publishing || product.imageUrls.length < 1}
              onClick={() => void handlePublish()}
            >
              {publishing ? "Publishing…" : "Publish listing"}
            </Button>
          )}
          {isActive && (
            <Button variant="outline" size="sm" disabled={unpublishing} onClick={() => void handleUnpublish()}>
              {unpublishing ? "Unpublishing…" : "Unpublish"}
            </Button>
          )}
          {isArchived && (
            <Button variant="primary" size="sm" disabled={restoring} onClick={() => void handleRestore()}>
              {restoring ? "Restoring…" : "Restore to draft"}
            </Button>
          )}
          {!isArchived && (
            <Button variant="outline" size="sm" disabled={archiving} onClick={() => void handleArchive()}>
              {archiving ? "Archiving…" : "Archive"}
            </Button>
          )}
          <Button asChild variant="outline" size="sm">
            <Link href={ROUTES.seller.products}>All listings</Link>
          </Button>
        </div>
      </div>

      <div className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_320px]">
        <div className="space-y-6">
          <Card>
            <CardContent className="p-6">
              <ProductImageUpload
                productId={product.id}
                images={listingImagesFromProduct(product)}
                productStatus={product.status}
                readOnly={isArchived}
                onUpdated={refreshProduct}
              />
            </CardContent>
          </Card>

          <Card>
            <CardContent className="space-y-3 p-6">
              <p className="label-caps text-muted-foreground">Description</p>
              <p className="text-sm leading-relaxed">{product.description || "No description yet."}</p>
            </CardContent>
          </Card>

          {!isArchived && (
            <Card>
              <CardContent className="space-y-4 p-6">
                <p className="label-caps text-muted-foreground">Availability blocks</p>
                {productBlocks.length > 0 ? (
                  <ul className="space-y-2 text-sm">
                    {productBlocks.map((block) => (
                      <li key={block.id} className="text-muted-foreground">
                        {block.variantSize}: {block.startDate} → {block.endDate}
                        {block.reason ? ` · ${block.reason}` : ""}
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="text-sm text-muted-foreground">No blocked dates for this listing.</p>
                )}
                <ProductAvailabilityBlockForm
                  productId={product.id}
                  variants={product.variants}
                  onCreated={refreshBlocks}
                />
              </CardContent>
            </Card>
          )}
        </div>

        <div className="space-y-4">
          <Card>
            <CardContent className="space-y-4 p-6">
              <div className="flex items-center gap-3">
                <StatusBadge status={product.status.toLowerCase()} />
                <span className="text-sm text-muted-foreground">{product.city}</span>
              </div>
              <div>
                <p className="label-caps text-muted-foreground">Rental price</p>
                <p className="mt-1 font-mono text-2xl">{formatCurrency(product.pricePerDay)}/day</p>
              </div>
              <div>
                <p className="label-caps text-muted-foreground">Deposit</p>
                <p className="mt-1 font-mono text-lg">{formatCurrency(product.deposit)}</p>
              </div>
              {product.audience && (
                <div>
                  <p className="label-caps text-muted-foreground">Audience</p>
                  <p className="mt-1 capitalize">{product.audience}</p>
                </div>
              )}
              {product.garmentType && (
                <div>
                  <p className="label-caps text-muted-foreground">Garment type</p>
                  <p className="mt-1 capitalize">{product.garmentType.replace(/-/g, " ")}</p>
                </div>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardContent className="space-y-4 p-6">
              <p className="label-caps text-muted-foreground">Stock by size</p>
              {isArchived ? (
                <div className="space-y-2">
                  {product.variants.map((variant) => (
                    <div key={variant.id} className="flex items-center justify-between text-sm">
                      <span>{variant.size}</span>
                      <span className="text-muted-foreground">{variant.availableQuantity} available</span>
                    </div>
                  ))}
                </div>
              ) : (
                <ProductStockEditor productId={product.id} variants={product.variants} onUpdated={refreshProduct} />
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
