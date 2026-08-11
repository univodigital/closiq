"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { validateRentalDates } from "@/features/checkout/utils/rental-dates";

const STORAGE_KEY = "closiq_rental_dates";

export interface RentalDates {
  start: string;
  end: string;
}

interface RentalDatesContextValue {
  dates: RentalDates | null;
  hasDates: boolean;
  setDates: (start: string, end: string) => void;
  clearDates: () => void;
  dateError: string | null;
}

const RentalDatesContext = createContext<RentalDatesContextValue | null>(null);

function readStoredDates(): RentalDates | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as RentalDates;
    if (parsed?.start && parsed?.end) return parsed;
  } catch {
    /* ignore */
  }
  return null;
}

function writeStoredDates(dates: RentalDates | null) {
  if (typeof window === "undefined") return;
  if (!dates) {
    localStorage.removeItem(STORAGE_KEY);
    return;
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(dates));
}

export function RentalDatesProvider({ children }: { children: React.ReactNode }) {
  const [dates, setDatesState] = useState<RentalDates | null>(null);
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    setDatesState(readStoredDates());
    setHydrated(true);
  }, []);

  const setDates = useCallback((start: string, end: string) => {
    const next = { start, end };
    setDatesState(next);
    writeStoredDates(next);
  }, []);

  const clearDates = useCallback(() => {
    setDatesState(null);
    writeStoredDates(null);
  }, []);

  const dateError = useMemo(() => {
    if (!hydrated || !dates) return null;
    return validateRentalDates(dates.start, dates.end);
  }, [dates, hydrated]);

  const value = useMemo(
    () => ({
      dates: hydrated ? dates : null,
      hasDates: !!(hydrated && dates?.start && dates?.end && !dateError),
      setDates,
      clearDates,
      dateError,
    }),
    [dates, hydrated, setDates, clearDates, dateError],
  );

  return <RentalDatesContext.Provider value={value}>{children}</RentalDatesContext.Provider>;
}

export function useRentalDates() {
  const ctx = useContext(RentalDatesContext);
  if (!ctx) throw new Error("useRentalDates must be used within RentalDatesProvider");
  return ctx;
}
