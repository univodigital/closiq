import Image from "next/image";
import Link from "next/link";
import { cn } from "@/lib/utils";

const HEIGHTS = {
  xs: 32,
  sm: 40,
  nav: 60,
  md: 52,
  lg: 72,
  xl: 96,
  hero: 176,
} as const;

type LogoSize = keyof typeof HEIGHTS;

type LogoVariant = "horizontal" | "stacked";

const VARIANTS = {
  horizontal: {
    src: "/logo.svg",
    /** Full lockup viewBox width / height (1935 × 1465). */
    aspect: 1935 / 1465,
  },
  stacked: {
    src: "/logo-stacked.png",
    /** Icon + wordmark + tagline — square asset. */
    aspect: 1,
  },
} as const satisfies Record<LogoVariant, { src: string; aspect: number }>;

export function Logo({
  href,
  size = "nav",
  variant = "horizontal",
  className,
  priority = false,
}: {
  href?: string;
  size?: LogoSize;
  variant?: LogoVariant;
  className?: string;
  priority?: boolean;
}) {
  const { src, aspect } = VARIANTS[variant];
  const height = HEIGHTS[size];
  const width = Math.round(height * aspect);

  const image = (
    <span
      className={cn("inline-flex shrink-0 items-center justify-center overflow-hidden", className)}
      style={{ height, width }}
    >
      <Image
        src={src}
        alt="Closiq"
        width={width}
        height={height}
        priority={priority}
        className="h-full w-auto max-w-none object-contain object-center"
      />
    </span>
  );

  if (href === undefined) return image;

  return (
    <Link href={href} className="inline-flex shrink-0 items-center">
      {image}
    </Link>
  );
}
