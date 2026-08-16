import {
  BarChart3,
  CalendarCheck,
  CalendarRange,
  LayoutDashboard,
  Settings,
  Shirt,
  Wallet,
  type LucideIcon,
} from "lucide-react";
import { ROUTES } from "./routes";

export type SellerNavItem = {
  href: string;
  label: string;
  icon: LucideIcon;
  /** When true, only exact pathname matches count as active (no prefix matching). */
  exact?: boolean;
};

export const SELLER_NAV_ITEMS: SellerNavItem[] = [
  { href: ROUTES.seller.dashboard, label: "Dashboard", icon: LayoutDashboard, exact: true },
  { href: ROUTES.seller.products, label: "Listings", icon: Shirt },
  { href: ROUTES.seller.bookings, label: "Bookings", icon: CalendarCheck },
  { href: ROUTES.seller.inventory, label: "Inventory", icon: CalendarRange },
  { href: ROUTES.seller.wallet, label: "Wallet", icon: Wallet },
  { href: ROUTES.seller.analytics, label: "Analytics", icon: BarChart3 },
];

export const SELLER_SETTINGS_ITEM: SellerNavItem = {
  href: ROUTES.seller.settings,
  label: "Settings",
  icon: Settings,
};

function normalizePathname(pathname: string): string {
  if (pathname.length > 1 && pathname.endsWith("/")) {
    return pathname.slice(0, -1);
  }
  return pathname;
}

export function isSellerNavActive(
  pathname: string,
  href: string,
  exact?: boolean,
): boolean {
  const path = normalizePathname(pathname);
  const target = normalizePathname(href);

  if (exact) {
    return path === target;
  }
  return path === target || (target !== "/" && path.startsWith(`${target}/`));
}

export function isSellerNavItemActive(pathname: string, item: SellerNavItem): boolean {
  return isSellerNavActive(pathname, item.href, item.exact);
}

export function sellerPageTitle(pathname: string): string {
  const item =
    SELLER_NAV_ITEMS.find((i) => isSellerNavItemActive(pathname, i)) ??
    (isSellerNavItemActive(pathname, SELLER_SETTINGS_ITEM) ? SELLER_SETTINGS_ITEM : undefined);
  return item?.label ?? "Seller";
}
