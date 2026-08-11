"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  ArrowLeft,
  FolderTree,
  LayoutDashboard,
  MessageSquare,
  Shirt,
  Store,
  Users,
} from "lucide-react";
import { ROUTES } from "@/shared/constants/routes";
import { cn } from "@/lib/utils";

const items = [
  { href: ROUTES.admin.dashboard, label: "Dashboard", icon: LayoutDashboard },
  { href: ROUTES.admin.users, label: "Users", icon: Users },
  { href: ROUTES.admin.products, label: "Products", icon: Shirt },
  { href: ROUTES.admin.categories, label: "Categories", icon: FolderTree },
  { href: ROUTES.admin.reviews, label: "Reviews", icon: MessageSquare },
  { href: ROUTES.admin.sellerApplications, label: "Seller applications", icon: Store },
];

export function AdminSidebar() {
  const pathname = usePathname();

  return (
    <aside className="hidden w-60 shrink-0 border-r border-border bg-background lg:block">
      <div className="sticky top-0 flex h-screen flex-col p-6">
        <p className="label-caps mb-6 text-muted-foreground">Admin console</p>
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
        <div className="mt-auto border-t border-border pt-4">
          <Link
            href={ROUTES.home}
            className="flex items-center gap-3 px-3 py-2 text-sm text-muted-foreground hover:text-foreground"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to shop
          </Link>
        </div>
      </div>
    </aside>
  );
}
