"use client";

import Link from "next/link";
import Image from "next/image";
import { useQuery } from "@tanstack/react-query";
import { useParams, useRouter } from "next/navigation";
import { useEffect } from "react";
import { sellerService } from "@/features/seller/services";
import { PageHeader } from "@/shared/components/layout/Container";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { StatusBadge } from "@/components/ui/badge";
import { formatCurrency } from "@/lib/format";
import { ROUTES } from "@/shared/constants/routes";

export default function SellerProductDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const productId = params.id;

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

  const product = data?.data;

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
        <Button asChild variant="outline" size="sm">
          <Link href={ROUTES.seller.products}>All listings</Link>
        </Button>
      </div>

      <div className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_320px]">
        <div className="space-y-6">
          <div className="grid gap-3 sm:grid-cols-2">
            {product.imageUrls.map((url) => (
              <div key={url} className="relative aspect-[3/4] overflow-hidden rounded-sm bg-muted">
                <Image src={url} alt={product.title} fill className="object-cover" sizes="(max-width:768px) 100vw, 40vw" />
              </div>
            ))}
          </div>
          <Card>
            <CardContent className="space-y-3 p-6">
              <p className="label-caps text-muted-foreground">Description</p>
              <p className="text-sm leading-relaxed">{product.description || "No description yet."}</p>
            </CardContent>
          </Card>
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
            </CardContent>
          </Card>

          <Card>
            <CardContent className="space-y-3 p-6">
              <p className="label-caps text-muted-foreground">Variants & stock</p>
              {product.variants.map((variant) => (
                <div key={variant.id} className="flex items-center justify-between text-sm">
                  <span>{variant.size}</span>
                  <span className="text-muted-foreground">
                    {variant.availableQuantity} available · {variant.status.toLowerCase()}
                  </span>
                </div>
              ))}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
