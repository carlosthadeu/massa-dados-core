CREATE TABLE IF NOT EXISTS adm.model_template (
    template_id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    created_by TEXT NOT NULL,
    definition JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS adm.model_example (
    example_id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL REFERENCES adm.model_template (template_id) ON DELETE CASCADE,
    source_file TEXT NOT NULL,
    dataset_key TEXT NOT NULL,
    records JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (template_id, dataset_key)
);

CREATE TABLE IF NOT EXISTS adm.model_field_map_version (
    template_id BIGINT NOT NULL REFERENCES adm.model_template (template_id) ON DELETE CASCADE,
    version INTEGER NOT NULL CHECK (version > 0),
    field_maps JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (template_id, version)
);
