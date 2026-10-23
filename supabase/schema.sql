create extension if not exists pgcrypto;

create table if not exists public.profiles (
    id uuid primary key references auth.users on delete cascade,
    email text not null,
    display_name text not null
);

alter table public.profiles
    add column if not exists preferred_weekday smallint not null default 7
    check (preferred_weekday between 1 and 7),
    add column if not exists preferred_start_time time not null default '10:00',
    add column if not exists latest_end_time time not null default '18:00';

insert into public.profiles (id, email, display_name)
select
    id,
    lower(email),
    coalesce(raw_user_meta_data->>'display_name', split_part(email, '@', 1))
from auth.users
on conflict (id) do nothing;

create table if not exists public.homes (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    created_by uuid not null default auth.uid() references auth.users,
    created_at timestamptz not null default now()
);

create table if not exists public.home_members (
    home_id uuid references public.homes on delete cascade,
    user_id uuid references public.profiles (id) on delete cascade,
    role text not null check (role in ('ADMIN', 'HOMIE')),
    joined_at timestamptz not null default now(),
    primary key (home_id, user_id)
);

create table if not exists public.invitations (
    id uuid primary key default gen_random_uuid(),
    home_id uuid not null references public.homes on delete cascade,
    email text not null,
    role text not null check (role in ('ADMIN', 'HOMIE')),
    invited_by uuid not null default auth.uid() references auth.users,
    status text not null default 'PENDING'
        check (status in ('PENDING', 'ACCEPTED', 'DECLINED')),
    created_at timestamptz not null default now()
);

create table if not exists public.tasks (
    id uuid primary key default gen_random_uuid(),
    home_id uuid not null references public.homes on delete cascade,
    what text not null,
    location text not null,
    frequency_text text not null,
    duration_minutes int not null check (duration_minutes >= 0),
    assignee_kind text not null
        check (assignee_kind in ('EVERYONE', 'UNASSIGNED', 'PEOPLE')),
    assignee_ids uuid[] not null,
    recurrence jsonb not null,
    next_date date not null,
    created_at timestamptz not null default now()
);

alter table public.tasks
    add column if not exists start_time time not null;

alter table public.profiles alter column display_name drop default;
alter table public.homes alter column name drop default;
alter table public.tasks
    alter column location drop default,
    alter column frequency_text drop default,
    alter column duration_minutes drop default,
    alter column assignee_ids drop default,
    alter column start_time drop default;

create unique index if not exists invitations_pending_unique
    on public.invitations (home_id, email) where status = 'PENDING';
create index if not exists invitations_email_idx on public.invitations (email);
create index if not exists home_members_user_idx on public.home_members (user_id);
create index if not exists tasks_home_idx on public.tasks (home_id);

create or replace function public.is_member(target_home uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1 from public.home_members
        where home_id = target_home and user_id = auth.uid()
    );
$$;

create or replace function public.is_admin(target_home uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1 from public.home_members
        where home_id = target_home and user_id = auth.uid() and role = 'ADMIN'
    );
$$;

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    insert into public.profiles (id, email, display_name)
    values (
        new.id,
        lower(new.email),
        coalesce(new.raw_user_meta_data->>'display_name', split_part(new.email, '@', 1))
    );
    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();

create or replace function public.ensure_admin_remains()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    if old.role = 'ADMIN'
        and (tg_op = 'DELETE' or new.role <> 'ADMIN')
        and exists (select 1 from public.homes where id = old.home_id)
        and not exists (
            select 1 from public.home_members
            where home_id = old.home_id and role = 'ADMIN' and user_id <> old.user_id
        )
    then
        raise exception 'LAST_ADMIN';
    end if;
    if tg_op = 'DELETE' then
        return old;
    end if;
    return new;
end;
$$;

drop trigger if exists ensure_admin_remains on public.home_members;
create trigger ensure_admin_remains
    before update of role or delete on public.home_members
    for each row execute function public.ensure_admin_remains();

alter table public.profiles enable row level security;
alter table public.homes enable row level security;
alter table public.home_members enable row level security;
alter table public.invitations enable row level security;
alter table public.tasks enable row level security;

revoke all on public.profiles, public.homes, public.home_members,
    public.invitations, public.tasks from anon;
revoke insert, delete on public.homes from authenticated;
revoke update on public.homes from authenticated;
revoke insert, update, delete on public.invitations from authenticated;
revoke insert on public.home_members from authenticated;
revoke update on public.home_members from authenticated;
revoke insert, delete on public.profiles from authenticated;
revoke update on public.profiles from authenticated;
grant update (name) on public.homes to authenticated;
grant update (role) on public.home_members to authenticated;
grant update (display_name, preferred_weekday, preferred_start_time, latest_end_time) on public.profiles to authenticated;

drop policy if exists profiles_select on public.profiles;
create policy profiles_select on public.profiles for select to authenticated
    using (
        id = auth.uid()
        or exists (
            select 1
            from public.home_members mine
            join public.home_members theirs on theirs.home_id = mine.home_id
            where mine.user_id = auth.uid() and theirs.user_id = profiles.id
        )
    );

drop policy if exists profiles_update on public.profiles;
create policy profiles_update on public.profiles for update to authenticated
    using (id = auth.uid())
    with check (id = auth.uid());

drop policy if exists homes_select on public.homes;
create policy homes_select on public.homes for select to authenticated
    using (
        public.is_member(id)
        or exists (
            select 1 from public.invitations
            where invitations.home_id = homes.id
                and invitations.status = 'PENDING'
                and invitations.email = lower(auth.jwt()->>'email')
        )
    );

