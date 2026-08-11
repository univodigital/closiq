"use client";

import Link from "next/link";
import { ProfileMenu } from "@/shared/components/layout/ProfileMenu";
import { ROUTES } from "@/shared/constants/routes";

export function SellerTopBar() {
  return (
    <header className="sticky top-0 z-40 hidden items-center justify-end gap-4 border-b border-border bg-background/90 px-8 py-4 backdrop-blur-md lg:flex">
      <Link
        href={ROUTES.home}
        className="text-sm text-muted-foreground transition-colors hover:text-foreground"
      >
        ← Shop
      </Link>
      <ProfileMenu />
    </header>
  );
}
