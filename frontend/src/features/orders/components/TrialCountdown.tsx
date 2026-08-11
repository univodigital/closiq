"use client";

import { useEffect, useState } from "react";

function formatCountdown(totalSeconds: number): string {
  const clamped = Math.max(0, totalSeconds);
  const minutes = Math.floor(clamped / 60);
  const seconds = clamped % 60;
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}

export function TrialCountdown({ expiresAt }: { expiresAt: string }) {
  const [remaining, setRemaining] = useState(() => secondsUntil(expiresAt));

  useEffect(() => {
    const tick = () => setRemaining(secondsUntil(expiresAt));
    tick();
    const id = window.setInterval(tick, 1000);
    return () => window.clearInterval(id);
  }, [expiresAt]);

  return (
    <p
      className="font-mono text-4xl tabular-nums tracking-tight text-accent"
      aria-live="polite"
      aria-label={`Accept within ${formatCountdown(remaining)}`}
    >
      {formatCountdown(remaining)}
    </p>
  );
}

function secondsUntil(iso: string): number {
  const end = Date.parse(iso);
  if (Number.isNaN(end)) return 0;
  return Math.ceil((end - Date.now()) / 1000);
}

export function isTrialExpired(expiresAt: string, expired?: boolean): boolean {
  if (expired) return true;
  return secondsUntil(expiresAt) <= 0;
}
