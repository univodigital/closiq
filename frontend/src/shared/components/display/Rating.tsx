import { cn } from "@/lib/utils";

export function Rating({ value, count }: { value: number; count?: number }) {
  return (
    <div className="flex items-center gap-1.5 text-sm">
      <span className="text-accent">★</span>
      <span className="font-medium">{value.toFixed(1)}</span>
      {count != null && <span className="text-muted-foreground">({count})</span>}
    </div>
  );
}

export function ReviewCard({
  rating,
  comment,
  author,
  className,
}: {
  rating: number;
  comment: string;
  author: string;
  className?: string;
}) {
  return (
    <blockquote className={cn("rounded-sm border border-border bg-muted/50 p-5", className)}>
      <Rating value={rating} />
      <p className="mt-3 text-sm leading-relaxed text-foreground">&ldquo;{comment}&rdquo;</p>
      <footer className="mt-4 label-caps text-muted-foreground">— {author}</footer>
    </blockquote>
  );
}
