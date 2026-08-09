"use client";

import { useState } from "react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import type {
  SellerBusinessType,
  SubmitSellerApplicationInput,
} from "@/features/seller/services/seller-application.service";

const BUSINESS_TYPES: { value: SellerBusinessType; label: string }[] = [
  { value: "INDIVIDUAL", label: "Individual" },
  { value: "PROPRIETORSHIP", label: "Proprietorship" },
  { value: "PARTNERSHIP", label: "Partnership" },
  { value: "PRIVATE_LIMITED", label: "Private limited" },
];

export function SellerApplicationForm({
  onSubmit,
}: {
  onSubmit: (input: SubmitSellerApplicationInput) => Promise<void>;
}) {
  const [businessName, setBusinessName] = useState("");
  const [businessType, setBusinessType] = useState<SellerBusinessType>("INDIVIDUAL");
  const [city, setCity] = useState("Mumbai");
  const [description, setDescription] = useState("");
  const [gstNumber, setGstNumber] = useState("");
  const [panNumber, setPanNumber] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await onSubmit({
        businessName,
        businessType,
        city,
        description: description || undefined,
        gstNumber: gstNumber || undefined,
        panNumber,
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to submit application");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="label-caps mb-2 block text-muted-foreground">Business name</label>
        <Input
          required
          value={businessName}
          onChange={(e) => setBusinessName(e.target.value)}
          placeholder="House of Meera"
          maxLength={100}
        />
      </div>

      <div>
        <label className="label-caps mb-2 block text-muted-foreground">Business type</label>
        <select
          value={businessType}
          onChange={(e) => setBusinessType(e.target.value as SellerBusinessType)}
          className="flex h-10 w-full rounded-sm border border-input bg-background px-3 py-2 text-sm"
        >
          {BUSINESS_TYPES.map((type) => (
            <option key={type.value} value={type.value}>
              {type.label}
            </option>
          ))}
        </select>
      </div>

      <div>
        <label className="label-caps mb-2 block text-muted-foreground">City</label>
        <Input
          required
          value={city}
          onChange={(e) => setCity(e.target.value)}
          placeholder="Mumbai"
          maxLength={50}
        />
      </div>

      <div>
        <label className="label-caps mb-2 block text-muted-foreground">Description (optional)</label>
        <textarea
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          rows={3}
          maxLength={500}
          placeholder="Tell us about your inventory and rental experience."
          className="flex min-h-[5rem] w-full rounded-sm border border-input bg-background px-3 py-2 text-sm"
        />
      </div>

      <div>
        <label className="label-caps mb-2 block text-muted-foreground">PAN</label>
        <Input
          required
          value={panNumber}
          onChange={(e) => setPanNumber(e.target.value.toUpperCase())}
          placeholder="ABCDE1234F"
          pattern="^[A-Z]{5}[0-9]{4}[A-Z]{1}$"
          title="Enter a valid PAN (e.g. ABCDE1234F)"
          maxLength={10}
        />
      </div>

      <div>
        <label className="label-caps mb-2 block text-muted-foreground">GSTIN (optional)</label>
        <Input
          value={gstNumber}
          onChange={(e) => setGstNumber(e.target.value.toUpperCase())}
          placeholder="22AAAAA0000A1Z5"
          maxLength={15}
        />
      </div>

      {error && <p className="text-sm text-rose-deep">{error}</p>}

      <Button type="submit" variant="gold" disabled={submitting}>
        {submitting ? "Submitting…" : "Submit application"}
      </Button>
    </form>
  );
}
