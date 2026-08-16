"use client";

import { useEffect } from "react";
import { toast } from "sonner";
import { consumeLoggedOutToastPending } from "@/features/auth/lib/logout-session";

export function LoggedOutToastListener() {
  useEffect(() => {
    if (!consumeLoggedOutToastPending()) return;
    toast.success("Successfully logged out");
  }, []);

  return null;
}
