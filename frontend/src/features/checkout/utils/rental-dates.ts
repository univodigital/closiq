import type { AvailabilityData } from "@/shared/types";

/** Must match backend `closiq.booking.rental-lead-days`. */
export const RENTAL_LEAD_DAYS = 2;

export type RentalLimits = {
  minRentalDays?: number;
  maxRentalDays?: number | null;
};

function addDays(isoDate: string, days: number): string {
  const date = new Date(`${isoDate}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

export function todayIsoDate(): string {
  return new Date().toISOString().slice(0, 10);
}

/** Earliest selectable delivery date (today + lead days). */
export function earliestRentalStartDate(leadDays = RENTAL_LEAD_DAYS): string {
  return addDays(todayIsoDate(), leadDays);
}

export function rentalDayCount(start: string, end: string): number {
  const startMs = Date.parse(start);
  const endMs = Date.parse(end);
  if (Number.isNaN(startMs) || Number.isNaN(endMs) || endMs < startMs) return 0;
  return Math.floor((endMs - startMs) / 86_400_000) + 1;
}

export function validateRentalDates(start: string, end: string): string | null {
  if (!start || !end) return "Select delivery and return dates to continue.";
  const earliest = earliestRentalStartDate();
  if (start < earliest) {
    return `Delivery must be at least ${RENTAL_LEAD_DAYS} days from today.`;
  }
  if (end < start) return "Return date must be on or after delivery date.";
  return null;
}

function resolveLimits(
  data: AvailabilityData | undefined,
  limits?: RentalLimits,
): RentalLimits {
  return {
    minRentalDays: data?.minRentalDays ?? limits?.minRentalDays ?? 1,
    maxRentalDays: data?.maxRentalDays ?? limits?.maxRentalDays,
  };
}

function rentalPeriodError(days: number, limits: RentalLimits): string | null {
  const min = limits.minRentalDays ?? 1;
  if (days < min) {
    return `Minimum rental is ${min} day${min === 1 ? "" : "s"}.`;
  }
  if (limits.maxRentalDays != null && days > limits.maxRentalDays) {
    return `Maximum rental is ${limits.maxRentalDays} days.`;
  }
  return null;
}

function rangesOverlap(
  startA: string,
  endA: string,
  startB: string,
  endB: string,
): boolean {
  return startA <= endB && endA >= startB;
}

/** Extend end date by buffer days (used when checking inventory holds). */
export function effectiveRentalEnd(end: string, bufferDays = 0): string {
  if (!end || bufferDays <= 0) return end;
  const date = new Date(`${end}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() + bufferDays);
  return date.toISOString().slice(0, 10);
}

export function isRentalRangeAvailable(
  data: AvailabilityData | undefined,
  start: string,
  end: string,
  limits?: RentalLimits,
): boolean {
  const dateError = validateRentalDates(start, end);
  if (dateError) return false;

  const days = rentalDayCount(start, end);
  const resolved = resolveLimits(data, limits);
  if (rentalPeriodError(days, resolved)) return false;

  if (!data) return false;

  const buffer = data.bufferDaysAfterReturn ?? 0;
  const checkEnd = effectiveRentalEnd(end, buffer);

  for (const unavailable of data.unavailableDates) {
    if (unavailable >= start && unavailable <= checkEnd) return false;
  }

  for (const range of [...data.bookedRanges, ...data.blockedRanges]) {
    if (rangesOverlap(start, checkEnd, range.start, range.end)) return false;
  }

  return true;
}

export function rentalDatesError(
  data: AvailabilityData | undefined,
  start: string,
  end: string,
  limits?: RentalLimits,
): string | null {
  const basic = validateRentalDates(start, end);
  if (basic) return basic;

  const days = rentalDayCount(start, end);
  const resolved = resolveLimits(data, limits);
  const periodError = rentalPeriodError(days, resolved);
  if (periodError) return periodError;

  if (!data) return null;

  const buffer = data.bufferDaysAfterReturn ?? 0;
  const checkEnd = effectiveRentalEnd(end, buffer);

  const blockedInRange = data.unavailableDates.filter(
    (d) => d >= start && d <= checkEnd,
  );
  if (blockedInRange.length > 0) {
    const next = data.nextAvailableDate;
    if (next) {
      return `These dates aren't available. Next opening: ${next}.`;
    }
    return "Selected dates aren't available. Choose different dates.";
  }

  for (const range of [...data.bookedRanges, ...data.blockedRanges]) {
    if (rangesOverlap(start, checkEnd, range.start, range.end)) {
      const next = data.nextAvailableDate;
      if (next) {
        return `These dates aren't available. Next opening: ${next}.`;
      }
      return "Selected dates aren't available. Choose different dates.";
    }
  }

  return null;
}

export function formatRentalLimits(limits?: RentalLimits): string | null {
  if (!limits?.minRentalDays && limits?.maxRentalDays == null) return null;
  const min = limits.minRentalDays ?? 1;
  if (limits.maxRentalDays != null) {
    if (min === limits.maxRentalDays) {
      return `Rental period: ${min} day${min === 1 ? "" : "s"}.`;
    }
    return `Rental period: ${min}–${limits.maxRentalDays} days.`;
  }
  return `Minimum rental: ${min} day${min === 1 ? "" : "s"}.`;
}
