import { Skeleton } from "@/components/ui/skeleton";
import { Container } from "@/shared/components/layout/Container";

export default function Loading() {
  return (
    <Container className="py-14">
      <Skeleton className="mb-8 h-10 w-48" />
      <div className="grid grid-cols-2 gap-5 md:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <Skeleton key={i} className="aspect-[3/4]" />
        ))}
      </div>
    </Container>
  );
}
