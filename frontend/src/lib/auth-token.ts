const ACCESS_TOKEN_KEY = "closiq_access_token";

export function getAccessToken(): string | null {
  if (typeof sessionStorage === "undefined") return null;
  return sessionStorage.getItem(ACCESS_TOKEN_KEY);
}

export function setAccessToken(token: string | null) {
  if (typeof sessionStorage === "undefined") return;
  if (token) {
    sessionStorage.setItem(ACCESS_TOKEN_KEY, token);
  } else {
    sessionStorage.removeItem(ACCESS_TOKEN_KEY);
  }
}
