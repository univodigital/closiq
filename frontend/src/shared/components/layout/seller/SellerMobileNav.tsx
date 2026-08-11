"use client";

import { useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Drawer } from "vaul";
import { sellerPageTitle } from "@/shared/constants/seller-nav";
import { ROUTES } from "@/shared/constants/routes";
import { AnimatedMenuButton } from "./AnimatedMenuButton";
import { SellerBrand } from "./SellerBrand";
import { SellerNav } from "./SellerNav";
import { ProfileMenu } from "@/shared/components/layout/ProfileMenu";

export function SellerMobileNav() {
  const [open, setOpen] = useState(false);
  const pathname = usePathname();
  const title = sellerPageTitle(pathname);

  return (
    <Drawer.Root open={open} onOpenChange={setOpen}>
      <div className="border-b border-border px-5 py-4 lg:hidden">
        <div className="flex items-start justify-between gap-4">
          <div className="flex min-w-0 items-start gap-3">
            <AnimatedMenuButton open={open} onClick={() => setOpen((value) => !value)} />
            <div className="min-w-0">
              <SellerBrand className="block" />
              <div className="mt-4">
                <p className="font-heading text-lg leading-tight">{title}</p>
              </div>
            </div>
          </div>

          <div className="flex shrink-0 items-center gap-2 pt-0.5">
            <ProfileMenu />
            <Link
              href={ROUTES.home}
              className="text-sm text-muted-foreground transition-colors hover:text-foreground"
            >
              Shop
            </Link>
          </div>
        </div>
      </div>

      <Drawer.Portal>
        <Drawer.Overlay className="fixed inset-0 z-50 bg-foreground/20" />
        <Drawer.Content
          className="fixed inset-x-0 bottom-0 z-50 flex max-h-[85vh] flex-col rounded-t-sm border-t border-border bg-background outline-none"
          aria-describedby={undefined}
        >
          <div className="mx-auto mt-3 h-1 w-12 shrink-0 rounded-full bg-border" />
          <div className="flex min-h-0 flex-1 flex-col overflow-y-auto px-5 py-6 pb-[env(safe-area-inset-bottom)]">
            <Drawer.Title className="label-caps mb-6 text-muted-foreground">
              Seller navigation
            </Drawer.Title>
            <SellerNav
              className="min-h-[40vh] flex-1"
              onNavigate={() => setOpen(false)}
            />
          </div>
        </Drawer.Content>
      </Drawer.Portal>
    </Drawer.Root>
  );
}
