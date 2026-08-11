"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/providers/AuthProvider";
import { useBag } from "@/providers/BagProvider";
import { Button } from "@/components/ui/button";
import { ROUTES } from "@/shared/constants/routes";
import { useCheckoutParams } from "@/features/checkout/hooks/useCheckoutParams";
import { CheckoutLayoutShell, CheckoutTwoColumn } from "@/features/checkout/components/CheckoutLayoutShell";
import { AddressSelector } from "@/features/checkout/components/AddressSelector";
import { GuestPincodeField } from "@/features/checkout/components/GuestPincodeField";
import { PriceDetails } from "@/features/checkout/components/PriceDetails";
import { calculateBagPricing, loadBagLines } from "@/features/checkout/utils/bag-pricing";
import {
  hasBlockingAvailabilityIssues,
  validateBagLinesAvailability,
} from "@/features/checkout/utils/bag-availability";
import { isCompleteBagItem } from "@/features/checkout/bag/bag-store";
import type { Address } from "@/shared/types";

export default function CheckoutAddressPage() {
  const router = useRouter();
  const { user, refreshUser, isAuthenticated } = useAuth();
  const { items: bagItems } = useBag();
  const { addressId, pincode, fullQuery, withAddress, couponCode } = useCheckoutParams();
  const [guestPincode, setGuestPincode] = useState(pincode);
  const [guestServiceable, setGuestServiceable] = useState<boolean | null>(null);

  const completeItems = bagItems.filter(isCompleteBagItem);
  const bagKey = JSON.stringify(completeItems);

  const addresses = user?.addresses ?? [];

  const selectedAddress = useMemo(() => {
    if (!addresses.length) return undefined;
    if (addressId) return addresses.find((a) => a.id === addressId);
    return addresses.find((a) => a.isDefault) ?? addresses[0];
  }, [addresses, addressId]);

  const effectivePincode = isAuthenticated
    ? pincode || selectedAddress?.pincode || ""
    : guestPincode || pincode;

  const pricing = useQuery({
    queryKey: ["bag-pricing", bagKey, effectivePincode, couponCode],
    queryFn: async () => {
      const lines = await loadBagLines(completeItems);
      return calculateBagPricing(lines, {
        pincode: effectivePincode || undefined,
        couponCode: couponCode || undefined,
      });
    },
    enabled: completeItems.length > 0 && effectivePincode.length === 6,
  });

  const availabilityQuery = useQuery({
    queryKey: ["bag-availability", bagKey],
    queryFn: async () => {
      const lines = await loadBagLines(completeItems);
      return validateBagLinesAvailability(lines);
    },
    enabled: completeItems.length > 0,
    refetchOnWindowFocus: true,
  });

  function handleSelect(address: Address) {
    router.push(`${ROUTES.checkout.address}?${withAddress(address.id, address.pincode)}`);
  }

  function guestPaymentQuery() {
    const qs = new URLSearchParams();
    if (couponCode) qs.set("couponCode", couponCode);
    if (effectivePincode) qs.set("pincode", effectivePincode);
    return qs.toString();
  }

  const paymentQs = isAuthenticated
    ? selectedAddress
      ? fullQuery({ addressId: selectedAddress.id, pincode: selectedAddress.pincode })
      : fullQuery()
    : guestPaymentQuery();

  const sidebarQuery = isAuthenticated
    ? selectedAddress
      ? fullQuery({ addressId: selectedAddress.id, pincode: selectedAddress.pincode })
      : fullQuery()
    : guestPaymentQuery();

  const canContinue = isAuthenticated
    ? completeItems.length > 0 &&
      !!selectedAddress &&
      selectedAddress.serviceable !== false &&
      pricing.data?.serviceable !== false &&
      !availabilityQuery.isLoading &&
      !hasBlockingAvailabilityIssues(availabilityQuery.data ?? [])
    : completeItems.length > 0 &&
      effectivePincode.length === 6 &&
      guestServiceable === true &&
      pricing.data?.serviceable !== false &&
      !availabilityQuery.isLoading &&
      !hasBlockingAvailabilityIssues(availabilityQuery.data ?? []);

  if (!completeItems.length) {
    return (
      <CheckoutLayoutShell step="address" queryString="">
        <p className="text-muted-foreground">Your bag is empty. Add a rental before choosing an address.</p>
        <Button asChild variant="outline" className="mt-4">
          <Link href={ROUTES.checkout.bag}>Back to bag</Link>
        </Button>
      </CheckoutLayoutShell>
    );
  }

  return (
    <CheckoutLayoutShell step="address" queryString={sidebarQuery}>
      <CheckoutTwoColumn
        main={
          isAuthenticated ? (
            <AddressSelector
              addresses={addresses}
              selectedId={selectedAddress?.id ?? ""}
              onSelect={handleSelect}
              userName={user?.displayName}
              defaultPhone={user?.phone}
              onAddressesChanged={refreshUser}
            />
          ) : (
            <GuestPincodeField
              pincode={guestPincode}
              onPincodeChange={(value, serviceable) => {
                setGuestPincode(value);
                setGuestServiceable(serviceable);
                if (value.length === 6) {
                  const qs = new URLSearchParams();
                  if (couponCode) qs.set("couponCode", couponCode);
                  qs.set("pincode", value);
                  router.replace(`${ROUTES.checkout.address}?${qs.toString()}`);
                }
              }}
            />
          )
        }
        sidebar={
          <>
            <p className="text-sm text-muted-foreground">
              {completeItems.length} {completeItems.length === 1 ? "item" : "items"} in bag
            </p>
            <PriceDetails pricing={pricing.data} />
            {isAuthenticated && selectedAddress && !selectedAddress.serviceable && (
              <p className="text-sm text-destructive">Selected pincode is not serviceable yet.</p>
            )}
            {!isAuthenticated && guestServiceable === false && (
              <p className="text-sm text-destructive">Enter a serviceable pincode to continue.</p>
            )}
            {hasBlockingAvailabilityIssues(availabilityQuery.data ?? []) && (
              <p className="text-sm text-destructive">
                Some bag items are unavailable.{" "}
                <Link href={ROUTES.checkout.bag} className="underline">
                  Update your bag
                </Link>
              </p>
            )}
            <Button asChild variant="rent" size="lg" className="w-full" disabled={!canContinue}>
              <Link
                href={
                  canContinue
                    ? isAuthenticated
                      ? `${ROUTES.checkout.payment}?${paymentQs}`
                      : `${ROUTES.checkout.payment}?${paymentQs}`
                    : "#"
                }
              >
                {isAuthenticated ? "Continue to payment" : "Review & continue to payment"}
              </Link>
            </Button>
          </>
        }
      />
    </CheckoutLayoutShell>
  );
}
