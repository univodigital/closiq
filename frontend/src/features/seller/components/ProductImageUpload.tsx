"use client";

import Image from "next/image";
import { useRef, useState } from "react";
import { Trash2 } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { deleteProductImage } from "../services/seller-product-management.service";
import { uploadProductImage, validateProductImageFile } from "../lib/product-image-upload";
import type { SellerListingImage } from "../types";
import { ApiError } from "@/lib/api-client";

const MAX_IMAGES = 8;

type UploadSlot =
  | { kind: "done"; url: string }
  | { kind: "failed"; file: File; message: string; sortOrder: number };

export function ProductImageUpload({
  productId,
  images,
  productStatus,
  readOnly = false,
  onUpdated,
}: {
  productId: string;
  images: SellerListingImage[];
  productStatus?: string;
  readOnly?: boolean;
  onUpdated: () => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [failedSlots, setFailedSlots] = useState<UploadSlot[]>([]);

  const canAddMore = !readOnly && images.length + failedSlots.length < MAX_IMAGES;
  const isActive = productStatus === "ACTIVE";

  async function uploadFile(file: File, sortOrder: number) {
    await uploadProductImage(productId, file, sortOrder);
  }

  async function handleFiles(selected: FileList | null) {
    if (!selected?.length || uploading || readOnly) return;

    const files = Array.from(selected);
    const remaining = MAX_IMAGES - images.length - failedSlots.length;
    if (files.length > remaining) {
      setFailedSlots((prev) => [
        ...prev,
        {
          kind: "failed",
          file: files[0],
          message: `You can add up to ${MAX_IMAGES} images (${remaining} remaining)`,
          sortOrder: images.length + prev.length,
        },
      ]);
      return;
    }

    setUploading(true);
    let sortOrder = images.length;
    const nextFailed: UploadSlot[] = [];

    for (const file of files) {
      const validationError = validateProductImageFile(file);
      if (validationError) {
        nextFailed.push({ kind: "failed", file, message: validationError, sortOrder });
        sortOrder += 1;
        continue;
      }

      try {
        await uploadFile(file, sortOrder);
        sortOrder += 1;
      } catch (error) {
        nextFailed.push({
          kind: "failed",
          file,
          message: error instanceof Error ? error.message : "Image upload failed",
          sortOrder,
        });
        sortOrder += 1;
      }
    }

    setFailedSlots((prev) => [...prev, ...nextFailed]);
    if (files.length > nextFailed.length) {
      onUpdated();
    }
    setUploading(false);
    if (inputRef.current) {
      inputRef.current.value = "";
    }
  }

  async function retryFailed(slot: Extract<UploadSlot, { kind: "failed" }>) {
    setUploading(true);
    try {
      await uploadFile(slot.file, slot.sortOrder);
      setFailedSlots((prev) => prev.filter((item) => item !== slot));
      onUpdated();
    } catch (error) {
      setFailedSlots((prev) =>
        prev.map((item) =>
          item === slot
            ? {
                ...slot,
                message: error instanceof Error ? error.message : "Image upload failed",
              }
            : item,
        ),
      );
    } finally {
      setUploading(false);
    }
  }

  async function handleDelete(image: SellerListingImage) {
    if (readOnly || !image.id) return;

    if (isActive && images.length <= 1) {
      toast.error("Active listings must keep at least one photo");
      return;
    }

    if (!window.confirm("Remove this photo from the listing?")) {
      return;
    }

    setDeletingId(image.id);
    try {
      await deleteProductImage(productId, image.id);
      toast.success("Photo removed");
      onUpdated();
    } catch (error) {
      toast.error(error instanceof ApiError ? error.message : "Could not remove photo");
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <div className="space-y-4">
      {canAddMore && (
        <input
          ref={inputRef}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          multiple
          className="hidden"
          onChange={(event) => void handleFiles(event.target.files)}
        />
      )}

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="label-caps text-muted-foreground">Photos</p>
          <p className="mt-1 text-xs text-muted-foreground">
            {readOnly
              ? "Archived listings cannot be edited."
              : "Add at least one photo before publishing. JPEG, PNG, or WebP up to 10 MB each."}
          </p>
        </div>
        {canAddMore && (
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={uploading}
            onClick={() => inputRef.current?.click()}
          >
            {uploading ? "Uploading…" : images.length ? "Add photos" : "Upload photos"}
          </Button>
        )}
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        {images.map((image, index) => (
          <div key={image.id || image.url} className="group relative aspect-[3/4] overflow-hidden rounded-sm bg-muted">
            <Image
              src={image.url}
              alt={`Listing photo ${index + 1}`}
              fill
              className="object-cover"
              sizes="(max-width:768px) 100vw, 40vw"
            />
            {index === 0 ? (
              <span className="absolute left-2 top-2 rounded-sm bg-background/90 px-2 py-0.5 text-[10px] uppercase tracking-wider text-muted-foreground">
                Cover
              </span>
            ) : null}
            {!readOnly && image.id ? (
              <button
                type="button"
                aria-label="Remove photo"
                disabled={deletingId === image.id || uploading}
                onClick={() => void handleDelete(image)}
                className="absolute right-2 top-2 flex h-8 w-8 items-center justify-center rounded-sm bg-background/90 text-muted-foreground opacity-0 transition-opacity hover:bg-background hover:text-destructive focus-visible:opacity-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring group-hover:opacity-100"
              >
                <Trash2 className="h-4 w-4" />
              </button>
            ) : null}
          </div>
        ))}

        {failedSlots.map((slot, index) =>
          slot.kind === "failed" ? (
            <div
              key={`failed-${slot.sortOrder}-${index}`}
              className="flex aspect-[3/4] flex-col items-center justify-center rounded-sm border border-dashed border-destructive/40 bg-destructive/5 p-4 text-center"
            >
              <p className="text-sm font-medium">{slot.file.name}</p>
              <p className="mt-2 text-xs text-destructive">{slot.message}</p>
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="mt-4"
                disabled={uploading}
                onClick={() => void retryFailed(slot)}
              >
                Retry
              </Button>
            </div>
          ) : null,
        )}
      </div>

      {images.length === 0 && failedSlots.length === 0 && !readOnly ? (
        <button
          type="button"
          disabled={uploading || !canAddMore}
          onClick={() => inputRef.current?.click()}
          className="flex min-h-48 w-full flex-col items-center justify-center rounded-sm border border-dashed border-input bg-muted/30 px-6 py-10 text-center transition-colors hover:bg-muted/50 disabled:cursor-not-allowed disabled:opacity-60"
        >
          <p className="text-sm font-medium">No photos yet</p>
          <p className="mt-1 text-xs text-muted-foreground">Click to upload your first listing photo</p>
        </button>
      ) : null}
    </div>
  );
}
