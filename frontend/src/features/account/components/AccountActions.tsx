"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { LogOut, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/providers/AuthProvider";
import { deleteAccount } from "@/features/user/services";
import { ROUTES } from "@/shared/constants/routes";

export function AccountActions({ className }: { className?: string }) {
  const router = useRouter();
  const { logout } = useAuth();
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleLogout() {
    await logout();
    router.push(ROUTES.home);
  }

  async function handleDeleteAccount() {
    const confirmed = window.confirm(
      "Delete your account permanently? This cannot be undone and you will lose access to rentals, wishlist, and order history.",
    );
    if (!confirmed) return;

    const typed = window.prompt('Type "DELETE" to confirm account deletion');
    if (typed !== "DELETE") return;

    setDeleting(true);
    setError(null);
    try {
      await deleteAccount();
      await logout();
      router.push(ROUTES.home);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to delete account");
    } finally {
      setDeleting(false);
    }
  }

  return (
    <div className={className}>
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between rounded-sm border border-border p-4">
        <div>
          <p className="font-medium">Log out</p>
          <p className="mt-1 text-sm text-muted-foreground">Sign out on this device.</p>
        </div>
        <Button variant="outline" size="sm" onClick={handleLogout}>
          <LogOut className="mr-2 h-4 w-4" />
          Log out
        </Button>
      </div>

      <div className="mt-4 rounded-sm border border-rose-deep/30 p-4">
        <p className="font-medium text-rose-deep">Delete account</p>
        <p className="mt-1 text-sm text-muted-foreground">
          Permanently remove your account and personal data. Active rentals must be completed first.
        </p>
        {error && <p className="mt-2 text-sm text-rose-deep">{error}</p>}
        <Button
          variant="destructive"
          size="sm"
          className="mt-4"
          disabled={deleting}
          onClick={handleDeleteAccount}
        >
          <Trash2 className="mr-2 h-4 w-4" />
          {deleting ? "Deleting…" : "Delete account"}
        </Button>
      </div>
    </div>
  );
}
