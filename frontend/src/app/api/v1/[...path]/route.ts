import { NextRequest, NextResponse } from "next/server";

const FORWARD_REQUEST_HEADERS = [
  "accept",
  "accept-language",
  "authorization",
  "content-type",
  "cookie",
];

export const runtime = "nodejs";
export const dynamic = "force-dynamic";
export const maxDuration = 60;

type RouteContext = { params: Promise<{ path: string[] }> };

function resolveBackendTarget(): string {
  const configured = process.env.API_PROXY_TARGET?.replace(/\/$/, "");
  if (configured) return configured;
  if (process.env.NODE_ENV === "development") return "http://localhost:8081";
  return "";
}

async function proxy(request: NextRequest, context: RouteContext) {
  const backend = resolveBackendTarget();
  if (!backend) {
    return NextResponse.json(
      {
        title: "Service Unavailable",
        detail:
          "API_PROXY_TARGET is not configured. Set it in Vercel to your backend URL (e.g. http://YOUR_EC2_IP:8081), then redeploy.",
      },
      { status: 503 },
    );
  }

  const { path } = await context.params;
  const target = new URL(`/api/v1/${path.join("/")}`, backend);
  target.search = request.nextUrl.search;

  const headers = new Headers();
  for (const name of FORWARD_REQUEST_HEADERS) {
    const value = request.headers.get(name);
    if (value) headers.set(name, value);
  }

  const init: RequestInit = {
    method: request.method,
    headers,
    redirect: "manual",
  };

  if (request.method !== "GET" && request.method !== "HEAD") {
    init.body = await request.arrayBuffer();
  }

  let backendResponse: Response;
  try {
    backendResponse = await fetch(target, init);
  } catch (error) {
    console.error("API proxy fetch failed:", target.toString(), error);
    return NextResponse.json(
      {
        title: "Bad Gateway",
        detail: `Could not reach backend at ${backend}.`,
      },
      { status: 502 },
    );
  }

  const responseHeaders = new Headers();

  backendResponse.headers.forEach((value, key) => {
    const lower = key.toLowerCase();
    if (lower === "transfer-encoding" || lower === "connection") return;
    responseHeaders.append(key, value);
  });

  return new NextResponse(backendResponse.body, {
    status: backendResponse.status,
    headers: responseHeaders,
  });
}

export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const PATCH = proxy;
export const DELETE = proxy;
export const OPTIONS = proxy;
