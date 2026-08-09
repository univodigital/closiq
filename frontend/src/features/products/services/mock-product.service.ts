import productsData from "@/mocks/data/products.json";
import categoriesData from "@/mocks/data/categories.json";
import reviewsData from "@/mocks/data/reviews.json";
import { delay } from "@/mocks/utils/delay";
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

const products = productsData as Product[];
const categories = categoriesData as Category[];
const reviews = reviewsData as Review[];

function wrap<T>(data: T): ApiResponse<T> {
  return {
    success: true,
    data,
    meta: { requestId: crypto.randomUUID(), timestamp: new Date().toISOString() },
  };
}

function filterProducts(list: Product[], params?: ProductListParams): Product[] {
  let result = [...list];
  if (params?.occasion) {
    const occasions = params.occasion.split(",");
    result = result.filter((p) => occasions.includes(p.occasion));
  }
  if (params?.audience) {
    result = result.filter((p) => p.audience === params.audience);
  }
  if (params?.garmentType) {
    result = result.filter((p) => p.garmentType === params.garmentType);
  }
  if (params?.size) {
    const sizes = params.size.split(",");
    result = result.filter((p) => p.variants.some((v) => sizes.includes(v.size) && v.available));
  }
  if (params?.minPrice != null) result = result.filter((p) => p.pricePerDay >= params.minPrice!);
  if (params?.maxPrice != null) result = result.filter((p) => p.pricePerDay <= params.maxPrice!);
  if (params?.trending) result = result.filter((p) => p.trending);
  if (params?.q) {
    const q = params.q.toLowerCase();
    result = result.filter(
      (p) =>
        p.title.toLowerCase().includes(q) ||
        p.designer.toLowerCase().includes(q) ||
        p.description.toLowerCase().includes(q),
    );
  }
  if (params?.sort === "pricePerDay:asc") result.sort((a, b) => a.pricePerDay - b.pricePerDay);
  if (params?.sort === "pricePerDay:desc") result.sort((a, b) => b.pricePerDay - a.pricePerDay);
  if (params?.sort === "createdAt:desc") {
    result.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
  }
  return result;
}

export class MockProductService implements ProductService {
  async listProducts(params?: ProductListParams) {
    await delay(300);
    return wrap(filterProducts(products, params));
  }

  async getProduct(slug: string) {
    await delay(200);
    const product = products.find((p) => p.slug === slug);
    if (!product) throw new Error("Product not found");
    return wrap(product);
  }

  async searchProducts(q: string, params?: ProductListParams) {
    await delay(350);
    return wrap(filterProducts(products, { ...params, q }));
  }

  async getFilters() {
    await delay(200);
    const filters: ProductFilters = {
      occasions: categories.map((c) => ({ slug: c.slug, name: c.name, count: c.productCount })),
      sizes: ["XS", "S", "M", "L", "XL"].map((s) => ({
        value: s,
        count: products.filter((p) => p.variants.some((v) => v.size === s)).length,
      })),
      priceRange: {
        min: Math.min(...products.map((p) => p.pricePerDay)),
        max: Math.max(...products.map((p) => p.pricePerDay)),
      },
      cities: [{ value: "Mumbai", count: products.length }],
    };
    return wrap(filters);
  }

  async getRelatedProducts(slug: string) {
    await delay(250);
    const product = products.find((p) => p.slug === slug);
    if (!product) return wrap([]);
    return wrap(products.filter((p) => p.slug !== slug && p.occasion === product.occasion).slice(0, 4));
  }

  async getProductReviews(slug: string) {
    await delay(200);
    const product = products.find((p) => p.slug === slug);
    if (!product) return wrap([]);
    return wrap(reviews.filter((r) => r.productId === product.id));
  }
}

export class MockCategoryService implements CategoryService {
  async listCategories() {
    await delay(200);
    return wrap(categories.sort((a, b) => a.sortOrder - b.sortOrder));
  }

  async getCategoryProducts(slug: string, params?: ProductListParams) {
    await delay(300);
    const cat = categories.find((c) => c.slug === slug);
    if (!cat) throw new Error("Category not found");
    const filtered =
      slug === "new-in"
        ? products
        : products.filter((p) => p.occasion === slug || p.categoryId === cat.id);
    return wrap(filterProducts(filtered, params));
  }
}

export class MockHomeService implements HomeService {
  async getFeaturedProducts() {
    await delay(300);
    return wrap(products.slice(0, 4));
  }

  async getTrendingProducts() {
    await delay(300);
    return wrap(products.filter((p) => p.trending));
  }
}

export const mockProductService = new MockProductService();
export const mockCategoryService = new MockCategoryService();
export const mockHomeService = new MockHomeService();
