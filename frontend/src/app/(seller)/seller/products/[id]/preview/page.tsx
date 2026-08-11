"use client";

import Image from "next/image";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useParams } from "next/navigation";
import { previewSellerProduct } from "@/features/seller/services/seller-product-management.service";
import { mapProductDetail } from "@/lib/api-mappers";
import { Container, PageHeader } from "@/shared/components/layout/Container";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { formatCurrency } from "@/lib/format";
import { ROUTES } from "@/shared/constants/routes";

export default function SellerProductPreviewPage() {
  const params = useParams<{ id: string }>();
  const productId = params.id;

  const preview = useQuery({
    queryKey: ["seller", "products", productId, "preview"],
    queryFn: async () => {
      const raw = await previewSellerProduct(productId);
      return mapProductDetail(raw as Parameters<typeof mapProductDetail>[0]);
    },
    enabled: !!productId,
  });

  const product = preview.data;

  if (preview.isLoading) {
    return <p className="text-muted-foreground">Loading preview…</p>;
  }

  if (preview.error || !product) {
    return (
      <div>
        <PageHeader title="Preview unavailable" />
        <Button asChild variant="outline" size="sm">
          <Link href={ROUTES.seller.product(productId)}>Back to listing</Link>
        </Button>
      </div>
    );
  }

  return (
    <Container className="py-8">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <PageHeader title="Preview as customer" description="This is how shoppers will see your listing." />
        <div className="flex gap-2">
          <Badge>Draft preview</Badge>
          <Button asChild variant="outline" size="sm">
            <Link href={ROUTES.seller.product(productId)}>Back to editor</Link>
          </Button>
        </div>
      </div>

      <div className="grid gap-10 lg:grid-cols-2">
        <div className="space-y-3">
          {product.images.map((url, index) => (
            <div key={`${url}-${index}`} className="relative aspect-[3/4] overflow-hidden rounded-sm bg-muted">
              <Image src={url} alt={product.title} fill className="object-cover" sizes="(max-width:768px) 100vw, 50vw" />
            </div>
          ))}
        </div>

        <div className="space-y-6">
          <div>
            <p className="text-sm text-muted-foreground">{product.designer}</p>
            <h1 className="font-heading text-3xl">{product.title}</h1>
            <p className="mt-2 capitalize text-muted-foreground">
              {product.occasion?.replace(/-/g, " ")} · {product.city}
            </p>
          </div>

          <div>
            <p className="font-mono text-2xl">{formatCurrency(product.pricePerDay)}/day</p>
            <p className="text-sm text-muted-foreground">Deposit {formatCurrency(product.deposit)}</p>
          </div>

          <p className="text-sm leading-relaxed text-muted-foreground">{product.description}</p>

          <div>
            <p className="label-caps mb-2 text-muted-foreground">Sizes</p>
            <div className="flex flex-wrap gap-2">
              {product.variants.map((variant) => (
                <span
                  key={variant.id}
                  className="rounded-sm border border-border px-3 py-1 text-sm capitalize"
                >
                  {variant.size}
                  {!variant.available ? " · unavailable" : ""}
                </span>
              ))}
            </div>
          </div>

          <p className="text-xs text-muted-foreground">
            Preview only — this listing is not published or searchable until you publish it.
          </p>
        </div>
      </div>
    </Container>
  );
}
