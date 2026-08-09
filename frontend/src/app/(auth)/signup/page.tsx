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
import { ApiError } from "@/lib/api-client";

const passwordSchema = z
  .string()
  .min(8, "At least 8 characters")
  .regex(/[A-Z]/, "Include an uppercase letter")
  .regex(/\d/, "Include a number");

const schema = z.object({
  username: z
    .string()
    .min(3, "At least 3 characters")
    .max(30, "Max 30 characters")
    .regex(/^[a-zA-Z0-9_]+$/, "Letters, numbers, and underscores only"),
  phone: z.string().regex(/^(\+91)?[6-9]\d{9}$/, "Enter valid 10-digit mobile"),
  password: passwordSchema,
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
      const { otpSessionId } = await authService.register(phone, true);
      sessionStorage.setItem("otpSessionId", otpSessionId);
      sessionStorage.setItem("otpPhone", phone);
      sessionStorage.setItem("registerUsername", data.username);
      sessionStorage.setItem("registerPassword", data.password);
      router.push("/signup/verify?mode=register");
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        toast.error("Username is already taken");
      } else {
        toast.error("Could not send OTP");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Create account</CardTitle>
        <p className="text-sm text-muted-foreground">Set up your username, mobile, and password</p>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="label-caps mb-2 block text-muted-foreground">Username</label>
            <Input {...register("username")} placeholder="your_username" autoComplete="username" error={errors.username?.message} />
          </div>
          <div>
            <label className="label-caps mb-2 block text-muted-foreground">Mobile</label>
            <div className="flex gap-2">
              <span className="flex h-11 items-center rounded-sm border border-border px-3 text-sm">+91</span>
              <Input {...register("phone")} placeholder="9876543210" autoComplete="tel" error={errors.phone?.message} />
            </div>
          </div>
          <div>
            <label className="label-caps mb-2 block text-muted-foreground">Password</label>
            <Input
              {...register("password")}
              type="password"
              placeholder="Min 8 chars, 1 uppercase, 1 number"
              autoComplete="new-password"
              error={errors.password?.message}
            />
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
