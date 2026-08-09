"use client";

import { useMemo } from "react";
import { useSearchParams } from "next/navigation";

/** Checkout URL params shared across steps — bag line items live in localStorage. */
export function useCheckoutParams() {
  const searchParams = useSearchParams();

  const params = useMemo(
    () => ({
      addressId: searchParams.get("addressId") ?? "",
      pincode: searchParams.get("pincode") ?? "",
      couponCode: searchParams.get("couponCode") ?? "",
      /** Legacy single-item deep links — bag page imports then clears these. */
      slug: searchParams.get("slug") ?? "",
      size: searchParams.get("size") ?? "",
      start: searchParams.get("start") ?? "",
      end: searchParams.get("end") ?? "",
    }),
    [searchParams],
  );

  const baseQuery = useMemo(() => {
    const qs = new URLSearchParams();
    if (params.couponCode) qs.set("couponCode", params.couponCode);
    return qs.toString();
  }, [params.couponCode]);

  function withAddress(addressId: string, pincode: string) {
    const qs = new URLSearchParams(baseQuery);
    qs.set("addressId", addressId);
    qs.set("pincode", pincode);
    return qs.toString();
  }

  function fullQuery(overrides?: { addressId?: string; pincode?: string; couponCode?: string }) {
    const qs = new URLSearchParams();
    const coupon = overrides?.couponCode ?? params.couponCode;
    const aid = overrides?.addressId ?? params.addressId;
    const pc = overrides?.pincode ?? params.pincode;
    if (coupon) qs.set("couponCode", coupon);
    if (aid) qs.set("addressId", aid);
    if (pc) qs.set("pincode", pc);
    return qs.toString();
  }

  const hasLegacyItemParams = !!(params.slug && params.size && params.start && params.end);

  return { ...params, baseQuery, withAddress, fullQuery, hasLegacyItemParams };
}
