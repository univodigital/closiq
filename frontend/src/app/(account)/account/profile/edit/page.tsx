"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Container, PageHeader } from "@/shared/components/layout/Container";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/providers/AuthProvider";
import { updateProfile } from "@/features/user/services";
import { ROUTES } from "@/shared/constants/routes";

export default function ProfileEditPage() {
  const router = useRouter();
  const { user, refreshUser } = useAuth();
  const [firstName, setFirstName] = useState(user?.firstName ?? "");
  const [lastName, setLastName] = useState(user?.lastName ?? "");
  const [alternatePhone, setAlternatePhone] = useState(user?.alternatePhone ?? "");
  const [alternateEmail, setAlternateEmail] = useState(user?.alternateEmail ?? "");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSaving(true);
    try {
      await updateProfile({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        alternatePhone: alternatePhone.trim(),
        alternateEmail: alternateEmail.trim(),
      });
      await refreshUser();
      router.push(ROUTES.account.profile);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to save profile");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Container narrow embedded>
      <PageHeader title="Edit profile" />
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="label-caps mb-2 block text-muted-foreground">First name</label>
          <Input required value={firstName} placeholder="First name" onChange={(e) => setFirstName(e.target.value)} />
        </div>
        <div>
          <label className="label-caps mb-2 block text-muted-foreground">Last name</label>
          <Input required value={lastName} placeholder="Last name" onChange={(e) => setLastName(e.target.value)} />
        </div>
        <div className="rounded-sm border border-border p-4 text-sm">
          <p className="font-medium">Phone, email, password & username</p>
          <p className="mt-1 text-muted-foreground">
            Update your contact details, avatar, and security settings on the security page.
          </p>
          <Button asChild variant="outline" size="sm" className="mt-3">
            <Link href={ROUTES.account.security}>Open security settings</Link>
          </Button>
        </div>
        <div>
          <label className="label-caps mb-2 block text-muted-foreground">Alternate number</label>
          <Input
            value={alternatePhone}
            type="tel"
            inputMode="tel"
            placeholder="Alternate number"
            onChange={(e) => setAlternatePhone(e.target.value)}
          />
        </div>
        <div>
          <label className="label-caps mb-2 block text-muted-foreground">Alternate email</label>
          <Input
            value={alternateEmail}
            type="email"
            placeholder="Alternate email"
            onChange={(e) => setAlternateEmail(e.target.value)}
          />
        </div>
        {error && <p className="text-sm text-rose-deep">{error}</p>}
        <div className="flex flex-wrap gap-3">
          <Button type="submit" variant="primary" disabled={saving}>
            {saving ? "Saving…" : "Save profile"}
          </Button>
          <Button type="button" variant="outline" onClick={() => router.push(ROUTES.account.profile)}>
            Cancel
          </Button>
        </div>
      </form>
    </Container>
  );
}
