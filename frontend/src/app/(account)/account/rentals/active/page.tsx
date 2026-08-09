"use client";

import { Container, PageHeader } from "@/shared/components/layout/Container";
import { RentalsList } from "@/features/account/components/RentalsList";

export default function ActiveRentalsPage() {
  return (
    <Container narrow embedded>
      <PageHeader title="Active Rentals" description="Pieces currently with you or ready for trial" />
      <RentalsList
        filter="active"
        emptyTitle="No active rentals"
        emptyDescription="When a rental is live, it will show up here."
      />
    </Container>
  );
}
