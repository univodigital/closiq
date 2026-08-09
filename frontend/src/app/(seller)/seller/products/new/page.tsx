"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { ProductImageUpload } from "@/features/seller/components/ProductImageUpload";
import { sellerService, type CreateSellerProductInput } from "@/features/seller/services";
import { categoryService } from "@/features/products/services";
import { PageHeader } from "@/shared/components/layout/Container";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
import { ROUTES } from "@/shared/constants/routes";
import {
  SHOP_AUDIENCE_LABELS,
  SHOP_AUDIENCE_SLUGS,
  shopGarmentCategories,
  type ShopAudienceSlug,
} from "@/shared/constants/shop-nav";
import { ApiError } from "@/lib/api-client";

const DEFAULT_VARIANT = { size: "M", quantity: 1 };

export default function SellerProductNewPage() {
  const queryClient = useQueryClient();
  const categories = useQuery({
    queryKey: ["categories"],
    queryFn: () => categoryService.listCategories(),
  });

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [designer, setDesigner] = useState("");
  const [pricePerDay, setPricePerDay] = useState("");
  const [deposit, setDeposit] = useState("");
  const [city, setCity] = useState("Mumbai");
  const [audience, setAudience] = useState<ShopAudienceSlug>("women");
  const [garmentType, setGarmentType] = useState("sarees");
  const [variants, setVariants] = useState([DEFAULT_VARIANT]);
  const [submitting, setSubmitting] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [createdProductId, setCreatedProductId] = useState<string | null>(null);
  const [imageUrls, setImageUrls] = useState<string[]>([]);

  const selectedCategory = categories.data?.data.find((c) => c.id === categoryId);
  const garmentOptions = shopGarmentCategories(audience);

  function handleAudienceChange(nextAudience: ShopAudienceSlug) {
    setAudience(nextAudience);
    const options = shopGarmentCategories(nextAudience);
    setGarmentType(options[0]?.slug ?? "");
  }

  function updateVariant(index: number, field: "size" | "quantity", value: string) {
    setVariants((current) =>
      current.map((variant, i) =>
        i === index
          ? {
              ...variant,
              [field]: field === "quantity" ? Math.max(1, Number(value) || 1) : value,
            }
          : variant,
      ),
    );
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();

    if (!selectedCategory) {
      toast.error("Select a category");
      return;
    }

    if (!garmentType) {
      toast.error("Select a garment type");
      return;
    }

    if (description.trim().length < 50) {
      toast.error("Description must be at least 50 characters");
      return;
    }

    const payload: CreateSellerProductInput = {
      title: title.trim(),
      description: description.trim(),
      categoryId: selectedCategory.id,
      occasion: selectedCategory.slug,
      audience,
      garmentType,
      designer: designer.trim() || undefined,
      pricePerDay: Number(pricePerDay),
      deposit: Number(deposit),
      city: city.trim(),
      variants: variants.filter((v) => v.size.trim()),
    };

    setSubmitting(true);
    try {
      const res = await sellerService.createProduct(payload);
      setCreatedProductId(res.data.id);
      setImageUrls(res.data.imageUrl ? [res.data.imageUrl] : []);
      toast.success("Draft created — add photos below");
      await queryClient.invalidateQueries({ queryKey: ["seller", "products"] });
    } catch (error) {
      toast.error(error instanceof ApiError ? error.message : "Could not create listing");
    } finally {
      setSubmitting(false);
    }
  }

  async function refreshImages() {
    if (!createdProductId) return;
    const res = await sellerService.getProduct(createdProductId);
    setImageUrls(res.data.imageUrls);
  }

  async function handlePublish() {
    if (!createdProductId) return;
    if (imageUrls.length < 1) {
      toast.error("Add at least one photo before publishing");
      return;
    }

    setPublishing(true);
    try {
      await sellerService.publishProduct(createdProductId);
      toast.success("Listing published");
      await queryClient.invalidateQueries({ queryKey: ["seller", "products"] });
    } catch (error) {
      toast.error(error instanceof ApiError ? error.message : "Could not publish listing");
    } finally {
      setPublishing(false);
    }
  }

  const draftCreated = createdProductId !== null;

  return (
    <div>
      <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
        <PageHeader
          title="New listing"
          description={
            draftCreated
              ? "Draft saved. Upload photos and publish when ready."
              : "Fill in the details, then add photos and publish."
          }
        />
        <Button asChild variant="outline" size="sm">
          <Link href={ROUTES.seller.products}>Cancel</Link>
        </Button>
      </div>

      <Card>
        <CardContent className="p-6">
          <form onSubmit={handleSubmit} className="space-y-6">
            <fieldset disabled={draftCreated} className="space-y-6 disabled:opacity-70">
            <div>
              <label className="label-caps mb-2 block text-muted-foreground">Title</label>
              <Input
                required
                minLength={5}
                maxLength={100}
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Emerald draped saree"
              />
            </div>

            <div>
              <label className="label-caps mb-2 block text-muted-foreground">Description</label>
              <textarea
                required
                minLength={50}
                maxLength={2000}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Describe fabric, fit, styling notes, and care instructions (min. 50 characters)."
                className="min-h-32 w-full rounded-sm border border-input bg-background px-3 py-2 text-sm"
              />
              <p className="mt-1 text-xs text-muted-foreground">{description.length}/2000</p>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="label-caps mb-2 block text-muted-foreground">Occasion</label>
                <select
                  required
                  value={categoryId}
                  onChange={(e) => setCategoryId(e.target.value)}
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
                  onChange={(e) => setDesigner(e.target.value)}
                  placeholder="House of Meera"
                  maxLength={100}
                />
              </div>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="label-caps mb-2 block text-muted-foreground">Audience</label>
                <select
                  required
                  value={audience}
                  onChange={(e) => handleAudienceChange(e.target.value as ShopAudienceSlug)}
                  className="flex h-10 w-full rounded-sm border border-input bg-background px-3 py-2 text-sm"
                >
                  {SHOP_AUDIENCE_SLUGS.map((slug) => (
                    <option key={slug} value={slug}>
                      {SHOP_AUDIENCE_LABELS[slug]}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="label-caps mb-2 block text-muted-foreground">Garment type</label>
                <select
                  required
                  value={garmentType}
                  onChange={(e) => setGarmentType(e.target.value)}
                  className="flex h-10 w-full rounded-sm border border-input bg-background px-3 py-2 text-sm"
                >
                  {garmentOptions.map((option) => (
                    <option key={option.slug} value={option.slug}>
                      {option.label}
                    </option>
                  ))}
                </select>
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
                  onChange={(e) => setPricePerDay(e.target.value)}
                  placeholder="1299"
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
                  onChange={(e) => setDeposit(e.target.value)}
                  placeholder="3000"
                />
              </div>
              <div>
                <label className="label-caps mb-2 block text-muted-foreground">City</label>
                <Input
                  required
                  value={city}
                  onChange={(e) => setCity(e.target.value)}
                  placeholder="Mumbai"
                  maxLength={50}
                />
              </div>
            </div>

            <div>
              <div className="mb-3 flex items-center justify-between">
                <label className="label-caps text-muted-foreground">Sizes & stock</label>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setVariants((current) => [...current, { size: "", quantity: 1 }])}
                >
                  Add size
                </Button>
              </div>
              <div className="space-y-2">
                {variants.map((variant, index) => (
                  <div key={index} className="flex gap-2">
                    <Input
                      required
                      value={variant.size}
                      onChange={(e) => updateVariant(index, "size", e.target.value)}
                      placeholder="M"
                      className="max-w-[120px]"
                    />
                    <Input
                      required
                      type="number"
                      min={1}
                      value={variant.quantity}
                      onChange={(e) => updateVariant(index, "quantity", e.target.value)}
                      placeholder="Qty"
                      className="max-w-[120px]"
                    />
                  </div>
                ))}
              </div>
            </div>
            </fieldset>

            {!draftCreated ? (
              <Button type="submit" variant="primary" disabled={submitting || categories.isLoading}>
                {submitting ? "Creating…" : "Create draft listing"}
              </Button>
            ) : (
              <p className="text-sm text-muted-foreground">Draft saved. Details are locked — add photos below.</p>
            )}
          </form>
        </CardContent>
      </Card>

      {draftCreated && createdProductId && (
        <Card className="mt-6">
          <CardContent className="space-y-6 p-6">
            <ProductImageUpload
              productId={createdProductId}
              imageUrls={imageUrls}
              onUploaded={() => void refreshImages()}
            />
            <div className="flex flex-wrap gap-3">
              <Button
                type="button"
                variant="primary"
                disabled={publishing || imageUrls.length < 1}
                onClick={() => void handlePublish()}
              >
                {publishing ? "Publishing…" : "Publish listing"}
              </Button>
              <Button asChild variant="outline">
                <Link href={ROUTES.seller.product(createdProductId)}>View listing</Link>
              </Button>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
