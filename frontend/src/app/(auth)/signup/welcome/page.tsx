"use client";

import { useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ROUTES } from "@/shared/constants/routes";
import { useAuth } from "@/providers/AuthProvider";

export default function SignupWelcomePage() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading } = useAuth();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.replace(ROUTES.signup);
    }
  }, [isAuthenticated, isLoading, router]);

  const firstName = user?.firstName ?? "there";

  return (
    <Card>
      <CardHeader>
        <CardTitle>Welcome to Closiq, {firstName}!</CardTitle>
        <p className="text-sm text-muted-foreground">
          Your account is ready. Explore curated fashion rentals and purchases tailored for you.
        </p>
      </CardHeader>
      <CardContent>
        <Button asChild variant="primary" size="lg" className="w-full">
          <Link href={ROUTES.home}>Continue to home</Link>
        </Button>
      </CardContent>
    </Card>
  );
}
