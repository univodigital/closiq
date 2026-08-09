"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  buildAccountNavSections,
  isAccountNavActive,
  type AccountNavItem,
  type AccountNavSection,
} from "@/shared/constants/account-nav";
import { cn } from "@/lib/utils";

function NavLink({
  item,
  pathname,
  onNavigate,
}: {
  item: AccountNavItem;
  pathname: string;
  onNavigate?: () => void;
}) {
  const active = isAccountNavActive(pathname, item);

  return (
    <Link
      href={item.href}
      onClick={onNavigate}
      aria-current={active ? "page" : undefined}
      className={cn(
        "block rounded-sm px-3 py-2 text-sm transition-colors outline-none focus-visible:ring-2 focus-visible:ring-ring",
        active
          ? "bg-muted font-medium text-foreground"
          : "text-muted-foreground hover:bg-muted/60 hover:text-foreground",
      )}
    >
      {item.label}
    </Link>
  );
}

function NavSection({
  section,
  pathname,
  onNavigate,
}: {
  section: AccountNavSection;
  pathname: string;
  onNavigate?: () => void;
}) {
  return (
    <div>
      <p className="label-caps mb-2 px-3 text-muted-foreground">{section.title}</p>
      <ul className="space-y-0.5" role="list">
        {section.items.map((item) => (
          <li key={item.href}>
            <NavLink item={item} pathname={pathname} onNavigate={onNavigate} />
          </li>
        ))}
      </ul>
    </div>
  );
}

export function AccountNav({
  isSeller,
  className,
  onNavigate,
}: {
  isSeller: boolean;
  className?: string;
  onNavigate?: () => void;
}) {
  const pathname = usePathname();
  const sections = buildAccountNavSections(isSeller);

  return (
    <nav className={cn("space-y-6", className)} aria-label="Account navigation">
      {sections.map((section) => (
        <NavSection
          key={section.id}
          section={section}
          pathname={pathname}
          onNavigate={onNavigate}
        />
      ))}
    </nav>
  );
}

export function accountNavFlat(isSeller: boolean) {
  return buildAccountNavSections(isSeller).flatMap((s) => s.items);
}

export function accountPageTitle(pathname: string, isSeller: boolean): string {
  const item = accountNavFlat(isSeller).find((i) => isAccountNavActive(pathname, i));
  return item?.label ?? "Account";
}
