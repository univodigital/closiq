"use client";

import { useEffect, useState } from "react";

export function HoldTimer({
  expiresAt,
  onExpired,
}: {
  expiresAt: string;
  onExpired?: () => void;
}) {
  const [remainingMs, setRemainingMs] = useState(() =>
    Math.max(0, new Date(expiresAt).getTime() - Date.now()),
  );

  useEffect(() => {
    const tick = () => {
      const next = Math.max(0, new Date(expiresAt).getTime() - Date.now());
      setRemainingMs(next);
      if (next === 0) onExpired?.();
    };
    tick();
    const id = window.setInterval(tick, 1000);
    return () => window.clearInterval(id);
  }, [expiresAt, onExpired]);

  const totalSeconds = Math.ceil(remainingMs / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  const label = `${minutes}:${seconds.toString().padStart(2, "0")}`;
  const expired = remainingMs <= 0;

  return (
    <div
      className={
        expired
          ? "rounded-sm border border-destructive/40 bg-destructive/5 p-4 text-sm text-destructive"
          : "rounded-sm border border-accent/40 bg-muted/30 p-4 text-sm"
      }
      role="timer"
      aria-live="polite"
    >
      {expired ? (
        <p>Your checkout hold expired. Start again to reserve your items.</p>
      ) : (
        <p>
          Complete payment in{" "}
          <span className="font-mono font-semibold tabular-nums">{label}</span>
        </p>
      )}
    </div>
  );
}
