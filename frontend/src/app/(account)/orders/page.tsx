"use client";

import Link from "next/link";
import Image from "next/image";
import { useQuery } from "@tanstack/react-query";
import { Package } from "lucide-react";
import { orderService } from "@/features/orders/services";
import { Container, PageHeader } from "@/shared/components/layout/Container";
import { StatusBadge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
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
      <PageHeader title="Orders" description="Track rentals, payments, and refunds" />
      {isLoading ? (
        <div className="space-y-4">
          {Array.from({ length: 3 }).map((_, i) => (
            <OrderCardSkeleton key={i} />
          ))}
        </div>
      ) : orders.length === 0 ? (
        <EmptyState icon={Package} title="No orders yet" actionLabel="Browse" actionHref={ROUTES.products} />
      ) : (
        <div className="space-y-4">
          {orders.map((o) => {
            const pending = o.paymentPending || o.status === "pending_payment";
            return (
              <div
                key={o.id}
                className="rounded-sm border border-border p-4 transition-colors hover:bg-muted/30"
              >
                <Link href={ROUTES.order(o.id)} className="flex gap-4">
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
                    {pending && (
                      <p className="mt-2 text-sm font-medium text-warning">Payment pending</p>
                    )}
                    {o.refundDetails && o.refundDetails.status !== "PROCESSED" && (
                      <p className="mt-2 text-sm text-muted-foreground">Refund processing</p>
                    )}
                  </div>
                </Link>
                {pending && (
                  <Button asChild size="sm" variant="rent" className="mt-4 w-full sm:w-auto">
                    <Link href={ROUTES.checkout.payment}>Complete payment</Link>
                  </Button>
                )}
              </div>
            );
          })}
        </div>
      )}
    </Container>
  );
}
