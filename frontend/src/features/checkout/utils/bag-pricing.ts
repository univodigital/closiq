import type { CheckoutSummary, Product } from "@/shared/types";
import type { BagItem } from "@/features/checkout/bag/bag-store";
import { findVariantBySize } from "@/features/checkout/utils/product-variant";
import { productService } from "@/features/products/services";
import { checkoutService } from "@/features/seller/services";

export type BagLine = {
  item: BagItem;
  product: Product;
  variantId: string;
};

export async function loadBagLines(items: BagItem[]): Promise<BagLine[]> {
  const lines: BagLine[] = [];
  for (const item of items) {
    try {
      const res = await productService.getProduct(item.slug);
      const product = res.data;
      const variant = findVariantBySize(product, item.size);
      if (!variant) continue;
      lines.push({ item, product, variantId: variant.id });
    } catch {
      // Skip missing products
    }
  }
  return lines;
}

export async function calculateBagPricing(
  lines: BagLine[],
  options?: { pincode?: string; couponCode?: string },
): Promise<CheckoutSummary | undefined> {
  if (!lines.length) return undefined;

  const summaries: CheckoutSummary[] = [];
  for (const line of lines) {
    const res = await checkoutService.calculate({
      productId: line.product.id,
      variantId: line.variantId,
      rentalStartDate: line.item.start,
      rentalEndDate: line.item.end,
      pincode: options?.pincode,
      // Apply coupon once on the first line only
      couponCode: summaries.length === 0 ? options?.couponCode : undefined,
    });
    summaries.push(res.data);
  }

  return mergeCheckoutSummaries(summaries);
}

export function mergeCheckoutSummaries(summaries: CheckoutSummary[]): CheckoutSummary {
  const first = summaries[0];
  const rentalLines = summaries.flatMap((s, index) =>
    s.lineItems
      .filter((li) => li.type === "RENTAL")
      .map((li) =>
        summaries.length > 1 ? { ...li, label: `Item ${index + 1}: ${li.label}` } : li,
      ),
  );

  const aggregated = new Map<string, { type: CheckoutSummary["lineItems"][number]["type"]; label: string; amount: number }>();
  for (const summary of summaries) {
    for (const li of summary.lineItems) {
      if (li.type === "RENTAL") continue;
      const existing = aggregated.get(li.type);
      if (existing) {
        existing.amount += li.amount;
      } else {
        aggregated.set(li.type, { type: li.type, label: li.label, amount: li.amount });
      }
    }
  }

  return {
    rentalDays: Math.max(...summaries.map((s) => s.rentalDays)),
    lineItems: [...rentalLines, ...aggregated.values()],
    subtotal: summaries.reduce((sum, s) => sum + s.subtotal, 0),
    discountAmount: summaries.reduce((sum, s) => sum + s.discountAmount, 0),
    totalAmount: summaries.reduce((sum, s) => sum + s.totalAmount, 0),
    depositAmount: summaries.reduce((sum, s) => sum + s.depositAmount, 0),
    payNowAmount: summaries.reduce((sum, s) => sum + s.payNowAmount, 0),
    currency: first.currency,
    serviceable: summaries.every((s) => s.serviceable !== false),
  };
}
