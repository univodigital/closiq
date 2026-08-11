"use client";

import Link from "next/link";
import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useAuth } from "@/providers/AuthProvider";
import { Container, PageHeader } from "@/shared/components/layout/Container";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { StatusBadge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { SellerApplicationForm } from "@/features/account/components/SellerApplicationForm";
import {
  fetchMySellerApplication,
  submitSellerApplication,
  type SellerApplicationDetail,
} from "@/features/seller/services/seller-application.service";
import { ROUTES } from "@/shared/constants/routes";

function formatDate(value?: string | null) {
  if (!value) return "—";
  return new Date(value).toLocaleDateString("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

function applicationStatusMessage(application: SellerApplicationDetail) {
  switch (application.status) {
    case "PENDING":
    case "UNDER_REVIEW":
      return "Your application is currently being reviewed. We will notify you once verification is complete.";
    case "VERIFIED":
      return "Your seller account has been approved. Refresh your session if seller tools are not visible yet.";
    case "REJECTED":
      return application.canReapply
        ? "Review the reason below, update your details, and submit your application again."
        : "Your application was rejected. Contact support if you need help.";
    case "SUSPENDED":
      return "Your seller application is suspended. Contact support for assistance.";
    case "DRAFT":
      return "Complete and submit your application to begin verification.";
    default:
      return null;
  }
}

function ApplicationStatusCard({ application }: { application: SellerApplicationDetail }) {
  const message = applicationStatusMessage(application);

  return (
    <Card>
      <CardContent className="space-y-4 p-6">
        <div className="flex flex-wrap items-center gap-3">
          <p className="font-heading text-lg">{application.businessName}</p>
          <StatusBadge status={application.status.toLowerCase()} />
        </div>

        <div className="grid gap-3 text-sm sm:grid-cols-2">
          <div>
            <p className="label-caps text-muted-foreground">Submitted</p>
            <p className="mt-1">{formatDate(application.submittedAt)}</p>
          </div>
          <div>
            <p className="label-caps text-muted-foreground">Reviewed</p>
            <p className="mt-1">{formatDate(application.reviewedAt)}</p>
          </div>
        </div>

        {message && <p className="text-sm text-muted-foreground">{message}</p>}

        {application.status === "REJECTED" && application.rejectionReason?.trim() && (
          <div className="rounded-sm border border-rose-deep/30 bg-rose-deep/5 p-4 text-sm">
            <p className="label-caps text-rose-deep">Rejection reason</p>
            <p className="mt-2 text-muted-foreground">{application.rejectionReason.trim()}</p>
          </div>
        )}

        {application.status === "VERIFIED" && (
          <div className="flex flex-wrap gap-3">
            <Button asChild variant="primary">
              <Link href={ROUTES.seller.dashboard}>Go to seller dashboard</Link>
            </Button>
          </div>
        )}

        {application.documents.length > 0 && (
          <div>
            <p className="label-caps mb-2 text-muted-foreground">Documents</p>
            <ul className="space-y-2 text-sm">
              {application.documents.map((doc) => (
                <li key={`${doc.type}-${doc.uploadedAt}`} className="flex items-center justify-between gap-3">
                  <span>{doc.type.replaceAll("_", " ")}</span>
                  <StatusBadge status={doc.status.toLowerCase()} />
                </li>
              ))}
            </ul>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

export default function BecomeSellerPage() {
  const queryClient = useQueryClient();
  const { user, isLoading: authLoading, hasRole, refreshUser } = useAuth();
  const isSeller = hasRole("SELLER");
  const [showReapplyForm, setShowReapplyForm] = useState(false);

  const { data: application, isLoading: applicationLoading } = useQuery({
    queryKey: ["seller-application", "me"],
    queryFn: fetchMySellerApplication,
    enabled: !authLoading && !isSeller,
  });

  const submitMutation = useMutation({
    mutationFn: submitSellerApplication,
    onSuccess: async () => {
      setShowReapplyForm(false);
      await queryClient.invalidateQueries({ queryKey: ["seller-application", "me"] });
      await refreshUser();
      toast.success("Seller application submitted for review");
    },
  });

  const loading = authLoading || (!isSeller && applicationLoading);
  const isRejected = application?.status === "REJECTED";
  const canReapply = Boolean(application?.canReapply);
  const showApplicationForm =
    !application || (isRejected && canReapply && showReapplyForm);

  return (
    <Container narrow embedded>
      <PageHeader
        title="Become a seller"
        description="List your premium pieces on Closiq and earn from every rental."
      />

      {loading ? (
        <Card>
          <CardContent className="space-y-4 p-6">
            <Skeleton className="h-5 w-48" />
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
          </CardContent>
        </Card>
      ) : isSeller ? (
        <Card>
          <CardContent className="space-y-4 p-6">
            <div className="flex flex-wrap items-center gap-3">
              <p className="font-heading text-lg">{user?.sellerProfile?.businessName ?? "Seller account"}</p>
              <StatusBadge status="verified" />
            </div>
            <p className="text-sm text-muted-foreground">
              Your seller profile is active
              {user?.sellerProfile?.city ? ` in ${user.sellerProfile.city}` : ""}.
              {user?.sellerProfile?.listingCount
                ? ` You have ${user.sellerProfile.listingCount} active listing${user.sellerProfile.listingCount === 1 ? "" : "s"}.`
                : " Start by adding your first product."}
            </p>
            <div className="flex flex-wrap gap-3">
              <Button asChild variant="primary">
                <Link href={ROUTES.seller.dashboard}>Go to seller dashboard</Link>
              </Button>
              <Button asChild variant="outline">
                <Link href={ROUTES.seller.products}>Manage products</Link>
              </Button>
            </div>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {application && <ApplicationStatusCard application={application} />}

          {isRejected && canReapply && !showReapplyForm && (
            <Card>
              <CardContent className="space-y-4 p-6">
                <p className="text-sm text-muted-foreground">
                  Update your business details and resubmit when you are ready.
                </p>
                <Button variant="primary" onClick={() => setShowReapplyForm(true)}>
                  Fix &amp; reapply
                </Button>
              </CardContent>
            </Card>
          )}

          {showApplicationForm && (
            <Card>
              <CardContent className="space-y-4 p-6">
                <p className="text-sm text-muted-foreground">
                  {isRejected
                    ? "Update your details and submit a new application for review."
                    : "Join as a verified seller. Submit your business details and KYC information to get started."}
                </p>
                <SellerApplicationForm
                  initialValues={
                    isRejected && application
                      ? {
                          businessName: application.businessName,
                          city: application.city,
                        }
                      : undefined
                  }
                  submitLabel={isRejected ? "Resubmit application" : "Submit application"}
                  onSubmit={async (input) => {
                    await submitMutation.mutateAsync(input);
                  }}
                />
              </CardContent>
            </Card>
          )}
        </div>
      )}
    </Container>
  );
}
