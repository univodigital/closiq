"use client";

import { useState } from "react";
import { Plus } from "lucide-react";
import { cn } from "@/lib/utils";
import type { Address } from "@/shared/types";
import { Button } from "@/components/ui/button";
import { AddressForm } from "@/features/account/components/AddressForm";
import {
  createAddress,
  deleteAddress,
  updateAddress,
} from "@/features/user/services";

type AddressFormMode = "closed" | "add" | "edit";

export function AddressSelector({
  addresses,
  selectedId,
  onSelect,
  userName,
  defaultPhone,
  onAddressesChanged,
}: {
  addresses: Address[];
  selectedId: string;
  onSelect: (address: Address) => void;
  userName?: string;
  defaultPhone?: string;
  onAddressesChanged: () => Promise<void>;
}) {
  const [formMode, setFormMode] = useState<AddressFormMode>("closed");
  const [editing, setEditing] = useState<Address | null>(null);

  const openAdd = () => {
    setEditing(null);
    setFormMode("add");
  };

  async function handleCreate(values: Parameters<typeof createAddress>[0]) {
    const created = await createAddress(values);
    await onAddressesChanged();
    setFormMode("closed");
    onSelect(created);
  }

  async function handleUpdate(values: Parameters<typeof updateAddress>[1]) {
    if (!editing) return;
    await updateAddress(editing.id, values);
    await onAddressesChanged();
    setEditing(null);
    setFormMode("closed");
  }

  async function handleDelete(address: Address) {
    if (!window.confirm(`Remove ${address.label} address?`)) return;
    await deleteAddress(address.id);
    await onAddressesChanged();
  }

  if (formMode === "add") {
    return (
      <AddressForm
        defaultPhone={defaultPhone}
        onSubmit={handleCreate}
        onCancel={() => setFormMode("closed")}
        submitLabel="Save address"
      />
    );
  }

  if (formMode === "edit" && editing) {
    return (
      <AddressForm
        initial={editing}
        onSubmit={handleUpdate}
        onCancel={() => {
          setEditing(null);
          setFormMode("closed");
        }}
        submitLabel="Save changes"
      />
    );
  }

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="font-heading text-xl">Select delivery address</h2>
        <Button variant="outline" size="sm" onClick={openAdd}>
          Add new address
        </Button>
      </div>

      <p className="label-caps mt-6 text-muted-foreground">Saved addresses</p>

      <div className="mt-4 space-y-3">
        {addresses.map((address) => {
          const selected = selectedId === address.id;
          return (
            <label
              key={address.id}
              className={cn(
                "block cursor-pointer rounded-sm border p-4 transition-colors",
                selected ? "border-accent bg-muted/20" : "border-border hover:border-muted-foreground/40",
              )}
            >
              <div className="flex gap-3">
                <input
                  type="radio"
                  name="checkout-address"
                  checked={selected}
                  onChange={() => onSelect(address)}
                  className="mt-1"
                />
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-medium">{userName ?? "You"}</span>
                    <span className="label-caps text-muted-foreground">{address.label}</span>
                    {address.isDefault && (
                      <span className="text-xs text-muted-foreground">· Default</span>
                    )}
                  </div>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {address.line1}
                    {address.line2 ? `, ${address.line2}` : ""}
                  </p>
                  <p className="text-sm text-muted-foreground">
                    {address.city}, {address.state} – {address.pincode}
                  </p>
                  {address.phone && (
                    <p className="mt-1 text-sm text-muted-foreground">Mobile: {address.phone}</p>
                  )}
                  {address.serviceable && (
                    <p className="mt-2 text-xs text-success">Serviceable for delivery</p>
                  )}
                  {selected && (
                    <div className="mt-3 flex gap-2">
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={(e) => {
                          e.preventDefault();
                          setEditing(address);
                          setFormMode("edit");
                        }}
                      >
                        Edit
                      </Button>
                      <Button
                        type="button"
                        variant="destructive"
                        size="sm"
                        onClick={(e) => {
                          e.preventDefault();
                          handleDelete(address);
                        }}
                      >
                        Remove
                      </Button>
                    </div>
                  )}
                </div>
              </div>
            </label>
          );
        })}

        <button
          type="button"
          onClick={openAdd}
          className="flex w-full items-center gap-4 rounded-sm border border-dashed border-border p-4 text-left transition-colors hover:border-accent hover:bg-muted/20"
        >
          <span className="flex h-10 w-10 items-center justify-center rounded-full border border-border">
            <Plus className="h-5 w-5 text-muted-foreground" />
          </span>
          <span>
            <span className="block font-medium">Add new address</span>
            <span className="mt-0.5 block text-sm text-muted-foreground">
              Save a new address for faster checkout
            </span>
          </span>
        </button>
      </div>
    </div>
  );
}
