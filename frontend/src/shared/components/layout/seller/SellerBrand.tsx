import Link from "next/link";
import { ROUTES } from "@/shared/constants/routes";
import { Logo } from "@/shared/components/layout/Logo";

export function SellerBrand({ className }: { className?: string }) {
  return (
    <Link href={ROUTES.home} className={className}>
      <Logo size="sm" />
      <span className="label-caps mt-3 block text-muted-foreground">Seller mode</span>
    </Link>
  );
}
