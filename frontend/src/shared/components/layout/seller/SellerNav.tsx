"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import {
  isSellerNavItemActive,
  SELLER_NAV_ITEMS,
  SELLER_SETTINGS_ITEM,
} from "@/shared/constants/seller-nav";
import { ROUTES } from "@/shared/constants/routes";
import { cn } from "@/lib/utils";

export function SellerNav({
  className,
  onNavigate,
  showFooter = true,
}: {
  className?: string;
  onNavigate?: () => void;
  showFooter?: boolean;
}) {
  const pathname = usePathname();
  const settingsActive = isSellerNavItemActive(pathname, SELLER_SETTINGS_ITEM);
  const SettingsIcon = SELLER_SETTINGS_ITEM.icon;

  return (
    <nav className={cn("flex flex-col", className)} aria-label="Seller navigation">
      <ul className="flex flex-col gap-1" role="list">
        {SELLER_NAV_ITEMS.map(({ href, label, icon: Icon, exact }) => {
          const active = isSellerNavItemActive(pathname, { href, label, icon: Icon, exact });

          return (
            <li key={href}>
              <Link
                href={href}
                onClick={onNavigate}
                aria-current={active ? "page" : undefined}
                className={cn(
                  "flex items-center gap-3 rounded-sm px-3 py-2.5 text-sm transition-colors outline-none focus-visible:ring-2 focus-visible:ring-ring",
                  active
                    ? "bg-muted font-medium text-foreground"
                    : "text-muted-foreground hover:bg-muted/50 hover:text-foreground",
                )}
              >
                <Icon className="h-4 w-4 shrink-0" aria-hidden />
                {label}
              </Link>
            </li>
          );
        })}
      </ul>

      {showFooter ? (
        <div className="mt-auto space-y-1 border-t border-border pt-4">
          <Link
            href={ROUTES.home}
            onClick={onNavigate}
            className="flex items-center gap-3 rounded-sm px-3 py-2 text-sm text-muted-foreground transition-colors hover:bg-muted/50 hover:text-foreground"
          >
            <ArrowLeft className="h-4 w-4 shrink-0" aria-hidden />
            Back to shop
          </Link>
          <Link
            href={SELLER_SETTINGS_ITEM.href}
            onClick={onNavigate}
            aria-current={settingsActive ? "page" : undefined}
            className={cn(
              "flex items-center gap-3 rounded-sm px-3 py-2 text-sm transition-colors outline-none focus-visible:ring-2 focus-visible:ring-ring",
              settingsActive
                ? "bg-muted font-medium text-foreground"
                : "text-muted-foreground hover:bg-muted/50 hover:text-foreground",
            )}
          >
            <SettingsIcon className="h-4 w-4 shrink-0" aria-hidden />
            {SELLER_SETTINGS_ITEM.label}
          </Link>
        </div>
      ) : null}
    </nav>
  );
}
