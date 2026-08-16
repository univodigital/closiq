import { ROUTES } from "@/shared/constants/routes";
import { Logo } from "@/shared/components/layout/Logo";
import { CheckoutProgress } from "@/features/checkout/components/CheckoutProgress";
import type { CheckoutStep } from "@/features/checkout/components/CheckoutProgress";

export function CheckoutLayoutShell({
  children,
  step,
  queryString,
}: {
  children: React.ReactNode;
  step: CheckoutStep;
  queryString: string;
}) {
  return (
    <div className="min-h-screen bg-background">
      <header className="sticky top-0 z-50 border-b border-border bg-card/95 backdrop-blur-sm">
        <div className="mx-auto max-w-6xl px-4 md:px-8">
          <div className="flex min-h-[4.75rem] flex-col justify-center gap-4 py-2 sm:flex-row sm:items-center sm:justify-between">
            <Logo href={ROUTES.home} size="nav" priority />
            <CheckoutProgress current={step} queryString={queryString} />
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-8 md:px-8">{children}</main>
    </div>
  );
}

export function CheckoutTwoColumn({
  main,
  sidebar,
}: {
  main: React.ReactNode;
  sidebar: React.ReactNode;
}) {
  return (
    <div className="grid gap-8 lg:grid-cols-[1fr_340px] lg:items-start">
      <div>{main}</div>
      <aside className="space-y-4 lg:sticky lg:top-8">{sidebar}</aside>
    </div>
  );
}
