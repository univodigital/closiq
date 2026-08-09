"use client";

import Image from "next/image";
import { useRef, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { uploadProductImage } from "../lib/product-image-upload";

const MAX_IMAGES = 8;

export function ProductImageUpload({
  productId,
  imageUrls,
  onUploaded,
}: {
  productId: string;
  imageUrls: string[];
  onUploaded: () => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);

  const canAddMore = imageUrls.length < MAX_IMAGES;

  async function handleFiles(selected: FileList | null) {
    if (!selected?.length || uploading) return;

    const files = Array.from(selected);
    const remaining = MAX_IMAGES - imageUrls.length;
    if (files.length > remaining) {
      toast.error(`You can add up to ${MAX_IMAGES} images (${remaining} remaining)`);
      return;
    }

    setUploading(true);
    try {
      let sortOrder = imageUrls.length;
      for (const file of files) {
        await uploadProductImage(productId, file, sortOrder);
        sortOrder += 1;
      }
      toast.success(files.length === 1 ? "Photo added" : `${files.length} photos added`);
      onUploaded();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Could not upload photo");
    } finally {
      setUploading(false);
      if (inputRef.current) {
        inputRef.current.value = "";
      }
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
            Add at least one photo before publishing. JPEG, PNG, or WebP up to 10 MB each.
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
            {uploading ? "Uploading…" : imageUrls.length ? "Add photos" : "Upload photos"}
          </Button>
        )}
      </div>

      {imageUrls.length > 0 ? (
        <div className="grid gap-3 sm:grid-cols-2">
          {imageUrls.map((url) => (
            <div key={url} className="relative aspect-[3/4] overflow-hidden rounded-sm bg-muted">
              <Image
                src={url}
                alt="Listing photo"
                fill
                className="object-cover"
                sizes="(max-width:768px) 100vw, 40vw"
              />
            </div>
          ))}
        </div>
      ) : (
        <button
          type="button"
          disabled={uploading || !canAddMore}
          onClick={() => inputRef.current?.click()}
          className="flex min-h-48 w-full flex-col items-center justify-center rounded-sm border border-dashed border-input bg-muted/30 px-6 py-10 text-center transition-colors hover:bg-muted/50 disabled:cursor-not-allowed disabled:opacity-60"
        >
          <p className="text-sm font-medium">No photos yet</p>
          <p className="mt-1 text-xs text-muted-foreground">Click to upload your first listing photo</p>
        </button>
      )}
    </div>
  );
}
