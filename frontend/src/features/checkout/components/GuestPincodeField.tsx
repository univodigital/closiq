"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { checkoutService } from "@/features/seller/services";

export function GuestPincodeField({
  pincode,
  onPincodeChange,
}: {
  pincode: string;
  onPincodeChange: (pincode: string, serviceable: boolean) => void;
}) {
  const [checking, setChecking] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [serviceable, setServiceable] = useState<boolean | null>(null);

  async function checkPincode(value: string) {
    if (value.length !== 6) {
      setMessage(null);
      setServiceable(null);
      return;
    }
    setChecking(true);
    try {
      const res = await checkoutService.checkPincode(value);
      const ok = res.data.serviceable;
      setServiceable(ok);
      setMessage(
        ok
          ? res.data.city
            ? `Delivery available in ${res.data.city}.`
            : "Delivery available for this pincode."
          : "This pincode is not serviceable yet.",
      );
      onPincodeChange(value, ok);
    } catch {
      setServiceable(null);
      setMessage("Could not verify pincode. Try again.");
    } finally {
      setChecking(false);
    }
  }

  return (
    <div>
      <h2 className="font-heading text-xl">Delivery pincode</h2>
      <p className="mt-2 text-sm text-muted-foreground">
        Enter your pincode to see pricing and delivery availability. Sign in at payment to complete your order.
      </p>
      <div className="mt-6 flex max-w-xs gap-2">
        <Input
          inputMode="numeric"
          maxLength={6}
          placeholder="6-digit pincode"
          value={pincode}
          onChange={(e) => {
            const value = e.target.value.replace(/\D/g, "").slice(0, 6);
            onPincodeChange(value, serviceable === true);
            if (value.length === 6) void checkPincode(value);
          }}
        />
        <Button
          type="button"
          variant="outline"
          disabled={pincode.length !== 6 || checking}
          onClick={() => void checkPincode(pincode)}
        >
          {checking ? "…" : "Check"}
        </Button>
      </div>
      {message && (
        <p className={`mt-2 text-sm ${serviceable ? "text-success" : "text-destructive"}`}>{message}</p>
      )}
    </div>
  );
}
