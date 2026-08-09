import type { TimelineEvent } from "@/shared/types";
import { cn } from "@/lib/utils";

export function OrderTimeline({ events }: { events: TimelineEvent[] }) {
  return (
    <ol className="relative space-y-0 border-l border-border pl-6">
      {events.map((event, i) => (
        <li key={`${event.status}-${i}`} className="relative pb-8 last:pb-0">
          <span
            className={cn(
              "absolute -left-[25px] flex h-3 w-3 rounded-full border-2 border-background",
              event.completed ? "bg-success" : event.current ? "bg-accent" : "bg-border",
            )}
          />
          <p className={cn("text-sm font-medium", event.current && "text-accent")}>{event.label}</p>
          {event.timestamp && (
            <p className="mt-1 text-xs text-muted-foreground">
              {new Date(event.timestamp).toLocaleString("en-IN", {
                day: "numeric",
                month: "short",
                hour: "2-digit",
                minute: "2-digit",
              })}
            </p>
          )}
        </li>
      ))}
    </ol>
  );
}
