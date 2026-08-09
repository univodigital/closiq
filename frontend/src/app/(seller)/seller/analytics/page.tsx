"use client";

import { useQuery } from "@tanstack/react-query";
import { sellerService } from "@/features/seller/services";
import { PageHeader } from "@/shared/components/layout/Container";
import { Card, CardContent } from "@/components/ui/card";
import { formatCurrency } from "@/lib/format";

export default function SellerAnalyticsPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["seller", "analytics"],
    queryFn: () => sellerService.getAnalytics(),
  });

  const a = data?.data;

  return (
    <div>
      <PageHeader title="Analytics" description={`Last ${a?.period ?? "30d"}`} />
      {isLoading ? (
        <p className="text-muted-foreground">Loading…</p>
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {[
              { label: "Views", value: a?.views },
              { label: "Visitors", value: a?.uniqueVisitors },
              { label: "Bookings", value: a?.bookings },
              { label: "Revenue", value: formatCurrency(a?.revenue ?? 0) },
            ].map((s) => (
              <Card key={s.label}>
                <CardContent className="p-5">
                  <p className="label-caps text-muted-foreground">{s.label}</p>
                  <p className="mt-2 font-mono text-xl">{s.value}</p>
                </CardContent>
              </Card>
            ))}
          </div>
          <section className="mt-10">
            <h2 className="label-caps mb-4 text-muted-foreground">Top products</h2>
            {a?.topProducts.map((p) => (
              <div key={p.productId} className="flex justify-between border-b border-border py-3 text-sm">
                <span>{p.title}</span>
                <span className="text-muted-foreground">{p.bookings} bookings</span>
              </div>
            ))}
          </section>
        </>
      )}
    </div>
  );
}
