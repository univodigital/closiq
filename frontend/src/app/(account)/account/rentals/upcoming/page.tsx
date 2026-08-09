"use client";

import { Container, PageHeader } from "@/shared/components/layout/Container";
import { RentalsList } from "@/features/account/components/RentalsList";

export default function UpcomingDeliveriesPage() {
  return (
    <Container narrow embedded>
      <PageHeader title="Upcoming Deliveries" description="Confirmed rentals on their way to you" />
      <RentalsList
        filter="upcoming"
        emptyTitle="No upcoming deliveries"
        emptyDescription="Book a piece and track delivery status here."
      />
    </Container>
  );
}
