"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { toast } from "sonner";
import { useAuth } from "@/providers/AuthProvider";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ROUTES } from "@/shared/constants/routes";

export default function VerifyOtpPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const returnUrl = searchParams.get("returnUrl") ?? ROUTES.home;
  const mode = searchParams.get("mode") ?? "login";
  const { verifyOtp } = useAuth();
  const [otp, setOtp] = useState("");
  const [loading, setLoading] = useState(false);
  const [phone, setPhone] = useState("");

  useEffect(() => {
    setPhone(sessionStorage.getItem("otpPhone") ?? "your phone");
  }, []);

  const onVerify = async () => {
    const otpSessionId = sessionStorage.getItem("otpSessionId");
    if (!otpSessionId) {
      toast.error("Session expired. Please start again.");
      router.push(mode === "register" ? ROUTES.signup : ROUTES.login);
      return;
    }
    setLoading(true);
    try {
      if (mode === "register") {
        const username = sessionStorage.getItem("registerUsername");
        const password = sessionStorage.getItem("registerPassword");
        if (!username || !password) {
          toast.error("Registration details missing. Please start again.");
          router.push(ROUTES.signup);
          return;
        }
        await verifyOtp(otpSessionId, otp, { username, password });
        sessionStorage.removeItem("registerUsername");
        sessionStorage.removeItem("registerPassword");
      } else {
        await verifyOtp(otpSessionId, otp);
      }
      sessionStorage.removeItem("otpSessionId");
      sessionStorage.removeItem("otpPhone");
      toast.success("Welcome to Closiq");
      router.push(returnUrl);
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Invalid OTP");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Enter OTP</CardTitle>
        <p className="text-sm text-muted-foreground">
          Sent to {phone}
        </p>
      </CardHeader>
      <CardContent className="space-y-4">
        <div>
          <label className="label-caps mb-2 block text-muted-foreground">6-digit code</label>
          <Input
            value={otp}
            onChange={(e) => setOtp(e.target.value.replace(/\D/g, "").slice(0, 6))}
            placeholder="123456"
            maxLength={6}
            className="font-mono text-center text-lg tracking-[0.5em]"
            aria-label="OTP code"
          />
        </div>
        <Button variant="primary" size="lg" onClick={onVerify} disabled={loading || otp.length !== 6}>
          {loading ? "Verifying…" : "Verify & continue"}
        </Button>
        <p className="text-center text-xs text-muted-foreground">
          Check the backend console for your OTP
        </p>
      </CardContent>
    </Card>
  );
}
