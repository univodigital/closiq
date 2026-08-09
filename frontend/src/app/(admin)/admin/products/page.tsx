"use client";

import Image from "next/image";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { adminService } from "@/features/admin/services";
import { PageHeader } from "@/shared/components/layout/Container";
import { Button } from "@/components/ui/button";
import { StatusBadge } from "@/components/ui/badge";
import { formatCurrency } from "@/lib/format";

export default function AdminProductsPage() {
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ["admin", "products"],
    queryFn: () => adminService.listProducts(),
  });

  const archiveMutation = useMutation({
    mutationFn: (productId: string) => adminService.deleteProduct(productId),
    onSuccess: () => {
      toast.success("Product archived");
      queryClient.invalidateQueries({ queryKey: ["admin", "products"] });
    },
    onError: (err: Error) => toast.error(err.message),
  });

  const activateMutation = useMutation({
    mutationFn: (productId: string) => adminService.updateProduct(productId, { status: "ACTIVE" }),
    onSuccess: () => {
      toast.success("Product activated");
      queryClient.invalidateQueries({ queryKey: ["admin", "products"] });
    },
    onError: (err: Error) => toast.error(err.message),
  });

  return (
    <div>
      <PageHeader title="Products" description="Moderate listings across the platform" />
      {isLoading ? (
        <p className="text-muted-foreground">Loading…</p>
      ) : (
        <div className="space-y-3">
          {data?.data.map((product) => (
            <div
              key={product.id}
              className="flex flex-wrap items-center gap-4 rounded-sm border border-border p-4"
            >
              <div className="relative h-16 w-12 overflow-hidden rounded-sm bg-muted">
                {product.primaryImageUrl ? (
                  <Image src={product.primaryImageUrl} alt="" fill className="object-cover" sizes="48px" />
                ) : null}
              </div>
              <div className="flex-1">
                <p className="font-medium">{product.title}</p>
                <p className="text-sm text-muted-foreground">
                  {formatCurrency(product.pricePerDay)}/day · {product.sellerBusinessName ?? "No seller"}
                </p>
              </div>
              <StatusBadge status={product.status.toLowerCase()} />
              {product.status !== "ACTIVE" ? (
                <Button
                  size="sm"
                  variant="outline"
                  disabled={activateMutation.isPending}
                  onClick={() => activateMutation.mutate(product.id)}
                >
                  Activate
                </Button>
              ) : null}
              <Button
                size="sm"
                variant="outline"
                disabled={archiveMutation.isPending}
                onClick={() => archiveMutation.mutate(product.id)}
              >
                Archive
              </Button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
