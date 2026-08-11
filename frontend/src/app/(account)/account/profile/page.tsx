"use client";

import Link from "next/link";
import { useAuth } from "@/providers/AuthProvider";
import { Container, PageHeader } from "@/shared/components/layout/Container";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { ROUTES } from "@/shared/constants/routes";
import { Skeleton } from "@/components/ui/skeleton";
import { maskPhone } from "@/lib/phone-mask";

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
              <div className="flex items-center gap-4">
                {user?.avatarUrl ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img src={user.avatarUrl} alt="" className="h-16 w-16 rounded-full object-cover" />
                ) : (
                  <div className="flex h-16 w-16 items-center justify-center rounded-full bg-muted text-lg font-medium">
                    {user?.firstName?.charAt(0) ?? "?"}
                  </div>
                )}
                <div>
                  <p className="font-medium">{user?.displayName}</p>
                  {user?.username && <p className="text-sm text-muted-foreground">@{user.username}</p>}
                </div>
              </div>

              <ProfileField label="Username" value={user?.username} />
              <ProfileField
                label="Email"
                value={
                  user?.email
                    ? `${user.email}${user.emailVerified ? " (verified)" : " (unverified)"}`
                    : undefined
                }
              />
              {user?.pendingEmail && (
                <ProfileField label="Pending email" value={`${user.pendingEmail} (awaiting verification)`} />
              )}
              <ProfileField label="Contact number" value={user?.phone ? maskPhone(user.phone) : undefined} />
              <ProfileField label="Alternate number" value={user?.alternatePhone} />
              <ProfileField label="Alternate email" value={user?.alternateEmail} />

              <div className="flex flex-wrap gap-3 pt-4">
                <Button asChild variant="outline" size="sm">
                  <Link href={ROUTES.account.profileEdit}>Edit profile</Link>
                </Button>
                <Button asChild variant="outline" size="sm">
                  <Link href={ROUTES.account.security}>Security settings</Link>
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
