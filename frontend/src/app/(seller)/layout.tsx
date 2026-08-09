import Link from "next/link";
import { SellerGuard } from "@/features/seller/components/SellerGuard";
import { SellerSidebar } from "@/shared/components/layout/SellerSidebar";
import { SellerProvider } from "@/providers/SellerProvider";
import { ROUTES } from "@/shared/constants/routes";

export default function SellerLayout({ children }: { children: React.ReactNode }) {
  return (
    <SellerProvider>
      <SellerGuard>
        <div className="flex min-h-screen">
          <SellerSidebar />
          <div className="flex min-h-screen flex-1 flex-col">
            <header className="flex items-center justify-between border-b border-border px-5 py-4 lg:px-8">
              <Link href={ROUTES.seller.dashboard} className="font-heading text-xl lg:hidden">
                Closiq<span className="text-accent">.</span> Seller
              </Link>
              <Link href={ROUTES.home} className="text-sm text-muted-foreground hover:text-foreground">
                ← Shop
              </Link>
            </header>
            <main className="flex-1 p-5 lg:p-8">{children}</main>
          </div>
        </div>
      </SellerGuard>
    </SellerProvider>
  );
}