drop policy if exists homes_update on public.homes;
create policy homes_update on public.homes for update to authenticated
    using (public.is_admin(id))
    with check (public.is_admin(id));

drop policy if exists home_members_select on public.home_members;
create policy home_members_select on public.home_members for select to authenticated
    using (public.is_member(home_id));

drop policy if exists home_members_update on public.home_members;
create policy home_members_update on public.home_members for update to authenticated
    using (public.is_admin(home_id))
    with check (public.is_admin(home_id));

drop policy if exists home_members_delete on public.home_members;
create policy home_members_delete on public.home_members for delete to authenticated
    using (public.is_admin(home_id) or user_id = auth.uid());

drop policy if exists invitations_select on public.invitations;
create policy invitations_select on public.invitations for select to authenticated
    using (public.is_admin(home_id) or email = lower(auth.jwt()->>'email'));

drop policy if exists tasks_all on public.tasks;
create policy tasks_all on public.tasks for all to authenticated
    using (public.is_member(home_id))
    with check (public.is_member(home_id));

drop function if exists public.create_home(text);
create function public.create_home(home_name text)
returns public.homes
language plpgsql
security definer
set search_path = public
as $$
declare
    new_home public.homes;
begin
    insert into public.homes (name)
    values (trim(home_name))
    returning * into new_home;
    insert into public.home_members (home_id, user_id, role)
    values (new_home.id, auth.uid(), 'ADMIN');
    return new_home;
end;
$$;

create or replace function public.invite_homie(
    target_home uuid,
    invitee_email text,
    invitee_role text
)
returns public.invitations
language plpgsql
security definer
set search_path = public
as $$
declare
    normalized_email text := lower(trim(invitee_email));
    new_invitation public.invitations;
begin
    if not public.is_admin(target_home) then
        raise exception 'NOT_ADMIN';
    end if;
    if invitee_role not in ('ADMIN', 'HOMIE') then
        raise exception 'INVALID_ROLE';
    end if;
    if normalized_email !~ '^[^@\s]+@[^@\s]+$' then
        raise exception 'INVALID_EMAIL';
    end if;
    if exists (
        select 1
        from public.profiles
        join public.home_members on home_members.user_id = profiles.id
        where profiles.email = normalized_email and home_members.home_id = target_home
    ) then
        raise exception 'ALREADY_MEMBER';
    end if;
    if exists (
        select 1 from public.invitations
        where home_id = target_home and email = normalized_email and status = 'PENDING'
    ) then
        raise exception 'DUPLICATE_INVITATION';
    end if;
    insert into public.invitations (home_id, email, role)
    values (target_home, normalized_email, invitee_role)
    returning * into new_invitation;
    return new_invitation;
end;
$$;

create or replace function public.accept_invitation(invitation_id uuid)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
    pending public.invitations;
begin
    select * into pending from public.invitations
    where id = invitation_id
        and status = 'PENDING'
        and email = lower(auth.jwt()->>'email')
    for update;
    if not found then
        raise exception 'INVITATION_NOT_FOUND';
    end if;
    insert into public.home_members (home_id, user_id, role)
    values (pending.home_id, auth.uid(), pending.role)
    on conflict do nothing;
    update public.invitations set status = 'ACCEPTED' where id = pending.id;
    return pending.home_id;
end;
$$;

create or replace function public.decline_invitation(invitation_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    update public.invitations set status = 'DECLINED'
    where id = invitation_id
        and status = 'PENDING'
        and email = lower(auth.jwt()->>'email');
    if not found then
        raise exception 'INVITATION_NOT_FOUND';
    end if;
end;
$$;

create or replace function public.save_tasks(target_home uuid, new_tasks jsonb)
returns setof public.tasks
language plpgsql
security definer
set search_path = public
as $$
begin
    if not public.is_member(target_home) then
        raise exception 'NOT_MEMBER';
    end if;
    delete from public.tasks where home_id = target_home;
    return query
    insert into public.tasks (
        home_id, what, location, frequency_text, duration_minutes,
        assignee_kind, assignee_ids, recurrence, next_date, start_time
    )
    select
        target_home,
        incoming.what,
        incoming.location,
        incoming.frequency_text,
        incoming.duration_minutes,
        incoming.assignee_kind,
        coalesce(
            (select array_agg(id::uuid)
             from jsonb_array_elements_text(incoming.assignee_ids) as id),
            '{}'
        ),
        incoming.recurrence,
        incoming.next_date,
        incoming.start_time
    from jsonb_to_recordset(new_tasks) as incoming (
        what text,
        location text,
        frequency_text text,
        duration_minutes int,
        assignee_kind text,
        assignee_ids jsonb,
        recurrence jsonb,
        next_date date,
        start_time time
    )
    returning *;
end;
$$;

revoke execute on function public.is_member(uuid), public.is_admin(uuid),
    public.create_home(text), public.invite_homie(uuid, text, text),
    public.accept_invitation(uuid), public.decline_invitation(uuid),
    public.save_tasks(uuid, jsonb) from public, anon;
revoke execute on function public.handle_new_user(),
    public.ensure_admin_remains() from public, anon, authenticated;
grant execute on function public.is_member(uuid), public.is_admin(uuid),
    public.create_home(text), public.invite_homie(uuid, text, text),
    public.accept_invitation(uuid), public.decline_invitation(uuid),
    public.save_tasks(uuid, jsonb) to authenticated;

notify pgrst, 'reload schema';
