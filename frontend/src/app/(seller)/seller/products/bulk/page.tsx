"use client";

import Link from "next/link";
import { useState } from "react";
import { toast } from "sonner";
import {
  importBulkProducts,
  previewBulkImport,
  type BulkImportPreview,
  type BulkImportResult,
} from "@/features/seller/services/seller-product-management.service";
import { PageHeader } from "@/shared/components/layout/Container";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { ROUTES } from "@/shared/constants/routes";
import { ApiError } from "@/lib/api-client";

const SAMPLE_CSV = `title,description,categorySlug,audience,garmentType,designer,pricePerDay,deposit,city,variants
Sample Sherwani Copy,This is a sample listing description long enough to pass validation for bulk import preview.,wedding,men,sherwanis,Designer One,300000,500000,Mumbai,M:2|L:1`;

export default function SellerBulkUploadPage() {
  const [csvContent, setCsvContent] = useState("");
  const [preview, setPreview] = useState<BulkImportPreview | null>(null);
  const [result, setResult] = useState<BulkImportResult | null>(null);
  const [loading, setLoading] = useState(false);

  async function handlePreview() {
    setLoading(true);
    setResult(null);
    try {
      const data = await previewBulkImport(csvContent);
      setPreview(data);
    } catch (error) {
      toast.error(error instanceof ApiError ? error.message : "Could not validate CSV");
    } finally {
      setLoading(false);
    }
  }

  async function handleImport() {
    setLoading(true);
    try {
      const data = await importBulkProducts(csvContent);
      setResult(data);
      toast.success(`Imported ${data.importedCount} of ${data.totalRows} products`);
    } catch (error) {
      toast.error(error instanceof ApiError ? error.message : "Import failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="space-y-8">
      <PageHeader
        title="Bulk product upload"
        description="Upload multiple draft listings from a CSV file."
      />

      <Card>
        <CardContent className="space-y-4 p-6">
          <div className="flex flex-wrap gap-2">
            <Button asChild variant="outline" size="sm">
              <Link href={ROUTES.seller.products}>Back to listings</Link>
            </Button>
            <Button variant="outline" size="sm" onClick={() => setCsvContent(SAMPLE_CSV)}>
              Load sample CSV
            </Button>
          </div>
          <textarea
            className="min-h-56 w-full rounded-sm border border-input px-3 py-2 font-mono text-xs"
            value={csvContent}
            onChange={(event) => setCsvContent(event.target.value)}
            placeholder="Paste CSV content here"
          />
          <p className="text-xs text-muted-foreground">
            Required columns: title, description, categorySlug, audience, garmentType, designer,
            pricePerDay, deposit, city, variants. Variants format: M:2|L:1
          </p>
          <div className="flex gap-2">
            <Button size="sm" disabled={!csvContent.trim() || loading} onClick={() => void handlePreview()}>
              Validate CSV
            </Button>
            <Button
              size="sm"
              variant="primary"
              disabled={!preview || preview.validRows === 0 || loading}
              onClick={() => void handleImport()}
            >
              Import valid products
            </Button>
          </div>
        </CardContent>
      </Card>

      {preview ? (
        <Card>
          <CardContent className="space-y-4 p-6">
            <h2 className="font-medium">Validation summary</h2>
            <p className="text-sm text-muted-foreground">
              {preview.totalRows} rows · {preview.validRows} valid · {preview.errorRows} errors
            </p>
            <div className="space-y-2">
              {preview.rows.map((row) => (
                <div key={row.rowNumber} className="rounded-sm border border-border p-3 text-sm">
                  <p>
                    Row {row.rowNumber}: {row.title || "(untitled)"}{" "}
                    {row.valid ? "✓" : "✕"}
                  </p>
                  {!row.valid ? (
                    <ul className="mt-1 list-disc pl-5 text-destructive">
                      {row.errors.map((error) => (
                        <li key={error}>{error}</li>
                      ))}
                    </ul>
                  ) : null}
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      ) : null}

      {result ? (
        <Card>
          <CardContent className="space-y-4 p-6">
            <h2 className="font-medium">Import result</h2>
            <p className="text-sm text-muted-foreground">
              Imported {result.importedCount} · Failed {result.failedCount}
            </p>
            <div className="space-y-2">
              {result.results.map((row) => (
                <div key={row.rowNumber} className="rounded-sm border border-border p-3 text-sm">
                  Row {row.rowNumber}: {row.title} {row.success ? "✓" : "✕"}
                  {!row.success && row.error ? (
                    <p className="mt-1 text-destructive">{row.error}</p>
                  ) : null}
                  {row.success && row.productId ? (
                    <Link
                      href={ROUTES.seller.product(row.productId)}
                      className="mt-1 inline-block text-accent hover:underline"
                    >
                      Open listing
                    </Link>
                  ) : null}
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      ) : null}
    </div>
  );
}
