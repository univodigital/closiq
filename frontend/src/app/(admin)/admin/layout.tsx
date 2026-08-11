import Link from "next/link";
import { AdminSidebar } from "@/shared/components/layout/AdminSidebar";
import { AdminGuard } from "@/features/admin/components/AdminGuard";
import { Logo } from "@/shared/components/layout/Logo";
import { ROUTES } from "@/shared/constants/routes";

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <AdminGuard>
      <div className="flex min-h-screen">
        <AdminSidebar />
        <div className="flex min-h-screen flex-1 flex-col">
          <header className="flex items-center justify-between border-b border-border px-5 py-4 lg:px-8">
            <div className="flex items-center gap-2 lg:hidden">
              <Logo href={ROUTES.admin.dashboard} size="xs" />
              <span className="font-heading text-sm text-muted-foreground">Admin</span>
            </div>
            <Link href={ROUTES.home} className="text-sm text-muted-foreground hover:text-foreground">
              ← Shop
            </Link>
          </header>
          <main className="flex-1 p-5 lg:p-8">{children}</main>
        </div>
      </div>
    </AdminGuard>
  );
}
