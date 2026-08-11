"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ROUTES } from "@/shared/constants/routes";
import { maskPhone } from "@/lib/phone-mask";

export default function SignupExistingAccountPage() {
  const searchParams = useSearchParams();
  const phone = searchParams.get("phone") ?? "";
  const loginHref = phone
    ? `${ROUTES.login}?identifier=${encodeURIComponent(phone)}`
    : ROUTES.login;

  return (
    <Card>
      <CardHeader>
        <CardTitle>Account exists — log in instead?</CardTitle>
        <p className="text-sm text-muted-foreground">
          {phone
            ? `A Closiq account is already registered with ${maskPhone(phone)}.`
            : "A Closiq account already exists with this phone number."}
        </p>
      </CardHeader>
      <CardContent className="space-y-4">
        <Button asChild variant="primary" size="lg" className="w-full">
          <Link href={loginHref}>Log in</Link>
        </Button>
        <p className="text-center text-sm text-muted-foreground">
          Wrong number?{" "}
          <Link href={ROUTES.signup} className="text-accent hover:underline">
            Start over
          </Link>
        </p>
      </CardContent>
    </Card>
  );
}
