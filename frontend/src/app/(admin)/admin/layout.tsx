import Link from "next/link";
import { AdminSidebar } from "@/shared/components/layout/AdminSidebar";
import { AdminGuard } from "@/features/admin/components/AdminGuard";
import { Logo } from "@/shared/components/layout/Logo";
import { ROUTES } from "@/shared/constants/routes";

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <AdminGuard>
      <header className="sticky top-0 z-50 border-b border-border bg-card/95 backdrop-blur-sm">
        <div className="mx-auto max-w-6xl px-4 md:px-8">
          <div className="flex min-h-[4.75rem] items-center justify-between gap-3 py-2">
            <Logo href={ROUTES.admin.dashboard} size="nav" priority />
            <Link
              href={ROUTES.home}
              className="text-sm text-muted-foreground transition-colors hover:text-foreground"
            >
              ← Shop
            </Link>
          </div>
        </div>
      </header>
      <div className="flex min-h-[calc(100vh-4.75rem)]">
        <AdminSidebar />
        <main className="flex-1 p-5 lg:p-8">{children}</main>
      </div>
    </AdminGuard>
  );
}
