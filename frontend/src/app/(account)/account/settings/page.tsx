"use client";

import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Container, PageHeader } from "@/shared/components/layout/Container";
import { AccountActions } from "@/features/account/components/AccountActions";
import { Button } from "@/components/ui/button";
import { ROUTES } from "@/shared/constants/routes";
import {
  fetchNotificationPreferences,
  updateNotificationPreferences,
  type NotificationPreferences,
} from "@/features/user/services/notification-settings.service";

function PreferenceRow({
  label,
  description,
  checked,
  disabled,
  onChange,
}: {
  label: string;
  description?: string;
  checked: boolean;
  disabled?: boolean;
  onChange: (value: boolean) => void;
}) {
  return (
    <div className="flex items-start justify-between gap-4 border-b border-border pb-4">
      <div className="space-y-1">
        <span className="block">{label}</span>
        {description ? <p className="text-xs text-muted-foreground">{description}</p> : null}
      </div>
      <input
        type="checkbox"
        checked={checked}
        disabled={disabled}
        aria-label={label}
        onChange={(event) => onChange(event.target.checked)}
        className="mt-1 disabled:cursor-not-allowed disabled:opacity-50"
      />
    </div>
  );
}

export default function SettingsPage() {
  const qc = useQueryClient();
  const { data, isLoading, isError } = useQuery({
    queryKey: ["notification-preferences"],
    queryFn: fetchNotificationPreferences,
  });

  const mutation = useMutation({
    mutationFn: updateNotificationPreferences,
    onSuccess: (prefs) => {
      qc.setQueryData(["notification-preferences"], prefs);
    },
  });

  function patch(partial: Partial<NotificationPreferences>) {
    mutation.mutate(partial);
  }

  return (
    <Container narrow embedded>
      <PageHeader title="Settings" description="Notification and account preferences" />
      <div className="space-y-8 text-sm">
        <section className="space-y-3 rounded-sm border border-border p-4">
          <h2 className="font-medium">Security & profile</h2>
          <p className="text-muted-foreground">
            Change your phone, email, password, username, or avatar.
          </p>
          <Button asChild variant="outline" size="sm">
            <Link href={ROUTES.account.security}>Manage security settings</Link>
          </Button>
        </section>

        <section className="space-y-4">
          <h2 className="font-medium">Notifications</h2>
          {isLoading ? <p className="text-muted-foreground">Loading preferences…</p> : null}
          {isError ? (
            <p className="text-destructive">Could not load notification preferences.</p>
          ) : null}
          {data ? (
            <>
              <PreferenceRow
                label="Email notifications"
                description="Order, delivery, and return updates by email."
                checked={data.emailEnabled}
                onChange={(emailEnabled) => patch({ emailEnabled })}
              />
              <PreferenceRow
                label="SMS notifications"
                description={
                  data.smsAvailable
                    ? "Order and delivery updates by SMS."
                    : "SMS notifications are currently unavailable."
                }
                checked={data.smsEnabled}
                disabled={!data.smsAvailable || mutation.isPending}
                onChange={(smsEnabled) => patch({ smsEnabled })}
              />
              <PreferenceRow
                label="Push notifications"
                description={
                  data.pushAvailable
                    ? "Alerts in the Closiq app when supported."
                    : "Push notifications are not available yet."
                }
                checked={data.pushEnabled}
                disabled={!data.pushAvailable || mutation.isPending}
                onChange={(pushEnabled) => patch({ pushEnabled })}
              />
              <div className="border-b border-border pb-2 pt-2">
                <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                  Categories
                </p>
              </div>
              <PreferenceRow
                label="Order updates"
                description="Booking confirmed and payment-related updates."
                checked={data.orderUpdates}
                disabled={mutation.isPending}
                onChange={(orderUpdates) => patch({ orderUpdates })}
              />
              <PreferenceRow
                label="Return reminders"
                description="Reminder before your rental end date."
                checked={data.returnReminders}
                disabled={mutation.isPending}
                onChange={(returnReminders) => patch({ returnReminders })}
              />
              <PreferenceRow
                label="Promotions"
                description="Offers and style inspiration."
                checked={data.promotions}
                disabled={mutation.isPending}
                onChange={(promotions) => patch({ promotions })}
              />
            </>
          ) : null}
        </section>

        <section className="space-y-4 border-t border-border pt-6">
          <h2 className="font-medium">Account</h2>
          <AccountActions />
        </section>
      </div>
    </Container>
  );
}
