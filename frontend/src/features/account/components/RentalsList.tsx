"use client";

import Link from "next/link";
import Image from "next/image";
import { useQuery } from "@tanstack/react-query";
import { Package } from "lucide-react";
import { orderService } from "@/features/orders/services";
import { StatusBadge } from "@/components/ui/badge";
import { EmptyState } from "@/shared/components/feedback/EmptyState";
import { OrderCardSkeleton } from "@/components/ui/skeleton";
import { formatCurrency, formatDateRange } from "@/lib/format";
import { ROUTES } from "@/shared/constants/routes";
import type { Order, OrderStatus } from "@/shared/types";

const RENTAL_FILTERS: Record<string, OrderStatus[]> = {
  active: ["rental_active", "trial_ready"],
  upcoming: ["confirmed", "out_for_delivery"],
  history: ["returned", "deposit_refunded", "cancelled"],
  returns: ["return_scheduled", "returned"],
};

export function RentalsList({
  filter,
  emptyTitle,
  emptyDescription,
}: {
  filter: keyof typeof RENTAL_FILTERS;
  emptyTitle: string;
  emptyDescription?: string;
}) {
  const { data, isLoading } = useQuery({
    queryKey: ["orders"],
    queryFn: () => orderService.listOrders(),
  });

  const allowed = new Set(RENTAL_FILTERS[filter]);
  const orders = (data?.data ?? []).filter((o: Order) => allowed.has(o.status));

  if (isLoading) {
    return (
      <div className="space-y-4">
        {Array.from({ length: 3 }).map((_, i) => (
          <OrderCardSkeleton key={i} />
        ))}
      </div>
    );
  }

  if (orders.length === 0) {
    return (
      <EmptyState
        icon={Package}
        title={emptyTitle}
        description={emptyDescription}
        actionLabel="Browse collection"
        actionHref={ROUTES.products}
      />
    );
  }

  return (
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
  );
}
