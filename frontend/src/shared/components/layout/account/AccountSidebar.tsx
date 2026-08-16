"use client";

import { useAuth } from "@/providers/AuthProvider";
import { AccountNav } from "./AccountNav";

export function AccountSidebar() {
  const { hasRole } = useAuth();

  return (
    <aside
      className="hidden w-52 shrink-0 md:block lg:w-56"
      aria-label="Account sidebar"
    >
      <div className="sticky top-14 max-h-[calc(100vh-3.5rem)] overflow-y-auto pb-8">
        <AccountNav isSeller={hasRole("SELLER")} />
      </div>
    </aside>
  );
}
