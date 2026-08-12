"use client";

import Link from "next/link";
import Image from "next/image";
import { useQuery } from "@tanstack/react-query";
import { ArrowRight } from "lucide-react";
import { homeService, categoryService } from "@/features/products/services";
import { useListingParams } from "@/features/products/hooks/useListingParams";
import { ListingDateBar } from "@/features/products/components/ListingDateBar";
import { WishlistProductCard } from "@/features/wishlist/components/WishlistProductCard";
import { Container } from "@/shared/components/layout/Container";
import { Button } from "@/components/ui/button";
import { ProductCardSkeleton } from "@/components/ui/skeleton";
import { ReviewCard } from "@/shared/components/display/Rating";
import { ROUTES } from "@/shared/constants/routes";
import reviewsData from "@/mocks/data/reviews.json";

export default function HomePage() {
  const dateParams = useListingParams();
  const featured = useQuery({
    queryKey: ["home", "featured", dateParams.startDate, dateParams.endDate],
    queryFn: () => homeService.getFeaturedProducts(dateParams),
    staleTime: 60_000,
    refetchOnWindowFocus: true,
  });
  const trending = useQuery({
    queryKey: ["home", "trending", dateParams.startDate, dateParams.endDate],
    queryFn: () => homeService.getTrendingProducts(dateParams),
    staleTime: 60_000,
    refetchOnWindowFocus: true,
  });
  const categories = useQuery({ queryKey: ["categories"], queryFn: () => categoryService.listCategories() });

  function scrollToDateBar() {
    document.getElementById("listing-date-bar")?.scrollIntoView({ behavior: "smooth", block: "center" });
  }

  return (
    <>
      <section className="border-b border-border bg-primary text-primary-foreground">
        <Container className="grid gap-10 py-16 md:grid-cols-2 md:py-24">
          <div className="flex flex-col justify-center">
            <p className="label-caps mb-4 text-primary-foreground/70">Premium rental · Mumbai</p>
            <h1 className="text-4xl md:text-5xl lg:text-[52px]">
              Rent the look.<br />
              <span className="italic text-primary-foreground/90">Try at home.</span>
            </h1>
            <p className="mt-6 max-w-md leading-relaxed text-primary-foreground/75">
              Curated designer pieces with a mandatory 15-minute home trial — keep it only if you love it.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Button asChild variant="secondary" size="md" className="border-0 bg-primary-foreground text-primary hover:bg-primary-foreground/90">
                <Link href={ROUTES.products}>Browse collection</Link>
              </Button>
              <Button asChild variant="secondary" size="md" className="border-primary-foreground/30 text-primary-foreground">
                <Link href={ROUTES.occasion("wedding")}>Wedding edit</Link>
              </Button>
            </div>
          </div>
          <div className="relative aspect-[4/5] overflow-hidden rounded-sm">
            <Image
              src="https://images.unsplash.com/photo-1596783074918-c84cb06531ca?auto=format&fit=crop&w=800&q=80"
              alt="Premium rental fashion"
              fill
              className="object-cover"
              priority
              sizes="(max-width: 768px) 100vw, 50vw"
            />
          </div>
        </Container>
      </section>

      <Container className="py-14 md:py-16">
        <ListingDateBar className="mb-10 w-full max-w-md rounded-sm border border-border bg-card p-5 shadow-sm" />
        <div className="mb-8 flex items-end justify-between">
          <h2 className="text-2xl md:text-3xl">Shop by occasion</h2>
        </div>
        <div className="grid grid-cols-2 gap-4 md:grid-cols-4 md:gap-5">
          {categories.isLoading
            ? Array.from({ length: 4 }).map((_, i) => <div key={i} className="aspect-[4/5] animate-pulse bg-muted" />)
            : categories.data?.data
                .filter((c) => c.featured)
                .map((cat) => (
                  <Link key={cat.id} href={ROUTES.occasion(cat.slug)} className="group relative aspect-[4/5] overflow-hidden rounded-sm">
                    <Image src={cat.image} alt={cat.name} fill className="object-cover transition-transform group-hover:scale-[1.02]" sizes="25vw" />
                    <div className="absolute inset-x-0 bottom-0 bg-navy-dark/90 p-4">
                      <p className="font-heading text-lg text-primary-foreground">{cat.name}</p>
                      <p className="text-xs text-primary-foreground/75">{cat.productCount} pieces</p>
                    </div>
                  </Link>
                ))}
        </div>
      </Container>

      <section className="border-y border-border bg-muted/40 py-14 md:py-16">
        <Container>
          <div className="mb-8 flex items-center justify-between">
            <h2 className="text-2xl md:text-3xl">Featured</h2>
            <Link href={ROUTES.products} className="flex items-center gap-1 text-sm text-muted-foreground hover:text-accent">
              View all <ArrowRight className="h-4 w-4" />
            </Link>
          </div>
          <div className="grid grid-cols-2 gap-5 md:grid-cols-4 md:gap-7">
            {featured.isLoading
              ? Array.from({ length: 4 }).map((_, i) => <ProductCardSkeleton key={i} />)
              : featured.data?.data.map((p) => (
                  <WishlistProductCard key={p.id} product={p} onChangeDates={scrollToDateBar} />
                ))}
          </div>
        </Container>
      </section>

      {trending.data?.data.length ? (
        <Container className="py-14 md:py-16">
          <h2 className="mb-8 text-2xl md:text-3xl">Trending now</h2>
          <div className="grid grid-cols-2 gap-5 md:grid-cols-3 md:gap-7">
            {trending.data.data.map((p) => (
              <WishlistProductCard key={p.id} product={p} onChangeDates={scrollToDateBar} />
            ))}
          </div>
        </Container>
      ) : null}

      <section className="border-t border-line-dark bg-primary py-16 text-primary-foreground">
        <Container>
          <h2 className="mb-10 text-2xl md:text-3xl">What renters say</h2>
          <div className="grid gap-5 md:grid-cols-2">
            {reviewsData.slice(0, 2).map((r) => (
              <ReviewCard
                key={r.id}
                rating={r.rating}
                comment={r.body}
                author={`${r.authorName} · ${r.authorContext}`}
                className="border-primary-foreground/20 bg-primary-foreground/5 text-primary-foreground"
              />
            ))}
          </div>
        </Container>
      </section>
    </>
  );
}
