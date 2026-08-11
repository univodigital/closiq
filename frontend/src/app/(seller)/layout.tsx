import Link from "next/link";
import { SellerGuard } from "@/features/seller/components/SellerGuard";
import { SellerSidebar } from "@/shared/components/layout/SellerSidebar";
import { SellerMobileNav } from "@/shared/components/layout/seller/SellerMobileNav";
import { SellerTopBar } from "@/shared/components/layout/seller/SellerTopBar";
import { SellerProvider } from "@/providers/SellerProvider";

export default function SellerLayout({ children }: { children: React.ReactNode }) {
  return (
    <SellerProvider>
      <SellerGuard>
        <div className="flex min-h-screen">
          <SellerSidebar />
          <div className="flex min-h-screen flex-1 flex-col">
            <SellerMobileNav />
            <SellerTopBar />
            <main className="flex-1 p-5 lg:p-8">{children}</main>
          </div>
        </div>
      </SellerGuard>
    </SellerProvider>
  );
}
