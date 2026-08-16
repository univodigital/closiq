"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import {
  BAG_STORAGE_KEY,
  getBagHref,
  isCompleteBagItem,
  readBagItemsFromStorage,
  writeBagItemsToStorage,
  type BagItem,
} from "@/features/checkout/bag/bag-store";
import {
  fetchServerCart,
  mergeGuestCart,
  replaceServerCart,
} from "@/features/checkout/services/cart.service";
import { useAuth } from "@/providers/AuthProvider";

interface BagContextValue {
  items: BagItem[];
  count: number;
  href: string;
  /** False until localStorage has been read on the client. */
  hydrated: boolean;
  addItem: (item: BagItem) => void;
  removeItem: (slug: string) => void;
  clear: () => void;
  refresh: () => void;
}

const BagContext = createContext<BagContextValue | null>(null);

/** Merge by slug — later lists win on conflict. */
function mergeBySlug(...lists: BagItem[][]): BagItem[] {
  const map = new Map<string, BagItem>();
  for (const list of lists) {
    for (const item of list) {
      if (item.slug) map.set(item.slug, item);
    }
  }
  return Array.from(map.values());
}

export function BagProvider({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const [items, setItems] = useState<BagItem[]>([]);
  const [hydrated, setHydrated] = useState(false);
  const syncTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const mergedOnLogin = useRef(false);
  const wasAuthenticated = useRef(false);

  const refresh = useCallback(() => {
    setItems(readBagItemsFromStorage());
    setHydrated(true);
  }, []);

  const syncToServer = useCallback(
    async (nextItems: BagItem[]) => {
      if (!isAuthenticated) return;
      try {
        const synced = await replaceServerCart(nextItems.filter(isCompleteBagItem));
        writeBagItemsToStorage(synced);
        setItems(synced);
      } catch {
        // Keep local state if sync fails — server remains authoritative on next load.
      }
    },
    [isAuthenticated],
  );

  const scheduleSync = useCallback(
    (nextItems: BagItem[]) => {
      if (!isAuthenticated) return;
      if (syncTimer.current) clearTimeout(syncTimer.current);
      syncTimer.current = setTimeout(() => {
        void syncToServer(nextItems);
      }, 400);
    },
    [isAuthenticated, syncToServer],
  );

  useEffect(() => {
    refresh();
    const onStorage = (event: StorageEvent) => {
      if (event.key && event.key !== BAG_STORAGE_KEY) return;
      setItems(readBagItemsFromStorage());
    };
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, [refresh]);

  useEffect(() => {
    if (authLoading) return;

    if (!isAuthenticated) {
      if (wasAuthenticated.current) {
        refresh();
      }
      wasAuthenticated.current = false;
      mergedOnLogin.current = false;
      return;
    }

    wasAuthenticated.current = true;

    if (mergedOnLogin.current) return;
    mergedOnLogin.current = true;

    const guestItems = readBagItemsFromStorage().filter(isCompleteBagItem);

    void (async () => {
      try {
        const merged = guestItems.length
          ? await mergeGuestCart(guestItems)
          : await fetchServerCart();
        writeBagItemsToStorage(merged);
        setItems(merged);
        setHydrated(true);
      } catch {
        refresh();
      }
    })();
  }, [authLoading, isAuthenticated, refresh]);

  const addItem = useCallback(
    (item: BagItem) => {
      if (!item.slug) return;
      setItems((prev) => {
        const next = mergeBySlug(readBagItemsFromStorage(), prev, [item]);
        writeBagItemsToStorage(next);
        scheduleSync(next);
        return next;
      });
      setHydrated(true);
    },
    [scheduleSync],
  );

  const removeItem = useCallback(
    (slug: string) => {
      setItems((prev) => {
        const fromStorage = readBagItemsFromStorage();
        const merged = mergeBySlug(fromStorage, prev);
        const next = merged.filter((i) => i.slug !== slug);
        writeBagItemsToStorage(next);
        scheduleSync(next);
        return next;
      });
    },
    [scheduleSync],
  );

  const clear = useCallback(() => {
    writeBagItemsToStorage([]);
    setItems([]);
    if (isAuthenticated) {
      void replaceServerCart([]).catch(() => undefined);
    }
  }, [isAuthenticated]);

  const count = useMemo(() => items.filter(isCompleteBagItem).length, [items]);
  const href = getBagHref();

  const value = useMemo(
    () => ({ items, count, href, hydrated, addItem, removeItem, clear, refresh }),
    [items, count, href, hydrated, addItem, removeItem, clear, refresh],
  );

  return <BagContext.Provider value={value}>{children}</BagContext.Provider>;
}

export function useBag() {
  const ctx = useContext(BagContext);
  if (!ctx) throw new Error("useBag must be used within BagProvider");
  return ctx;
}
