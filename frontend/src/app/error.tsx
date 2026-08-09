"use client";

import { Button } from "@/components/ui/button";
import { Container } from "@/shared/components/layout/Container";

export default function Error({ reset }: { reset: () => void }) {
  return (
    <Container className="flex min-h-[60vh] flex-col items-center justify-center py-20 text-center">
      <h1 className="font-heading text-3xl">Something went wrong</h1>
      <p className="mt-4 text-sm text-muted-foreground">We couldn&apos;t load this page.</p>
      <Button variant="outline" className="mt-8" onClick={reset}>Try again</Button>
    </Container>
  );
}
