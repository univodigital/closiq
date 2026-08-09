import wishlistData from "@/mocks/data/wishlist.json";
import productsData from "@/mocks/data/products.json";
import { delay } from "@/mocks/utils/delay";
import type { ApiResponse, Product } from "@/shared/types";
import type { WishlistService } from "./wishlist.service";

const products = productsData as Product[];
const wishlistRaw = wishlistData as { items: Array<{ productId: string }> };
let wishlistIds = wishlistRaw.items.map((w) => w.productId);

function wrap<T>(data: T): ApiResponse<T> {
  return {
    success: true,
    data,
    meta: { requestId: crypto.randomUUID(), timestamp: new Date().toISOString() },
  };
}

export class MockWishlistService implements WishlistService {
  async list() {
    await delay(250);
    return wrap(products.filter((p) => wishlistIds.includes(p.id)));
  }

  async add(productId: string) {
    await delay(200);
    if (!wishlistIds.includes(productId)) wishlistIds.push(productId);
    return wrap({ productId });
  }

  async remove(productId: string) {
    await delay(200);
    wishlistIds = wishlistIds.filter((id) => id !== productId);
  }
}

export const mockWishlistService = new MockWishlistService();
