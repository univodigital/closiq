"use client";

import { Container, PageHeader } from "@/shared/components/layout/Container";
import { RentalsList } from "@/features/account/components/RentalsList";

export default function ReturnsPage() {
  return (
    <Container narrow embedded>
      <PageHeader title="Returns" description="Scheduled and completed returns" />
      <RentalsList
        filter="returns"
        emptyTitle="No returns scheduled"
        emptyDescription="Return pickups and confirmations appear here."
      />
    </Container>
  );
}
