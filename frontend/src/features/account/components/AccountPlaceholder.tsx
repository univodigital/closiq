import Link from "next/link";
import { Container, PageHeader } from "@/shared/components/layout/Container";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

export function AccountPlaceholder({
  title,
  description,
  actionLabel,
  actionHref,
}: {
  title: string;
  description: string;
  actionLabel?: string;
  actionHref?: string;
}) {
  return (
    <Container narrow embedded>
      <PageHeader title={title} />
      <Card>
        <CardContent className="space-y-4 p-6">
          <p className="text-sm text-muted-foreground">{description}</p>
          {actionLabel && actionHref && (
            <Button asChild variant="outline" size="sm">
              <Link href={actionHref}>{actionLabel}</Link>
            </Button>
          )}
        </CardContent>
      </Card>
    </Container>
  );
}
