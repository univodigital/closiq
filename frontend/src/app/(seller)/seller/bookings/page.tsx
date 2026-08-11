"use client";

import Link from "next/link";
import Image from "next/image";
import { useQuery } from "@tanstack/react-query";
import { AcceptCountdown } from "@/features/seller/components/AcceptCountdown";
import { sellerService } from "@/features/seller/services";
import { PageHeader } from "@/shared/components/layout/Container";
import { StatusBadge } from "@/components/ui/badge";
import { formatCurrency, formatDateRange } from "@/lib/format";
import { ROUTES } from "@/shared/constants/routes";

export default function SellerBookingsPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["seller", "bookings"],
    queryFn: () => sellerService.listBookings(),
  });

  return (
    <div>
      <PageHeader title="Bookings" description="Incoming and active rentals" />
      {isLoading ? (
        <p className="text-muted-foreground">Loading…</p>
      ) : (
        <div className="space-y-3">
          {data?.data.map((b) => (
            <Link
              key={b.id}
              href={ROUTES.seller.booking(b.id)}
              className="flex gap-4 rounded-sm border border-border p-4 hover:bg-muted/30"
            >
              <div className="relative h-16 w-12 shrink-0 overflow-hidden rounded-sm bg-muted">
                <Image src={b.productImage} alt="" fill className="object-cover" sizes="48px" />
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap justify-between gap-2">
                  <p className="font-medium">{b.productTitle}</p>
                  <StatusBadge status={b.status} />
                </div>
                <p className="text-sm text-muted-foreground">
                  {b.customerName} · {formatDateRange(b.rentalStart, b.rentalEnd)}
                </p>
                {b.acceptDeadlineAt && !b.acceptanceExpired && b.status === "confirmed" ? (
                  <p className="mt-1 text-xs text-warning">
                    Accept within{" "}
                    <AcceptCountdown deadlineAt={b.acceptDeadlineAt} expired={b.acceptanceExpired} />
                  </p>
                ) : null}
                <p className="mt-1 font-mono text-sm text-success">+{formatCurrency(b.earnings)}</p>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
