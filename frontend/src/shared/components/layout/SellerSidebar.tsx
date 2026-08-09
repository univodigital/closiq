"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  ArrowLeft,
  BarChart3,
  CalendarCheck,
  CalendarRange,
  LayoutDashboard,
  Settings,
  Shirt,
  Wallet,
} from "lucide-react";
import { ROUTES } from "@/shared/constants/routes";
import { cn } from "@/lib/utils";

const items = [
  { href: ROUTES.seller.dashboard, label: "Dashboard", icon: LayoutDashboard },
  { href: ROUTES.seller.products, label: "Listings", icon: Shirt },
  { href: ROUTES.seller.bookings, label: "Bookings", icon: CalendarCheck },
  { href: ROUTES.seller.inventory, label: "Inventory", icon: CalendarRange },
  { href: ROUTES.seller.wallet, label: "Wallet", icon: Wallet },
  { href: ROUTES.seller.analytics, label: "Analytics", icon: BarChart3 },
];

export function SellerSidebar() {
  const pathname = usePathname();

  return (
    <aside className="hidden w-60 shrink-0 border-r border-border bg-background lg:block">
      <div className="sticky top-0 flex h-screen flex-col p-6">
        <p className="label-caps mb-6 text-muted-foreground">Seller mode</p>
        <nav className="flex flex-1 flex-col gap-1">
          {items.map(({ href, label, icon: Icon }) => (
            <Link
              key={href}
              href={href}
              className={cn(
                "flex items-center gap-3 rounded-sm px-3 py-2.5 text-sm transition-colors",
                pathname === href || pathname.startsWith(`${href}/`)
                  ? "bg-muted text-foreground"
                  : "text-muted-foreground hover:bg-muted/50 hover:text-foreground",
              )}
            >
              <Icon className="h-4 w-4" />
              {label}
            </Link>
          ))}
        </nav>
        <div className="mt-auto space-y-1 border-t border-border pt-4">
          <Link
            href={ROUTES.home}
            className="flex items-center gap-3 px-3 py-2 text-sm text-muted-foreground hover:text-foreground"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to shop
          </Link>
          <Link
            href={ROUTES.seller.settings}
            className="flex items-center gap-3 px-3 py-2 text-sm text-muted-foreground hover:text-foreground"
          >
            <Settings className="h-4 w-4" />
            Settings
          </Link>
        </div>
      </div>
    </aside>
  );
}
