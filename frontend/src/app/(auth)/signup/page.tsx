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
import { ROUTES } from "@/shared/constants/routes";

const schema = z.object({
  phone: z.string().regex(/^(\+91)?[6-9]\d{9}$/, "Enter valid 10-digit mobile"),
  acceptTerms: z.literal(true, { errorMap: () => ({ message: "You must accept the terms" }) }),
});

type FormData = z.infer<typeof schema>;

export default function SignupPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  const onSubmit = async (data: FormData) => {
    setLoading(true);
    try {
      const phone = data.phone.startsWith("+91") ? data.phone : `+91${data.phone}`;
      const result = await authService.register(phone, true);
      sessionStorage.setItem("otpSessionId", result.otpSessionId);
      sessionStorage.setItem("otpPhone", result.phone ?? phone);
      sessionStorage.setItem("otpResendIn", String(result.resendAvailableInSeconds ?? 60));
      router.push("/signup/verify?mode=register");
    } catch {
      toast.error("Could not send OTP");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Create account</CardTitle>
        <p className="text-sm text-muted-foreground">Enter your mobile number to get started</p>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="label-caps mb-2 block text-muted-foreground">Mobile</label>
            <div className="flex gap-2">
              <span className="flex h-11 items-center rounded-sm border border-border px-3 text-sm">+91</span>
              <Input {...register("phone")} placeholder="Mobile number" autoComplete="tel" error={errors.phone?.message} />
            </div>
          </div>
          <label className="flex items-start gap-2 text-sm">
            <input type="checkbox" {...register("acceptTerms")} className="mt-1" />
            <span>I agree to the Terms of Service</span>
          </label>
          {errors.acceptTerms && <p className="text-xs text-destructive">{errors.acceptTerms.message}</p>}
          <Button type="submit" variant="primary" size="lg" disabled={loading}>
            {loading ? "Sending OTP…" : "Continue"}
          </Button>
        </form>
        <p className="mt-6 text-center text-sm text-muted-foreground">
          Already have an account? <Link href={ROUTES.login} className="text-accent hover:underline">Sign in</Link>
        </p>
      </CardContent>
    </Card>
  );
}
