export const LOGGED_OUT_TOAST_KEY = "closiq:logged-out-toast";

export function markLoggedOutToastPending() {
  sessionStorage.setItem(LOGGED_OUT_TOAST_KEY, "1");
}

export function consumeLoggedOutToastPending(): boolean {
  if (sessionStorage.getItem(LOGGED_OUT_TOAST_KEY) !== "1") return false;
  sessionStorage.removeItem(LOGGED_OUT_TOAST_KEY);
  return true;
}
