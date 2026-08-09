import { redirect } from "next/navigation";
import { ROUTES } from "@/shared/constants/routes";

/** Legacy review step → bag (Bag → Address → Payment flow). */
export default async function CheckoutReviewRedirect({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const params = await searchParams;
  const qs = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (typeof value === "string") qs.set(key, value);
  }
  const query = qs.toString();
  redirect(query ? `${ROUTES.checkout.bag}?${query}` : ROUTES.checkout.bag);
}
