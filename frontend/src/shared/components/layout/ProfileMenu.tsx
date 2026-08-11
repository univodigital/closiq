"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import * as DropdownMenu from "@radix-ui/react-dropdown-menu";
import {
  ArrowLeftRight,
  ChevronDown,
  Heart,
  LogOut,
  Package,
  Store,
  User,
} from "lucide-react";
import { useAuth } from "@/providers/AuthProvider";
import { useAppMode } from "@/providers/AppModeProvider";
import { ROUTES } from "@/shared/constants/routes";
import { cn } from "@/lib/utils";

const itemClass =
  "flex cursor-pointer items-center gap-2 rounded-sm px-3 py-2 text-sm text-foreground outline-none hover:bg-muted focus:bg-muted";

export function ProfileMenu() {
  const router = useRouter();
  const pathname = usePathname();
  const { user, isAuthenticated, hasRole, logout } = useAuth();
  const { setMode } = useAppMode();

  if (!isAuthenticated) {
    return (
      <Link
        href={ROUTES.login}
        className="flex h-9 w-9 items-center justify-center text-muted-foreground hover:text-foreground"
        aria-label="Sign in"
      >
        <User className="h-5 w-5" />
      </Link>
    );
  }

  const initials = user?.displayName
    ?.split(" ")
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();

  const isSeller = hasRole("SELLER");
  const inSellerArea = pathname.startsWith("/seller");

  function switchToSeller() {
    setMode("seller");
    router.push(ROUTES.seller.dashboard);
  }

  function switchToShop() {
    setMode("shop");
    router.push(ROUTES.home);
  }

  return (
    <DropdownMenu.Root>
      <DropdownMenu.Trigger asChild>
        <button
          type="button"
          className="flex items-center gap-2 rounded-sm px-2 py-1.5 text-sm text-muted-foreground outline-none transition-colors hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring"
          aria-label="Account menu"
        >
          {user?.avatarUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={user.avatarUrl}
              alt=""
              className="h-8 w-8 rounded-full border border-border object-cover"
            />
          ) : (
            <span className="flex h-8 w-8 items-center justify-center rounded-full border border-border bg-muted text-xs font-medium text-foreground">
              {initials ?? <User className="h-4 w-4" />}
            </span>
          )}
          <span className="hidden max-w-[8rem] truncate md:inline">{user?.firstName}</span>
          <ChevronDown className="hidden h-4 w-4 md:block" />
        </button>
      </DropdownMenu.Trigger>

      <DropdownMenu.Portal>
        <DropdownMenu.Content
          align="end"
          sideOffset={8}
          className="z-50 min-w-[12rem] rounded-sm border border-border bg-card p-1 shadow-md"
        >
          <DropdownMenu.Label className="px-3 py-2 text-xs text-muted-foreground">
            {user?.displayName}
          </DropdownMenu.Label>
          <DropdownMenu.Separator className="my-1 h-px bg-border" />

          <DropdownMenu.Item asChild>
            <Link href={ROUTES.account.overview} className={itemClass}>
              <User className="h-4 w-4 text-muted-foreground" />
              My Account
            </Link>
          </DropdownMenu.Item>

          <DropdownMenu.Item asChild>
            <Link href={ROUTES.orders} className={itemClass}>
              <Package className="h-4 w-4 text-muted-foreground" />
              Orders
            </Link>
          </DropdownMenu.Item>

          <DropdownMenu.Item asChild>
            <Link href={ROUTES.wishlist} className={itemClass}>
              <Heart className="h-4 w-4 text-muted-foreground" />
              Wishlist
            </Link>
          </DropdownMenu.Item>

          <DropdownMenu.Separator className="my-1 h-px bg-border" />

          {isSeller ? (
            <>
              <DropdownMenu.Item asChild>
                <Link href={ROUTES.seller.dashboard} className={itemClass}>
                  <Store className="h-4 w-4 text-muted-foreground" />
                  Seller Dashboard
                </Link>
              </DropdownMenu.Item>
              <DropdownMenu.Item
                className={itemClass}
                onSelect={(e) => {
                  e.preventDefault();
                  if (inSellerArea) switchToShop();
                  else switchToSeller();
                }}
              >
                <ArrowLeftRight className="h-4 w-4 text-muted-foreground" />
                {inSellerArea ? "Switch to shopping" : "Switch to seller"}
              </DropdownMenu.Item>
            </>
          ) : (
            <DropdownMenu.Item asChild>
              <Link href={ROUTES.account.becomeSeller} className={itemClass}>
                <Store className="h-4 w-4 text-muted-foreground" />
                Become a seller
              </Link>
            </DropdownMenu.Item>
          )}

          <DropdownMenu.Separator className="my-1 h-px bg-border" />

          <DropdownMenu.Item
            className={itemClass}
            onSelect={async (e) => {
              e.preventDefault();
              await logout();
              router.push(ROUTES.home);
            }}
          >
            <LogOut className="h-4 w-4 text-muted-foreground" />
            Logout
          </DropdownMenu.Item>
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  );
}
