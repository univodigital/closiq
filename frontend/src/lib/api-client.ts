import { getAccessToken, setAccessToken } from "./auth-token";
import type { ApiMeta, ApiResponse } from "@/shared/types";

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8081/api/v1";

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly code?: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export interface ApiFetchOptions extends RequestInit {
  /** When false, the request is sent without the stored access token. */
  auth?: boolean;
}

interface ProblemBody {
  detail?: string;
  title?: string;
  code?: string;
}

const SESSION_EXPIRED_CODES = new Set(["UNAUTHORIZED", "TOKEN_EXPIRED"]);

function isProviderAuthFailure(detail?: string): boolean {
  if (!detail) return false;
  const normalized = detail.toLowerCase();
  return normalized.includes("razorpay") || normalized.includes("payment provider");
}

function mightBeExpiredSession(status: number, code?: string, detail?: string): boolean {
  if (status !== 401 || !getAccessToken()) {
    return false;
  }
  if (isProviderAuthFailure(detail)) {
    return false;
  }
  if (code && !SESSION_EXPIRED_CODES.has(code)) {
    return false;
  }
  return true;
}

async function refreshAccessToken(): Promise<boolean> {
  try {
    const response = await fetch(`${API_BASE}/auth/refresh`, {
      method: "POST",
      credentials: "include",
    });
    if (!response.ok) return false;
    const envelope = (await response.json()) as { data?: { accessToken?: string } };
    if (!envelope.data?.accessToken) return false;
    setAccessToken(envelope.data.accessToken);
    return true;
  } catch {
    return false;
  }
}

async function parseProblemBody(response: Response): Promise<ProblemBody> {
  try {
    return (await response.json()) as ProblemBody;
  } catch {
    return {};
  }
}

export async function apiFetch<T>(
  path: string,
  init: ApiFetchOptions = {},
): Promise<T> {
  const envelope = await apiFetchRaw<T>(path, init);
  return envelope.data;
}

export async function apiFetchEnvelope<T>(
  path: string,
  init: ApiFetchOptions = {},
): Promise<ApiResponse<T>> {
  const envelope = await apiFetchRaw<T>(path, init);
  return {
    success: envelope.success,
    data: envelope.data,
    meta: envelope.meta ?? {},
  };
}

async function apiFetchRaw<T>(
  path: string,
  init: ApiFetchOptions = {},
  retried = false,
): Promise<{ success: boolean; data: T; meta?: ApiMeta }> {
  const { auth = true, ...requestInit } = init;
  const headers = new Headers(requestInit.headers);
  if (
    !headers.has("Content-Type") &&
    requestInit.body &&
    !(requestInit.body instanceof FormData)
  ) {
    headers.set("Content-Type", "application/json");
  }

  if (auth) {
    const token = getAccessToken();
    if (token) {
      headers.set("Authorization", `Bearer ${token}`);
    }
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...requestInit,
    headers,
    credentials: "include",
  });

  if (!response.ok) {
    const problem = await parseProblemBody(response);

    if (
      auth &&
      !retried &&
      mightBeExpiredSession(response.status, problem.code, problem.detail) &&
      (await refreshAccessToken())
    ) {
      return apiFetchRaw<T>(path, init, true);
    }

    if (auth && mightBeExpiredSession(response.status, problem.code, problem.detail)) {
      setAccessToken(null);
      if (typeof window !== "undefined") {
        window.dispatchEvent(new Event("closiq:session-expired"));
      }
    }

    throw new ApiError(
      problem.detail ?? problem.title ?? response.statusText,
      response.status,
      problem.code,
    );
  }

  if (response.status === 204) {
    return { success: true, data: undefined as T };
  }

  const envelope = (await response.json()) as { success: boolean; data: T; meta?: ApiMeta };
  return envelope;
}
