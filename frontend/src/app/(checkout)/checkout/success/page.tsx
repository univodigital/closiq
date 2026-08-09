"use client";

import { useEffect } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { ROUTES } from "@/shared/constants/routes";
import { useBag } from "@/providers/BagProvider";

export default function CheckoutSuccessPage() {
  const { clear } = useBag();

  useEffect(() => {
    clear();
  }, [clear]);

  return (
    <div className="py-12 text-center">
      <p className="label-caps text-success">Booking confirmed</p>
      <h1 className="mt-4 font-heading text-3xl">You&apos;re all set</h1>
      <p className="mx-auto mt-4 max-w-md text-sm text-muted-foreground">
        Your rental is confirmed. We&apos;ll notify you when your piece is out for delivery. Remember — you have a 15-minute home trial.
      </p>
      <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:justify-center">
        <Button asChild variant="primary">
          <Link href={ROUTES.orders}>Track order</Link>
        </Button>
        <Button asChild variant="outline">
          <Link href={ROUTES.home}>Continue shopping</Link>
        </Button>
      </div>
    </div>
  );
}
