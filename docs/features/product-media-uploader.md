# Product Media Uploader

## Overview
The Product Media Uploader in the admin catalog manages product images through drag/drop, browse upload, clipboard paste, and URL attachment/import.

## Business Rules
- Supported image formats: `jpg`, `jpeg`, `png`, `webp`, `gif` (`svg` currently disabled).
- Max file size: 8MB.
- Duplicate detection is hash-based to prevent re-uploading the same image.
- Clipboard image paste is accepted and queued for upload.

## Architecture
Frontend component:
- `frontend/admin-dashboard/src/features/catalog/ProductMediaUploader.jsx`

Integration points:
- Upload internal asset API
- Import external URL API
- Commerce product media add/update/delete APIs

## Backend
Uploader depends on existing media endpoints for:
- file upload
- URL import to internal storage
- product media association updates
- public static file serving via `/uploads/**` for internal asset previews in the admin UI

Storage decision:
- Binary media content is not stored in PostgreSQL (`BYTEA` is intentionally avoided for this flow).
- PostgreSQL stores only media metadata and product associations.
- Physical files are stored in filesystem/object storage paths managed by backend media storage settings.

## Frontend
User interactions:
- Drag and drop images into dropzone.
- Click dropzone to browse local files.
- Paste image from clipboard (`Ctrl/Cmd + V`).
- Attach external URL directly.
- Import URL to internal storage.
- Reorder, replace, set primary, or remove media.

Clipboard reliability note:
- Some browsers provide pasted images without a filename.
- Uploader now normalizes such files to a generated filename with extension derived from MIME type before validation.

## Configuration
Current uploader constants:
- `MAX_FILE_BYTES = 8MB`
- `MAX_PARALLEL_UPLOADS = 3`
- `ACCEPTED_FORMATS = [jpg, jpeg, png, webp, gif, svg]`
- `ENABLE_SVG = false`

## Usage Example
1. Open product media section in Commerce Catalog.
2. Copy an image (screenshot or image from another app).
3. Focus the page and paste.
4. Image appears in upload queue and then in media grid.

## Edge Cases
- Clipboard image with empty filename is auto-normalized.
- Duplicate hash uploads are blocked with an inline error message.
- Invalid MIME/extension mismatch is rejected.
- Paste events containing non-image data are ignored.
- If `/uploads/**` is not publicly readable, internal media cards show broken images.

## Notes
- URL text copy helpers can be added later if product teams need direct copy actions from media cards.
