import { AccountPlaceholder } from "@/features/account/components/AccountPlaceholder";
import { ROUTES } from "@/shared/constants/routes";

export default function SavedLooksPage() {
  return (
    <AccountPlaceholder
      title="Saved Looks"
      description="Curate complete outfits from pieces you love. Saved looks help you plan rentals for upcoming occasions."
      actionLabel="Explore wishlist"
      actionHref={ROUTES.wishlist}
    />
  );
}
