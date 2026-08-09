"use client";

import { Container, PageHeader } from "@/shared/components/layout/Container";
import { AccountActions } from "@/features/account/components/AccountActions";

export default function SettingsPage() {
  return (
    <Container narrow embedded>
      <PageHeader title="Settings" description="Notification and account preferences" />
      <div className="space-y-8 text-sm">
        <section className="space-y-4">
          <h2 className="font-medium">Notifications</h2>
          <div className="flex items-center justify-between border-b border-border pb-4">
            <span>Email notifications</span>
            <input type="checkbox" defaultChecked aria-label="Email notifications" />
          </div>
          <div className="flex items-center justify-between border-b border-border pb-4">
            <span>SMS order updates</span>
            <input type="checkbox" defaultChecked aria-label="SMS order updates" />
          </div>
          <div className="flex items-center justify-between pb-4">
            <span>Promotions</span>
            <input type="checkbox" aria-label="Promotions" />
          </div>
        </section>

        <section className="space-y-4 border-t border-border pt-6">
          <h2 className="font-medium">Account</h2>
          <AccountActions />
        </section>
      </div>
    </Container>
  );
}
