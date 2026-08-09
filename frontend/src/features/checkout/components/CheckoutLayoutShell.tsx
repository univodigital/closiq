import Link from "next/link";
import { ROUTES } from "@/shared/constants/routes";
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
      <header className="border-b border-border px-5 py-5 md:px-12">
        <div className="mx-auto flex max-w-6xl flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <Link href={ROUTES.home} className="font-heading text-xl">
            Closiq<span className="text-accent">.</span>
          </Link>
          <CheckoutProgress current={step} queryString={queryString} />
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-5 py-8 md:px-12">{children}</main>
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
