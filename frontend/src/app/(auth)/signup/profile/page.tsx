"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { ROUTES } from "@/shared/constants/routes";

/** Profile details are collected on the signup page before OTP verification. */
export default function SignupProfilePage() {
  const router = useRouter();

  useEffect(() => {
    router.replace(ROUTES.signup);
  }, [router]);

  return null;
}
