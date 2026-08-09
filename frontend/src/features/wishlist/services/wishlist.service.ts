import type { ApiResponse, Product } from "@/shared/types";

export interface WishlistService {
  list(): Promise<ApiResponse<Product[]>>;
  add(productId: string): Promise<ApiResponse<{ productId: string }>>;
  remove(productId: string): Promise<void>;
}
