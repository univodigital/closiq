"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { adminService } from "@/features/admin/services";
import { useAuth } from "@/providers/AuthProvider";
import { PageHeader } from "@/shared/components/layout/Container";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { StatusBadge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import type { UserRole } from "@/shared/types";

const ROLE_OPTIONS: { value: UserRole; label: string; hint?: string }[] = [
  { value: "CUSTOMER", label: "Customer", hint: "Default buyer account" },
  { value: "SELLER", label: "Seller", hint: "Can list products and manage bookings" },
  { value: "ADMIN", label: "Admin", hint: "Full platform administration access" },
];

function normalizeRoles(roles: UserRole[]): UserRole[] {
  const next = new Set<UserRole>(roles);
  next.add("CUSTOMER");
  return ROLE_OPTIONS.map((option) => option.value).filter((role) => next.has(role));
}

function RoleCheckboxes({
  roles,
  onChange,
  disableAdminRemoval,
}: {
  roles: UserRole[];
  onChange: (roles: UserRole[]) => void;
  disableAdminRemoval?: boolean;
}) {
  const selected = new Set(normalizeRoles(roles));

  const toggleRole = (role: UserRole, checked: boolean) => {
    if (role === "CUSTOMER") {
      return;
    }
    if (role === "ADMIN" && disableAdminRemoval && !checked) {
      return;
    }

    const next = new Set(selected);
    if (checked) {
      next.add(role);
    } else {
      next.delete(role);
    }
    next.add("CUSTOMER");
    onChange(normalizeRoles([...next]));
  };

  return (
    <div className="flex flex-wrap gap-4">
      {ROLE_OPTIONS.map((option) => {
        const isCustomer = option.value === "CUSTOMER";
        const isAdminLocked = option.value === "ADMIN" && disableAdminRemoval;
        const checked = selected.has(option.value);

        return (
          <label key={option.value} className="flex min-w-[140px] items-start gap-2 text-sm">
            <input
              type="checkbox"
              className="mt-0.5"
              checked={checked}
              disabled={isCustomer || isAdminLocked}
              onChange={(event) => toggleRole(option.value, event.target.checked)}
            />
            <span>
              <span className="font-medium">{option.label}</span>
              {option.hint ? <span className="mt-0.5 block text-xs text-muted-foreground">{option.hint}</span> : null}
            </span>
          </label>
        );
      })}
    </div>
  );
}

export default function AdminUsersPage() {
  const queryClient = useQueryClient();
  const { user: currentUser } = useAuth();
  const [showCreate, setShowCreate] = useState(false);
  const [phone, setPhone] = useState("");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [createRoles, setCreateRoles] = useState<UserRole[]>(["CUSTOMER"]);
  const [editingRolesUserId, setEditingRolesUserId] = useState<string | null>(null);
  const [editedRoles, setEditedRoles] = useState<UserRole[]>([]);

  const { data, isLoading } = useQuery({
    queryKey: ["admin", "users"],
    queryFn: () => adminService.listUsers(),
  });

  const createMutation = useMutation({
    mutationFn: () =>
      adminService.createUser({
        phone: phone.startsWith("+91") ? phone : `+91${phone}`,
        firstName,
        lastName,
        email: email || undefined,
        roles: normalizeRoles(createRoles),
      }),
    onSuccess: () => {
      toast.success("User created");
      queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
      setShowCreate(false);
      setPhone("");
      setFirstName("");
      setLastName("");
      setEmail("");
      setCreateRoles(["CUSTOMER"]);
    },
    onError: (err: Error) => toast.error(err.message),
  });

  const updateRolesMutation = useMutation({
    mutationFn: ({ userId, roles }: { userId: string; roles: UserRole[] }) =>
      adminService.updateUser(userId, { roles: normalizeRoles(roles) }),
    onSuccess: () => {
      toast.success("Roles updated");
      setEditingRolesUserId(null);
      queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
    },
    onError: (err: Error) => toast.error(err.message),
  });

  const suspendMutation = useMutation({
    mutationFn: (userId: string) => adminService.updateUser(userId, { status: "SUSPENDED" }),
    onSuccess: () => {
      toast.success("User suspended");
      queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
    },
    onError: (err: Error) => toast.error(err.message),
  });

  const activateMutation = useMutation({
    mutationFn: (userId: string) => adminService.updateUser(userId, { status: "ACTIVE" }),
    onSuccess: () => {
      toast.success("User activated");
      queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
    },
    onError: (err: Error) => toast.error(err.message),
  });

  const deleteMutation = useMutation({
    mutationFn: (userId: string) => adminService.deleteUser(userId),
    onSuccess: () => {
      toast.success("User removed");
      queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
    },
    onError: (err: Error) => toast.error(err.message),
  });

  const startEditingRoles = (userId: string, roles: string[]) => {
    setEditingRolesUserId(userId);
    setEditedRoles(normalizeRoles(roles as UserRole[]));
  };

  return (
    <div>
      <div className="mb-8 flex items-end justify-between">
        <PageHeader title="Users" description="Manage platform accounts and roles" />
        <Button variant="primary" size="sm" onClick={() => setShowCreate((v) => !v)}>
          {showCreate ? "Cancel" : "Add user"}
        </Button>
      </div>

      {showCreate && (
        <Card className="mb-6">
          <CardContent className="grid gap-4 p-5">
            <div className="grid gap-3 sm:grid-cols-2">
              <Input placeholder="Phone (10 digits)" value={phone} onChange={(e) => setPhone(e.target.value)} />
              <Input placeholder="Email (optional)" value={email} onChange={(e) => setEmail(e.target.value)} />
              <Input placeholder="First name" value={firstName} onChange={(e) => setFirstName(e.target.value)} />
              <Input placeholder="Last name" value={lastName} onChange={(e) => setLastName(e.target.value)} />
            </div>
            <div>
              <p className="mb-2 text-sm font-medium">Roles</p>
              <RoleCheckboxes roles={createRoles} onChange={setCreateRoles} />
            </div>
            <div>
              <Button
                variant="primary"
                disabled={createMutation.isPending}
                onClick={() => createMutation.mutate()}
              >
                Create user
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {isLoading ? (
        <p className="text-muted-foreground">Loading…</p>
      ) : (
        <div className="space-y-3">
          {data?.data.map((user) => {
            const isEditingRoles = editingRolesUserId === user.id;
            const isCurrentUser = currentUser?.id === user.id;

            return (
              <div key={user.id} className="rounded-sm border border-border p-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p className="font-medium">{user.displayName}</p>
                    <p className="text-sm text-muted-foreground">
                      {user.phone} · {user.userCode}
                    </p>
                    <p className="text-xs text-muted-foreground">{user.roles.join(", ")}</p>
                  </div>
                  <div className="flex flex-wrap items-center gap-2">
                    <StatusBadge status={user.status.toLowerCase()} />
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() =>
                        isEditingRoles
                          ? setEditingRolesUserId(null)
                          : startEditingRoles(user.id, user.roles)
                      }
                    >
                      {isEditingRoles ? "Cancel" : "Manage roles"}
                    </Button>
                    {user.status === "ACTIVE" ? (
                      <Button
                        size="sm"
                        variant="outline"
                        disabled={suspendMutation.isPending || isCurrentUser}
                        onClick={() => suspendMutation.mutate(user.id)}
                      >
                        Suspend
                      </Button>
                    ) : (
                      <Button
                        size="sm"
                        variant="outline"
                        disabled={activateMutation.isPending}
                        onClick={() => activateMutation.mutate(user.id)}
                      >
                        Activate
                      </Button>
                    )}
                    <Button
                      size="sm"
                      variant="outline"
                      disabled={deleteMutation.isPending || isCurrentUser}
                      onClick={() => deleteMutation.mutate(user.id)}
                    >
                      Remove
                    </Button>
                  </div>
                </div>

                {isEditingRoles ? (
                  <div className="mt-4 space-y-3 border-t border-border pt-4">
                    <RoleCheckboxes
                      roles={editedRoles}
                      onChange={setEditedRoles}
                      disableAdminRemoval={isCurrentUser}
                    />
                    {isCurrentUser ? (
                      <p className="text-xs text-muted-foreground">
                        You cannot remove your own admin role.
                      </p>
                    ) : null}
                    <Button
                      size="sm"
                      variant="primary"
                      disabled={updateRolesMutation.isPending}
                      onClick={() =>
                        updateRolesMutation.mutate({
                          userId: user.id,
                          roles: editedRoles,
                        })
                      }
                    >
                      Save roles
                    </Button>
                  </div>
                ) : null}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
