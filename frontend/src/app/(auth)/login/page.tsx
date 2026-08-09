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

const schema = z.object({
  phone: z.string().regex(/^(\+91)?[6-9]\d{9}$/, "Enter valid 10-digit mobile"),
});

type FormData = z.infer<typeof schema>;

export default function LoginPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const returnUrl = searchParams.get("returnUrl") ?? ROUTES.home;
  const { login } = useAuth();
  const [loading, setLoading] = useState(false);

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { phone: "" },
  });

  const onSubmit = async (data: FormData) => {
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

  return (
    <Card>
      <CardHeader>
        <CardTitle>Welcome back</CardTitle>
        <p className="text-sm text-muted-foreground">Sign in with your mobile number</p>
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
            {loading ? "Sending OTP…" : "Continue"}
          </Button>
        </form>
        <p className="mt-6 text-center text-sm text-muted-foreground">
          New to Closiq?{" "}
          <Link href={ROUTES.signup} className="text-accent underline-offset-4 hover:underline">
            Create account
          </Link>
        </p>
        <p className="text-center text-xs text-muted-foreground">
          Check the backend console for your OTP
        </p>
      </CardContent>
    </Card>
  );
}
