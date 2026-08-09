"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { useAuth } from "@/providers/AuthProvider";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ApiError } from "@/lib/api-client";
import { ROUTES } from "@/shared/constants/routes";
import { cn } from "@/lib/utils";

const otpSchema = z.object({
  phone: z.string().regex(/^(\+91)?[6-9]\d{9}$/, "Enter valid 10-digit mobile"),
});

const passwordSchema = z.object({
  identifier: z.string().min(1, "Enter phone or username"),
  password: z.string().min(1, "Password is required"),
});

type OtpFormData = z.infer<typeof otpSchema>;
type PasswordFormData = z.infer<typeof passwordSchema>;

type LoginMode = "otp" | "password";

export default function LoginPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const returnUrl = searchParams.get("returnUrl") ?? ROUTES.home;
  const { login, loginWithPassword } = useAuth();
  const [mode, setMode] = useState<LoginMode>("otp");
  const [loading, setLoading] = useState(false);

  const otpForm = useForm<OtpFormData>({
    resolver: zodResolver(otpSchema),
    defaultValues: { phone: "" },
  });

  const passwordForm = useForm<PasswordFormData>({
    resolver: zodResolver(passwordSchema),
    defaultValues: { identifier: "", password: "" },
  });

  const onOtpSubmit = async (data: OtpFormData) => {
    setLoading(true);
    try {
      const phone = data.phone.startsWith("+91") ? data.phone : `+91${data.phone}`;
      const { otpSessionId } = await login(phone);
      sessionStorage.setItem("otpSessionId", otpSessionId);
      sessionStorage.setItem("otpPhone", phone);
      router.push(`/signup/verify?returnUrl=${encodeURIComponent(returnUrl)}&mode=login`);
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        toast.error("This number is not registered. Create an account first.");
      } else if (error instanceof ApiError) {
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
      await loginWithPassword(data.identifier, data.password);
      toast.success("Welcome back");
      router.push(returnUrl);
    } catch (error) {
      if (error instanceof ApiError) {
        toast.error(error.message);
      } else {
        toast.error("Invalid phone/username or password");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Welcome back</CardTitle>
        <p className="text-sm text-muted-foreground">Sign in with OTP or password</p>
      </CardHeader>
      <CardContent>
        <div className="mb-6 flex rounded-sm border border-border p-1">
          <button
            type="button"
            onClick={() => setMode("otp")}
            className={cn(
              "flex-1 rounded-sm px-3 py-2 text-sm transition-colors",
              mode === "otp" ? "bg-accent text-accent-foreground" : "text-muted-foreground hover:text-foreground",
            )}
          >
            OTP
          </button>
          <button
            type="button"
            onClick={() => setMode("password")}
            className={cn(
              "flex-1 rounded-sm px-3 py-2 text-sm transition-colors",
              mode === "password" ? "bg-accent text-accent-foreground" : "text-muted-foreground hover:text-foreground",
            )}
          >
            Password
          </button>
        </div>

        {mode === "otp" ? (
          <form onSubmit={otpForm.handleSubmit(onOtpSubmit)} className="space-y-4">
            <div>
              <label className="label-caps mb-2 block text-muted-foreground">Mobile</label>
              <div className="flex gap-2">
                <span className="flex h-11 items-center rounded-sm border border-border px-3 text-sm text-muted-foreground">+91</span>
                <Input {...otpForm.register("phone")} placeholder="9876543210" error={otpForm.formState.errors.phone?.message} />
              </div>
            </div>
            <Button type="submit" variant="primary" size="lg" disabled={loading}>
              {loading ? "Sending OTP…" : "Send OTP"}
            </Button>
          </form>
        ) : (
          <form onSubmit={passwordForm.handleSubmit(onPasswordSubmit)} className="space-y-4">
            <div>
              <label className="label-caps mb-2 block text-muted-foreground">Phone or username</label>
              <Input
                {...passwordForm.register("identifier")}
                placeholder="9876543210 or your_username"
                autoComplete="username"
                error={passwordForm.formState.errors.identifier?.message}
              />
            </div>
            <div>
              <label className="label-caps mb-2 block text-muted-foreground">Password</label>
              <Input
                {...passwordForm.register("password")}
                type="password"
                placeholder="Your password"
                autoComplete="current-password"
                error={passwordForm.formState.errors.password?.message}
              />
            </div>
            <div className="flex items-center justify-between">
              <Button type="submit" variant="primary" size="lg" disabled={loading}>
                {loading ? "Signing in…" : "Sign in"}
              </Button>
              <Link href={ROUTES.forgotPassword} className="text-sm text-accent hover:underline">
                Forgot password?
              </Link>
            </div>
          </form>
        )}

        <p className="mt-6 text-center text-sm text-muted-foreground">
          New to Closiq?{" "}
          <Link href={ROUTES.signup} className="text-accent underline-offset-4 hover:underline">
            Create account
          </Link>
        </p>
        {mode === "otp" && (
          <p className="text-center text-xs text-muted-foreground">
            Check the backend console for your OTP
          </p>
        )}
      </CardContent>
    </Card>
  );
}
