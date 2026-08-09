"use client";

import { createContext, useContext, useMemo, useState } from "react";

type AppMode = "shop" | "seller";

interface AppModeContextValue {
  mode: AppMode;
  setMode: (mode: AppMode) => void;
  toggleMode: () => void;
}

const AppModeContext = createContext<AppModeContextValue | null>(null);

export function AppModeProvider({ children }: { children: React.ReactNode }) {
  const [mode, setMode] = useState<AppMode>("shop");

  const value = useMemo(
    () => ({
      mode,
      setMode,
      toggleMode: () => setMode((m) => (m === "shop" ? "seller" : "shop")),
    }),
    [mode],
  );

  return (
    <AppModeContext.Provider value={value}>{children}</AppModeContext.Provider>
  );
}

export function useAppMode() {
  const ctx = useContext(AppModeContext);
  if (!ctx) throw new Error("useAppMode must be used within AppModeProvider");
  return ctx;
}
