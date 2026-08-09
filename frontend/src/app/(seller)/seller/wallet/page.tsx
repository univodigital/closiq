"use client";

import { useQuery } from "@tanstack/react-query";
import { sellerService } from "@/features/seller/services";
import { PageHeader } from "@/shared/components/layout/Container";
import { Card, CardContent } from "@/components/ui/card";
import { formatCurrency } from "@/lib/format";

export default function SellerWalletPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["seller", "wallet"],
    queryFn: () => sellerService.getWallet(),
  });

  const wallet = data?.data;

  return (
    <div>
      <PageHeader title="Wallet" />
      {isLoading ? (
        <p className="text-muted-foreground">Loading…</p>
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2">
            <Card>
              <CardContent className="p-5">
                <p className="label-caps text-muted-foreground">Available</p>
                <p className="mt-2 font-mono text-3xl">{formatCurrency(wallet?.availableBalance ?? 0)}</p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="p-5">
                <p className="label-caps text-muted-foreground">Pending</p>
                <p className="mt-2 font-mono text-3xl">{formatCurrency(wallet?.pendingBalance ?? 0)}</p>
              </CardContent>
            </Card>
          </div>
          <section className="mt-10">
            <h2 className="label-caps mb-4 text-muted-foreground">Recent transactions</h2>
            <div className="space-y-2">
              {wallet?.transactions.map((t) => (
                <div key={t.id} className="flex justify-between rounded-sm border border-border px-4 py-3 text-sm">
                  <span>{t.label}</span>
                  <span className={`font-mono ${t.amount >= 0 ? "text-success" : "text-muted-foreground"}`}>
                    {t.amount >= 0 ? "+" : ""}{formatCurrency(t.amount)}
                  </span>
                </div>
              ))}
            </div>
          </section>
        </>
      )}
    </div>
  );
}
