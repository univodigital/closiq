# Dev DDL scripts

## Primary file

**[`closiq-dev-schema.sql`](closiq-dev-schema.sql)** — run this once on an empty Supabase dev database.

- **~1,260 lines** — all schema through Flyway V33
- **Excludes** demo seeds: V5, V7, V22, V23, V24, V25
- **Includes** required reference rows: roles, Mumbai pincodes, Shadowfax provider

## After running

Set in `application-dev.properties` or AWS env (EC2 / App Runner):

```properties
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=33
```

## Optional demo data (run separately later)

| File | Location |
|------|----------|
| `V5__catalog_seed.sql` | `src/main/resources/db/migration/` |
| `V7__inventory_seed.sql` | same |
| `V25__seller_user_seed.sql` | same (OTP test users) |

Full deployment guide: [`../DEV-DEPLOYMENT.md`](../DEV-DEPLOYMENT.md)
