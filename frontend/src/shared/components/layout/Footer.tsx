import Link from "next/link";
import { Container } from "./Container";
import { Logo } from "./Logo";
import { ROUTES } from "@/shared/constants/routes";

export function Footer() {
  return (
    <footer className="mt-auto border-t border-border bg-card py-12">
      <Container>
        <div className="flex flex-col gap-8 md:flex-row md:justify-between">
          <div>
            <Logo href={ROUTES.home} size="nav" />
            <p className="mt-2 max-w-xs text-sm text-muted-foreground">
              Premium clothing rental with a 15-minute home trial. Mumbai.
            </p>
          </div>
          <div className="flex gap-12 text-sm">
            <div className="space-y-2">
              <p className="label-caps text-muted-foreground">Shop</p>
              <Link href="/products" className="block hover:text-accent">All products</Link>
              <Link href="/support" className="block hover:text-accent">Help</Link>
            </div>
            <div className="space-y-2">
              <p className="label-caps text-muted-foreground">Legal</p>
              <Link href="/support/faq" className="block hover:text-accent">FAQ</Link>
              <Link href="#" className="block hover:text-accent">Terms</Link>
            </div>
          </div>
        </div>
        <p className="mt-10 text-xs text-muted-foreground">© {new Date().getFullYear()} Closiq. All rights reserved.</p>
      </Container>
    </footer>
  );
}
