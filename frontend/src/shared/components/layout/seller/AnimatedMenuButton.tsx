"use client";

import { cn } from "@/lib/utils";

export function AnimatedMenuButton({
  open,
  onClick,
  className,
}: {
  open: boolean;
  onClick: () => void;
  className?: string;
}) {
  return (
    <button
      type="button"
      aria-label={open ? "Close seller menu" : "Open seller menu"}
      aria-expanded={open}
      onClick={onClick}
      className={cn(
        "mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-sm text-foreground transition-colors hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
        className,
      )}
    >
      <span className="relative block h-4 w-5" aria-hidden>
        <span
          className={cn(
            "absolute left-0 block h-0.5 w-5 rounded-full bg-current transition-all duration-300 ease-out",
            open ? "top-[7px] rotate-45" : "top-0",
          )}
        />
        <span
          className={cn(
            "absolute left-0 top-[7px] block h-0.5 w-5 rounded-full bg-current transition-all duration-300 ease-out",
            open ? "scale-x-0 opacity-0" : "scale-x-100 opacity-100",
          )}
        />
        <span
          className={cn(
            "absolute left-0 block h-0.5 w-5 rounded-full bg-current transition-all duration-300 ease-out",
            open ? "top-[7px] -rotate-45" : "top-[14px]",
          )}
        />
      </span>
    </button>
  );
}
