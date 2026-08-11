"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { toast } from "sonner";
import { useAuth } from "@/providers/AuthProvider";
import { authService } from "@/features/auth/services";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ROUTES } from "@/shared/constants/routes";
import { getSafeReturnUrl } from "@/lib/safe-return-url";
import { maskPhone } from "@/lib/phone-mask";
import { ApiError } from "@/lib/api-client";

export default function VerifyOtpPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const returnUrl = getSafeReturnUrl(searchParams.get("returnUrl"));
  const mode = searchParams.get("mode") ?? "login";
  const { verifyLoginOtp, verifyRegistrationOtp } = useAuth();
  const [otp, setOtp] = useState("");
  const [loading, setLoading] = useState(false);
  const [resendLoading, setResendLoading] = useState(false);
  const [phone, setPhone] = useState("");
  const [resendIn, setResendIn] = useState(0);
  const [resendExhausted, setResendExhausted] = useState(false);

  useEffect(() => {
    const storedPhone = sessionStorage.getItem("otpPhone");
    const deliveryHint = sessionStorage.getItem("otpDeliveryHint");
    setPhone(storedPhone ?? deliveryHint ?? "");
    const initialResend = Number(sessionStorage.getItem("otpResendIn") ?? "60");
    setResendIn(Number.isFinite(initialResend) ? initialResend : 60);
  }, []);

  useEffect(() => {
    if (resendIn <= 0) return;
    const timer = window.setInterval(() => {
      setResendIn((prev) => Math.max(0, prev - 1));
    }, 1000);
    return () => window.clearInterval(timer);
  }, [resendIn]);

  const onResend = useCallback(async () => {
    const otpSessionId = sessionStorage.getItem("otpSessionId");
    if (!otpSessionId || resendIn > 0 || resendExhausted) return;
    setResendLoading(true);
    try {
      const result = await authService.resendOtp(otpSessionId);
      setResendIn(result.resendAvailableInSeconds);
      sessionStorage.setItem("otpResendIn", String(result.resendAvailableInSeconds));
      toast.success("OTP resent");
    } catch (error) {
      if (error instanceof ApiError && error.status === 429) {
        setResendExhausted(true);
        toast.error(error.message || "Maximum resend attempts reached");
      } else if (error instanceof ApiError) {
        toast.error(error.message);
      } else {
        toast.error("Could not resend OTP");
      }
    } finally {
      setResendLoading(false);
    }
  }, [resendIn, resendExhausted]);

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
        const result = await verifyRegistrationOtp(otpSessionId, otp);
        if (result.existingAccount) {
          sessionStorage.removeItem("otpSessionId");
          sessionStorage.removeItem("otpResendIn");
          router.push(`/signup/existing?phone=${encodeURIComponent(result.phone ?? phone)}`);
          return;
        }
        if (result.requiresProfile) {
          sessionStorage.setItem("registerOtpVerified", "1");
          router.push("/signup/profile");
          return;
        }
        if (result.authenticated) {
          sessionStorage.removeItem("otpSessionId");
          sessionStorage.removeItem("otpResendIn");
          router.push("/signup/welcome");
          return;
        }
      } else {
        await verifyLoginOtp(otpSessionId, otp);
        sessionStorage.removeItem("otpSessionId");
        sessionStorage.removeItem("otpDeliveryHint");
        sessionStorage.removeItem("otpChannel");
        sessionStorage.removeItem("otpResendIn");
        toast.success("Welcome back");
        router.push(returnUrl);
      }
    } catch (e) {
      if (e instanceof ApiError && e.status === 403) {
        toast.error(e.message);
      } else {
        toast.error(e instanceof Error ? e.message : "Invalid OTP");
      }
    } finally {
      setLoading(false);
    }
  };

  const maskedPhone = phone.includes("@") ? phone : maskPhone(phone);

  return (
    <Card>
      <CardHeader>
        <CardTitle>Enter OTP</CardTitle>
        <p className="text-sm text-muted-foreground">
          {phone.includes("@")
            ? `Code sent to ${phone}`
            : phone
              ? `Code sent to ${maskedPhone}`
              : "Enter the 6-digit code we sent you"}
        </p>
      </CardHeader>
      <CardContent className="space-y-4">
        <div>
          <label className="label-caps mb-2 block text-muted-foreground">6-digit code</label>
          <Input
            value={otp}
            onChange={(e) => setOtp(e.target.value.replace(/\D/g, "").slice(0, 6))}
            placeholder="6-digit code"
            maxLength={6}
            className="font-mono text-center text-lg tracking-[0.5em]"
            aria-label="OTP code"
          />
        </div>
        <Button variant="primary" size="lg" className="w-full" onClick={onVerify} disabled={loading || otp.length !== 6}>
          {loading ? "Verifying…" : mode === "login" ? "Verify & Login" : "Verify OTP"}
        </Button>
        <div className="text-center">
          {resendExhausted ? (
            <p className="text-xs text-muted-foreground">
              {mode === "login" ? "Resend limit reached. Start again from login." : "Resend limit reached. Start again from sign up."}
            </p>
          ) : resendIn > 0 ? (
            <p className="text-xs text-muted-foreground">Resend in {resendIn}s</p>
          ) : (
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={onResend}
              disabled={resendLoading}
              className="text-accent"
            >
              {resendLoading ? "Sending…" : "Resend OTP"}
            </Button>
          )}
        </div>
        {mode === "register" && (
          <p className="text-center text-xs text-muted-foreground">
            <Link href={ROUTES.signup} className="text-accent hover:underline">Change phone number</Link>
          </p>
        )}
        {mode === "login" && (
          <p className="text-center text-xs text-muted-foreground">
            <Link href={ROUTES.login} className="text-accent hover:underline">Back to login</Link>
          </p>
        )}
      </CardContent>
    </Card>
  );
}
