"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { ProductImageUpload } from "@/features/seller/components/ProductImageUpload";
import { listingImagesFromProduct } from "@/features/seller/lib/listing-images";
import { sellerService } from "@/features/seller/services";
import { updateProduct } from "@/features/seller/services/seller-product-management.service";
import { categoryService } from "@/features/products/services";
import { PageHeader } from "@/shared/components/layout/Container";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
import { ROUTES } from "@/shared/constants/routes";
import { ApiError } from "@/lib/api-client";

export default function SellerProductEditPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const queryClient = useQueryClient();
  const productId = params.id;

  const categories = useQuery({
    queryKey: ["categories"],
    queryFn: () => categoryService.listCategories(),
  });

  const { data, isLoading, error } = useQuery({
    queryKey: ["seller", "products", productId],
    queryFn: () => sellerService.getProduct(productId),
    enabled: !!productId,
  });

  const product = data?.data;
  const isArchived = product?.status === "ARCHIVED";

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [designer, setDesigner] = useState("");
  const [pricePerDay, setPricePerDay] = useState("");
  const [deposit, setDeposit] = useState("");
  const [city, setCity] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [initialized, setInitialized] = useState(false);

  useEffect(() => {
    if (!product || initialized) return;
    setTitle(product.title);
    setDescription(product.description);
    setCategoryId(product.categoryId ?? "");
    setDesigner("");
    setPricePerDay(String(product.pricePerDay));
    setDeposit(String(product.deposit));
    setCity(product.city);
    setInitialized(true);
  }, [product, initialized]);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!product) return;

    if (description.trim().length < 50) {
      toast.error("Description must be at least 50 characters");
      return;
    }

    setSubmitting(true);
    try {
      await updateProduct(product.id, {
        title: title.trim(),
        description: description.trim(),
        categoryId: categoryId || undefined,
        designer: designer.trim() || undefined,
        pricePerDay: Number(pricePerDay),
        deposit: Number(deposit),
        city: city.trim(),
      });
      toast.success("Listing updated");
      await queryClient.invalidateQueries({ queryKey: ["seller", "products", productId] });
      await queryClient.invalidateQueries({ queryKey: ["seller", "products"] });
      router.push(ROUTES.seller.product(product.id));
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Could not update listing");
    } finally {
      setSubmitting(false);
    }
  }

  if (isLoading) {
    return <p className="text-muted-foreground">Loading listing…</p>;
  }

  if (error || !product) {
    return (
      <div>
        <PageHeader title="Listing not found" />
        <Button asChild variant="outline" size="sm">
          <Link href={ROUTES.seller.products}>Back to listings</Link>
        </Button>
      </div>
    );
  }

  if (isArchived) {
    return (
      <div>
        <PageHeader
          title="Archived listing"
          description="Restore this listing to draft to edit and publish again."
        />
        <div className="flex flex-wrap gap-2">
          <Button variant="primary" size="sm" onClick={() => router.push(ROUTES.seller.product(product.id))}>
            View listing
          </Button>
          <Button asChild variant="outline" size="sm">
            <Link href={ROUTES.seller.products}>Back to listings</Link>
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div>
      <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
        <PageHeader title="Edit listing" description={product.productCode} />
        <Button asChild variant="outline" size="sm">
          <Link href={ROUTES.seller.product(product.id)}>Cancel</Link>
        </Button>
      </div>

      <div className="space-y-6">
        <Card>
          <CardContent className="p-6">
            <ProductImageUpload
              productId={product.id}
              images={listingImagesFromProduct(product)}
              productStatus={product.status}
              onUpdated={() => {
                void queryClient.invalidateQueries({ queryKey: ["seller", "products", productId] });
                void queryClient.invalidateQueries({ queryKey: ["seller", "products"] });
              }}
            />
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label className="label-caps mb-2 block text-muted-foreground">Title</label>
              <Input
                required
                minLength={5}
                maxLength={100}
                value={title}
                onChange={(event) => setTitle(event.target.value)}
              />
            </div>

            <div>
              <label className="label-caps mb-2 block text-muted-foreground">Description</label>
              <textarea
                required
                minLength={50}
                maxLength={2000}
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                className="min-h-32 w-full rounded-sm border border-input bg-background px-3 py-2 text-sm"
              />
              <p className="mt-1 text-xs text-muted-foreground">{description.length}/2000</p>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="label-caps mb-2 block text-muted-foreground">Occasion</label>
                <select
                  value={categoryId}
                  onChange={(event) => setCategoryId(event.target.value)}
                  className="flex h-10 w-full rounded-sm border border-input bg-background px-3 py-2 text-sm"
                >
                  <option value="">Select occasion</option>
                  {categories.data?.data.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="label-caps mb-2 block text-muted-foreground">Designer / brand (optional)</label>
                <Input
                  value={designer}
                  onChange={(event) => setDesigner(event.target.value)}
                  placeholder="House of Meera"
                  maxLength={100}
                />
              </div>
            </div>

            <div className="grid gap-4 sm:grid-cols-3">
              <div>
                <label className="label-caps mb-2 block text-muted-foreground">Price per day (₹)</label>
                <Input
                  required
                  type="number"
                  min={100}
                  max={50000}
                  value={pricePerDay}
                  onChange={(event) => setPricePerDay(event.target.value)}
                />
              </div>
              <div>
                <label className="label-caps mb-2 block text-muted-foreground">Deposit (₹)</label>
                <Input
                  required
                  type="number"
                  min={100}
                  max={100000}
                  value={deposit}
                  onChange={(event) => setDeposit(event.target.value)}
                />
              </div>
              <div>
                <label className="label-caps mb-2 block text-muted-foreground">City</label>
                <Input
                  required
                  value={city}
                  onChange={(event) => setCity(event.target.value)}
                  maxLength={50}
                />
              </div>
            </div>

            {(product.audience || product.garmentType) && (
              <p className="text-xs text-muted-foreground">
                Audience ({product.audience}) and garment type ({product.garmentType}) cannot be changed after
                creation. Duplicate the listing to use different values.
              </p>
            )}

            <Button type="submit" variant="primary" disabled={submitting || categories.isLoading}>
              {submitting ? "Saving…" : "Save changes"}
            </Button>
          </form>
        </CardContent>
      </Card>
      </div>
    </div>
  );
}
