export const ROUTES = {
  home: "/",
  login: "/login",
  signup: "/signup",
  signupVerify: "/signup/verify",
  products: "/products",
  product: (slug: string) => `/products/${slug}`,
  search: "/search",
  wishlist: "/wishlist",
  checkout: {
    bag: "/checkout/bag",
    address: "/checkout/address",
    review: "/checkout/review",
    payment: "/checkout/payment",
    success: "/checkout/success",
    failed: "/checkout/failed",
  },
  orders: "/orders",
  order: (id: string) => `/orders/${id}`,
  /** @deprecated Use ROUTES.account.overview */
  profile: "/account",
  /** @deprecated Use ROUTES.account.profileEdit */
  profileEdit: "/account/profile/edit",
  /** @deprecated Use ROUTES.account.addresses */
  addresses: "/account/addresses",
  /** @deprecated Use ROUTES.account.settings */
  settings: "/account/settings",
  /** @deprecated Use ROUTES.account.notifications */
  notifications: "/account/notifications",
  account: {
    overview: "/account",
    profile: "/account/profile",
    profileEdit: "/account/profile/edit",
    addresses: "/account/addresses",
    settings: "/account/settings",
    notifications: "/account/notifications",
    paymentMethods: "/account/payment-methods",
    deposits: "/account/deposits",
    savedLooks: "/account/saved-looks",
    becomeSeller: "/account/become-seller",
    rentals: {
      active: "/account/rentals/active",
      upcoming: "/account/rentals/upcoming",
      history: "/account/rentals/history",
      returns: "/account/rentals/returns",
    },
  },
  support: "/support",
  supportFaq: "/support/faq",
  seller: {
    apply: "/seller/apply",
    dashboard: "/seller",
    products: "/seller/products",
    productNew: "/seller/products/new",
    product: (id: string) => `/seller/products/${id}`,
    productEdit: (id: string) => `/seller/products/${id}/edit`,
    bookings: "/seller/bookings",
    booking: (id: string) => `/seller/bookings/${id}`,
    inventory: "/seller/inventory",
    wallet: "/seller/wallet",
    analytics: "/seller/analytics",
    settings: "/seller/settings",
  },
  admin: {
    dashboard: "/admin",
    users: "/admin/users",
    products: "/admin/products",
    reviews: "/admin/reviews",
    sellerApplications: "/admin/seller-applications",
  },
  occasion: (slug: string) => `/occasions/${slug}`,
  shop: {
    all: (audience: string) => `/shop/${audience}`,
    occasion: (audience: string, occasion: string) => `/shop/${audience}/occasion/${occasion}`,
    category: (audience: string, category: string) => `/shop/${audience}/category/${category}`,
    discover: (audience: string, slug: string) => `/shop/${audience}/${slug}`,
  },
} as const;

export const AUDIENCES = [
  { slug: "men", label: "Men" },
  { slug: "women", label: "Women" },
  { slug: "kids", label: "Kids" },
] as const;

/** @deprecated Import from shop-nav.ts */
export const SHOP_OCCASIONS = [
  { slug: "wedding", label: "Wedding" },
  { slug: "festival", label: "Festival" },
  { slug: "office", label: "Office" },
  { slug: "party", label: "Party" },
] as const;

/** @deprecated Use ROUTES.shop.discover(audience, "new-in") or shop-nav SHOP_OCCASIONS */
export const OCCASIONS = [
  ...SHOP_OCCASIONS,
  { slug: "new-in", label: "New in" },
] as const;

export const MUMBAI_SERVICEABLE_PINCODES = [
  "400001", "400013", "400026", "400028", "400050", "400051", "400052", "400076", "400703", "400706",
];

export const ORDER_STATUS_LABELS: Record<string, string> = {
  confirmed: "Confirmed",
  out_for_delivery: "Out for delivery",
  trial_ready: "Trial ready",
  rental_active: "Rental active",
  return_scheduled: "Return scheduled",
  returned: "Returned",
  deposit_refunded: "Deposit refunded",
  cancelled: "Cancelled",
};

export const SESSION_COOKIE = "closiq_session";
