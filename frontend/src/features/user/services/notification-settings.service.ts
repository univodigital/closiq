import { apiFetch } from "@/lib/api-client";

export interface NotificationPreferences {
  emailEnabled: boolean;
  smsEnabled: boolean;
  pushEnabled: boolean;
  orderUpdates: boolean;
  returnReminders: boolean;
  promotions: boolean;
  sellerBookingAlerts: boolean;
  emailAvailable: boolean;
  smsAvailable: boolean;
  pushAvailable: boolean;
}

export type UpdateNotificationPreferencesInput = Partial<
  Pick<
    NotificationPreferences,
    | "emailEnabled"
    | "smsEnabled"
    | "pushEnabled"
    | "orderUpdates"
    | "returnReminders"
    | "promotions"
    | "sellerBookingAlerts"
  >
>;

export async function fetchNotificationPreferences(): Promise<NotificationPreferences> {
  return apiFetch<NotificationPreferences>("/users/me/settings/notifications");
}

export async function updateNotificationPreferences(
  input: UpdateNotificationPreferencesInput,
): Promise<NotificationPreferences> {
  return apiFetch<NotificationPreferences>("/users/me/settings/notifications", {
    method: "PATCH",
    body: JSON.stringify(input),
  });
}
