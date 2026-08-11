"use client";

import { useEffect, useState } from "react";

function formatRemaining(ms: number): string {
  if (ms <= 0) return "00:00:00";
  const totalSeconds = Math.floor(ms / 1000);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  return [hours, minutes, seconds].map((n) => String(n).padStart(2, "0")).join(":");
}

interface AcceptCountdownProps {
  deadlineAt: string;
  expired?: boolean;
  className?: string;
}

export function AcceptCountdown({ deadlineAt, expired, className }: AcceptCountdownProps) {
  const [remaining, setRemaining] = useState(() => {
    const deadline = new Date(deadlineAt).getTime();
    return deadline - Date.now();
  });

  useEffect(() => {
    const deadline = new Date(deadlineAt).getTime();
    const tick = () => setRemaining(deadline - Date.now());
    tick();
    const id = window.setInterval(tick, 1000);
    return () => window.clearInterval(id);
  }, [deadlineAt]);

  const isExpired = expired || remaining <= 0;

  return (
    <span
      className={className}
      aria-live="polite"
      aria-label={isExpired ? "Acceptance window expired" : `Accept within ${formatRemaining(remaining)}`}
    >
      {isExpired ? "Expired" : formatRemaining(remaining)}
    </span>
  );
}
