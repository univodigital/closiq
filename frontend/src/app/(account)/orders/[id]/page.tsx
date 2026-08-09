"use client";

import { use } from "react";
import Image from "next/image";
import { useQuery } from "@tanstack/react-query";
import { orderService, bookingService } from "@/features/orders/services";
import { Container, PageHeader } from "@/shared/components/layout/Container";
import { StatusBadge } from "@/components/ui/badge";
import { OrderTimeline } from "@/shared/components/display/OrderTimeline";
import { Button } from "@/components/ui/button";
import { formatCurrency, formatDateRange } from "@/lib/format";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";

export default function OrderDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const qc = useQueryClient();
  const { data, isLoading } = useQuery({
    queryKey: ["order", id],
    queryFn: () => orderService.getOrder(id),
  });

  const order = data?.data;

  if (isLoading || !order) {
    return (
      <Container narrow embedded>
        <PageHeader title="Order" />
        <p className="text-muted-foreground">Loading…</p>
      </Container>
    );
  }

  const handleTrialAccept = async () => {
    await bookingService.acceptTrial(order.id);
    toast.success("Enjoy your rental!");
    qc.invalidateQueries({ queryKey: ["order", id] });
  };

  return (
    <Container narrow embedded>
      <PageHeader title={order.productTitle} breadcrumb={`Order ${order.orderNumber}`} />
      <div className="relative mb-6 aspect-video overflow-hidden rounded-sm bg-muted">
        <Image src={order.productImage} alt="" fill className="object-cover" sizes="768px" />
      </div>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <StatusBadge status={order.status} />
        <p className="font-mono text-sm">{formatCurrency(order.totalPaid)} paid</p>
      </div>
      <p className="text-sm text-muted-foreground">
        {formatDateRange(order.rentalStart, order.rentalEnd)} · Size {order.variantSize}
      </p>

      {order.status === "trial_ready" && (
        <div className="mt-8 rounded-sm border border-accent/30 bg-gold-light p-6">
          <p className="label-caps text-accent">15-minute home trial</p>
          <p className="mt-2 font-heading text-xl">Your agent has arrived</p>
          <p className="mt-2 text-sm text-muted-foreground">Try the outfit at home. Keep it or return immediately — no rental charge if you reject.</p>
          <div className="mt-6 flex flex-wrap gap-3">
            <Button variant="primary" onClick={handleTrialAccept}>Keep & start rental</Button>
            <Button variant="destructive">Return now</Button>
          </div>
        </div>
      )}

      <section className="mt-10">
        <h2 className="label-caps mb-6 text-muted-foreground">Timeline</h2>
        <OrderTimeline events={order.timeline} />
      </section>
    </Container>
  );
}
