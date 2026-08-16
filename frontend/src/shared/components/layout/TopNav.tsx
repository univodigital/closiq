"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { Heart, Search } from "lucide-react";
import { BagNavLink } from "@/shared/components/layout/BagNavLink";
import { Logo } from "@/shared/components/layout/Logo";
import { ProfileMenu } from "@/shared/components/layout/ProfileMenu";
import {
  SHOP_AUDIENCE_LABELS,
  SHOP_DISCOVER,
  SHOP_OCCASIONS,
  shopGarmentCategories,
  type ShopAudienceSlug,
} from "@/shared/constants/shop-nav";
import { AUDIENCES, ROUTES } from "@/shared/constants/routes";
import { cn } from "@/lib/utils";

const HOVER_CLOSE_DELAY_MS = 120;

function NavColumn({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="shrink-0">
      <p className="label-caps mb-1.5 whitespace-nowrap text-[11px] text-muted-foreground">{title}</p>
      <ul className="space-y-0.5">{children}</ul>
    </div>
  );
}

function MegaMenuLink({
  href,
  label,
  active,
}: {
  href: string;
  label: string;
  active: boolean;
}) {
  const router = useRouter();

  return (
    <li>
      <a
        href={href}
        onClick={(e) => {
          e.preventDefault();
          router.push(href);
        }}
        className={cn(
          "block rounded-sm px-1.5 py-1 text-sm transition-colors hover:bg-muted hover:text-accent",
          active ? "font-medium text-accent" : "text-foreground/90",
        )}
      >
        {label}
      </a>
    </li>
  );
}

function ShopMegaPanel({
  audience,
  pathname,
}: {
  audience: ShopAudienceSlug;
  pathname: string;
}) {
  const possessive = SHOP_AUDIENCE_LABELS[audience];
  const categories = shopGarmentCategories(audience);

  return (
    <div className="px-4 py-3">
      <div className="flex gap-8">
        <NavColumn title="Shop by occasion">
          {SHOP_OCCASIONS.map((item) => (
            <MegaMenuLink
              key={item.slug}
              href={ROUTES.shop.occasion(audience, item.slug)}
              label={item.label}
              active={pathname === ROUTES.shop.occasion(audience, item.slug)}
            />
          ))}
        </NavColumn>

        <NavColumn title="Shop by category">
          {categories.map((item) => (
            <MegaMenuLink
              key={item.slug}
              href={ROUTES.shop.category(audience, item.slug)}
              label={item.label}
              active={pathname === ROUTES.shop.category(audience, item.slug)}
            />
          ))}
        </NavColumn>

        <NavColumn title="Discover">
          {SHOP_DISCOVER.map((item) => {
            const href =
              item.slug === "all"
                ? ROUTES.shop.all(audience)
                : ROUTES.shop.discover(audience, item.slug);
            return (
              <MegaMenuLink
                key={item.slug}
                href={href}
                label={item.slug === "all" ? `All ${possessive}` : item.label}
                active={pathname === href}
              />
            );
          })}
        </NavColumn>
      </div>
    </div>
  );
}

function ShopNavTrigger({
  href,
  label,
  isOpen,
  isActive,
  onOpen,
}: {
  href: string;
  label: string;
  isOpen: boolean;
  isActive: boolean;
  onOpen: () => void;
}) {
  return (
    <Link
      href={href}
      aria-expanded={isOpen}
      aria-haspopup="true"
      onMouseEnter={onOpen}
      onFocus={onOpen}
      className={cn(
        "label-caps relative px-1 py-2 text-muted-foreground transition-colors",
        "hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2",
        (isOpen || isActive) && "text-accent",
        isOpen && "text-foreground underline decoration-accent decoration-2 underline-offset-8",
      )}
    >
      {label}
    </Link>
  );
}

export function TopNav() {
  const pathname = usePathname();
  const [openAudience, setOpenAudience] = useState<ShopAudienceSlug | null>(null);
  const closeTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const cancelClose = useCallback(() => {
    if (closeTimerRef.current) {
      clearTimeout(closeTimerRef.current);
      closeTimerRef.current = null;
    }
  }, []);

  const scheduleClose = useCallback(() => {
    cancelClose();
    closeTimerRef.current = setTimeout(() => setOpenAudience(null), HOVER_CLOSE_DELAY_MS);
  }, [cancelClose]);

  const openMenu = useCallback(
    (slug: ShopAudienceSlug) => {
      cancelClose();
      setOpenAudience(slug);
    },
    [cancelClose],
  );

  // Close after route change — do not unmount Links in onClick (cancels navigation).
  useEffect(() => {
    cancelClose();
    setOpenAudience(null);
  }, [pathname, cancelClose]);

  function isAudienceActive(slug: ShopAudienceSlug) {
    const base = `/shop/${slug}`;
    return pathname === base || pathname.startsWith(`${base}/`);
  }

  return (
    <header
      className="relative sticky top-0 z-50 border-b border-border bg-card/95 backdrop-blur-sm"
      onMouseLeave={scheduleClose}
    >
      <div className="relative mx-auto max-w-6xl px-4 md:px-8">
        <div className="relative flex min-h-14 items-center justify-between gap-3 py-1.5">
          <Logo href={ROUTES.home} size="sm" priority />

          <nav
            className="absolute left-1/2 hidden -translate-x-1/2 items-center gap-5 lg:flex"
            aria-label="Shop"
          >
            {AUDIENCES.map((audience) => {
              const slug = audience.slug as ShopAudienceSlug;
              const isOpen = openAudience === slug;
              return (
                <ShopNavTrigger
                  key={slug}
                  href={ROUTES.shop.all(slug)}
                  label={audience.label}
                  isOpen={isOpen}
                  isActive={isAudienceActive(slug)}
                  onOpen={() => openMenu(slug)}
                />
              );
            })}
            <Link
              href={ROUTES.occasion("new-in")}
              className={cn(
                "label-caps px-1 py-2 text-muted-foreground transition-colors hover:text-foreground",
                pathname === ROUTES.occasion("new-in") && "text-accent",
              )}
            >
              New In
            </Link>
          </nav>

          <div className="ml-auto flex items-center gap-2 md:gap-4">
            <Link
              href={ROUTES.search}
              className="flex h-9 w-9 items-center justify-center text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
              aria-label="Search"
            >
              <Search className="h-5 w-5" />
            </Link>
            <BagNavLink />
            <Link
              href={ROUTES.wishlist}
              className="flex h-9 w-9 items-center justify-center text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
              aria-label="Wishlist"
            >
              <Heart className="h-5 w-5" />
            </Link>
            <ProfileMenu />
          </div>
        </div>

        {openAudience && (
          <div
            className="absolute left-1/2 top-full z-[60] -translate-x-1/2 pt-1"
            onMouseEnter={cancelClose}
            onMouseLeave={scheduleClose}
          >
            <div className="w-[32rem] max-w-[calc(100vw-2rem)] rounded-sm border border-border bg-card shadow-md">
              <div key={openAudience} className="mega-menu-blink">
                <ShopMegaPanel audience={openAudience} pathname={pathname} />
              </div>
            </div>
          </div>
        )}
      </div>
    </header>
  );
}
