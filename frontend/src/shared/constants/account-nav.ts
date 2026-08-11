import { ROUTES } from "./routes";

export type AccountNavItem = {
  href: string;
  label: string;
  /** Path prefixes that mark this item active (defaults to exact href match). */
  matchPaths?: string[];
  /** When true, only exact pathname matches count as active (no prefix matching). */
  exact?: boolean;
};

export type AccountNavSection = {
  id: string;
  title: string;
  items: AccountNavItem[];
};

function matches(pathname: string, item: AccountNavItem): boolean {
  const paths = item.matchPaths ?? [item.href];
  return paths.some((p) => {
    if (item.exact) {
      return pathname === p;
    }
    return pathname === p || (p !== "/" && pathname.startsWith(`${p}/`));
  });
}

export function isAccountNavActive(pathname: string, item: AccountNavItem): boolean {
  return matches(pathname, item);
}

export function buildAccountNavSections(isSeller: boolean): AccountNavSection[] {
  const sections: AccountNavSection[] = [
    {
      id: "overview",
      title: "Overview",
      items: [{ href: ROUTES.account.overview, label: "Overview", exact: true }],
    },
    {
      id: "rentals",
      title: "Rentals",
      items: [
        { href: ROUTES.account.rentals.active, label: "Active Rentals" },
        { href: ROUTES.account.rentals.upcoming, label: "Upcoming Deliveries" },
        { href: ROUTES.account.rentals.history, label: "Rental History" },
        { href: ROUTES.account.rentals.returns, label: "Returns" },
      ],
    },
    {
      id: "closet",
      title: "My Closet",
      items: [
        { href: ROUTES.wishlist, label: "Wishlist", matchPaths: [ROUTES.wishlist] },
        { href: ROUTES.account.savedLooks, label: "Saved Looks" },
      ],
    },
    {
      id: "account",
      title: "Account",
      items: [
        {
          href: ROUTES.account.profile,
          label: "Profile",
          matchPaths: [ROUTES.account.profile, ROUTES.account.profileEdit],
        },
        { href: ROUTES.account.addresses, label: "Addresses" },
        { href: ROUTES.account.paymentMethods, label: "Payment Methods" },
        { href: ROUTES.account.deposits, label: "Security Deposits" },
        { href: ROUTES.account.notifications, label: "Notifications" },
        {
          href: ROUTES.account.settings,
          label: "Settings",
          matchPaths: [ROUTES.account.settings, ROUTES.account.security],
        },
      ],
    },
  ];

  if (!isSeller) {
    sections.push({
      id: "seller",
      title: "Seller",
      items: [
        {
          href: ROUTES.account.becomeSeller,
          label: "Become a Seller",
          matchPaths: [ROUTES.account.becomeSeller, ROUTES.seller.apply],
        },
      ],
    });
  }

  return sections;
}
