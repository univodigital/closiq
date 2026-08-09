"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { adminService } from "@/features/admin/services";
import { PageHeader } from "@/shared/components/layout/Container";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { StatusBadge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";

export default function AdminUsersPage() {
  const queryClient = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [phone, setPhone] = useState("");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");

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
      }),
    onSuccess: () => {
      toast.success("User created");
      queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
      setShowCreate(false);
      setPhone("");
      setFirstName("");
      setLastName("");
      setEmail("");
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

  return (
    <div>
      <div className="mb-8 flex items-end justify-between">
        <PageHeader title="Users" description="Manage platform accounts" />
        <Button variant="primary" size="sm" onClick={() => setShowCreate((v) => !v)}>
          {showCreate ? "Cancel" : "Add user"}
        </Button>
      </div>

      {showCreate && (
        <Card className="mb-6">
          <CardContent className="grid gap-3 p-5 sm:grid-cols-2">
            <Input placeholder="Phone (10 digits)" value={phone} onChange={(e) => setPhone(e.target.value)} />
            <Input placeholder="Email (optional)" value={email} onChange={(e) => setEmail(e.target.value)} />
            <Input placeholder="First name" value={firstName} onChange={(e) => setFirstName(e.target.value)} />
            <Input placeholder="Last name" value={lastName} onChange={(e) => setLastName(e.target.value)} />
            <Button
              variant="primary"
              disabled={createMutation.isPending}
              onClick={() => createMutation.mutate()}
            >
              Create user
            </Button>
          </CardContent>
        </Card>
      )}

      {isLoading ? (
        <p className="text-muted-foreground">Loading…</p>
      ) : (
        <div className="space-y-3">
          {data?.data.map((user) => (
            <div
              key={user.id}
              className="flex flex-wrap items-center justify-between gap-3 rounded-sm border border-border p-4"
            >
              <div>
                <p className="font-medium">{user.displayName}</p>
                <p className="text-sm text-muted-foreground">
                  {user.phone} · {user.userCode}
                </p>
                <p className="text-xs text-muted-foreground">{user.roles.join(", ")}</p>
              </div>
              <div className="flex items-center gap-2">
                <StatusBadge status={user.status.toLowerCase()} />
                {user.status === "ACTIVE" ? (
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={suspendMutation.isPending}
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
                  disabled={deleteMutation.isPending}
                  onClick={() => deleteMutation.mutate(user.id)}
                >
                  Remove
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
