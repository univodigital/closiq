import { cn } from "@/lib/utils";

export function Container({
  children,
  className,
  narrow,
  embedded,
}: {
  children: React.ReactNode;
  className?: string;
  narrow?: boolean;
  /** Use inside account layout — skips outer horizontal padding already applied by the shell. */
  embedded?: boolean;
}) {
  return (
    <div
      className={cn(
        "w-full",
        narrow ? "max-w-3xl" : !embedded && "max-w-7xl",
        !embedded && "mx-auto px-5 md:px-12",
        className,
      )}
    >
      {children}
    </div>
  );
}

export function PageHeader({
  title,
  description,
  breadcrumb,
  actions,
}: {
  title: string;
  description?: string;
  breadcrumb?: string;
  actions?: React.ReactNode;
}) {
  return (
    <div className="mb-8 md:mb-12">
      {breadcrumb && <p className="label-caps mb-2 text-muted-foreground">{breadcrumb}</p>}
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="font-heading text-3xl md:text-4xl">{title}</h1>
          {description && <p className="mt-2 max-w-2xl text-muted-foreground">{description}</p>}
        </div>
        {actions}
      </div>
    </div>
  );
}
