# Carousel Management

## Implementation summary

This module adds enterprise-grade hero slide management across backend, admin dashboard, and storefront.

- Backend exposes protected admin endpoints under `/api/v1/admin/carousels` and a public storefront endpoint under `/api/v1/carousels/hero`.
- Admin dashboard now has a dedicated workspace at `/admin/commerce/carousels` with list, create/edit, bulk actions, reorder, and preview.
- Storefront home page now consumes live hero slides from backend instead of hardcoded local slide data.

## Backend endpoints

### Admin

- `GET /api/v1/admin/carousels`
- `POST /api/v1/admin/carousels`
- `GET /api/v1/admin/carousels/{carouselId}`
- `PUT /api/v1/admin/carousels/{carouselId}`
- `DELETE /api/v1/admin/carousels/{carouselId}`
- `POST /api/v1/admin/carousels/{carouselId}/restore`
- `PATCH /api/v1/admin/carousels/{carouselId}/status`
- `PATCH /api/v1/admin/carousels/{carouselId}/publish`
- `PATCH /api/v1/admin/carousels/reorder`
- `POST /api/v1/admin/carousels/{carouselId}/duplicate`
- `GET /api/v1/admin/carousels/{carouselId}/preview`
- `POST /api/v1/admin/carousels/bulk-action`

### Storefront

- `GET /api/v1/carousels/hero`

Query params:

- `storeId`
- `channelId`
- `locale`
- `audienceSegment`
- `previewToken`

## Field model

Supported fields include:

- `id`
- `title`
- `slug`
- `description`
- `imageDesktop`
- `imageMobile`
- `altText`
- `linkType`
- `linkValue`
- `openInNewTab`
- `buttonText`
- `secondaryButtonText`
- `secondaryLinkType`
- `secondaryLinkValue`
- `secondaryOpenInNewTab`
- `position`
- `status`
- `visibility`
- `startAt`
- `endAt`
- `audienceSegment`
- `targetingRulesJson`
- `storeId`
- `channelId`
- `locale`
- `priority`
- `backgroundStyle`
- `themeMetadataJson`
- `published`
- `publishedAt`
- `createdBy`
- `updatedBy`
- `createdAt`
- `updatedAt`
- `deletedAt`
- `deletedBy`
- `pinned`
- `previewToken`
- `versionNumber`
- `analyticsKey`
- `experimentKey`

## Request examples

### Create / update payload

```json
{
  "title": "Summer category launch",
  "slug": "summer-category-launch",
  "description": "Seasonal feature banner for the summer product push.",
  "imageDesktop": "https://cdn.example.com/hero/summer-desktop.jpg",
  "imageMobile": "https://cdn.example.com/hero/summer-mobile.jpg",
  "altText": "Summer promotional banner",
  "linkType": "CATEGORY",
  "linkValue": "summer",
  "openInNewTab": false,
  "buttonText": "Shop summer",
  "secondaryButtonText": "View deals",
  "secondaryLinkType": "INTERNAL",
  "secondaryLinkValue": "/deals",
  "secondaryOpenInNewTab": false,
  "position": 1,
  "status": "ACTIVE",
  "visibility": "PUBLIC",
  "startAt": "2026-03-10T00:00:00Z",
  "endAt": "2026-04-01T00:00:00Z",
  "audienceSegment": null,
  "targetingRulesJson": "{\"device\":\"all\",\"country\":\"US\"}",
  "storeId": null,
  "channelId": "web",
  "locale": "en-US",
  "priority": 90,
  "backgroundStyle": "gradient",
  "themeMetadataJson": "{\"contentPosition\":\"left\",\"textColor\":\"light\"}",
  "published": true,
  "pinned": true,
  "analyticsKey": "hero_summer_2026",
  "experimentKey": "homepage-hero-v2"
}
```

### Reorder payload

```json
{
  "items": [
    { "id": "58a5a7c7-3b4f-42ed-95f5-8395d63dca30", "position": 1 },
    { "id": "e65ab2b9-65a5-4f74-b57a-e2ba67c6d9c4", "position": 2 }
  ]
}
```

### Bulk action payload

