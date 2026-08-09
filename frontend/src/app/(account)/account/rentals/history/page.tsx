"use client";

import { Container, PageHeader } from "@/shared/components/layout/Container";
import { RentalsList } from "@/features/account/components/RentalsList";

export default function RentalHistoryPage() {
  return (
    <Container narrow embedded>
      <PageHeader title="Rental History" description="Completed and closed rentals" />
      <RentalsList
        filter="history"
        emptyTitle="No rental history yet"
        emptyDescription="Your past rentals will be listed here."
      />
    </Container>
  );
}
