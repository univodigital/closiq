import { AccountPlaceholder } from "@/features/account/components/AccountPlaceholder";
import { ROUTES } from "@/shared/constants/routes";

export default function PaymentMethodsPage() {
  return (
    <AccountPlaceholder
      title="Payment Methods"
      description="Saved cards and UPI IDs for faster checkout will appear here. Payment methods are managed securely at checkout for now."
      actionLabel="Browse collection"
      actionHref={ROUTES.products}
    />
  );
}
