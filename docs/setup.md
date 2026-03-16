# Setup

## Product Generator Environment Variables

These variables configure existing-product enrichment, optional LLM generation, and inventory mirror behavior.

| Variable | Required | Default | Purpose |
| --- | --- | --- | --- |
| `APP_PRODUCT_GENERATOR_PRODUCT_URL_TEMPLATE` | No | `https://store.example.com/products/{id}` | Template for generated QR payload. Must include `{id}` token. |
| `APP_PRODUCT_GENERATOR_LLM_ENABLED` | No | `false` | Enables external LLM description generation. |
| `APP_PRODUCT_GENERATOR_LLM_ENDPOINT` | No | `https://api.openai.com/v1/chat/completions` | OpenAI-compatible chat completions endpoint URL. |
| `APP_PRODUCT_GENERATOR_LLM_API_KEY` | Conditionally | empty | Bearer token for LLM endpoint; required when LLM is enabled. |
| `APP_PRODUCT_GENERATOR_LLM_MODEL` | No | `gpt-4o-mini` | Model name passed in LLM request body. |
| `APP_PRODUCT_GENERATOR_LLM_TIMEOUT_MS` | No | `8000` | Connect/read timeout in milliseconds for LLM calls. |
| `APP_PRODUCT_GENERATOR_MIRROR_MAX_ATTEMPTS` | No | `5` | Max retry attempts per mirror job before `FAILED`. |
| `APP_PRODUCT_GENERATOR_MIRROR_BATCH_SIZE` | No | `25` | Number of due mirror jobs processed per worker run. |
| `APP_PRODUCT_GENERATOR_MIRROR_RETRY_BASE_SECONDS` | No | `60` | Base delay used for exponential backoff retries. |
| `APP_PRODUCT_GENERATOR_MIRROR_WORKER_DELAY_MS` | No | `30000` | Fixed delay between mirror worker runs. |

## Notes
- If LLM is disabled or unavailable, description generation uses template fallback text.
- Mirror sync requires bridge mappings in `product_generator_bridge`.
- Product enrichment migrations are in `V19__product_generator_existing_products.sql`.
