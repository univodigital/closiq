import { getAccessToken, setAccessToken } from "./auth-token";
import type { ApiMeta, ApiResponse } from "@/shared/types";

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8081/api/v1";

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
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

async function parseError(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as ProblemBody;
    return body.detail ?? body.title ?? response.statusText;
  } catch {
    return response.statusText || "Request failed";
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
    if (
      auth &&
      !retried &&
      response.status === 401 &&
      (await refreshAccessToken())
    ) {
      return apiFetchRaw<T>(path, init, true);
    }

    if (auth && (response.status === 401 || response.status === 403) && getAccessToken()) {
      setAccessToken(null);
      if (typeof window !== "undefined") {
        window.dispatchEvent(new Event("closiq:session-expired"));
      }
    }
    throw new ApiError(await parseError(response), response.status);
  }

  if (response.status === 204) {
    return { success: true, data: undefined as T };
  }

  const envelope = (await response.json()) as { success: boolean; data: T; meta?: ApiMeta };
  return envelope;
}
