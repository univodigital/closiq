"use client";

import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { formatCurrency, formatDateRange } from "@/lib/format";
import { ROUTES } from "@/shared/constants/routes";
import { cn } from "@/lib/utils";
import { useCheckoutParams } from "@/features/checkout/hooks/useCheckoutParams";
import { useBag } from "@/providers/BagProvider";
import { CheckoutLayoutShell, CheckoutTwoColumn } from "@/features/checkout/components/CheckoutLayoutShell";
import { PriceDetails } from "@/features/checkout/components/PriceDetails";
import { RentalDateFields } from "@/features/checkout/components/RentalDateFields";
import { rentalDatesError } from "@/features/checkout/utils/rental-dates";
import {
  defaultAvailableSize,
  findVariantBySize,
} from "@/features/checkout/utils/product-variant";
import { calculateBagPricing, loadBagLines } from "@/features/checkout/utils/bag-pricing";
import {
  hasBlockingAvailabilityIssues,
  validateBagLinesAvailability,
} from "@/features/checkout/utils/bag-availability";
import { BagItemAvailabilityBadge } from "@/features/checkout/components/BagItemAvailabilityBadge";
import { isCompleteBagItem, type BagItem } from "@/features/checkout/bag/bag-store";
import { availabilityService } from "@/features/orders/services";
import { productService } from "@/features/products/services";

