
CREATE TABLE "tbl_rjournal"
(
  id bigserial,
  refno text,
  details jsonb DEFAULT '[]'::jsonb,
  sendout text,
  created timestamp with time zone default now(),
  CONSTRAINT tbl_rjournal_pkey PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE "tbl_rjournal"
  OWNER TO postgres;

CREATE TABLE "tbl_rjournal_users"
(
  id bigserial,
  refno text,
  details jsonb DEFAULT '{}'::jsonb,
  created timestamp with time zone default now(),
  CONSTRAINT tbl_rjournal_users_pkey PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE "tbl_rjournal_users"
  OWNER TO postgres;
