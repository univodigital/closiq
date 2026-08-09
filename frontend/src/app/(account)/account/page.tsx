"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Calendar, Heart, MapPin, Package } from "lucide-react";
import { useAuth } from "@/providers/AuthProvider";
import { orderService } from "@/features/orders/services";
import { wishlistService } from "@/features/wishlist/services";
import { Container, PageHeader } from "@/shared/components/layout/Container";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ROUTES } from "@/shared/constants/routes";
import { Skeleton } from "@/components/ui/skeleton";
import { AccountActions } from "@/features/account/components/AccountActions";

function QuickLink({
  href,
  label,
  detail,
  icon: Icon,
}: {
  href: string;
  label: string;
  detail: string;
  icon: React.ComponentType<{ className?: string }>;
}) {
  return (
    <Link
      href={href}
      className="flex items-start gap-4 rounded-sm border border-border p-4 transition-colors hover:bg-muted/30"
    >
      <Icon className="mt-0.5 h-5 w-5 shrink-0 text-accent" />
      <div>
        <p className="font-medium">{label}</p>
        <p className="mt-1 text-sm text-muted-foreground">{detail}</p>
      </div>
    </Link>
  );
}

export default function AccountOverviewPage() {
  const { user, isLoading, hasRole } = useAuth();
  const { data: ordersData } = useQuery({
    queryKey: ["orders"],
    queryFn: () => orderService.listOrders(),
    enabled: !isLoading,
  });
  const { data: wishlistData } = useQuery({
    queryKey: ["wishlist"],
    queryFn: () => wishlistService.list(),
    enabled: !isLoading,
  });

  const activeCount =
    ordersData?.data.filter((o) =>
      ["rental_active", "trial_ready", "confirmed", "out_for_delivery"].includes(o.status),
    ).length ?? 0;
  const wishlistCount = wishlistData?.data.length ?? 0;

  return (
    <Container narrow embedded>
      <PageHeader
        title="Overview"
        description={
          isLoading ? undefined : `Welcome back${user?.firstName ? `, ${user.firstName}` : ""}`
        }
      />

      {isLoading ? (
        <div className="space-y-4">
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-24 w-full" />
        </div>
      ) : (
        <>
          <div className="mb-8 grid gap-4 sm:grid-cols-2">
            <Card>
              <CardContent className="p-5">
                <p className="label-caps text-muted-foreground">Active rentals</p>
                <p className="mt-2 font-heading text-3xl">{activeCount}</p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="p-5">
                <p className="label-caps text-muted-foreground">Saved pieces</p>
                <p className="mt-2 font-heading text-3xl">{wishlistCount}</p>
              </CardContent>
            </Card>
          </div>

          <div className="space-y-3">
            <QuickLink
              href={ROUTES.account.rentals.active}
              label="Active rentals"
              detail="Track pieces currently with you"
              icon={Package}
            />
            <QuickLink
              href={ROUTES.account.rentals.upcoming}
              label="Upcoming deliveries"
              detail="See what is on its way"
              icon={Calendar}
            />
            <QuickLink
              href={ROUTES.wishlist}
              label="Wishlist"
              detail={`${wishlistCount} saved ${wishlistCount === 1 ? "piece" : "pieces"}`}
              icon={Heart}
            />
            <QuickLink
              href={ROUTES.account.addresses}
              label="Delivery addresses"
              detail="Manage where rentals are delivered"
              icon={MapPin}
            />
          </div>

          {!hasRole("SELLER") && (
            <Card className="mt-8">
              <CardContent className="flex flex-col gap-4 p-6 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="font-heading text-lg">List on Closiq</p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Earn from your premium wardrobe with verified seller tools.
                  </p>
                </div>
                <Button asChild variant="gold" size="sm">
                  <Link href={ROUTES.account.becomeSeller}>Become a seller</Link>
                </Button>
              </CardContent>
            </Card>
          )}

          <div className="mt-8">
            <h2 className="mb-4 font-heading text-lg">Account</h2>
            <AccountActions />
          </div>
        </>
      )}
    </Container>
  );
}
