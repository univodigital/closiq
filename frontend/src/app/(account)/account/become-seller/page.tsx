"use client";

import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
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

export default function BecomeSellerPage() {
  const queryClient = useQueryClient();
  const { user, isLoading: authLoading, hasRole, refreshUser } = useAuth();
  const isSeller = hasRole("SELLER");

  const { data: application, isLoading: applicationLoading } = useQuery({
    queryKey: ["seller-application", "me"],
    queryFn: fetchMySellerApplication,
    enabled: !authLoading && !isSeller,
  });

  const submitMutation = useMutation({
    mutationFn: submitSellerApplication,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["seller-application", "me"] });
      await refreshUser();
    },
  });

  const loading = authLoading || (!isSeller && applicationLoading);

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
              <Button asChild variant="gold">
                <Link href={ROUTES.seller.dashboard}>Go to seller dashboard</Link>
              </Button>
              <Button asChild variant="outline">
                <Link href={ROUTES.seller.products}>Manage products</Link>
              </Button>
            </div>
          </CardContent>
        </Card>
      ) : application && application.status !== "REJECTED" ? (
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
            {application.status === "PENDING" || application.status === "UNDER_REVIEW" ? (
              <p className="text-sm text-muted-foreground">
                We are reviewing your application and KYC documents. You will be notified once verification is complete.
              </p>
            ) : application.status === "VERIFIED" ? (
              <p className="text-sm text-muted-foreground">
                Your application is approved. Refresh your session if seller tools are not visible yet.
              </p>
            ) : null}
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
      ) : (
        <Card>
          <CardContent className="space-y-4 p-6">
            {application?.status === "REJECTED" && (
              <div className="rounded-sm border border-rose-deep/30 bg-rose-deep/5 p-4 text-sm">
                <p className="font-medium text-rose-deep">Previous application rejected</p>
                <p className="mt-1 text-muted-foreground">
                  {application.rejectionReason?.trim() ||
                    "Please review your details and submit a new application."}
                </p>
              </div>
            )}
            <p className="text-sm text-muted-foreground">
              Join as a verified seller. Submit your business details and KYC information to get started.
            </p>
            <SellerApplicationForm
              onSubmit={async (input) => {
                await submitMutation.mutateAsync(input);
              }}
            />
          </CardContent>
        </Card>
      )}
    </Container>
  );
}
