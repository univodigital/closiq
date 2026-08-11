"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { useAuth } from "@/providers/AuthProvider";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ROUTES } from "@/shared/constants/routes";

const passwordSchema = z
  .string()
  .min(8, "At least 8 characters")
  .regex(/[A-Z]/, "Include an uppercase letter")
  .regex(/\d/, "Include a number");

const schema = z.object({
  otp: z.string().regex(/^\d{6}$/, "Enter 6-digit OTP"),
  newPassword: passwordSchema,
  confirmPassword: z.string(),
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: "Passwords do not match",
  path: ["confirmPassword"],
});

type FormData = z.infer<typeof schema>;

export default function ResetPasswordPage() {
  const router = useRouter();
  const { resetPassword } = useAuth();
  const [loading, setLoading] = useState(false);
  const [phone, setPhone] = useState("");

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  useEffect(() => {
    const otpSessionId = sessionStorage.getItem("otpSessionId");
    if (!otpSessionId) {
      router.replace(ROUTES.forgotPassword);
      return;
    }
    setPhone(sessionStorage.getItem("otpDeliveryHint") ?? "your phone and email");
  }, [router]);

  const onSubmit = async (data: FormData) => {
    const otpSessionId = sessionStorage.getItem("otpSessionId");
    if (!otpSessionId) {
      toast.error("Session expired. Please start again.");
      router.push(ROUTES.forgotPassword);
      return;
    }
    setLoading(true);
    try {
      await resetPassword(otpSessionId, data.otp, data.newPassword);
      sessionStorage.removeItem("otpSessionId");
      sessionStorage.removeItem("otpDeliveryHint");
      sessionStorage.removeItem("otpChannel");
      toast.success("Password updated successfully");
      router.push(ROUTES.home);
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Could not reset password");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Reset password</CardTitle>
        <p className="text-sm text-muted-foreground">Enter the code sent to {phone}</p>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="label-caps mb-2 block text-muted-foreground">6-digit OTP</label>
            <Input
              {...register("otp")}
              placeholder="6-digit code"
              maxLength={6}
              className="font-mono text-center tracking-[0.5em]"
              error={errors.otp?.message}
            />
          </div>
          <div>
            <label className="label-caps mb-2 block text-muted-foreground">New password</label>
            <Input
              {...register("newPassword")}
              type="password"
              placeholder="New password"
              autoComplete="new-password"
              error={errors.newPassword?.message}
            />
          </div>
          <div>
            <label className="label-caps mb-2 block text-muted-foreground">Confirm password</label>
            <Input
              {...register("confirmPassword")}
              type="password"
              placeholder="Confirm password"
              autoComplete="new-password"
              error={errors.confirmPassword?.message}
            />
          </div>
          <Button type="submit" variant="primary" size="lg" className="w-full" disabled={loading}>
            {loading ? "Updating…" : "Update password"}
          </Button>
        </form>
        <p className="mt-6 text-center text-sm text-muted-foreground">
          <Link href={ROUTES.login} className="text-accent hover:underline">
            Back to sign in
          </Link>
        </p>
      </CardContent>
    </Card>
  );
}
