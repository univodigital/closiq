import Link from "next/link";
import { Container, PageHeader } from "@/shared/components/layout/Container";
import { ROUTES } from "@/shared/constants/routes";

export default function SupportPage() {
  return (
    <Container narrow className="py-10 md:py-14">
      <PageHeader title="Help & support" description="We're here for your rental journey." />
      <div className="space-y-4 text-sm">
        <p>For order issues, trial questions, or returns — reach us at <a href="mailto:hello@closiq.com" className="text-accent">hello@closiq.com</a></p>
        <Link href={ROUTES.supportFaq} className="block text-accent hover:underline">Read FAQ →</Link>
      </div>
    </Container>
  );
}
