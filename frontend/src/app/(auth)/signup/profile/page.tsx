"use client";

import { useEffect, useState } from "react";
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
import { ApiError } from "@/lib/api-client";
import type { Gender } from "@/shared/types";

const passwordSchema = z
  .string()
  .min(8, "At least 8 characters")
  .regex(/[A-Z]/, "Include an uppercase letter")
  .regex(/\d/, "Include a number");

const schema = z.object({
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
  email: z.string().email("Enter a valid email address").optional().or(z.literal("")),
  password: passwordSchema,
});

type FormData = z.infer<typeof schema>;

export default function SignupProfilePage() {
  const router = useRouter();
  const { completeRegistration } = useAuth();
  const [loading, setLoading] = useState(false);

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  useEffect(() => {
    const verified = sessionStorage.getItem("registerOtpVerified");
    const otpSessionId = sessionStorage.getItem("otpSessionId");
    if (!verified || !otpSessionId) {
      router.replace(ROUTES.signup);
    }
  }, [router]);

  const onSubmit = async (data: FormData) => {
    const otpSessionId = sessionStorage.getItem("otpSessionId");
    if (!otpSessionId) {
      toast.error("Session expired. Please start again.");
      router.push(ROUTES.signup);
      return;
    }
    setLoading(true);
    try {
      await completeRegistration(otpSessionId, {
        username: data.username,
        password: data.password,
        email: data.email?.trim() ? data.email.trim().toLowerCase() : undefined,
        firstName: data.firstName.trim(),
        lastName: data.lastName.trim(),
        gender: data.gender as Gender,
      });
      sessionStorage.removeItem("otpSessionId");
      sessionStorage.removeItem("otpPhone");
      sessionStorage.removeItem("otpResendIn");
      sessionStorage.removeItem("registerOtpVerified");
      router.push("/signup/welcome");
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        toast.error(error.message || "Profile details conflict with an existing account");
      } else {
        toast.error(error instanceof Error ? error.message : "Could not complete registration");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Complete your profile</CardTitle>
        <p className="text-sm text-muted-foreground">Tell us a bit about yourself to finish setting up your account</p>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
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
            <label className="label-caps mb-2 block text-muted-foreground">Email (optional)</label>
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
            <Input
              {...register("password")}
              type="password"
              placeholder="Password"
              autoComplete="new-password"
              error={errors.password?.message}
            />
          </div>
          <Button type="submit" variant="primary" size="lg" disabled={loading}>
            {loading ? "Creating account…" : "Complete registration"}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
