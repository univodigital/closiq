"use client";

import { useQuery } from "@tanstack/react-query";
import { sellerService } from "@/features/seller/services";
import { useSeller } from "@/providers/SellerProvider";
import { PageHeader } from "@/shared/components/layout/Container";
import { Card, CardContent } from "@/components/ui/card";
import { formatCurrency } from "@/lib/format";
import { Skeleton } from "@/components/ui/skeleton";

export default function SellerSettingsPage() {
  const { profile, isLoading: profileLoading } = useSeller();
  const { data: walletData, isLoading: walletLoading } = useQuery({
    queryKey: ["seller", "wallet"],
    queryFn: () => sellerService.getWallet(),
  });

  const loading = profileLoading || walletLoading;

  return (
    <div>
      <PageHeader title="Seller settings" description="Business profile and payout summary" />
      {loading ? (
        <Skeleton className="h-40 w-full max-w-lg" />
      ) : (
        <Card className="max-w-lg">
          <CardContent className="space-y-4 p-6">
            <div>
              <p className="label-caps text-muted-foreground">Business</p>
              <p className="mt-1 font-heading text-xl">{profile?.businessName}</p>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <p className="label-caps text-muted-foreground">City</p>
                <p className="mt-1">{profile?.city || "—"}</p>
              </div>
              <div>
                <p className="label-caps text-muted-foreground">Active listings</p>
                <p className="mt-1">{profile?.listingCount ?? 0}</p>
              </div>
              <div>
                <p className="label-caps text-muted-foreground">Rating</p>
                <p className="mt-1">{profile && profile.rating > 0 ? profile.rating.toFixed(1) : "—"}</p>
              </div>
              <div>
                <p className="label-caps text-muted-foreground">Available balance</p>
                <p className="mt-1 font-mono">{formatCurrency(walletData?.data.availableBalance ?? 0)}</p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
