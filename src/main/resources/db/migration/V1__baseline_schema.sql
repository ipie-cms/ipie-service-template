-- Service template schema.
--
-- The structures every service built from this template starts with: a business entity, a versioned
-- artefact table, and the two tables that make asynchronous messaging safe - the transactional
-- outbox an event is written to in the same transaction as the change it describes, and the record
-- of events already consumed that makes redelivery harmless.
--
-- A baseline rather than a history: it declares the schema as it stands, superseding the 9
-- migrations that preceded the repository's first commit. A new service renames these tables to its
-- own domain and adds to them; the standard columns and the two messaging tables stay.

-- pgcrypto is created but never commented on. pg_dump emits a COMMENT ON EXTENSION beside the
-- CREATE, and commenting on an extension requires owning it - which the service's migration role
-- does not, and should not. CREATE EXTENSION IF NOT EXISTS is a no-op when the extension is already
-- installed, so it stays: it documents the dependency and works on a fresh database provisioned by
-- someone who does have the privilege.
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;

CREATE TABLE public.documents (
    id uuid NOT NULL,
    case_id character varying(100) NOT NULL,
    doc_type character varying(100) NOT NULL,
    original_filename character varying(255) NOT NULL,
    storage_key character varying(512),
    content_type character varying(150) NOT NULL,
    size_bytes bigint NOT NULL,
    sha256_hash character varying(64) NOT NULL,
    status character varying(20) NOT NULL,
    version_number integer DEFAULT 1 NOT NULL,
    supersedes_id uuid,
    retention_until timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100) NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by character varying(100) NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by character varying(100),
    CONSTRAINT chk_documents_status CHECK (((status)::text = ANY ((ARRAY['CLEAN'::character varying, 'INFECTED'::character varying])::text[])))
);

CREATE TABLE public.outbox_events (
    event_id character varying(64) NOT NULL,
    payload text NOT NULL,
    occurred_at timestamp with time zone NOT NULL,
    published_at timestamp with time zone
);

CREATE TABLE public.processed_events (
    event_id character varying(128) NOT NULL,
    processed_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE public.users (
    id uuid NOT NULL,
    username character varying(64) NOT NULL,
    email character varying(254) NOT NULL,
    full_name character varying(200) NOT NULL,
    phone_number character varying(20),
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100) NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by character varying(100) NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by character varying(100),
    CONSTRAINT chk_users_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT documents_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.outbox_events
    ADD CONSTRAINT outbox_events_pkey PRIMARY KEY (event_id);

ALTER TABLE ONLY public.processed_events
    ADD CONSTRAINT processed_events_pkey PRIMARY KEY (event_id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uq_users_email UNIQUE (email);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uq_users_username UNIQUE (username);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

CREATE INDEX idx_documents_case_id ON public.documents USING btree (case_id);

CREATE INDEX idx_documents_sha256_hash ON public.documents USING btree (sha256_hash);

CREATE INDEX idx_outbox_events_unpublished ON public.outbox_events USING btree (occurred_at) WHERE (published_at IS NULL);

CREATE INDEX idx_users_created_at_id ON public.users USING btree (created_at, id);

CREATE INDEX idx_users_email_lower ON public.users USING btree (lower((email)::text));

CREATE INDEX idx_users_status ON public.users USING btree (status);

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT documents_supersedes_id_fkey FOREIGN KEY (supersedes_id) REFERENCES public.documents(id);
