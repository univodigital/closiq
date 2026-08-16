import { SellerGuard } from "@/features/seller/components/SellerGuard";
import { SellerSidebar } from "@/shared/components/layout/SellerSidebar";
import { SellerHeader } from "@/shared/components/layout/seller/SellerHeader";
import { SellerMobileNav } from "@/shared/components/layout/seller/SellerMobileNav";
import { SellerProvider } from "@/providers/SellerProvider";

export default function SellerLayout({ children }: { children: React.ReactNode }) {
  return (
    <SellerProvider>
      <SellerGuard>
        <SellerHeader />
        <div className="flex min-h-[calc(100vh-3.5rem)]">
          <SellerSidebar />
          <div className="flex min-w-0 flex-1 flex-col">
            <SellerMobileNav />
            <main className="flex-1 p-5 lg:p-8">{children}</main>
          </div>
        </div>
      </SellerGuard>
    </SellerProvider>
  );
}
