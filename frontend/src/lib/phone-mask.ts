/**
 * Masks an Indian phone number for display, e.g. +919876543210 → +91 XXXXXX3210
 */
export function maskPhone(phone: string): string {
  const digits = phone.replace(/\D/g, "");
  if (digits.length < 4) return phone;

  const local = digits.startsWith("91") && digits.length >= 12
    ? digits.slice(2)
    : digits.length === 10
      ? digits
      : digits.slice(-10);

  if (local.length !== 10) {
    return phone.slice(0, Math.max(0, phone.length - 4)) + "****";
  }

  return `+91 XXXXXX${local.slice(-4)}`;
}
