"use client";

import { use, useState, useEffect } from "react";
import Image from "next/image";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import { productService } from "@/features/products/services";
import { availabilityService } from "@/features/orders/services";
import { Container } from "@/shared/components/layout/Container";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Rating, ReviewCard } from "@/shared/components/display/Rating";
import { formatCurrency, formatDateRange } from "@/lib/format";
import { ROUTES } from "@/shared/constants/routes";
import { cn } from "@/lib/utils";
import { useAuth } from "@/providers/AuthProvider";
import { useBag } from "@/providers/BagProvider";
import { useRentalDates } from "@/providers/RentalDatesProvider";
import { WishlistButton } from "@/features/wishlist/components/WishlistButton";
import { RentalDateFields } from "@/features/checkout/components/RentalDateFields";
import { rentalDatesError, formatRentalLimits, isRentalRangeAvailable } from "@/features/checkout/utils/rental-dates";
import { Skeleton } from "@/components/ui/skeleton";

export default function ProductDetailPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = use(params);
  const router = useRouter();
  const { isAuthenticated } = useAuth();
  const { addItem } = useBag();
  const { dates: sharedDates, setDates: setSharedDates } = useRentalDates();
  const [selectedSize, setSelectedSize] = useState<string | null>(null);
  const [rentalStart, setRentalStart] = useState("");
  const [rentalEnd, setRentalEnd] = useState("");
  const [dateTouched, setDateTouched] = useState(false);

  useEffect(() => {
    if (sharedDates?.start && sharedDates?.end && !rentalStart && !rentalEnd) {
      setRentalStart(sharedDates.start);
      setRentalEnd(sharedDates.end);
    }
  }, [sharedDates, rentalStart, rentalEnd]);

  const product = useQuery({ queryKey: ["product", slug], queryFn: () => productService.getProduct(slug) });
  const reviews = useQuery({ queryKey: ["reviews", slug], queryFn: () => productService.getProductReviews(slug) });

  const p = product.data?.data;
  const rentalLimits = p
    ? { minRentalDays: p.minRentalDays, maxRentalDays: p.maxRentalDays }
    : undefined;
  const hasAvailableVariant = p?.variants.some((v) => v.available) ?? false;
  const size = selectedSize ?? p?.variants.find((v) => v.available)?.size ?? "";
  const variantId = p?.variants.find((v) => v.size === size)?.id;
  const datesSelected = !!rentalStart && !!rentalEnd;

  const availability = useQuery({
    queryKey: ["availability", slug, variantId, rentalStart, rentalEnd],
    queryFn: () =>
      availabilityService.getAvailability(slug, variantId!, {
        startDate: rentalStart,
        endDate: rentalEnd,
      }),
    enabled: !!variantId && datesSelected,
  });
  const dateError = dateTouched
    ? rentalDatesError(availability.data?.data, rentalStart, rentalEnd, rentalLimits)
    : null;

  const canProceed =
    hasAvailableVariant &&
    !!variantId &&
    datesSelected &&
    !dateError &&
    availability.isSuccess &&
    !availability.isFetching;

  if (product.isLoading) {
    return (
      <Container className="py-10">
        <div className="grid gap-10 lg:grid-cols-2">
          <Skeleton className="aspect-[3/4]" />
          <div className="space-y-4">
            <Skeleton className="h-8 w-2/3" />
            <Skeleton className="h-4 w-1/2" />
            <Skeleton className="h-12 w-1/3" />
          </div>
        </div>
      </Container>
    );
  }

  if (!p) return <Container className="py-20 text-center">Product not found</Container>;

  function handleRentClick() {
    setDateTouched(true);
    if (!p || !size || !variantId) return;

    if (availability.isFetching) {
      toast.message("Checking availability…");
      return;
    }

    const error = rentalDatesError(
      availability.data?.data,
      rentalStart,
      rentalEnd,
      rentalLimits,
    );
    if (error) {
      toast.error(error);
      return;
    }

    if (
      availability.data?.data &&
      !isRentalRangeAvailable(availability.data.data, rentalStart, rentalEnd, rentalLimits)
    ) {
      toast.error("These dates are no longer available. Choose different dates.");
      return;
    }

    const checkoutUrl = ROUTES.checkout.bag;
    addItem({
      slug: p.slug,
      size,
      start: rentalStart,
      end: rentalEnd,
    });

    // Defer navigation so the bag state/storage write is committed first.
    queueMicrotask(() => {
      if (isAuthenticated) {
        router.push(checkoutUrl);
      } else {
        router.push(`${ROUTES.login}?returnUrl=${encodeURIComponent(checkoutUrl)}`);
      }
    });
  }

  return (
    <Container className="py-8 md:py-12">
      <div className="grid gap-10 lg:grid-cols-[1.1fr_0.9fr] lg:gap-14">
        <div className="space-y-3">
          <div className="relative aspect-[3/4] overflow-hidden rounded-sm bg-muted">
            <Image src={p.images[0]} alt={p.title} fill className="object-cover" priority sizes="(max-width:1024px) 100vw, 55vw" />
          </div>
          {p.images.length > 1 && (
            <div className="grid grid-cols-4 gap-2">
              {p.images.slice(1).map((img, i) => (
                <div key={i} className="relative aspect-square overflow-hidden rounded-sm bg-muted">
                  <Image src={img} alt="" fill className="object-cover" sizes="100px" />
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="lg:sticky lg:top-24 lg:self-start">
          <p className="label-caps text-muted-foreground">{p.designer}</p>
          <h1 className="mt-2 font-heading text-3xl md:text-4xl">{p.title}</h1>
          <div className="mt-3 flex items-center gap-4">
            <Rating value={p.rating} count={p.reviewCount} />
            <Badge variant="trial">15-min home trial</Badge>
          </div>
          <p className="mt-6 font-mono text-2xl">
            {formatCurrency(p.pricePerDay)}<span className="text-base text-muted-foreground">/day</span>
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            Refundable deposit {formatCurrency(p.deposit)}
          </p>
          <p className="mt-6 text-sm leading-relaxed text-muted-foreground">{p.description}</p>

          <div className="mt-8">
            <p className="label-caps mb-3 text-muted-foreground">Size</p>
            <div className="flex flex-wrap gap-2">
              {p.variants.map((v) => (
                <button
                  key={v.id}
                  type="button"
                  disabled={!v.available}
                  onClick={() => setSelectedSize(v.size)}
                  className={cn(
                    "min-w-[44px] rounded-sm border px-4 py-2 text-sm transition-colors",
                    size === v.size ? "border-accent bg-gold-light text-accent" : "border-border hover:border-border-strong",
                    !v.available && "opacity-40 line-through",
                  )}
                >
                  {v.size}
                </button>
              ))}
            </div>
          </div>

          <div className="mt-8">
            <RentalDateFields
              start={rentalStart}
              end={rentalEnd}
              onStartChange={(value) => {
                setRentalStart(value);
                setDateTouched(true);
                if (rentalEnd && value > rentalEnd) setRentalEnd("");
                if (rentalEnd && value <= rentalEnd) setSharedDates(value, rentalEnd);
              }}
              onEndChange={(value) => {
                setRentalEnd(value);
                setDateTouched(true);
                if (rentalStart) setSharedDates(rentalStart, value);
              }}
              error={dateError}
            />
            {formatRentalLimits(rentalLimits) && (
              <p className="mt-2 text-xs text-muted-foreground">
                {formatRentalLimits(rentalLimits)}
              </p>
            )}
          </div>

          {!hasAvailableVariant && (
            <p className="mt-3 text-sm text-destructive">
              This piece is currently out of stock in all sizes.
            </p>
          )}

          {availability.data?.data.nextAvailableDate && !rentalStart && (
            <p className="mt-3 text-xs text-muted-foreground">
              Next available: {availability.data.data.nextAvailableDate}
            </p>
          )}

          {datesSelected && !dateError && (
            <p className="mt-3 text-sm text-success">{formatDateRange(rentalStart, rentalEnd)} · Available</p>
          )}

          {availability.isFetching && datesSelected && (
            <p className="mt-3 text-xs text-muted-foreground">Checking availability…</p>
          )}

          <div className="mt-8 flex flex-col gap-3 sm:flex-row">
            <Button
              type="button"
              variant="rent"
              size="lg"
              className="sm:flex-1"
              disabled={!canProceed}
              onClick={handleRentClick}
            >
              Rent this look
            </Button>
            <WishlistButton productId={p.id} className="sm:flex-1" />
          </div>

          {!datesSelected && (
            <p className="mt-3 text-xs text-muted-foreground">
              Select size and rental dates to continue. Availability is verified before checkout.
            </p>
          )}

          <p className="mt-4 text-xs text-muted-foreground">
            Sold by {p.sellerName} · {p.city}
          </p>
        </div>
      </div>

      {reviews.data?.data.length ? (
        <section className="mt-16 border-t border-border pt-12">
          <h2 className="mb-6 font-heading text-2xl">Reviews</h2>
          <div className="grid gap-4 md:grid-cols-2">
            {reviews.data.data.map((r) => (
              <ReviewCard key={r.id} rating={r.rating} comment={r.body} author={r.authorName} />
            ))}
          </div>
        </section>
      ) : null}
    </Container>
  );
}
