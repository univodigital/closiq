"use client";

import { useState } from "react";
import { Menu } from "lucide-react";
import { Drawer } from "vaul";
import { useAuth } from "@/providers/AuthProvider";
import { AccountNav, accountPageTitle } from "./AccountNav";
import { usePathname } from "next/navigation";
import { Button } from "@/components/ui/button";

export function AccountMobileNav() {
  const [open, setOpen] = useState(false);
  const { hasRole } = useAuth();
  const pathname = usePathname();
  const isSeller = hasRole("SELLER");
  const title = accountPageTitle(pathname, isSeller);

  return (
    <Drawer.Root open={open} onOpenChange={setOpen}>
      <div className="flex items-center justify-between gap-3 border-b border-border pb-4 md:hidden">
        <div>
          <p className="label-caps text-muted-foreground">Account</p>
          <p className="font-heading text-lg">{title}</p>
        </div>
        <Drawer.Trigger asChild>
          <Button variant="outline" size="sm" aria-label="Open account menu">
            <Menu className="h-4 w-4" />
            Menu
          </Button>
        </Drawer.Trigger>
      </div>

      <Drawer.Portal>
        <Drawer.Overlay className="fixed inset-0 z-50 bg-foreground/20" />
        <Drawer.Content
          className="fixed inset-x-0 bottom-0 z-50 flex max-h-[85vh] flex-col rounded-t-sm border-t border-border bg-background outline-none"
          aria-describedby={undefined}
        >
          <div className="mx-auto mt-3 h-1 w-12 shrink-0 rounded-full bg-border" />
          <div className="overflow-y-auto px-5 py-6 pb-[env(safe-area-inset-bottom)]">
            <Drawer.Title className="label-caps mb-6 text-muted-foreground">
              Account navigation
            </Drawer.Title>
            <AccountNav
              isSeller={isSeller}
              onNavigate={() => setOpen(false)}
            />
          </div>
        </Drawer.Content>
      </Drawer.Portal>
    </Drawer.Root>
  );
}
