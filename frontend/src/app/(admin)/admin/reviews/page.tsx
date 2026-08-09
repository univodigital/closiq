"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { adminService } from "@/features/admin/services";
import { PageHeader } from "@/shared/components/layout/Container";
import { Button } from "@/components/ui/button";
import { StatusBadge } from "@/components/ui/badge";

export default function AdminReviewsPage() {
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ["admin", "reviews"],
    queryFn: () => adminService.listReviews(),
  });

  const hideMutation = useMutation({
    mutationFn: (reviewId: string) => adminService.deleteReview(reviewId),
    onSuccess: () => {
      toast.success("Review hidden");
      queryClient.invalidateQueries({ queryKey: ["admin", "reviews"] });
    },
    onError: (err: Error) => toast.error(err.message),
  });

  const publishMutation = useMutation({
    mutationFn: (reviewId: string) => adminService.updateReview(reviewId, { status: "PUBLISHED" }),
    onSuccess: () => {
      toast.success("Review published");
      queryClient.invalidateQueries({ queryKey: ["admin", "reviews"] });
    },
    onError: (err: Error) => toast.error(err.message),
  });

  return (
    <div>
      <PageHeader title="Reviews" description="Moderate customer reviews" />
      {isLoading ? (
        <p className="text-muted-foreground">Loading…</p>
      ) : (
        <div className="space-y-3">
          {data?.data.map((review) => (
            <div key={review.id} className="rounded-sm border border-border p-4">
              <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
                <div>
                  <p className="font-medium">{review.productTitle}</p>
                  <p className="text-sm text-muted-foreground">
                    {review.authorDisplayName} · {review.productRating}★
                  </p>
                </div>
                <StatusBadge status={review.status.toLowerCase()} />
              </div>
              {review.title ? <p className="text-sm font-medium">{review.title}</p> : null}
              {review.body ? <p className="mt-1 text-sm text-muted-foreground">{review.body}</p> : null}
              <div className="mt-3 flex gap-2">
                {review.status !== "PUBLISHED" ? (
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={publishMutation.isPending}
                    onClick={() => publishMutation.mutate(review.id)}
                  >
                    Publish
                  </Button>
                ) : null}
                {review.status !== "HIDDEN" ? (
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={hideMutation.isPending}
                    onClick={() => hideMutation.mutate(review.id)}
                  >
                    Hide
                  </Button>
                ) : null}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
