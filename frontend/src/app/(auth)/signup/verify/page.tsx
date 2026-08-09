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
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [loading, setLoading] = useState(false);
  const [phone, setPhone] = useState("");

  useEffect(() => {
    setPhone(sessionStorage.getItem("otpPhone") ?? "your phone");
  }, []);

  const onVerify = async () => {
    const otpSessionId = sessionStorage.getItem("otpSessionId");
    if (!otpSessionId) {
      toast.error("Session expired. Please start again.");
      router.push(ROUTES.login);
      return;
    }
    setLoading(true);
    try {
      await verifyOtp(
        otpSessionId,
        otp,
        mode === "register" ? { firstName, lastName } : undefined,
      );
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
        {mode === "register" && (
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label-caps mb-2 block text-muted-foreground">First name</label>
              <Input value={firstName} onChange={(e) => setFirstName(e.target.value)} />
            </div>
            <div>
              <label className="label-caps mb-2 block text-muted-foreground">Last name</label>
              <Input value={lastName} onChange={(e) => setLastName(e.target.value)} />
            </div>
          </div>
        )}
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
