"use client";

import Link from "next/link";
import Image from "next/image";
import { useQuery } from "@tanstack/react-query";
import { Package } from "lucide-react";
import { orderService } from "@/features/orders/services";
import { Container, PageHeader } from "@/shared/components/layout/Container";
import { StatusBadge } from "@/components/ui/badge";
import { EmptyState } from "@/shared/components/feedback/EmptyState";
import { OrderCardSkeleton } from "@/components/ui/skeleton";
import { formatCurrency, formatDateRange } from "@/lib/format";
import { ROUTES } from "@/shared/constants/routes";

export default function OrdersPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["orders"],
    queryFn: () => orderService.listOrders(),
  });

  const orders = data?.data ?? [];

  return (
    <Container narrow embedded>
      <PageHeader title="Orders" description="Track rentals and returns" />
      {isLoading ? (
        <div className="space-y-4">
          {Array.from({ length: 3 }).map((_, i) => <OrderCardSkeleton key={i} />)}
        </div>
      ) : orders.length === 0 ? (
        <EmptyState icon={Package} title="No orders yet" actionLabel="Browse" actionHref={ROUTES.products} />
      ) : (
        <div className="space-y-4">
          {orders.map((o) => (
            <Link
              key={o.id}
              href={ROUTES.order(o.id)}
              className="flex gap-4 rounded-sm border border-border p-4 transition-colors hover:bg-muted/30"
            >
              <div className="relative h-24 w-20 shrink-0 overflow-hidden rounded-sm bg-muted">
                <Image src={o.productImage} alt="" fill className="object-cover" sizes="80px" />
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <p className="font-heading text-lg">{o.productTitle}</p>
                  <StatusBadge status={o.status} />
                </div>
                <p className="mt-1 text-sm text-muted-foreground">
                  {formatDateRange(o.rentalStart, o.rentalEnd)} · Size {o.variantSize}
                </p>
                <p className="mt-2 font-mono text-sm">{formatCurrency(o.totalPaid)}</p>
              </div>
            </Link>
          ))}
        </div>
      )}
    </Container>
  );
}
