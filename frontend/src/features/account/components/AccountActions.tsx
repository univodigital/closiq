"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { LogOut, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/providers/AuthProvider";
import { deleteAccount } from "@/features/user/services";
import { fetchDeleteAccountPreview } from "@/features/user/services/account-security.service";
import { ROUTES } from "@/shared/constants/routes";

export function AccountActions({ className }: { className?: string }) {
  const router = useRouter();
  const { logout } = useAuth();
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [previewLoading, setPreviewLoading] = useState(true);
  const [activeBookings, setActiveBookings] = useState(0);
  const [canDelete, setCanDelete] = useState(true);
  const [deleteMessage, setDeleteMessage] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetchDeleteAccountPreview()
      .then((preview) => {
        if (cancelled) return;
        setActiveBookings(preview.activeBookings);
        setCanDelete(preview.canDelete);
        setDeleteMessage(preview.message);
      })
      .catch(() => {
        if (!cancelled) {
          setDeleteMessage(
            "Deleting your account will deactivate access. Active bookings may be affected.",
          );
        }
      })
      .finally(() => {
        if (!cancelled) setPreviewLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  async function handleLogout() {
    await logout();
    router.push(ROUTES.home);
  }

  async function handleDeleteAccount() {
    if (!canDelete) return;

    const warning =
      activeBookings > 0
        ? `You have ${activeBookings} active booking(s). Complete or cancel them before deleting your account.`
        : [
            "Deleting your account will remove your access to Closiq.",
            "Any active bookings may be cancelled or otherwise affected — review them before continuing.",
            "Booking and payment history is retained for records.",
            "",
            "This cannot be undone.",
          ].join("\n");

    const confirmed = window.confirm(warning);
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
          {previewLoading
            ? "Checking account status…"
            : deleteMessage ??
              "Deleting your account will deactivate access. Active bookings may be affected."}
        </p>
        {activeBookings > 0 && (
          <p className="mt-2 text-sm font-medium text-rose-deep">
            {activeBookings} active booking{activeBookings === 1 ? "" : "s"} must be resolved first.
          </p>
        )}
        {error && <p className="mt-2 text-sm text-rose-deep">{error}</p>}
        <Button
          variant="destructive"
          size="sm"
          className="mt-4"
          disabled={deleting || previewLoading || !canDelete}
          onClick={handleDeleteAccount}
        >
          <Trash2 className="mr-2 h-4 w-4" />
          {deleting ? "Deleting…" : "Delete account"}
        </Button>
      </div>
    </div>
  );
}
