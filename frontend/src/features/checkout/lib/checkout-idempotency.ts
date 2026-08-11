const CHECKOUT_ATTEMPT_KEY = "closiq_checkout_attempt";

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
}

export function checkoutIdempotencyKey(suffix: string): string {
  return `${getCheckoutAttemptId()}-${suffix}`;
}
