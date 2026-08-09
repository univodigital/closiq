package com.closiq.user.service;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class UserPreferencesHelper {

    public Map<String, Object> read(Map<String, Object> preferences) {
        return preferences != null ? new HashMap<>(preferences) : new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public ShoppingPreferences getShopping(Map<String, Object> preferences) {
        Map<String, Object> map = read(preferences);
        return new ShoppingPreferences(
                (String) map.get("size"),
                map.get("occasions") instanceof List<?> list
                        ? list.stream().map(Object::toString).toList()
                        : List.of());
    }

    @SuppressWarnings("unchecked")
    public NotificationPreferences getNotifications(Map<String, Object> preferences) {
        Map<String, Object> map = read(preferences);
        Object raw = map.get("notifications");
        Map<String, Object> notif = raw instanceof Map<?, ?> m
                ? (Map<String, Object>) m
                : Map.of();
        return NotificationPreferences.defaultsWithOverrides(notif);
    }

    @SuppressWarnings("unchecked")
    public AccountSettings getAccount(Map<String, Object> preferences) {
        Map<String, Object> map = read(preferences);
        Object raw = map.get("account");
        Map<String, Object> account = raw instanceof Map<?, ?> m
                ? (Map<String, Object>) m
                : Map.of();
        return AccountSettings.defaultsWithOverrides(account);
    }

    public Map<String, Object> mergeShopping(Map<String, Object> preferences, ShoppingPreferences update) {
        Map<String, Object> map = read(preferences);
        if (update.size() != null) {
            map.put("size", update.size());
        }
        if (update.occasions() != null) {
            map.put("occasions", update.occasions());
        }
        return map;
    }

    public Map<String, Object> mergeNotifications(
            Map<String, Object> preferences, NotificationPreferences update) {

        Map<String, Object> map = read(preferences);
        Map<String, Object> existing = getNotifications(map).toMap();
        existing.putAll(update.toPartialMap());
        map.put("notifications", existing);
        return map;
    }

    public Map<String, Object> mergeAccount(Map<String, Object> preferences, AccountSettings update) {
        Map<String, Object> map = read(preferences);
        Map<String, Object> existing = getAccount(map).toMap();
        existing.putAll(update.toPartialMap());
        map.put("account", existing);
        return map;
    }

    public String getAvatarUrl(Map<String, Object> preferences) {
        Object value = read(preferences).get("avatarUrl");
        return value != null ? value.toString() : null;
    }

    public Map<String, Object> withAvatarUrl(Map<String, Object> preferences, String avatarUrl) {
        Map<String, Object> map = read(preferences);
        if (avatarUrl != null) {
            map.put("avatarUrl", avatarUrl);
        }
        return map;
    }

    public record ShoppingPreferences(String size, List<String> occasions) {
    }

    public record NotificationPreferences(
            boolean emailEnabled,
            boolean smsEnabled,
            boolean pushEnabled,
            boolean orderUpdates,
            boolean promotions,
            boolean sellerBookingAlerts) {

        static NotificationPreferences defaultsWithOverrides(Map<String, Object> overrides) {
            NotificationPreferences defaults = new NotificationPreferences(true, true, false, true, false, true);
            return new NotificationPreferences(
                    boolOr(overrides, "emailEnabled", defaults.emailEnabled()),
                    boolOr(overrides, "smsEnabled", defaults.smsEnabled()),
                    boolOr(overrides, "pushEnabled", defaults.pushEnabled()),
                    boolOr(overrides, "orderUpdates", defaults.orderUpdates()),
                    boolOr(overrides, "promotions", defaults.promotions()),
                    boolOr(overrides, "sellerBookingAlerts", defaults.sellerBookingAlerts()));
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("emailEnabled", emailEnabled);
            map.put("smsEnabled", smsEnabled);
            map.put("pushEnabled", pushEnabled);
            map.put("orderUpdates", orderUpdates);
            map.put("promotions", promotions);
            map.put("sellerBookingAlerts", sellerBookingAlerts);
            return map;
        }

        public Map<String, Object> toPartialMap() {
            return toMap();
        }

        private static boolean boolOr(Map<String, Object> map, String key, boolean defaultValue) {
            Object value = map.get(key);
            return value instanceof Boolean b ? b : defaultValue;
        }
    }

    public record AccountSettings(String language, boolean marketingOptIn) {

        static AccountSettings defaultsWithOverrides(Map<String, Object> overrides) {
            AccountSettings defaults = new AccountSettings("en-IN", false);
            return new AccountSettings(
                    overrides.get("language") != null ? overrides.get("language").toString() : defaults.language(),
                    overrides.get("marketingOptIn") instanceof Boolean b ? b : defaults.marketingOptIn());
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("language", language);
            map.put("marketingOptIn", marketingOptIn);
            return map;
        }

        public Map<String, Object> toPartialMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("language", language);
            map.put("marketingOptIn", marketingOptIn);
            return map;
        }
    }
}
