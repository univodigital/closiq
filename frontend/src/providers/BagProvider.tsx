"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
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
  const [items, setItems] = useState<BagItem[]>([]);
  const [hydrated, setHydrated] = useState(false);

  const refresh = useCallback(() => {
    setItems(readBagItemsFromStorage());
    setHydrated(true);
  }, []);

  useEffect(() => {
    refresh();
    const onStorage = (event: StorageEvent) => {
      if (event.key && event.key !== BAG_STORAGE_KEY) return;
      setItems(readBagItemsFromStorage());
    };
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, [refresh]);

  const addItem = useCallback((item: BagItem) => {
    if (!item.slug) return;
    setItems((prev) => {
      // Always union React state + storage so a pre-hydration add can't wipe prior items.
      const next = mergeBySlug(readBagItemsFromStorage(), prev, [item]);
      writeBagItemsToStorage(next);
      return next;
    });
    setHydrated(true);
  }, []);

  const removeItem = useCallback((slug: string) => {
    setItems((prev) => {
      const fromStorage = readBagItemsFromStorage();
      const merged = mergeBySlug(fromStorage, prev);
      const next = merged.filter((i) => i.slug !== slug);
      writeBagItemsToStorage(next);
      return next;
    });
  }, []);

  const clear = useCallback(() => {
    writeBagItemsToStorage([]);
    setItems([]);
  }, []);

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
