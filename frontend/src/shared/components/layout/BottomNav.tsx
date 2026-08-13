"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Home, Heart, Package, Search, User } from "lucide-react";
import { ROUTES } from "@/shared/constants/routes";
import { useAuth } from "@/providers/AuthProvider";
import { cn } from "@/lib/utils";

const tabs = [
  { href: ROUTES.home, label: "Home", icon: Home, protected: false },
  { href: ROUTES.search, label: "Search", icon: Search, protected: false },
  { href: ROUTES.wishlist, label: "Wishlist", icon: Heart, protected: true },
  { href: ROUTES.orders, label: "Orders", icon: Package, protected: true },
  { href: ROUTES.account.overview, label: "Account", icon: User, protected: true },
];

export function BottomNav() {
  const pathname = usePathname();
  const { isAuthenticated } = useAuth();

  return (
    <nav
      className="fixed bottom-0 left-0 right-0 z-40 border-t border-border bg-card pb-[env(safe-area-inset-bottom)] md:hidden"
      aria-label="Mobile navigation"
    >
      <div className="flex h-16 items-center justify-around">
        {tabs.map(({ href, label, icon: Icon, protected: isProtected }) => {
          const active =
            pathname === href ||
            (href === ROUTES.account.overview && pathname.startsWith("/account")) ||
            (href !== ROUTES.home && href !== ROUTES.account.overview && pathname.startsWith(href));
          return (
            <Link
              key={href}
              href={href}
              prefetch={!isProtected || isAuthenticated}
              className={cn(
                "flex flex-col items-center gap-1 px-2",
                active ? "text-accent" : "text-muted-foreground",
              )}
            >
              <Icon className="h-5 w-5" />
              <span className="label-caps text-[9px]">{label}</span>
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
