"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { toast } from "sonner";
import { requestSellerPayout } from "@/features/seller/services/seller-booking-management.service";
import { sellerService } from "@/features/seller/services";
import { PageHeader } from "@/shared/components/layout/Container";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { ApiError } from "@/lib/api-client";
import { formatCurrency } from "@/lib/format";
import Link from "next/link";
import { ROUTES } from "@/shared/constants/routes";

export default function SellerWalletPage() {
  const queryClient = useQueryClient();
  const [showPayoutForm, setShowPayoutForm] = useState(false);
  const [amount, setAmount] = useState("");
  const [payoutMethodId, setPayoutMethodId] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const { data, isLoading } = useQuery({
    queryKey: ["seller", "wallet"],
    queryFn: () => sellerService.getWallet(),
  });

  const wallet = data?.data;
  const minPayout = wallet?.minPayoutAmount ?? 500;

  const verifiedMethods = useMemo(
    () => (wallet?.payoutMethods ?? []).filter((m) => m.verified),
    [wallet?.payoutMethods],
  );

  const defaultMethod = verifiedMethods.find((m) => m.isDefault) ?? verifiedMethods[0];

  async function handleRequestPayout() {
    if (!wallet) return;

    const parsedAmount = Number(amount);
    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      toast.error("Enter a valid amount");
      return;
    }
    if (parsedAmount < minPayout) {
      toast.error(`Minimum payout is ${formatCurrency(minPayout)}`);
      return;
    }
    if (parsedAmount > wallet.availableBalance) {
      toast.error("Amount exceeds available balance");
      return;
    }

    const methodId = payoutMethodId || defaultMethod?.id;
    if (!methodId) {
      toast.error("Add and verify a bank account before requesting payout");
      return;
    }

    setSubmitting(true);
    try {
      await requestSellerPayout({
        amount: parsedAmount,
        payoutMethodId: methodId,
        idempotencyKey: crypto.randomUUID(),
      });
      toast.success("Payout requested");
      setShowPayoutForm(false);
      setAmount("");
      await queryClient.invalidateQueries({ queryKey: ["seller", "wallet"] });
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Could not request payout");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div>
      <PageHeader title="Wallet" />
      {isLoading ? (
        <p className="text-muted-foreground">Loading…</p>
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-3">
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
            <Card>
              <CardContent className="p-5">
                <p className="label-caps text-muted-foreground">Total earned</p>
                <p className="mt-2 font-mono text-3xl">{formatCurrency(wallet?.totalEarned ?? 0)}</p>
              </CardContent>
            </Card>
          </div>

          <div className="mt-6 flex flex-wrap items-center gap-3">
            <Button
              onClick={() => {
                setShowPayoutForm((v) => !v);
                if (!payoutMethodId && defaultMethod) setPayoutMethodId(defaultMethod.id);
              }}
              disabled={(wallet?.availableBalance ?? 0) < minPayout}
            >
              Request payout
            </Button>
            {!wallet?.payoutProviderConfigured ? (
              <p className="text-xs text-muted-foreground">
                Payout provider integration is required for actual bank transfer. Requests are recorded
                internally.
              </p>
            ) : null}
          </div>

          {showPayoutForm ? (
            <Card className="mt-4 max-w-md">
              <CardContent className="space-y-4 p-5">
                <h3 className="font-medium">Request payout</h3>
                {verifiedMethods.length === 0 ? (
                  <p className="text-sm text-muted-foreground">
                    Add and verify a bank account before requesting payout.{" "}
                    <Link href={ROUTES.seller.settings} className="underline">
                      Seller settings
                    </Link>
                  </p>
                ) : (
                  <>
                    <p className="text-sm text-muted-foreground">
                      Available: {formatCurrency(wallet?.availableBalance ?? 0)} · Min{" "}
                      {formatCurrency(minPayout)}
                    </p>
                    <div className="space-y-2">
                      <label htmlFor="payout-amount" className="text-sm text-muted-foreground">
                        Amount
                      </label>
                      <Input
                        id="payout-amount"
                        type="number"
                        min={minPayout}
                        max={wallet?.availableBalance ?? 0}
                        value={amount}
                        onChange={(e) => setAmount(e.target.value)}
                        placeholder={String(wallet?.availableBalance ?? 0)}
                      />
                    </div>
                    <div className="space-y-2">
                      <label htmlFor="payout-method" className="text-sm text-muted-foreground">
                        Bank account
                      </label>
                      <select
                        id="payout-method"
                        className="w-full rounded-sm border border-border bg-background px-3 py-2 text-sm"
                        value={payoutMethodId || defaultMethod?.id || ""}
                        onChange={(e) => setPayoutMethodId(e.target.value)}
                      >
                        {verifiedMethods.map((method) => (
                          <option key={method.id} value={method.id}>
                            {method.label}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="flex gap-3">
                      <Button onClick={handleRequestPayout} disabled={submitting}>
                        {submitting ? "Submitting…" : "Confirm payout"}
                      </Button>
                      <Button variant="outline" onClick={() => setShowPayoutForm(false)} disabled={submitting}>
                        Cancel
                      </Button>
                    </div>
                  </>
                )}
              </CardContent>
            </Card>
          ) : null}

          <section className="mt-10">
            <h2 className="label-caps mb-4 text-muted-foreground">Recent transactions</h2>
            <div className="space-y-2">
              {wallet?.transactions.map((t) => (
                <div key={t.id} className="flex justify-between rounded-sm border border-border px-4 py-3 text-sm">
                  <div>
                    <span>{t.label}</span>
                    {t.status !== "completed" ? (
                      <span className="ml-2 text-xs capitalize text-muted-foreground">{t.status}</span>
                    ) : null}
                  </div>
                  <span className={`font-mono ${t.amount >= 0 ? "text-success" : "text-muted-foreground"}`}>
                    {t.amount >= 0 ? "+" : ""}
                    {formatCurrency(t.amount)}
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
