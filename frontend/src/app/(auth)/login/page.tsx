"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { useAuth } from "@/providers/AuthProvider";
import { getAccessToken } from "@/lib/auth-token";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { PasswordInput } from "@/components/ui/password-input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ApiError } from "@/lib/api-client";
import { normalizeAuthIdentifier } from "@/lib/auth-identifier";
import { getSafeReturnUrl } from "@/lib/safe-return-url";
import { ROUTES } from "@/shared/constants/routes";
import { cn } from "@/lib/utils";

const identifierSchema = z.object({
  identifier: z.string().min(1, "Enter phone or email"),
});

const passwordSchema = z.object({
  identifier: z.string().min(1, "Enter phone or email"),
  password: z.string().min(1, "Password is required"),
});

type OtpFormData = z.infer<typeof identifierSchema>;
type PasswordFormData = z.infer<typeof passwordSchema>;

type LoginMode = "otp" | "password";

export default function LoginPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const returnUrl = getSafeReturnUrl(searchParams.get("returnUrl"));
  const { login, loginWithPassword, isAuthenticated, isLoading: authLoading, refreshUser } =
    useAuth();
  const [mode, setMode] = useState<LoginMode>("otp");
  const [loading, setLoading] = useState(false);

  const otpForm = useForm<OtpFormData>({
    resolver: zodResolver(identifierSchema),
    defaultValues: { identifier: searchParams.get("identifier") ?? "" },
  });

  const passwordForm = useForm<PasswordFormData>({
    resolver: zodResolver(passwordSchema),
    defaultValues: { identifier: searchParams.get("identifier") ?? "", password: "" },
  });

  useEffect(() => {
    const identifier = searchParams.get("identifier");
    if (identifier) {
      otpForm.setValue("identifier", identifier);
      passwordForm.setValue("identifier", identifier);
    }
  }, [searchParams, otpForm, passwordForm]);

  useEffect(() => {
    if (!authLoading && isAuthenticated) {
      router.replace(returnUrl);
    }
  }, [authLoading, isAuthenticated, returnUrl, router]);

  // Recover when browser back restores a pre-login snapshot but the token is still valid.
  useEffect(() => {
    if (authLoading || isAuthenticated) return;
    if (getAccessToken()) {
      void refreshUser();
    }
  }, [authLoading, isAuthenticated, refreshUser]);

  const onOtpSubmit = async (data: OtpFormData) => {
    setLoading(true);
    try {
      const { type, value } = normalizeAuthIdentifier(data.identifier);
      const result = await login(value);
      sessionStorage.setItem("otpSessionId", result.otpSessionId);
      sessionStorage.setItem("otpPhone", type === "phone" ? value : "");
      sessionStorage.setItem("otpDeliveryHint", type === "email" ? value : value);
      sessionStorage.setItem("otpChannel", type);
      sessionStorage.setItem("otpResendIn", String(result.resendAvailableInSeconds ?? 60));
      router.push(`/signup/verify?returnUrl=${encodeURIComponent(returnUrl)}&mode=login`);
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        toast.error("No account found with this phone or email.");
      } else if (error instanceof ApiError && error.status === 403) {
        toast.error(error.message);
      } else if (error instanceof ApiError) {
        toast.error(error.message);
      } else if (error instanceof Error) {
        toast.error(error.message);
      } else {
        toast.error("Could not send OTP");
      }
    } finally {
      setLoading(false);
    }
  };

  const onPasswordSubmit = async (data: PasswordFormData) => {
    setLoading(true);
    try {
      const { value } = normalizeAuthIdentifier(data.identifier);
      await loginWithPassword(value, data.password);
      toast.success("Welcome back");
      router.replace(returnUrl);
    } catch (error) {
      if (error instanceof ApiError) {
        toast.error(error.message);
      } else if (error instanceof Error) {
        toast.error(error.message);
      } else {
        toast.error("Invalid phone/email or password");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Login</CardTitle>
        <p className="text-sm text-muted-foreground">Sign in with OTP or password</p>
      </CardHeader>
      <CardContent>
        <div className="mb-6 flex rounded-sm border border-border p-1">
          <button
            type="button"
            onClick={() => setMode("otp")}
            className={cn(
              "flex-1 rounded-sm px-3 py-2 text-sm transition-colors",
              mode === "otp" ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:text-foreground",
            )}
          >
            Login with OTP
          </button>
          <button
            type="button"
            onClick={() => setMode("password")}
            className={cn(
              "flex-1 rounded-sm px-3 py-2 text-sm transition-colors",
              mode === "password" ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:text-foreground",
            )}
          >
            Password
          </button>
        </div>

        <div className="min-h-[248px]">
          {mode === "otp" ? (
            <form onSubmit={otpForm.handleSubmit(onOtpSubmit)} className="space-y-4">
              <div>
                <label className="label-caps mb-2 block text-muted-foreground">Phone or email</label>
                <Input
                  {...otpForm.register("identifier")}
                  placeholder="Phone or email"
                  autoComplete="username"
                  error={otpForm.formState.errors.identifier?.message}
                />
              </div>
              <Button type="submit" variant="primary" size="lg" className="w-full" disabled={loading}>
                {loading ? "Sending OTP…" : "Send OTP"}
              </Button>
            </form>
          ) : (
            <form onSubmit={passwordForm.handleSubmit(onPasswordSubmit)} className="space-y-4">
              <div>
                <label className="label-caps mb-2 block text-muted-foreground">Phone or email</label>
                <Input
                  {...passwordForm.register("identifier")}
                  placeholder="Phone or email"
                  autoComplete="username"
                  error={passwordForm.formState.errors.identifier?.message}
                />
              </div>
              <div>
                <div className="mb-2 flex items-center justify-between">
                  <label className="label-caps text-muted-foreground">Password</label>
                  <Link href={ROUTES.forgotPassword} className="text-xs text-accent hover:underline">
                    Forgot password?
                  </Link>
                </div>
                <PasswordInput
                  {...passwordForm.register("password")}
                  placeholder="Your password"
                  autoComplete="current-password"
                  error={passwordForm.formState.errors.password?.message}
                />
              </div>
              <Button type="submit" variant="primary" size="lg" className="w-full" disabled={loading}>
                {loading ? "Signing in…" : "Login"}
              </Button>
            </form>
          )}
        </div>

        <p className="mt-6 text-center text-sm text-muted-foreground">
          New to Closiq?{" "}
          <Link href={ROUTES.signup} className="text-accent underline-offset-4 hover:underline">
            Create account
          </Link>
        </p>
        <p className="mt-2 min-h-8 text-center text-xs text-muted-foreground">
          {mode === "otp" ? "OTP is sent to your mobile and email when available" : "\u00a0"}
        </p>
      </CardContent>
    </Card>
  );
}
