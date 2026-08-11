"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { authService } from "@/features/auth/services";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ApiError } from "@/lib/api-client";
import { normalizeAuthIdentifier } from "@/lib/auth-identifier";
import { ROUTES } from "@/shared/constants/routes";

const schema = z.object({
  identifier: z.string().min(1, "Enter phone or email"),
});

type FormData = z.infer<typeof schema>;

export default function ForgotPasswordPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { identifier: "" },
  });

  const onSubmit = async (data: FormData) => {
    setLoading(true);
    try {
      const { type, value } = normalizeAuthIdentifier(data.identifier);
      const { otpSessionId } = await authService.forgotPassword(value);
      sessionStorage.setItem("otpSessionId", otpSessionId);
      sessionStorage.setItem("otpDeliveryHint", value);
      sessionStorage.setItem("otpChannel", type);
      router.push(ROUTES.resetPassword);
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        toast.error("No account found with this phone or email.");
      } else if (error instanceof ApiError) {
        toast.error(error.message);
      } else if (error instanceof Error) {
        toast.error(error.message);
      } else {
        toast.error("Could not send reset OTP");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Forgot password</CardTitle>
        <p className="text-sm text-muted-foreground">
          We&apos;ll send a reset code to your mobile and email
        </p>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="label-caps mb-2 block text-muted-foreground">Phone or email</label>
            <Input
              {...register("identifier")}
              placeholder="Phone or email"
              autoComplete="username"
              error={errors.identifier?.message}
            />
          </div>
          <Button type="submit" variant="primary" size="lg" className="w-full" disabled={loading}>
            {loading ? "Sending OTP…" : "Send reset code"}
          </Button>
        </form>
        <p className="mt-6 text-center text-sm text-muted-foreground">
          Remember your password?{" "}
          <Link href={ROUTES.login} className="text-accent hover:underline">
            Back to sign in
          </Link>
        </p>
      </CardContent>
    </Card>
  );
}
