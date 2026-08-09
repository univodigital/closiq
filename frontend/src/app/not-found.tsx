import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Container } from "@/shared/components/layout/Container";
import { ROUTES } from "@/shared/constants/routes";

export default function NotFound() {
  return (
    <Container className="flex min-h-[60vh] flex-col items-center justify-center py-20 text-center">
      <p className="label-caps text-muted-foreground">404</p>
      <h1 className="mt-4 font-heading text-4xl">Page not found</h1>
      <p className="mt-4 max-w-sm text-sm text-muted-foreground">The page you&apos;re looking for doesn&apos;t exist or has moved.</p>
      <Button asChild variant="primary" className="mt-8">
        <Link href={ROUTES.home}>Back to home</Link>
      </Button>
    </Container>
  );
}
