"use client";

import { SellerBrand } from "@/shared/components/layout/seller/SellerBrand";
import { SellerNav } from "@/shared/components/layout/seller/SellerNav";

export function SellerSidebar() {
  return (
    <aside
      className="hidden w-60 shrink-0 border-r border-border bg-background lg:block"
      aria-label="Seller sidebar"
    >
      <div className="sticky top-14 flex h-[calc(100vh-3.5rem)] flex-col p-6">
        <SellerBrand className="mb-6 block" />
        <SellerNav className="flex-1" />
      </div>
    </aside>
  );
}
