import { ShopDiscoveryCard } from "@/features/shop/components/ShopDiscoveryCard";
import {
  shopLandingCategories,
  shopLandingDiscover,
  shopLandingOccasions,
} from "@/shared/constants/shop-landing";
import type { ShopAudienceSlug } from "@/shared/constants/shop-nav";

function LandingSection({
  title,
  description,
  children,
}: {
  title: string;
  description?: string;
  children: React.ReactNode;
}) {
  return (
    <section className="border-b border-border py-10 last:border-b-0 md:py-14">
      <div className="mb-6 md:mb-8">
        <p className="label-caps mb-2 text-muted-foreground">{title}</p>
        {description && <p className="max-w-xl text-sm text-muted-foreground">{description}</p>}
      </div>
      {children}
    </section>
  );
}

export function ShopLandingSections({ audience }: { audience: ShopAudienceSlug }) {
  const occasions = shopLandingOccasions(audience);
  const categories = shopLandingCategories(audience);
  const discover = shopLandingDiscover(audience);

  return (
    <div>
      <LandingSection
        title="Shop by occasion"
        description="Looks curated for the moments that matter."
      >
        <div className="grid grid-cols-2 gap-3 md:grid-cols-4 md:gap-4">
          {occasions.map((item) => (
            <ShopDiscoveryCard
              key={item.slug}
              href={item.href}
              label={item.label}
              image={item.image}
            />
          ))}
        </div>
      </LandingSection>

      <LandingSection
        title="Shop by category"
        description="Browse by silhouette and style."
      >
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 md:gap-4">
          {categories.map((item) => (
            <ShopDiscoveryCard
              key={item.slug}
              href={item.href}
              label={item.label}
              image={item.image}
              sizes="(max-width: 768px) 50vw, 20vw"
            />
          ))}
        </div>
      </LandingSection>

      <LandingSection title="Discover" description="Start with what’s moving now.">
        <div className="grid gap-3 md:grid-cols-2 md:gap-4">
          {discover.map((item) => (
            <ShopDiscoveryCard
              key={item.slug}
              href={item.href}
              label={item.label}
              image={item.image}
              caption={item.caption}
              featured
            />
          ))}
        </div>
      </LandingSection>
    </div>
  );
}
