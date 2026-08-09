"use client";

import Link from "next/link";
import { useAuth } from "@/providers/AuthProvider";
import { Container, PageHeader } from "@/shared/components/layout/Container";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { ROUTES } from "@/shared/constants/routes";
import { Skeleton } from "@/components/ui/skeleton";

function ProfileField({ label, value }: { label: string; value?: string | null }) {
  return (
    <div>
      <p className="label-caps text-muted-foreground">{label}</p>
      <p className="mt-1">{value?.trim() ? value : "—"}</p>
    </div>
  );
}

export default function ProfilePage() {
  const { user, isLoading, hasRole } = useAuth();

  return (
    <Container narrow embedded>
      <PageHeader title="Profile" description={isLoading ? undefined : user?.displayName} />
      <Card>
        <CardContent className="space-y-4 p-6">
          {isLoading ? (
            <>
              <div>
                <Skeleton className="h-3 w-16" />
                <Skeleton className="mt-2 h-5 w-36" />
              </div>
              <Skeleton className="h-10 w-full max-w-xs" />
            </>
          ) : (
            <>
              <ProfileField label="Name" value={user?.displayName} />
              <ProfileField label="Email" value={user?.email} />
              <ProfileField label="Contact number" value={user?.phone} />
              <ProfileField label="Alternate number" value={user?.alternatePhone} />
              <ProfileField label="Alternate email" value={user?.alternateEmail} />

              <div className="flex flex-wrap gap-3 pt-4">
                <Button asChild variant="outline" size="sm">
                  <Link href={ROUTES.account.profileEdit}>Edit profile</Link>
                </Button>
                {hasRole("ADMIN") && (
                  <Button asChild variant="primary" size="sm">
                    <Link href={ROUTES.admin.dashboard}>Admin console</Link>
                  </Button>
                )}
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </Container>
  );
}
