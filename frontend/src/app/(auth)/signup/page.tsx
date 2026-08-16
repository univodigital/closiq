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
import { PasswordInput } from "@/components/ui/password-input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ROUTES } from "@/shared/constants/routes";
import { ApiError } from "@/lib/api-client";
import type { Gender } from "@/shared/types";

const passwordSchema = z
  .string()
  .min(8, "At least 8 characters")
  .regex(/[A-Z]/, "Include an uppercase letter")
  .regex(/\d/, "Include a number");

const schema = z
  .object({
    phone: z.string().regex(/^(\+91)?[6-9]\d{9}$/, "Enter valid 10-digit mobile"),
    firstName: z.string().min(1, "First name is required").max(50, "Max 50 characters"),
    lastName: z.string().min(1, "Last name is required").max(50, "Max 50 characters"),
    gender: z.enum(["MALE", "FEMALE", "OTHER", "PREFER_NOT_TO_SAY"], {
      required_error: "Select your gender",
      invalid_type_error: "Select your gender",
    }),
    username: z
      .string()
      .min(3, "At least 3 characters")
      .max(30, "Max 30 characters")
      .regex(/^[a-zA-Z0-9_]+$/, "Letters, numbers, and underscores only"),
    email: z.string().email("Enter a valid email address"),
    password: passwordSchema,
    confirmPassword: z.string().min(1, "Confirm your password"),
    acceptTerms: z.literal(true, { errorMap: () => ({ message: "You must accept the terms" }) }),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Passwords do not match",
    path: ["confirmPassword"],
  });

type FormData = z.infer<typeof schema>;

export default function SignupPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);

  const { register, handleSubmit, setError, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  const onSubmit = async (data: FormData) => {
    setLoading(true);
    const phone = data.phone.startsWith("+91") ? data.phone : `+91${data.phone}`;
    try {
      const email = data.email.trim().toLowerCase();
      const result = await authService.register(phone, true, email);

      sessionStorage.setItem("otpSessionId", result.otpSessionId);
      sessionStorage.setItem("otpPhone", result.phone ?? phone);
      sessionStorage.setItem("otpDeliveryHint", email);
      sessionStorage.setItem("otpResendIn", String(result.resendAvailableInSeconds ?? 60));
      sessionStorage.setItem(
        "registerProfile",
        JSON.stringify({
          username: data.username,
          password: data.password,
          email,
          firstName: data.firstName.trim(),
          lastName: data.lastName.trim(),
          gender: data.gender as Gender,
        }),
      );

      router.push("/signup/verify?mode=register");
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        const message = error.message.toLowerCase();
        if (message.includes("email")) {
          setError("email", { message: error.message || "Email is already registered" });
        } else {
          router.push(`${ROUTES.signupExisting}?phone=${encodeURIComponent(phone)}`);
          return;
        }
      } else if (error instanceof ApiError) {
        toast.error(error.message);
      } else {
        toast.error("Could not continue with registration");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Create account</CardTitle>
        <p className="text-sm text-muted-foreground">Fill in your details, then verify with OTP sent to your phone and email</p>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="label-caps mb-2 block text-muted-foreground">Mobile</label>
            <div className="flex gap-2">
              <span className="flex h-11 shrink-0 items-center rounded-sm border border-border px-3 text-sm">+91</span>
              <Input {...register("phone")} placeholder="Mobile number" autoComplete="tel" error={errors.phone?.message} />
            </div>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="label-caps mb-2 block text-muted-foreground">First name</label>
              <Input {...register("firstName")} placeholder="First name" autoComplete="given-name" error={errors.firstName?.message} />
            </div>
            <div>
              <label className="label-caps mb-2 block text-muted-foreground">Last name</label>
              <Input {...register("lastName")} placeholder="Last name" autoComplete="family-name" error={errors.lastName?.message} />
            </div>
          </div>
          <div>
            <label className="label-caps mb-2 block text-muted-foreground">Gender</label>
            <select
              {...register("gender")}
              className="flex h-11 w-full rounded-sm border border-input bg-background px-3 py-2 text-sm"
            >
              <option value="">Select gender</option>
              <option value="MALE">Male</option>
              <option value="FEMALE">Female</option>
              <option value="OTHER">Other</option>
              <option value="PREFER_NOT_TO_SAY">Prefer not to say</option>
            </select>
            {errors.gender && <p className="mt-1 text-xs text-destructive">{errors.gender.message}</p>}
          </div>
          <div>
            <label className="label-caps mb-2 block text-muted-foreground">Username</label>
            <Input {...register("username")} placeholder="Username" autoComplete="username" error={errors.username?.message} />
          </div>
          <div>
            <label className="label-caps mb-2 block text-muted-foreground">Email</label>
            <Input
              {...register("email")}
              type="email"
              placeholder="Email"
              autoComplete="email"
              error={errors.email?.message}
            />
          </div>
          <div>
            <label className="label-caps mb-2 block text-muted-foreground">Password</label>
            <PasswordInput
              {...register("password")}
              placeholder="Password"
              autoComplete="new-password"
              error={errors.password?.message}
            />
          </div>
          <div>
            <label className="label-caps mb-2 block text-muted-foreground">Confirm password</label>
            <PasswordInput
              {...register("confirmPassword")}
              placeholder="Re-type password"
              autoComplete="new-password"
              error={errors.confirmPassword?.message}
            />
          </div>
          <label className="flex items-start gap-2 text-sm">
            <input type="checkbox" {...register("acceptTerms")} className="mt-1" />
            <span>I agree to the Terms of Service</span>
          </label>
          {errors.acceptTerms && <p className="text-xs text-destructive">{errors.acceptTerms.message}</p>}
          <Button type="submit" variant="primary" size="lg" className="w-full" disabled={loading}>
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
