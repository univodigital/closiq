import Image from "next/image";
import Link from "next/link";
import { cn } from "@/lib/utils";

const HEIGHTS = {
  xs: 32,
  sm: 40,
  md: 52,
  lg: 72,
  xl: 96,
  hero: 176,
} as const;

type LogoSize = keyof typeof HEIGHTS;
type LogoVariant = "full" | "icon";

const ICON_ASPECT = 1024 / 682;
const FULL_ASPECT = 1;

export function Logo({
  href,
  size = "sm",
  variant = "full",
  className,
  priority = false,
}: {
  href?: string;
  size?: LogoSize;
  variant?: LogoVariant;
  className?: string;
  priority?: boolean;
}) {
  const height = HEIGHTS[size];
  const src = variant === "icon" ? "/logo-icon.png" : "/logo.png";
  const aspect = variant === "icon" ? ICON_ASPECT : FULL_ASPECT;
  const width = Math.round(height * aspect);

  const image = (
    <Image
      src={src}
      alt="Closiq"
      width={width}
      height={height}
      priority={priority}
      className={cn("h-auto w-auto object-contain", className)}
      style={{ height, width: "auto" }}
    />
  );

  if (href === undefined) return image;

  return (
    <Link href={href} className="inline-flex shrink-0 items-center">
      {image}
    </Link>
  );
}
