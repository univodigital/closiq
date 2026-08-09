import { TopNav } from "@/shared/components/layout/TopNav";
import { BottomNav } from "@/shared/components/layout/BottomNav";
import { Footer } from "@/shared/components/layout/Footer";

export default function ShopLayout({ children }: { children: React.ReactNode }) {
  return (
    <>
      <TopNav />
      <main className="flex-1 pb-20 md:pb-0">{children}</main>
      <Footer />
      <BottomNav />
    </>
  );
}
