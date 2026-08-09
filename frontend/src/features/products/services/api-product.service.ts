import { apiFetchEnvelope } from "@/lib/api-client";
import {
  mapCategory,
  mapFilters,
  mapProductDetail,
  mapProductSummary,
  mapReview,
} from "@/lib/api-mappers";
import { toQuery } from "@/lib/api-query";
import type {
  ApiResponse,
  Category,
  Product,
  ProductFilters,
  ProductListParams,
  Review,
} from "@/shared/types";
import type {
  CategoryService,
  HomeService,
  ProductService,
} from "./product.service";

class ApiProductService implements ProductService {
  async listProducts(params?: ProductListParams) {
    const res = await apiFetchEnvelope<unknown[]>(
      `/products${toQuery({
        occasion: params?.occasion,
        audience: params?.audience,
        garmentType: params?.garmentType,
        size: params?.size,
        minPrice: params?.minPrice,
        maxPrice: params?.maxPrice,
        city: params?.city,
        featured: params?.featured,
        trending: params?.trending,
        sort: params?.sort,
        limit: params?.limit,
        pageToken: params?.pageToken,
      })}`,
    );
    return {
      ...res,
      data: res.data.map((item) => mapProductSummary(item as Parameters<typeof mapProductSummary>[0])),
    } satisfies ApiResponse<Product[]>;
  }

  async getProduct(slugOrId: string) {
    const res = await apiFetchEnvelope<unknown>(`/products/${encodeURIComponent(slugOrId)}`);
    return {
      ...res,
      data: mapProductDetail(res.data as Parameters<typeof mapProductDetail>[0]),
    } satisfies ApiResponse<Product>;
  }

  async searchProducts(q: string, params?: ProductListParams) {
    const res = await apiFetchEnvelope<unknown[]>(
      `/products/search${toQuery({
        q,
        occasion: params?.occasion,
        size: params?.size,
        minPrice: params?.minPrice,
        maxPrice: params?.maxPrice,
        sort: params?.sort,
        limit: params?.limit,
        pageToken: params?.pageToken,
      })}`,
    );
    return {
      ...res,
      data: res.data.map((item) => mapProductSummary(item as Parameters<typeof mapProductSummary>[0])),
    } satisfies ApiResponse<Product[]>;
  }

  async getFilters(params?: ProductListParams) {
    const res = await apiFetchEnvelope<unknown>(
      `/products/filters${toQuery({ occasion: params?.occasion, q: params?.q })}`,
    );
    return {
      ...res,
      data: mapFilters(res.data as Parameters<typeof mapFilters>[0]),
    } satisfies ApiResponse<ProductFilters>;
  }

  async getRelatedProducts(slugOrId: string) {
    const res = await apiFetchEnvelope<unknown[]>(
      `/products/${encodeURIComponent(slugOrId)}/related`,
    );
    return {
      ...res,
      data: res.data.map((item) => mapProductSummary(item as Parameters<typeof mapProductSummary>[0])),
    } satisfies ApiResponse<Product[]>;
  }

  async getProductReviews(slugOrId: string) {
    const res = await apiFetchEnvelope<unknown[]>(
      `/products/${encodeURIComponent(slugOrId)}/reviews`,
    );
    const productId = slugOrId;
    return {
      ...res,
      data: res.data.map((item) =>
        mapReview(item as Parameters<typeof mapReview>[0], productId),
      ),
    } satisfies ApiResponse<Review[]>;
  }
}

class ApiCategoryService implements CategoryService {
  async listCategories() {
    const res = await apiFetchEnvelope<unknown[]>("/categories");
    return {
      ...res,
      data: res.data.map((item) => mapCategory(item as Parameters<typeof mapCategory>[0])),
    } satisfies ApiResponse<Category[]>;
  }

  async getCategoryProducts(slug: string, params?: ProductListParams) {
    const res = await apiFetchEnvelope<unknown[]>(
      `/categories/${encodeURIComponent(slug)}/products${toQuery({
        size: params?.size,
        minPrice: params?.minPrice,
        maxPrice: params?.maxPrice,
        sort: params?.sort,
        limit: params?.limit,
        pageToken: params?.pageToken,
      })}`,
    );
    return {
      ...res,
      data: res.data.map((item) => mapProductSummary(item as Parameters<typeof mapProductSummary>[0])),
    } satisfies ApiResponse<Product[]>;
  }
}

class ApiHomeService implements HomeService {
  async getFeaturedProducts() {
    const res = await apiFetchEnvelope<unknown[]>("/home/featured-products");
    return {
      ...res,
      data: res.data.map((item) => mapProductSummary(item as Parameters<typeof mapProductSummary>[0])),
    } satisfies ApiResponse<Product[]>;
  }

  async getTrendingProducts() {
    const res = await apiFetchEnvelope<unknown[]>("/home/trending-products");
    return {
      ...res,
      data: res.data.map((item) => mapProductSummary(item as Parameters<typeof mapProductSummary>[0])),
    } satisfies ApiResponse<Product[]>;
  }
}

export const apiProductService = new ApiProductService();
export const apiCategoryService = new ApiCategoryService();
export const apiHomeService = new ApiHomeService();
