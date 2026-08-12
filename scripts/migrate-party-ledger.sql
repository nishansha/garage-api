-- ============================================================================
-- Party subledger migration: add party_type/party_id to app_journal_detail
-- ============================================================================
-- Run this ONCE against a database that predates the party-subledger feature.
-- Purely additive - nullable columns, no backfill. Existing journal rows simply
-- have no party (NULL), which is correct: they were posted before this feature
-- existed, and no attempt is made to retroactively derive customer/vendor for
-- historical entries here. Only new postings going forward will carry a party.
-- Take a database backup first - see scripts/backup.sh.
-- ============================================================================

BEGIN;

ALTER TABLE public.app_journal_detail ADD COLUMN party_type character varying(20);
ALTER TABLE public.app_journal_detail ADD COLUMN party_id bigint;

CREATE INDEX idx_jdetail_party ON public.app_journal_detail USING btree (tenant_id, party_type, party_id) WHERE (party_type IS NOT NULL);

COMMIT;

-- Sanity check after running:
--   SELECT column_name FROM information_schema.columns
--   WHERE table_name = 'app_journal_detail' AND column_name IN ('party_type', 'party_id');
--   -- should return both rows