```json
{
  "ids": [
    "58a5a7c7-3b4f-42ed-95f5-8395d63dca30",
    "e65ab2b9-65a5-4f74-b57a-e2ba67c6d9c4"
  ],
  "action": "PUBLISH"
}
```

## Response examples

### Admin detail

```json
{
  "success": true,
  "message": "Carousel slide",
  "data": {
    "id": "58a5a7c7-3b4f-42ed-95f5-8395d63dca30",
    "title": "Summer category launch",
    "slug": "summer-category-launch",
    "status": "ACTIVE",
    "published": true,
    "storefrontVisibleNow": true,
    "previewToken": "f149de3c-7808-48d5-bda6-0f0e46c61bd1"
  }
}
```

### Storefront hero response

```json
{
  "success": true,
  "message": "Hero carousel slides",
  "data": [
    {
      "id": "58a5a7c7-3b4f-42ed-95f5-8395d63dca30",
      "title": "Summer category launch",
      "slug": "summer-category-launch",
      "imageDesktop": "https://cdn.example.com/hero/summer-desktop.jpg",
      "imageMobile": "https://cdn.example.com/hero/summer-mobile.jpg",
      "altText": "Summer promotional banner",
      "linkType": "CATEGORY",
      "linkValue": "summer",
      "buttonText": "Shop summer",
      "backgroundStyle": "gradient",
      "themeMetadataJson": "{\"contentPosition\":\"left\",\"textColor\":\"light\"}",
      "analyticsKey": "hero_summer_2026",
      "experimentKey": "homepage-hero-v2"
    }
  ]
}
```

## Business rules

- Only slides that are `published`, not soft deleted, not archived, and inside their valid schedule window are returned by the storefront endpoint.
- `SCHEDULED` slides become storefront-visible automatically once `startAt` is reached.
- Expired slides remain in admin but are excluded from storefront results once `endAt` has passed.
- Ordering is deterministic:
  - `pinned DESC`
  - `priority DESC`
  - `position ASC`
  - `publishedAt DESC`
  - `createdAt DESC`
- Pinned slides are treated as an exclusive featured slot per `storeId` + `channelId` + `locale` scope; overlapping pinned publish windows are rejected.
- Global slides use `null` scope fields and are returned when no store/channel/locale-specific slide is requested.
- Storefront mobile rendering falls back from `imageMobile` to `imageDesktop` if the mobile asset is missing.

## Validation rules

- `title` and `imageDesktop` are required.
- `slug` is auto-generated from title when omitted.
- `endAt` must be later than `startAt`.
- External links must start with `http://` or `https://`.
- Internal/custom links must start with `/`.
- `targetingRulesJson` and `themeMetadataJson` must be valid JSON when supplied.
- `imageDesktop` and `imageMobile` must look like valid image asset references.

## Permissions

- All admin carousel endpoints require authenticated admin access.
- The storefront hero endpoint is public and explicitly whitelisted in backend security.

## Admin flow

- Admin users manage slides from `/admin/commerce/carousels`.
- The page supports:
  - filtering
  - pagination
  - bulk selection and actions
  - drag-and-drop reorder on the first page when sorted by `position ASC`
  - create / edit form
  - publish toggle
  - archive / restore
  - duplicate
  - backend preview diagnostics and preview token display

## Storefront consumption

- `frontend/storefront-noura/app/page.jsx` now calls `getHeroSlides()` instead of relying on local static hero data.
- `HeroCarousel` supports:
  - desktop and mobile imagery
  - alt text
  - internal and external CTA behavior
  - graceful no-carousel fallback

## Migration notes

- Migration file: `V11__enterprise_carousel_management.sql`
- New table: `carousel_slides`
- New indexes support scope, schedule, and storefront ordering queries.

## Assumptions

- Media selection currently uses validated image URLs / asset references rather than a dedicated media-library upload flow.
- `channelId` is modeled as a string because this codebase does not yet expose a dedicated channel entity in the root API stack.
- Targeting beyond `visibility` and `audienceSegment` is carried in `targetingRulesJson` for future extensibility.

## Integration notes

- Admin dashboard and storefront both use the new root-stack backend APIs, not the legacy profile-specific commerce endpoints.
- The storefront hero endpoint and admin DTOs intentionally share naming so future analytics and experimentation hooks can be added without remapping the stored schema.
