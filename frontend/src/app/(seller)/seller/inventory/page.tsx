"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { sellerService } from "@/features/seller/services";
import { PageHeader } from "@/shared/components/layout/Container";
import { Card, CardContent } from "@/components/ui/card";
import { ROUTES } from "@/shared/constants/routes";

export default function SellerInventoryPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["seller", "inventory", "blocks"],
    queryFn: () => sellerService.listInventoryBlocks(),
  });

  const blocks = data?.data ?? [];

  return (
    <div>
      <PageHeader title="Inventory calendar" description="Blocked and maintenance dates" />
      {isLoading ? (
        <p className="text-muted-foreground">Loading…</p>
      ) : blocks.length === 0 ? (
        <p className="text-sm text-muted-foreground">No blocked dates on your listings.</p>
      ) : (
        <div className="space-y-3">
          {blocks.map((block) => (
            <Card key={block.id}>
              <CardContent className="flex flex-wrap items-start justify-between gap-3 p-4 text-sm">
                <div>
                  <Link
                    href={ROUTES.seller.product(block.productId)}
                    className="font-medium hover:underline"
                  >
                    {block.productTitle}
                  </Link>
                  <p className="text-muted-foreground">
                    {block.variantSize} · {block.startDate} → {block.endDate}
                  </p>
                  {block.reason && <p className="mt-1 text-muted-foreground">{block.reason}</p>}
                </div>
                <span className="label-caps text-muted-foreground">{block.status.toLowerCase()}</span>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
