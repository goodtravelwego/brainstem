-- Starter for a NEW Supabase project only. Review before running in the SQL Editor.
-- Uses the Android Auth.kt REST contract; this is not the original server's schema.
begin;

create table public.app_profile (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
    created_at timestamptz not null default now(),
    real_name text not null,
    nickname text not null default '',
    local_code text,
    sex text,
    birth_date date not null,
    type text not null default 'adult' check (type in ('kid', 'adult', 'care')),
    handedness text,
    guardian_name text,
    guardian_relation text,
    guardian_phone text,
    consent_at timestamptz not null,
    photo_path text
);
create index app_profile_owner_created on public.app_profile (owner_id, created_at);
alter table public.app_profile enable row level security;
revoke all on public.app_profile from anon;
grant select, insert, update on public.app_profile to authenticated;

create policy profile_read_own on public.app_profile for select to authenticated
    using (owner_id = (select auth.uid()));
create policy profile_insert_own on public.app_profile for insert to authenticated
    with check (owner_id = (select auth.uid()));
create policy profile_update_own on public.app_profile for update to authenticated
    using (owner_id = (select auth.uid()))
    with check (owner_id = (select auth.uid()));

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('profile-photos', 'profile-photos', false, 262144, array['image/jpeg']);

create policy photo_read_own on storage.objects for select to authenticated
    using (bucket_id = 'profile-photos' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy photo_insert_own on storage.objects for insert to authenticated
    with check (bucket_id = 'profile-photos' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy photo_update_own on storage.objects for update to authenticated
    using (bucket_id = 'profile-photos' and (storage.foldername(name))[1] = (select auth.uid())::text)
    with check (bucket_id = 'profile-photos' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy photo_delete_own on storage.objects for delete to authenticated
    using (bucket_id = 'profile-photos' and (storage.foldername(name))[1] = (select auth.uid())::text);

commit;
