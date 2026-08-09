"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { adminService } from "@/features/admin/services";
import { PageHeader } from "@/shared/components/layout/Container";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { StatusBadge } from "@/components/ui/badge";

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
    },
    onError: (err: Error) => toast.error(err.message),
  });

  return (
    <div>
      <PageHeader title="Seller applications" description="Review and approve new sellers" />
      {isLoading ? (
        <p className="text-muted-foreground">Loading…</p>
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

              {(app.status === "PENDING" || app.status === "UNDER_REVIEW") && (
                <div className="mt-4 flex flex-wrap items-center gap-2">
                  <Button
                    size="sm"
                    variant="primary"
                    disabled={approveMutation.isPending}
                    onClick={() => approveMutation.mutate(app.applicationId)}
                  >
                    Approve
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => setRejectingId(app.applicationId)}
                  >
                    Reject
                  </Button>
                </div>
              )}

              {rejectingId === app.applicationId && (
                <div className="mt-3 flex flex-wrap gap-2">
                  <Input
                    placeholder="Rejection reason"
                    value={rejectReason}
                    onChange={(e) => setRejectReason(e.target.value)}
                  />
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={!rejectReason.trim() || rejectMutation.isPending}
                    onClick={() =>
                      rejectMutation.mutate({ applicationId: app.applicationId, reason: rejectReason })
                    }
                  >
                    Confirm reject
                  </Button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
