import { apiFetch, apiFetchEnvelope } from "@/lib/api-client";
import { mapProductSummary } from "@/lib/api-mappers";
import type { ApiResponse, Product } from "@/shared/types";
import type { WishlistService } from "./wishlist.service";

class ApiWishlistService implements WishlistService {
  async list() {
    const res = await apiFetchEnvelope<Array<{ product: unknown }>>("/users/me/wishlist");
    return {
      ...res,
      data: res.data.map((item) =>
        mapProductSummary(item.product as Parameters<typeof mapProductSummary>[0]),
      ),
    } satisfies ApiResponse<Product[]>;
  }

  async add(productId: string) {
    const res = await apiFetchEnvelope<{ productId: string }>("/users/me/wishlist", {
      method: "POST",
      body: JSON.stringify({ productId }),
    });
    return res;
  }

  async remove(productId: string) {
    await apiFetch<void>(`/users/me/wishlist/${encodeURIComponent(productId)}`, {
      method: "DELETE",
    });
  }
}

export const apiWishlistService = new ApiWishlistService();
