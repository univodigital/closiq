import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { SESSION_COOKIE } from "@/shared/constants/routes";

const protectedPrefixes = ["/wishlist", "/orders", "/account", "/profile", "/checkout"];
const sellerPrefixes = ["/seller"];
const adminPrefixes = ["/admin"];
const sellerPublic = ["/seller/apply"];

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const hasSession = request.cookies.has(SESSION_COOKIE);

  const isProtected = protectedPrefixes.some((p) => pathname.startsWith(p));
  const isSellerRoute =
    sellerPrefixes.some((p) => pathname.startsWith(p)) &&
    !sellerPublic.some((p) => pathname.startsWith(p));
  const isAdminRoute = adminPrefixes.some((p) => pathname.startsWith(p));

  if (isProtected && !hasSession) {
    const url = request.nextUrl.clone();
    url.pathname = "/login";
    url.searchParams.set("returnUrl", `${pathname}${request.nextUrl.search}`);
    return NextResponse.redirect(url);
  }

  if (isSellerRoute && !hasSession) {
    const url = request.nextUrl.clone();
    url.pathname = "/login";
    url.searchParams.set("returnUrl", `${pathname}${request.nextUrl.search}`);
    return NextResponse.redirect(url);
  }

  if (isAdminRoute && !hasSession) {
    const url = request.nextUrl.clone();
    url.pathname = "/login";
    url.searchParams.set("returnUrl", `${pathname}${request.nextUrl.search}`);
    return NextResponse.redirect(url);
  }

  if ((pathname === "/login" || pathname.startsWith("/signup")) && hasSession) {
    return NextResponse.redirect(new URL("/", request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    "/wishlist/:path*",
    "/orders/:path*",
    "/account/:path*",
    "/profile/:path*",
    "/checkout/:path*",
    "/seller/:path*",
    "/admin/:path*",
    "/login",
    "/signup/:path*",
  ],
};
