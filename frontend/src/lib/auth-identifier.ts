export type AuthIdentifierType = "phone" | "email";

export function normalizeAuthIdentifier(raw: string): { type: AuthIdentifierType; value: string } {
  const trimmed = raw.trim();
  if (!trimmed) {
    throw new Error("Phone or email is required");
  }

  if (trimmed.includes("@")) {
    const email = trimmed.toLowerCase();
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      throw new Error("Enter a valid email address");
    }
    return { type: "email", value: email };
  }

  const digits = trimmed.replace(/\D/g, "");
  const local = digits.startsWith("91") && digits.length === 12 ? digits.slice(2) : digits;
  if (!/^[6-9]\d{9}$/.test(local)) {
    throw new Error("Enter a valid 10-digit mobile number or email");
  }

  return { type: "phone", value: `+91${local}` };
}

export function formatAuthIdentifierHint(type: AuthIdentifierType, value: string): string {
  if (type === "email") {
    return value;
  }
  return value.replace("+91", "+91 ");
}
