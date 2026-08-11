import type { SellerListingDetail, SellerListingImage } from "../types";

export function listingImagesFromProduct(product: Pick<SellerListingDetail, "images" | "imageUrls">): SellerListingImage[] {
  if (product.images.length > 0) {
    return product.images;
  }

  return product.imageUrls.map((url, index) => ({
    id: "",
    url,
    sortOrder: index,
  }));
}
