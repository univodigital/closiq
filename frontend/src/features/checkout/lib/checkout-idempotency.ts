const CHECKOUT_ATTEMPT_KEY = "closiq_checkout_attempt";
const CHECKOUT_BAG_FINGERPRINT_KEY = "closiq_checkout_bag_fp";

export function getCheckoutAttemptId(): string {
  if (typeof window === "undefined") return crypto.randomUUID();
  let id = sessionStorage.getItem(CHECKOUT_ATTEMPT_KEY);
  if (!id) {
    id = crypto.randomUUID();
    sessionStorage.setItem(CHECKOUT_ATTEMPT_KEY, id);
  }
  return id;
}

export function resetCheckoutAttemptId(): void {
  if (typeof window === "undefined") return;
  sessionStorage.removeItem(CHECKOUT_ATTEMPT_KEY);
  sessionStorage.removeItem(CHECKOUT_BAG_FINGERPRINT_KEY);
}

/** Start a fresh idempotency scope when bag contents change mid-checkout. */
export function syncCheckoutAttemptWithBag(bagFingerprint: string): void {
  if (typeof window === "undefined") return;
  const stored = sessionStorage.getItem(CHECKOUT_BAG_FINGERPRINT_KEY);
  if (stored !== bagFingerprint) {
    sessionStorage.setItem(CHECKOUT_BAG_FINGERPRINT_KEY, bagFingerprint);
    sessionStorage.removeItem(CHECKOUT_ATTEMPT_KEY);
  }
}

export function checkoutIdempotencyKey(suffix: string): string {
  return `${getCheckoutAttemptId()}-${suffix}`;
}
