"use client";

import Link from "next/link";
import { Logo } from "@/shared/components/layout/Logo";
import { ProfileMenu } from "@/shared/components/layout/ProfileMenu";
import { ROUTES } from "@/shared/constants/routes";

/** Seller top bar — same shell dimensions as TopNav; logo left, Shop + account right. */
export function SellerHeader() {
  return (
    <header className="relative sticky top-0 z-50 border-b border-border bg-card/95 backdrop-blur-sm">
      <div className="relative mx-auto max-w-6xl px-4 md:px-8">
        <div className="relative flex min-h-14 items-center justify-between gap-3 py-1.5">
          <Logo href={ROUTES.home} size="sm" priority />

          <div className="flex items-center gap-2 md:gap-4">
            <Link
              href={ROUTES.home}
              className="text-sm text-muted-foreground transition-colors hover:text-foreground"
            >
              ← Shop
            </Link>
            <ProfileMenu />
          </div>
        </div>
      </div>
    </header>
  );
}
