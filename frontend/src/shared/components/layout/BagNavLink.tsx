"use client";

import Link from "next/link";
import { ShoppingBag } from "lucide-react";
import { useBag } from "@/providers/BagProvider";
import { cn } from "@/lib/utils";

export function BagNavLink({ className }: { className?: string }) {
  const { count, href } = useBag();

  return (
    <Link
      href={href}
      className={cn(
        "relative flex h-9 w-9 items-center justify-center text-muted-foreground hover:text-foreground",
        className,
      )}
      aria-label={count > 0 ? `Bag, ${count} ${count === 1 ? "item" : "items"}` : "Bag"}
    >
      <ShoppingBag className="h-5 w-5" />
      {count > 0 && (
        <span className="absolute -right-0.5 -top-0.5 flex h-[18px] min-w-[18px] items-center justify-center rounded-full bg-accent px-1 text-[10px] font-medium leading-none text-accent-foreground">
          {count > 9 ? "9+" : count}
        </span>
      )}
    </Link>
  );
}
