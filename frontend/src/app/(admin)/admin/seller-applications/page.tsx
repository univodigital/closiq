"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { adminService } from "@/features/admin/services";
import { PageHeader } from "@/shared/components/layout/Container";
import { Button } from "@/components/ui/button";
import { StatusBadge } from "@/components/ui/badge";

const MIN_REJECTION_REASON_LENGTH = 10;

export default function AdminSellerApplicationsPage() {
  const queryClient = useQueryClient();
  const [rejectingId, setRejectingId] = useState<string | null>(null);
  const [rejectReason, setRejectReason] = useState("");

  const { data, isLoading } = useQuery({
    queryKey: ["admin", "seller-applications"],
    queryFn: () => adminService.listSellerApplications(),
  });

  const approveMutation = useMutation({
    mutationFn: (applicationId: string) => adminService.approveSellerApplication(applicationId),
    onSuccess: () => {
      toast.success("Seller application approved");
      queryClient.invalidateQueries({ queryKey: ["admin", "seller-applications"] });
      queryClient.invalidateQueries({ queryKey: ["admin", "dashboard"] });
    },
    onError: (err: Error) => toast.error(err.message),
  });

  const rejectMutation = useMutation({
    mutationFn: ({ applicationId, reason }: { applicationId: string; reason: string }) =>
      adminService.rejectSellerApplication(applicationId, reason),
    onSuccess: () => {
      toast.success("Seller application rejected");
      setRejectingId(null);
      setRejectReason("");
      queryClient.invalidateQueries({ queryKey: ["admin", "seller-applications"] });
      queryClient.invalidateQueries({ queryKey: ["admin", "dashboard"] });
    },
    onError: (err: Error) => toast.error(err.message),
  });

  function cancelReject() {
    setRejectingId(null);
    setRejectReason("");
  }

  const trimmedReason = rejectReason.trim();
  const reasonTooShort =
    trimmedReason.length > 0 && trimmedReason.length < MIN_REJECTION_REASON_LENGTH;

  return (
    <div>
      <PageHeader title="Seller applications" description="Review and approve new sellers" />
      {isLoading ? (
        <p className="text-muted-foreground">Loading…</p>
      ) : data?.data.length === 0 ? (
        <p className="text-muted-foreground">No seller applications to review.</p>
      ) : (
        <div className="space-y-3">
          {data?.data.map((app) => (
            <div key={app.applicationId} className="rounded-sm border border-border p-4">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p className="font-medium">{app.businessName}</p>
                  <p className="text-sm text-muted-foreground">
                    {app.applicantName} · {app.applicantPhone}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {app.businessType} · {app.city}
                  </p>
                </div>
                <StatusBadge status={app.status.toLowerCase()} />
              </div>

              {app.status === "REJECTED" && app.rejectionReason?.trim() && (
                <div className="mt-3 rounded-sm border border-border bg-muted/30 p-3 text-sm">
                  <p className="label-caps text-muted-foreground">Rejection reason</p>
                  <p className="mt-1">{app.rejectionReason.trim()}</p>
                </div>
              )}

              {(app.status === "PENDING" || app.status === "UNDER_REVIEW") && (
                <div className="mt-4 flex flex-wrap items-center gap-2">
                  <Button
                    size="sm"
                    variant="primary"
                    disabled={approveMutation.isPending || rejectingId === app.applicationId}
                    onClick={() => approveMutation.mutate(app.applicationId)}
                  >
                    Approve
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={approveMutation.isPending}
                    onClick={() => {
                      setRejectingId(app.applicationId);
                      setRejectReason("");
                    }}
                  >
                    Reject
                  </Button>
                </div>
              )}

              {rejectingId === app.applicationId && (
                <div className="mt-4 space-y-3 rounded-sm border border-border bg-muted/20 p-4">
                  <div>
                    <p className="font-medium">Reject seller application</p>
                    <p className="mt-1 text-sm text-muted-foreground">
                      Provide a clear reason. The applicant will see this message on their application page.
                    </p>
                  </div>
                  <textarea
                    value={rejectReason}
                    onChange={(e) => setRejectReason(e.target.value)}
                    rows={4}
                    maxLength={2000}
                    placeholder="Example: Your business registration document is incomplete. Please upload the complete document and submit again."
                    className="flex min-h-[6rem] w-full rounded-sm border border-input bg-background px-3 py-2 text-sm"
                  />
                  {reasonTooShort && (
                    <p className="text-sm text-rose-deep">
                      Rejection reason must be at least {MIN_REJECTION_REASON_LENGTH} characters.
                    </p>
                  )}
                  <div className="flex flex-wrap gap-2">
                    <Button size="sm" variant="outline" onClick={cancelReject}>
                      Cancel
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      disabled={
                        trimmedReason.length < MIN_REJECTION_REASON_LENGTH || rejectMutation.isPending
                      }
                      onClick={() =>
                        rejectMutation.mutate({ applicationId: app.applicationId, reason: trimmedReason })
                      }
                    >
                      Reject application
                    </Button>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
