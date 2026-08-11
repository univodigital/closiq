"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { toast } from "sonner";
import {
  createAdminCategory,
  fetchAdminCategories,
  updateAdminCategory,
} from "@/features/admin/services/admin-category.service";
import { PageHeader } from "@/shared/components/layout/Container";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { StatusBadge } from "@/components/ui/badge";
import { ApiError } from "@/lib/api-client";

export default function AdminCategoriesPage() {
  const qc = useQueryClient();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");

  const categories = useQuery({
    queryKey: ["admin", "categories"],
    queryFn: fetchAdminCategories,
  });

  const createMutation = useMutation({
    mutationFn: createAdminCategory,
    onSuccess: () => {
      toast.success("Category created");
      setName("");
      setDescription("");
      void qc.invalidateQueries({ queryKey: ["admin", "categories"] });
      void qc.invalidateQueries({ queryKey: ["categories"] });
    },
    onError: (error) => {
      toast.error(error instanceof ApiError ? error.message : "Could not create category");
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: "ACTIVE" | "DEPRECATED" }) =>
      updateAdminCategory(id, { status }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["admin", "categories"] });
      void qc.invalidateQueries({ queryKey: ["categories"] });
    },
    onError: (error) => {
      toast.error(error instanceof ApiError ? error.message : "Could not update category");
    },
  });

  return (
    <div className="space-y-8">
      <PageHeader title="Categories" description="Manage occasion categories available to sellers" />

      <Card>
        <CardContent className="space-y-4 p-6">
          <h2 className="font-medium">Add category</h2>
          <div className="grid gap-3 md:grid-cols-2">
            <input
              className="rounded-sm border border-input px-3 py-2 text-sm"
              placeholder="Category name"
              value={name}
              onChange={(event) => setName(event.target.value)}
            />
            <input
              className="rounded-sm border border-input px-3 py-2 text-sm md:col-span-2"
              placeholder="Description (optional)"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
            />
          </div>
          <Button
            size="sm"
            disabled={!name.trim() || createMutation.isPending}
            onClick={() =>
              createMutation.mutate({
                name: name.trim(),
                description: description.trim() || undefined,
              })
            }
          >
            {createMutation.isPending ? "Creating…" : "Create category"}
          </Button>
        </CardContent>
      </Card>

      <div className="space-y-3">
        {categories.isLoading ? <p className="text-muted-foreground">Loading categories…</p> : null}
        {categories.data?.map((category) => (
          <Card key={category.id}>
            <CardContent className="flex flex-wrap items-center justify-between gap-4 p-4">
              <div>
                <div className="flex items-center gap-2">
                  <p className="font-medium">{category.name}</p>
                  <StatusBadge status={category.status.toLowerCase()} />
                </div>
                <p className="text-xs text-muted-foreground">
                  {category.slug} · {category.productCount} products
                </p>
                {category.description ? (
                  <p className="mt-1 text-sm text-muted-foreground">{category.description}</p>
                ) : null}
              </div>
              <Button
                variant="outline"
                size="sm"
                disabled={updateMutation.isPending}
                onClick={() =>
                  updateMutation.mutate({
                    id: category.id,
                    status: category.status === "ACTIVE" ? "DEPRECATED" : "ACTIVE",
                  })
                }
              >
                {category.status === "ACTIVE" ? "Deactivate" : "Activate"}
              </Button>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
