"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { notificationService } from "@/features/seller/services";
import { Container, PageHeader } from "@/shared/components/layout/Container";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export default function NotificationsPage() {
  const qc = useQueryClient();
  const { data, isLoading } = useQuery({
    queryKey: ["notifications"],
    queryFn: () => notificationService.list(),
  });

  const markAll = useMutation({
    mutationFn: () => notificationService.markAllRead(),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["notifications"] }),
  });

  const items = data?.data ?? [];

  return (
    <Container narrow embedded>
      <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
        <PageHeader title="Notifications" description={`${data?.meta.unreadCount ?? 0} unread`} />
        <Button variant="ghost" size="sm" onClick={() => markAll.mutate()}>
          Mark all read
        </Button>
      </div>
      {isLoading ? (
        <p className="text-muted-foreground">Loading…</p>
      ) : items.length === 0 ? (
        <p className="text-sm text-muted-foreground">No notifications yet.</p>
      ) : (
        <div className="space-y-2">
          {items.map((n) => (
            <Link
              key={n.id}
              href={n.deepLink}
              className={cn(
                "block rounded-sm border border-border p-4 transition-colors hover:bg-muted/30",
                !n.read && "border-l-2 border-l-accent bg-gold-light/30",
              )}
            >
              <p className="font-medium">{n.title}</p>
              <p className="mt-1 text-sm text-muted-foreground">{n.body}</p>
            </Link>
          ))}
        </div>
      )}
    </Container>
  );
}
