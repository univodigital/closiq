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
import { ROUTES } from "@/shared/constants/routes";

const schema = z.object({
  phone: z.string().regex(/^(\+91)?[6-9]\d{9}$/, "Enter valid 10-digit mobile"),
});

type FormData = z.infer<typeof schema>;

export default function ForgotPasswordPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { phone: "" },
  });

  const onSubmit = async (data: FormData) => {
    setLoading(true);
    try {
      const phone = data.phone.startsWith("+91") ? data.phone : `+91${data.phone}`;
      const { otpSessionId } = await authService.forgotPassword(phone);
      sessionStorage.setItem("otpSessionId", otpSessionId);
      sessionStorage.setItem("otpPhone", phone);
      router.push(ROUTES.resetPassword);
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        toast.error("This number is not registered.");
      } else if (error instanceof ApiError) {
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
        <p className="text-sm text-muted-foreground">We&apos;ll send an OTP to your registered mobile</p>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="label-caps mb-2 block text-muted-foreground">Mobile</label>
            <div className="flex gap-2">
              <span className="flex h-11 items-center rounded-sm border border-border px-3 text-sm text-muted-foreground">+91</span>
              <Input {...register("phone")} placeholder="9876543210" error={errors.phone?.message} />
            </div>
          </div>
          <Button type="submit" variant="primary" size="lg" disabled={loading}>
            {loading ? "Sending OTP…" : "Send reset OTP"}
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
