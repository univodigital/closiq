import { Container, PageHeader } from "@/shared/components/layout/Container";

const faqs = [
  { q: "How does the 15-minute trial work?", a: "When your delivery arrives, try the outfit at home for 15 minutes. Keep it to start your rental, or reject it for an immediate return with no rental charge." },
  { q: "When is my deposit refunded?", a: "After the item is returned and inspected, your deposit is refunded within 3–5 business days." },
  { q: "Which areas do you deliver to?", a: "We currently serve Mumbai pincodes. Pan-India expansion is coming soon." },
];

export default function FaqPage() {
  return (
    <Container narrow className="py-10 md:py-14">
      <PageHeader title="FAQ" breadcrumb="Support" />
      <div className="space-y-8">
        {faqs.map((f) => (
          <div key={f.q}>
            <h2 className="font-heading text-lg">{f.q}</h2>
            <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{f.a}</p>
          </div>
        ))}
      </div>
    </Container>
  );
}
