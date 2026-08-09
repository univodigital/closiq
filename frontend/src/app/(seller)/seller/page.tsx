"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { sellerService } from "@/features/seller/services";
import { useSeller } from "@/providers/SellerProvider";
import { PageHeader } from "@/shared/components/layout/Container";
import { Card, CardContent } from "@/components/ui/card";
import { StatusBadge } from "@/components/ui/badge";
import { formatCurrency } from "@/lib/format";
import { ROUTES } from "@/shared/constants/routes";
import { Skeleton } from "@/components/ui/skeleton";

export default function SellerDashboardPage() {
  const { profile } = useSeller();
  const { data, isLoading } = useQuery({
    queryKey: ["seller", "dashboard"],
    queryFn: () => sellerService.getDashboard(),
  });

  const dash = data?.data;

  if (isLoading) return <Skeleton className="h-64 w-full" />;

  return (
    <div>
      <PageHeader
        title="Dashboard"
        description={
          profile?.businessName
            ? `${profile.businessName} · ${dash?.summary.activeListings ?? 0} active listings`
            : dash?.summary
              ? `${dash.summary.activeListings} active listings`
              : undefined
        }
      />
      <div className="grid gap-4 sm:grid-cols-3">
        <Card>
          <CardContent className="p-5">
            <p className="label-caps text-muted-foreground">This month</p>
            <p className="mt-2 font-mono text-2xl">{formatCurrency(dash?.summary.earningsThisMonth ?? 0)}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-5">
            <p className="label-caps text-muted-foreground">Pending bookings</p>
            <p className="mt-2 font-heading text-2xl">{dash?.summary.pendingBookings ?? 0}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-5">
            <p className="label-caps text-muted-foreground">Listings</p>
            <p className="mt-2 font-heading text-2xl">{dash?.summary.activeListings ?? 0}</p>
          </CardContent>
        </Card>
      </div>
      {dash?.tasks.length ? (
        <section className="mt-10">
          <h2 className="label-caps mb-4 text-muted-foreground">Tasks</h2>
          <div className="space-y-2">
            {dash.tasks.map((t) => (
              <Link key={t.bookingId} href={ROUTES.seller.booking(t.bookingId)} className="block rounded-sm border border-border p-4 hover:bg-muted/30">
                <p className="text-sm font-medium">{t.type.replace(/_/g, " ")}</p>
                <p className="text-xs text-muted-foreground">Due {new Date(t.dueBy).toLocaleString("en-IN")}</p>
              </Link>
            ))}
          </div>
        </section>
      ) : null}
    </div>
  );
}
