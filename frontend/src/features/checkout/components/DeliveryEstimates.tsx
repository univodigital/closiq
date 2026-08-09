"use client";

import Image from "next/image";
import { formatDateRange } from "@/lib/format";
import type { Product } from "@/shared/types";

export function DeliveryEstimates({
  product,
  start,
  end,
}: {
  product?: Product;
  start: string;
  end: string;
}) {
  if (!product || !start || !end) return null;

  return (
    <div className="rounded-sm border border-border p-5">
      <p className="label-caps text-muted-foreground">Delivery estimates</p>
      <div className="mt-4 flex gap-3">
        <div className="relative h-16 w-12 shrink-0 overflow-hidden rounded-sm bg-muted">
          {product.images[0] && (
            <Image src={product.images[0]} alt={product.title} fill className="object-cover" sizes="48px" />
          )}
        </div>
        <div className="min-w-0 text-sm">
          <p className="line-clamp-2 font-medium">{product.title}</p>
          <p className="mt-1 text-muted-foreground">Rental · {formatDateRange(start, end)}</p>
          <p className="mt-1 text-muted-foreground">Estimated delivery by {formatDeliveryDate(start)}</p>
        </div>
      </div>
    </div>
  );
}

function formatDeliveryDate(start: string) {
  const d = new Date(start);
  d.setDate(d.getDate() - 1);
  return new Intl.DateTimeFormat("en-IN", { day: "numeric", month: "short", weekday: "short" }).format(d);
}
