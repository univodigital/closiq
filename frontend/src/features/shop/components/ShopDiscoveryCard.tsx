import Link from "next/link";
import Image from "next/image";
import { cn } from "@/lib/utils";

type ShopDiscoveryCardProps = {
  href: string;
  label: string;
  image: string;
  caption?: string;
  /** Wider editorial tile used in Discover. */
  featured?: boolean;
  className?: string;
  sizes?: string;
};

export function ShopDiscoveryCard({
  href,
  label,
  image,
  caption,
  featured = false,
  className,
  sizes,
}: ShopDiscoveryCardProps) {
  return (
    <Link
      href={href}
      className={cn(
        "group relative block overflow-hidden rounded-sm bg-muted",
        featured ? "aspect-[16/10] md:aspect-[2/1]" : "aspect-[3/4]",
        className,
      )}
    >
      <Image
        src={image}
        alt={label}
        fill
        className="object-cover transition-transform duration-700 ease-out group-hover:scale-[1.04]"
        sizes={sizes ?? (featured ? "(max-width: 768px) 100vw, 50vw" : "(max-width: 768px) 50vw, 25vw")}
      />
      <div className="absolute inset-0 bg-gradient-to-t from-primary/85 via-primary/25 to-transparent transition-opacity duration-500 group-hover:from-primary/90" />
      <div className="absolute inset-x-0 bottom-0 p-4 md:p-5">
        <p className="font-heading text-lg text-primary-foreground md:text-xl">{label}</p>
        {caption && (
          <p className="mt-1 text-xs text-primary-foreground/75 md:text-sm">{caption}</p>
        )}
      </div>
    </Link>
  );
}
