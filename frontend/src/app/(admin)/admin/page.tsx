"use client";

import { useQuery } from "@tanstack/react-query";
import { adminService } from "@/features/admin/services";
import { PageHeader } from "@/shared/components/layout/Container";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

export default function AdminDashboardPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["admin", "dashboard"],
    queryFn: () => adminService.getDashboard(),
  });

  const dash = data?.data;

  if (isLoading) return <Skeleton className="h-64 w-full" />;

  const stats = [
    { label: "Total users", value: dash?.totalUsers ?? 0 },
    { label: "Active users", value: dash?.activeUsers ?? 0 },
    { label: "Suspended users", value: dash?.suspendedUsers ?? 0 },
    { label: "Total products", value: dash?.totalProducts ?? 0 },
    { label: "Active products", value: dash?.activeProducts ?? 0 },
    { label: "Total reviews", value: dash?.totalReviews ?? 0 },
    { label: "Published reviews", value: dash?.publishedReviews ?? 0 },
    { label: "Pending seller apps", value: dash?.pendingSellerApplications ?? 0 },
  ];

  return (
    <div>
      <PageHeader title="Admin dashboard" description="Platform overview" />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((stat) => (
          <Card key={stat.label}>
            <CardContent className="p-5">
              <p className="label-caps text-muted-foreground">{stat.label}</p>
              <p className="mt-2 font-heading text-2xl">{stat.value}</p>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
