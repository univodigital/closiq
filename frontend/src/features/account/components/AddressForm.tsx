"use client";

import { useState } from "react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import type { Address } from "@/shared/types";
import type { CreateAddressInput } from "@/features/user/services";

const LABELS = ["Home", "Office", "Other"] as const;

type AddressFormValues = CreateAddressInput;

function emptyForm(defaultPhone?: string): AddressFormValues {
  return {
    label: "Home",
    line1: "",
    line2: "",
    city: "Mumbai",
    state: "Maharashtra",
    pincode: "",
    phone: defaultPhone ?? "",
    isDefault: false,
  };
}

function toFormValues(address: Address): AddressFormValues {
  return {
    label: address.label,
    line1: address.line1,
    line2: address.line2 ?? "",
    city: address.city,
    state: address.state,
    pincode: address.pincode,
    phone: address.phone,
    isDefault: address.isDefault,
  };
}

export function AddressForm({
  initial,
  defaultPhone,
  onSubmit,
  onCancel,
  submitLabel = "Save address",
}: {
  initial?: Address;
  defaultPhone?: string;
  onSubmit: (values: AddressFormValues) => Promise<void>;
  onCancel: () => void;
  submitLabel?: string;
}) {
  const [values, setValues] = useState<AddressFormValues>(
    initial ? toFormValues(initial) : emptyForm(defaultPhone),
  );
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSaving(true);
    try {
      await onSubmit({
        ...values,
        line2: values.line2?.trim() || undefined,
        phone: values.phone.trim(),
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to save address");
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4 rounded-sm border border-border p-5">
      <div>
        <label className="label-caps mb-2 block text-muted-foreground">Label</label>
        <select
          value={values.label}
          onChange={(e) => setValues((v) => ({ ...v, label: e.target.value }))}
          className="h-11 w-full rounded-sm border border-border bg-background px-3 text-sm"
        >
          {LABELS.map((label) => (
            <option key={label} value={label}>
              {label}
            </option>
          ))}
        </select>
      </div>
      <div>
        <label className="label-caps mb-2 block text-muted-foreground">Mobile number</label>
        <Input
          required
          type="tel"
          inputMode="tel"
          autoComplete="tel"
          placeholder="Mobile number"
          pattern="\+?[0-9]{10,15}"
          value={values.phone}
          onChange={(e) => setValues((v) => ({ ...v, phone: e.target.value.replace(/[^\d+]/g, "") }))}
        />
        <p className="mt-1 text-xs text-muted-foreground">
          Delivery updates and calls will go to this number only.
        </p>
      </div>
      <div>
        <label className="label-caps mb-2 block text-muted-foreground">Address line 1</label>
        <Input
          required
          value={values.line1}
          onChange={(e) => setValues((v) => ({ ...v, line1: e.target.value }))}
        />
      </div>
      <div>
        <label className="label-caps mb-2 block text-muted-foreground">Address line 2</label>
        <Input
          value={values.line2 ?? ""}
          onChange={(e) => setValues((v) => ({ ...v, line2: e.target.value }))}
        />
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <div>
          <label className="label-caps mb-2 block text-muted-foreground">City</label>
          <Input
            required
            value={values.city}
            onChange={(e) => setValues((v) => ({ ...v, city: e.target.value }))}
          />
        </div>
        <div>
          <label className="label-caps mb-2 block text-muted-foreground">State</label>
          <Input
            required
            value={values.state}
            onChange={(e) => setValues((v) => ({ ...v, state: e.target.value }))}
          />
        </div>
      </div>
      <div>
        <label className="label-caps mb-2 block text-muted-foreground">Pincode</label>
        <Input
          required
          inputMode="numeric"
          pattern="\d{6}"
          maxLength={6}
          value={values.pincode}
          onChange={(e) => setValues((v) => ({ ...v, pincode: e.target.value.replace(/\D/g, "") }))}
        />
      </div>
      <label className="flex items-center gap-2 text-sm">
        <input
          type="checkbox"
          checked={values.isDefault ?? false}
          onChange={(e) => setValues((v) => ({ ...v, isDefault: e.target.checked }))}
        />
        Set as default delivery address
      </label>
      {error && <p className="text-sm text-rose-deep">{error}</p>}
      <div className="flex flex-wrap gap-3 pt-2">
        <Button type="submit" variant="primary" size="sm" disabled={saving}>
          {saving ? "Saving…" : submitLabel}
        </Button>
        <Button type="button" variant="outline" size="sm" onClick={onCancel}>
          Cancel
        </Button>
      </div>
    </form>
  );
}
