import { TopNav } from "@/shared/components/layout/TopNav";
import { BottomNav } from "@/shared/components/layout/BottomNav";
import { AccountSidebar } from "@/shared/components/layout/account/AccountSidebar";
import { AccountMobileNav } from "@/shared/components/layout/account/AccountMobileNav";

export default function AccountLayout({ children }: { children: React.ReactNode }) {
  return (
    <>
      <TopNav />
      <div className="mx-auto flex w-full max-w-7xl flex-1 gap-6 px-5 py-8 md:gap-10 md:px-12 lg:py-12">
        <AccountSidebar />
        <div className="flex min-w-0 flex-1 flex-col">
          <AccountMobileNav />
          <main className="min-w-0 flex-1 pb-20 md:pb-0">{children}</main>
        </div>
      </div>
      <BottomNav />
    </>
  );
}
