CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE search_product_documents (
    product_id UUID PRIMARY KEY,
    product_code VARCHAR(120),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255),
    category_id UUID,
    category_name VARCHAR(255),
    brand_id UUID,
    brand_name VARCHAR(255),
    short_description VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    trending BOOLEAN NOT NULL DEFAULT FALSE,
    popularity_score INTEGER NOT NULL DEFAULT 0,
    average_rating DOUBLE PRECISION NOT NULL DEFAULT 0,
    review_count INTEGER NOT NULL DEFAULT 0,
    source_updated_at TIMESTAMPTZ,
    indexed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    search_document tsvector GENERATED ALWAYS AS (
        to_tsvector(
            'simple',
            COALESCE(name, '') || ' ' ||
            COALESCE(product_code, '') || ' ' ||
            COALESCE(slug, '') || ' ' ||
            COALESCE(category_name, '') || ' ' ||
            COALESCE(brand_name, '') || ' ' ||
            COALESCE(short_description, '')
        )
    ) STORED
);

CREATE INDEX ix_search_product_documents_active_popularity
    ON search_product_documents (active, trending, popularity_score DESC, source_updated_at DESC);

CREATE INDEX ix_search_product_documents_name_trgm
    ON search_product_documents
    USING gin (LOWER(name) gin_trgm_ops);

CREATE INDEX ix_search_product_documents_product_code_trgm
    ON search_product_documents
    USING gin (LOWER(product_code) gin_trgm_ops);

CREATE INDEX ix_search_product_documents_brand_name_trgm
    ON search_product_documents
    USING gin (LOWER(brand_name) gin_trgm_ops);

CREATE INDEX ix_search_product_documents_category_name_trgm
    ON search_product_documents
    USING gin (LOWER(category_name) gin_trgm_ops);

CREATE INDEX ix_search_product_documents_search_document
    ON search_product_documents
    USING gin (search_document);
