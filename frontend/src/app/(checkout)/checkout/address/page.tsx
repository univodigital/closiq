"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/providers/AuthProvider";
import { useBag } from "@/providers/BagProvider";
import { Button } from "@/components/ui/button";
import { ROUTES } from "@/shared/constants/routes";
import { useCheckoutParams } from "@/features/checkout/hooks/useCheckoutParams";
import { CheckoutLayoutShell, CheckoutTwoColumn } from "@/features/checkout/components/CheckoutLayoutShell";
import { AddressSelector } from "@/features/checkout/components/AddressSelector";
import { PriceDetails } from "@/features/checkout/components/PriceDetails";
import { calculateBagPricing, loadBagLines } from "@/features/checkout/utils/bag-pricing";
import { isCompleteBagItem } from "@/features/checkout/bag/bag-store";
import type { Address } from "@/shared/types";

export default function CheckoutAddressPage() {
  const router = useRouter();
  const { user, refreshUser } = useAuth();
  const { items: bagItems } = useBag();
  const { addressId, pincode, fullQuery, withAddress, couponCode } = useCheckoutParams();

  const completeItems = bagItems.filter(isCompleteBagItem);
  const bagKey = JSON.stringify(completeItems);

  const addresses = user?.addresses ?? [];

  const selectedAddress = useMemo(() => {
    if (!addresses.length) return undefined;
    if (addressId) return addresses.find((a) => a.id === addressId);
    return addresses.find((a) => a.isDefault) ?? addresses[0];
  }, [addresses, addressId]);

  const effectivePincode = pincode || selectedAddress?.pincode || "";

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

  function handleSelect(address: Address) {
    router.push(`${ROUTES.checkout.address}?${withAddress(address.id, address.pincode)}`);
  }

  const paymentQs = selectedAddress
    ? fullQuery({ addressId: selectedAddress.id, pincode: selectedAddress.pincode })
    : fullQuery();

  const sidebarQuery = selectedAddress
    ? fullQuery({ addressId: selectedAddress.id, pincode: selectedAddress.pincode })
    : fullQuery();

  const canContinue =
    completeItems.length > 0 &&
    !!selectedAddress &&
    selectedAddress.serviceable !== false &&
    pricing.data?.serviceable !== false;

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
          <AddressSelector
            addresses={addresses}
            selectedId={selectedAddress?.id ?? ""}
            onSelect={handleSelect}
            userName={user?.displayName}
            defaultPhone={user?.phone}
            onAddressesChanged={refreshUser}
          />
        }
        sidebar={
          <>
            <p className="text-sm text-muted-foreground">
              {completeItems.length} {completeItems.length === 1 ? "item" : "items"} in bag
            </p>
            <PriceDetails pricing={pricing.data} />
            {selectedAddress && !selectedAddress.serviceable && (
              <p className="text-sm text-destructive">Selected pincode is not serviceable yet.</p>
            )}
            <Button asChild variant="primary" size="lg" className="w-full" disabled={!canContinue}>
              <Link href={canContinue ? `${ROUTES.checkout.payment}?${paymentQs}` : "#"}>
                Continue to payment
              </Link>
            </Button>
          </>
        }
      />
    </CheckoutLayoutShell>
  );
}
