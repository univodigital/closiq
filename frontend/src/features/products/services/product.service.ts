import type {
  ApiResponse,
  Category,
  Product,
  ProductFilters,
  ProductListParams,
  Review,
} from "@/shared/types";

export interface ProductService {
  listProducts(params?: ProductListParams): Promise<ApiResponse<Product[]>>;
  getProduct(slug: string): Promise<ApiResponse<Product>>;
  searchProducts(q: string, params?: ProductListParams): Promise<ApiResponse<Product[]>>;
  getFilters(params?: ProductListParams): Promise<ApiResponse<ProductFilters>>;
  getRelatedProducts(slug: string): Promise<ApiResponse<Product[]>>;
  getProductReviews(slug: string): Promise<ApiResponse<Review[]>>;
}

export interface CategoryService {
  listCategories(): Promise<ApiResponse<Category[]>>;
  getCategoryProducts(slug: string, params?: ProductListParams): Promise<ApiResponse<Product[]>>;
}

export interface HomeService {
  getFeaturedProducts(): Promise<ApiResponse<Product[]>>;
  getTrendingProducts(): Promise<ApiResponse<Product[]>>;
}
