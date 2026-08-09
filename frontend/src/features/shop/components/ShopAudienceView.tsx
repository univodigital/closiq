import { Container, PageHeader } from "@/shared/components/layout/Container";
import { ShopLandingSections } from "@/features/shop/components/ShopLandingSections";
import { AUDIENCES } from "@/shared/constants/routes";
import type { ShopAudienceSlug } from "@/shared/constants/shop-nav";

const AUDIENCE_INTRO =
  "Explore occasion edits, silhouettes, and what’s new — then dive into the pieces you want to try at home.";

export function ShopAudienceView({ audience }: { audience: ShopAudienceSlug }) {
  const title = AUDIENCES.find((a) => a.slug === audience)?.label ?? audience;

  return (
    <Container className="py-10 md:py-14">
      <div key={audience} className="content-blink">
        <PageHeader title={title} description={AUDIENCE_INTRO} breadcrumb="Shop" />
        <ShopLandingSections audience={audience} />
      </div>
    </Container>
  );
}
