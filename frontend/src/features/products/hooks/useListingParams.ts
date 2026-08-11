"use client";

import { useMemo } from "react";
import { useRentalDates } from "@/providers/RentalDatesProvider";
import type { ProductListParams } from "@/shared/types";

/** Merge listing filters with shared rental dates when valid. */
export function useListingParams(base?: ProductListParams): ProductListParams {
  const { dates, hasDates } = useRentalDates();

  return useMemo(() => {
    if (!hasDates || !dates) return base ?? {};
    return {
      ...base,
      startDate: dates.start,
      endDate: dates.end,
    };
  }, [base, dates, hasDates]);
}
