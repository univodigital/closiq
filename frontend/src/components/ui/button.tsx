import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 whitespace-nowrap text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background disabled:pointer-events-none disabled:opacity-45",
  {
    variants: {
      variant: {
        /** Navy — buy actions and general primary CTAs */
        primary: "bg-primary text-primary-foreground hover:bg-navy-dark",
        /** Oxblood — rent actions and fashion-focused CTAs */
        rent: "bg-accent text-accent-foreground hover:bg-oxblood-hover",
        /** @deprecated Use `rent` — kept for backward compatibility */
        gold: "bg-accent text-accent-foreground hover:bg-oxblood-hover",
        /** Navy outline */
        buy: "border border-primary bg-transparent text-primary hover:bg-muted",
        secondary: "border border-primary bg-transparent text-primary hover:bg-muted",
        outline: "border border-border bg-card text-foreground hover:bg-muted",
        ghost: "text-muted-foreground hover:bg-muted hover:text-foreground",
        link: "h-auto px-0 text-primary underline-offset-4 hover:underline",
        destructive:
          "border border-destructive bg-transparent text-destructive hover:bg-error-muted",
      },
      size: {
        sm: "h-8 px-3 text-xs",
        md: "h-11 px-5",
        lg: "h-[52px] w-full px-6 text-base",
        icon: "h-10 w-10",
      },
    },
    defaultVariants: { variant: "primary", size: "md" },
  },
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean;
}

export function Button({ className, variant, size, asChild = false, ...props }: ButtonProps) {
  const Comp = asChild ? Slot : "button";
  return (
    <Comp className={cn(buttonVariants({ variant, size }), "rounded-sm", className)} {...props} />
  );
}

export { buttonVariants };
