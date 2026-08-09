"use client";

import { useState } from "react";
import { Container, PageHeader } from "@/shared/components/layout/Container";
import { useAuth } from "@/providers/AuthProvider";
import { Button } from "@/components/ui/button";
import { AddressForm } from "@/features/account/components/AddressForm";
import {
  createAddress,
  deleteAddress,
  updateAddress,
} from "@/features/user/services";
import type { Address } from "@/shared/types";

export default function AddressesPage() {
  const { user, refreshUser } = useAuth();
  const [mode, setMode] = useState<"list" | "add" | "edit">("list");
  const [editing, setEditing] = useState<Address | null>(null);

  async function handleCreate(values: Parameters<typeof createAddress>[0]) {
    await createAddress(values);
    await refreshUser();
    setMode("list");
  }

  async function handleUpdate(values: Parameters<typeof updateAddress>[1]) {
    if (!editing) return;
    await updateAddress(editing.id, values);
    await refreshUser();
    setEditing(null);
    setMode("list");
  }

  async function handleDelete(address: Address) {
    if (!window.confirm(`Remove ${address.label} address?`)) return;
    await deleteAddress(address.id);
    await refreshUser();
  }

  return (
    <Container narrow embedded>
      <PageHeader
        title="Addresses"
        description="Manage delivery locations for your rentals"
        actions={
          mode === "list" ? (
            <Button variant="primary" size="sm" onClick={() => setMode("add")}>
              Add address
            </Button>
          ) : undefined
        }
      />

      {mode === "add" && (
        <AddressForm
          defaultPhone={user?.phone}
          onSubmit={handleCreate}
          onCancel={() => setMode("list")}
          submitLabel="Add address"
        />
      )}

      {mode === "edit" && editing && (
        <AddressForm
          initial={editing}
          onSubmit={handleUpdate}
          onCancel={() => {
            setEditing(null);
            setMode("list");
          }}
          submitLabel="Save changes"
        />
      )}

      {mode === "list" && (
        <div className="space-y-4">
          {user?.addresses?.length ? (
            user.addresses.map((a) => (
              <div key={a.id} className="rounded-sm border border-border p-4">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="font-medium">
                      {a.label}
                      {a.isDefault && " · Default"}
                    </p>
                    <p className="mt-1 text-sm text-muted-foreground">
                      {a.line1}
                      {a.line2 ? `, ${a.line2}` : ""}, {a.city} {a.pincode}
                    </p>
                    {a.phone && (
                      <p className="mt-1 text-sm text-muted-foreground">Mobile: {a.phone}</p>
                    )}
                    {a.serviceable && <p className="mt-2 text-xs text-success">Serviceable</p>}
                  </div>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => {
                        setEditing(a);
                        setMode("edit");
                      }}
                    >
                      Edit
                    </Button>
                    <Button variant="destructive" size="sm" onClick={() => handleDelete(a)}>
                      Remove
                    </Button>
                  </div>
                </div>
              </div>
            ))
          ) : (
            <p className="text-sm text-muted-foreground">No saved addresses yet.</p>
          )}
        </div>
      )}
    </Container>
  );
}
