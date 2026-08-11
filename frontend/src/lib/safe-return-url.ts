import { ROUTES } from "@/shared/constants/routes";

const DEFAULT_RETURN_URL = ROUTES.home;

/**
 * Validates post-login return URLs to prevent open redirects.
 * Only same-origin relative paths are allowed.
 */
export function getSafeReturnUrl(raw: string | null | undefined): string {
  if (!raw || typeof raw !== "string") {
    return DEFAULT_RETURN_URL;
  }

  const trimmed = raw.trim();
  if (!trimmed.startsWith("/") || trimmed.startsWith("//")) {
    return DEFAULT_RETURN_URL;
  }

  if (trimmed.includes("://") || trimmed.includes("\\")) {
    return DEFAULT_RETURN_URL;
  }

  if (trimmed.startsWith("/login") || trimmed.startsWith("/signup")) {
    return DEFAULT_RETURN_URL;
  }

  return trimmed;
}
