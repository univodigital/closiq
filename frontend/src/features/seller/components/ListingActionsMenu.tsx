"use client";

import Link from "next/link";
import { useState } from "react";
import * as DropdownMenu from "@radix-ui/react-dropdown-menu";
import { Archive, Eye, MoreHorizontal, Pencil, Rocket, RotateCcw, Undo2 } from "lucide-react";
import { toast } from "sonner";
import {
  archiveProduct,
  restoreProduct,
  unpublishProduct,
} from "@/features/seller/services/seller-product-management.service";
import { sellerService } from "@/features/seller/services";
import { ROUTES } from "@/shared/constants/routes";
import { ApiError } from "@/lib/api-client";
import type { SellerListing } from "../types";

const itemClass =
  "flex cursor-pointer items-center gap-2 rounded-sm px-3 py-2 text-sm text-foreground outline-none hover:bg-muted focus:bg-muted";

export function ListingActionsMenu({
  listing,
  onUpdated,
}: {
  listing: SellerListing;
  onUpdated: () => void;
}) {
  const [busyAction, setBusyAction] = useState<string | null>(null);

  const isDraft = listing.status === "DRAFT";
  const isActive = listing.status === "ACTIVE";
  const isArchived = listing.status === "ARCHIVED";

  async function runAction(action: string, task: () => Promise<void>) {
    setBusyAction(action);
    try {
      await task();
      onUpdated();
    } catch (error) {
      toast.error(error instanceof ApiError ? error.message : "Action failed");
    } finally {
      setBusyAction(null);
    }
  }

  async function handlePublish() {
    await runAction("publish", async () => {
      await sellerService.publishProduct(listing.id);
      toast.success("Listing published");
    });
  }

  async function handleUnpublish() {
    if (
      !window.confirm("Unpublish this listing? It will be hidden from customers until you publish again.")
    ) {
      return;
    }
    await runAction("unpublish", async () => {
      await unpublishProduct(listing.id);
      toast.success("Listing unpublished");
    });
  }

  async function handleArchive() {
    if (
      !window.confirm(
        "Archive this listing? You can restore it later from the Archived tab.",
      )
    ) {
      return;
    }
    await runAction("archive", async () => {
      await archiveProduct(listing.id);
      toast.success("Listing archived");
    });
  }

  async function handleRestore() {
    await runAction("restore", async () => {
      await restoreProduct(listing.id);
      toast.success("Listing restored to draft");
    });
  }

  return (
    <DropdownMenu.Root>
      <DropdownMenu.Trigger asChild>
        <button
          type="button"
          aria-label={`Actions for ${listing.title}`}
          className="flex h-9 w-9 shrink-0 items-center justify-center rounded-sm text-muted-foreground transition-colors hover:bg-muted hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          <MoreHorizontal className="h-4 w-4" />
        </button>
      </DropdownMenu.Trigger>

      <DropdownMenu.Portal>
        <DropdownMenu.Content
          align="end"
          sideOffset={8}
          className="z-50 min-w-[11rem] rounded-sm border border-border bg-card p-1 shadow-md"
        >
          <DropdownMenu.Item asChild>
            <Link href={ROUTES.seller.product(listing.id)} className={itemClass}>
              <Eye className="h-4 w-4 text-muted-foreground" />
              View listing
            </Link>
          </DropdownMenu.Item>

          {!isArchived && (
            <DropdownMenu.Item asChild>
              <Link href={ROUTES.seller.productEdit(listing.id)} className={itemClass}>
                <Pencil className="h-4 w-4 text-muted-foreground" />
                Edit listing
              </Link>
            </DropdownMenu.Item>
          )}

          {isDraft && (
            <DropdownMenu.Item
              className={itemClass}
              disabled={busyAction !== null}
              onSelect={(event) => {
                event.preventDefault();
                void handlePublish();
              }}
            >
              <Rocket className="h-4 w-4 text-muted-foreground" />
              {busyAction === "publish" ? "Publishing…" : "Publish"}
            </DropdownMenu.Item>
          )}

          {isActive && (
            <DropdownMenu.Item
              className={itemClass}
              disabled={busyAction !== null}
              onSelect={(event) => {
                event.preventDefault();
                void handleUnpublish();
              }}
            >
              <Undo2 className="h-4 w-4 text-muted-foreground" />
              {busyAction === "unpublish" ? "Unpublishing…" : "Unpublish"}
            </DropdownMenu.Item>
          )}

          {isArchived && (
            <DropdownMenu.Item
              className={itemClass}
              disabled={busyAction !== null}
              onSelect={(event) => {
                event.preventDefault();
                void handleRestore();
              }}
            >
              <RotateCcw className="h-4 w-4 text-muted-foreground" />
              {busyAction === "restore" ? "Restoring…" : "Restore to draft"}
            </DropdownMenu.Item>
          )}

          {!isArchived && (
            <>
              <DropdownMenu.Separator className="my-1 h-px bg-border" />
              <DropdownMenu.Item
                className={itemClass}
                disabled={busyAction !== null}
                onSelect={(event) => {
                  event.preventDefault();
                  void handleArchive();
                }}
              >
                <Archive className="h-4 w-4 text-muted-foreground" />
                {busyAction === "archive" ? "Archiving…" : "Archive"}
              </DropdownMenu.Item>
            </>
          )}
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  );
}
