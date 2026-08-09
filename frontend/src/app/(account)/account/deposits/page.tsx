import { AccountPlaceholder } from "@/features/account/components/AccountPlaceholder";
import { ROUTES } from "@/shared/constants/routes";

export default function SecurityDepositsPage() {
  return (
    <AccountPlaceholder
      title="Security Deposits"
      description="Track refundable deposits held against active and past rentals. Deposit status updates automatically when rentals complete."
      actionLabel="View rental history"
      actionHref={ROUTES.account.rentals.history}
    />
  );
}
