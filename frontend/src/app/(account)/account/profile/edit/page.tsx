"use client";

import { useState } from "react";
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
  const [email, setEmail] = useState(user?.email ?? "");
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
        email: email.trim() || undefined,
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
          <Input required value={firstName} onChange={(e) => setFirstName(e.target.value)} />
        </div>
        <div>
          <label className="label-caps mb-2 block text-muted-foreground">Last name</label>
          <Input required value={lastName} onChange={(e) => setLastName(e.target.value)} />
        </div>
        <div>
          <label className="label-caps mb-2 block text-muted-foreground">Email</label>
          <Input value={email} type="email" onChange={(e) => setEmail(e.target.value)} />
        </div>
        <div>
          <label className="label-caps mb-2 block text-muted-foreground">Contact number</label>
          <Input value={user?.phone ?? ""} disabled readOnly />
          <p className="mt-1 text-xs text-muted-foreground">Primary number cannot be changed here.</p>
        </div>
        <div>
          <label className="label-caps mb-2 block text-muted-foreground">Alternate number</label>
          <Input
            value={alternatePhone}
            type="tel"
            inputMode="tel"
            onChange={(e) => setAlternatePhone(e.target.value)}
          />
        </div>
        <div>
          <label className="label-caps mb-2 block text-muted-foreground">Alternate email</label>
          <Input
            value={alternateEmail}
            type="email"
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