export default function CheckoutBagPage() {
  const router = useRouter();
  const { items: bagItems, addItem, removeItem, clear, refresh, hydrated } = useBag();
  const urlParams = useCheckoutParams();
  const [coupon, setCoupon] = useState(urlParams.couponCode);
  const [editingSlug, setEditingSlug] = useState<string | null>(null);
  const [draftSize, setDraftSize] = useState("");
  const [draftStart, setDraftStart] = useState("");
  const [draftEnd, setDraftEnd] = useState("");
  const [dateTouched, setDateTouched] = useState(false);

  // Import legacy ?slug=&size=&start=&end= deep links into the bag, then normalize URL.
  useEffect(() => {
    if (!hydrated) return;
    if (!urlParams.hasLegacyItemParams) return;
    addItem({
      slug: urlParams.slug,
      size: urlParams.size,
      start: urlParams.start,
      end: urlParams.end,
    });
    const qs = urlParams.fullQuery({ couponCode: urlParams.couponCode || undefined });
    router.replace(qs ? `${ROUTES.checkout.bag}?${qs}` : ROUTES.checkout.bag);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    hydrated,
    urlParams.hasLegacyItemParams,
    urlParams.slug,
    urlParams.size,
    urlParams.start,
    urlParams.end,
    addItem,
    router,
  ]);

  const completeItems = bagItems.filter(isCompleteBagItem);
  const bagKey = JSON.stringify(completeItems);

  const linesQuery = useQuery({
    queryKey: ["bag-lines", bagKey],
    queryFn: () => loadBagLines(completeItems),
    enabled: hydrated && completeItems.length > 0,
  });

  const pricingQuery = useQuery({
    queryKey: ["bag-pricing", bagKey, coupon],
    queryFn: async () => {
      const lines = await loadBagLines(completeItems);
      return calculateBagPricing(lines, { couponCode: coupon || undefined });
    },
    enabled: hydrated && completeItems.length > 0,
  });

  const availabilityQuery = useQuery({
    queryKey: ["bag-availability", bagKey],
    queryFn: async () => {
      const lines = await loadBagLines(completeItems);
      return validateBagLinesAvailability(lines);
    },
    enabled: hydrated && completeItems.length > 0,
    refetchOnWindowFocus: true,
  });

  const editingProduct = useQuery({
    queryKey: ["product", editingSlug],
    queryFn: () => productService.getProduct(editingSlug!),
    enabled: !!editingSlug,
  });
  const editingProductData = editingProduct.data?.data;
  const effectiveEditSize = draftSize || defaultAvailableSize(editingProductData);
  const effectiveEditVariant = findVariantBySize(editingProductData, effectiveEditSize);

  const editAvailability = useQuery({
    queryKey: ["availability", editingSlug, effectiveEditVariant?.id, draftStart, draftEnd],
    queryFn: () =>
      availabilityService.getAvailability(editingSlug!, effectiveEditVariant!.id, {
        startDate: draftStart,
        endDate: draftEnd,
      }),
    enabled: !!editingSlug && !!effectiveEditVariant?.id && !!draftStart && !!draftEnd,
  });

  const editDateError =
    dateTouched || (!!draftStart && !!draftEnd)
      ? rentalDatesError(editAvailability.data?.data, draftStart, draftEnd, editingProductData
          ? { minRentalDays: editingProductData.minRentalDays, maxRentalDays: editingProductData.maxRentalDays }
          : undefined)
      : null;

  function startEditing(item: BagItem) {
    setEditingSlug(item.slug);
    setDraftSize(item.size);
    setDraftStart(item.start);
    setDraftEnd(item.end);
    setDateTouched(false);
  }

  function cancelEditing() {
    setEditingSlug(null);
    setDateTouched(false);
  }

  function saveEditing() {
    setDateTouched(true);
    if (!editingSlug || !effectiveEditSize || !draftStart || !draftEnd || editDateError) return;
    addItem({
      slug: editingSlug,
      size: effectiveEditSize,
      start: draftStart,
      end: draftEnd,
    });
    setEditingSlug(null);
    refresh();
    availabilityQuery.refetch();
  }

  function removeLine(slug: string) {
    if (editingSlug === slug) cancelEditing();
    removeItem(slug);
  }

  function removeAll() {
    clear();
    cancelEditing();
  }

  const continueQs = urlParams.fullQuery({ couponCode: coupon || undefined });
  const availabilityResults = availabilityQuery.data ?? [];
  const hasAvailabilityIssues = hasBlockingAvailabilityIssues(availabilityResults);
  const canContinue =
    completeItems.length > 0 &&
    !editingSlug &&
    !pricingQuery.isLoading &&
    !availabilityQuery.isLoading &&
    !hasAvailabilityIssues;

  if (!hydrated) {
    return (
      <CheckoutLayoutShell step="bag" queryString="">
        <p className="text-muted-foreground">Loading bag…</p>
      </CheckoutLayoutShell>
    );
  }

  if (!bagItems.length) {
    return (
      <CheckoutLayoutShell step="bag" queryString="">
        <h1 className="font-heading text-3xl">Bag</h1>
        <p className="mt-4 text-muted-foreground">Your bag is empty.</p>
        <Button asChild variant="outline" className="mt-4">
          <Link href={ROUTES.products}>Browse products</Link>
        </Button>
      </CheckoutLayoutShell>
    );
  }

  const lines = linesQuery.data ?? [];

  return (
    <CheckoutLayoutShell step="bag" queryString={continueQs}>
      <CheckoutTwoColumn
        main={
          <div className="space-y-6">
            <div className="flex flex-wrap items-end justify-between gap-3">
              <h1 className="font-heading text-3xl">Bag</h1>
              <Button type="button" variant="outline" size="sm" onClick={removeAll}>
                Clear bag
              </Button>
            </div>

            {linesQuery.isLoading && <p className="text-sm text-muted-foreground">Loading items…</p>}
            {availabilityQuery.isFetching && !availabilityQuery.isLoading && (
              <p className="text-sm text-muted-foreground">Checking availability…</p>
            )}

            {hasAvailabilityIssues && !availabilityQuery.isLoading && (
              <div className="rounded-sm border border-destructive/40 bg-destructive/5 p-4 text-sm">
                <p className="font-medium text-destructive">
                  Some items are no longer available for your selected dates.
                </p>
                <p className="mt-1 text-muted-foreground">
                  Change dates or remove affected items to continue.
                </p>
              </div>
            )}

            <ul className="space-y-4">
              {bagItems.map((item) => {
                const line = lines.find((l) => l.item.slug === item.slug);
                const product = line?.product;
                const availability = availabilityResults.find((a) => a.slug === item.slug);
                const isEditing = editingSlug === item.slug;

                return (
                  <li key={item.slug} className="rounded-sm border border-border p-4">
                    <div className="flex gap-4">
                      <div className="relative h-28 w-20 shrink-0 overflow-hidden rounded-sm bg-muted">
                        {product?.images[0] && (
                          <Image
                            src={product.images[0]}
                            alt={product.title}
                            fill
                            className="object-cover"
                            sizes="80px"
                          />
                        )}
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="label-caps text-muted-foreground">
                          {product?.designer ?? "…"}
                        </p>
                        <p className="font-medium">{product?.title ?? item.slug}</p>
                        {isCompleteBagItem(item) ? (
                          <>
                            <p className="mt-1 text-sm text-muted-foreground">Size {item.size}</p>
                            <p className="mt-1 text-sm">{formatDateRange(item.start, item.end)}</p>
                            {availability && (
                              <BagItemAvailabilityBadge
                                status={availability.status}
                                message={availability.message}
                              />
                            )}
                          </>
                        ) : (
                          <p className="mt-1 text-sm text-destructive">Incomplete rental details</p>
                        )}
                        {product && (
                          <p className="mt-2 font-mono text-sm">
                            {formatCurrency(product.pricePerDay)}/day
                          </p>
                        )}
                        <div className="mt-3 flex flex-wrap gap-2">
                          {!isEditing && (
                            <Button
                              type="button"
                              variant="outline"
                              size="sm"
                              onClick={() => startEditing(item)}
                            >
                              {availability?.status === "available" ? "Edit details" : "Change dates"}
                            </Button>
                          )}
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() => removeLine(item.slug)}
                          >
                            Remove
                          </Button>
                        </div>
                      </div>
                    </div>

                    {isEditing && editingProductData && (
                      <div className="mt-4 space-y-4 border-t border-border pt-4">
                        <p className="font-medium">Edit rental details</p>
                        <div>
                          <p className="label-caps mb-2 text-muted-foreground">Size *</p>
                          <div className="flex flex-wrap gap-2">
                            {editingProductData.variants.map((v) => (
                              <button
                                key={v.id}
                                type="button"
                                disabled={!v.available}
                                onClick={() => setDraftSize(v.size)}
                                className={cn(
                                  "min-w-[44px] rounded-sm border px-4 py-2 text-sm",
                                  effectiveEditSize === v.size
                                    ? "border-accent bg-muted"
                                    : "border-border",
                                  !v.available && "opacity-40 line-through",
                                )}
                              >
                                {v.size}
                              </button>
                            ))}
                          </div>
                        </div>
                        <RentalDateFields
                          start={draftStart}
                          end={draftEnd}
                          onStartChange={(value) => {
                            setDraftStart(value);
                            setDateTouched(true);
                            if (draftEnd && value > draftEnd) setDraftEnd("");
                          }}
                          onEndChange={(value) => {
                            setDraftEnd(value);
                            setDateTouched(true);
                          }}
                          error={editDateError}
                        />
                        <div className="flex flex-wrap gap-2">
                          <Button
                            variant="outline"
                            onClick={saveEditing}
                            disabled={
                              !effectiveEditSize ||
                              !draftStart ||
                              !draftEnd ||
                              !!editDateError ||
                              editAvailability.isFetching
                            }
                          >
                            Update item
                          </Button>
                          <Button type="button" variant="secondary" onClick={cancelEditing}>
                            Cancel
                          </Button>
                        </div>
                      </div>
                    )}
                  </li>
                );
              })}
            </ul>

            <div className="rounded-sm border border-border p-4">
              <p className="label-caps text-muted-foreground">Offers</p>
              <div className="mt-3 flex gap-2">
                <Input
                  placeholder="Coupon code"
                  value={coupon}
                  onChange={(e) => setCoupon(e.target.value.toUpperCase())}
                />
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => pricingQuery.refetch()}
                  disabled={!completeItems.length}
                >
                  Apply
                </Button>
              </div>
              <p className="mt-2 text-xs text-muted-foreground">Try FIRST500 for ₹500 off (if eligible)</p>
            </div>
          </div>
        }
        sidebar={
          <>
            {canContinue ? (
              <>
                <PriceDetails pricing={pricingQuery.data} />
                <Button asChild variant="rent" size="lg" className="w-full">
                  <Link href={`${ROUTES.checkout.address}?${continueQs}`}>Continue</Link>
                </Button>
              </>
            ) : (
              <p className="text-sm text-muted-foreground">
                {editingSlug
                  ? "Save or cancel edits before continuing."
                  : hasAvailabilityIssues
                    ? "Resolve availability issues before continuing."
                    : availabilityQuery.isLoading
                      ? "Checking availability…"
                      : "Add complete size and rental dates for each item to continue."}
              </p>
            )}
          </>
        }
      />
    </CheckoutLayoutShell>
  );
}
