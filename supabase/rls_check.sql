begin;

create function public.rls_check_login(user_id uuid, user_email text)
returns void
language plpgsql
as $$
begin
    perform set_config(
        'request.jwt.claims',
        json_build_object('sub', user_id, 'email', user_email, 'role', 'authenticated')::text,
        true
    );
end;
$$;

create function public.rls_check_expect(statement text, expected text)
returns void
language plpgsql
as $$
declare
    actual text := 'no error';
begin
    begin
        execute statement;
    exception
        when others then
            actual := sqlerrm;
    end;
    if actual <> expected then
        raise exception 'ASSERT expected % but got %', expected, actual;
    end if;
end;
$$;

insert into auth.users (id, email, raw_user_meta_data) values
    ('00000000-0000-0000-0000-00000000000a', 'a@example.test', '{}'),
    ('00000000-0000-0000-0000-00000000000b', 'b@example.test', '{"display_name": "Bea"}'),
    ('00000000-0000-0000-0000-00000000000c', 'c@example.test', '{}');

do $$
begin
    assert (select count(*) from public.profiles
            where email like '%@example.test') = 3, 'profiles trigger';
    assert (select display_name from public.profiles
            where email = 'b@example.test') = 'Bea', 'display_name metadata';
    assert (select display_name from public.profiles
            where email = 'a@example.test') = 'a', 'display_name fallback';
end;
$$;

set local role authenticated;

select public.rls_check_login('00000000-0000-0000-0000-00000000000a', 'a@example.test');

do $$
declare
    new_home public.homes;
begin
    new_home := public.create_home('   ');
    assert new_home.name = 'MyHome', 'blank name becomes MyHome';
    perform set_config('rls_check.home', new_home.id::text, true);
    insert into public.tasks (home_id, what, assignee_kind, recurrence, next_date)
    values (new_home.id, 'Vacuum', 'UNASSIGNED', '{}'::jsonb, current_date);
    perform public.invite_homie(new_home.id, '  B@Example.test ', 'HOMIE');
    assert (select count(*) from public.invitations) = 1, 'admin sees invitation';
    assert (select count(*) from public.profiles) = 1, 'no shared home yet';
    perform public.rls_check_expect(
        format('select public.invite_homie(%L, %L, %L)',
            new_home.id, 'b@example.test', 'HOMIE'),
        'DUPLICATE_INVITATION'
    );
end;
$$;

select public.rls_check_login('00000000-0000-0000-0000-00000000000b', 'b@example.test');

do $$
declare
    target_home uuid := current_setting('rls_check.home')::uuid;
    affected bigint;
begin
    assert (select count(*) from public.tasks) = 0, 'outsider sees no tasks';
    assert (select count(*) from public.home_members) = 0, 'outsider sees no members';
    assert (select count(*) from public.homes) = 1, 'home visible through invitation';
    assert (select count(*) from public.invitations) = 1, 'invitee sees invitation';
    perform public.rls_check_expect(
        format('select public.invite_homie(%L, %L, %L)', target_home, 'c@example.test', 'HOMIE'),
        'NOT_ADMIN'
    );
    perform public.rls_check_expect(
        format('select public.save_tasks(%L, %L)', target_home, '[]'),
        'NOT_MEMBER'
    );
    perform public.rls_check_expect(
        format('select public.accept_invitation(%L)', gen_random_uuid()),
        'INVITATION_NOT_FOUND'
    );
    update public.homes set name = 'Hijacked' where id = target_home;
    get diagnostics affected = row_count;
    assert affected = 0, 'outsider cannot rename home';
    perform public.accept_invitation((select id from public.invitations));
    assert (select count(*) from public.tasks) = 1, 'tasks visible after accept';
    assert (select count(*) from public.home_members) = 2, 'members visible after accept';
    assert (select count(*) from public.profiles) = 2, 'shared-home profiles visible';
    assert (select role from public.home_members
            where user_id = '00000000-0000-0000-0000-00000000000b') = 'HOMIE',
        'role comes from invitation';
    update public.home_members set role = 'HOMIE'
    where user_id = '00000000-0000-0000-0000-00000000000a';
    get diagnostics affected = row_count;
    assert affected = 0, 'homie cannot demote admin';
    delete from public.home_members
    where user_id = '00000000-0000-0000-0000-00000000000a';
    get diagnostics affected = row_count;
    assert affected = 0, 'homie cannot remove admin';
    perform public.rls_check_expect(
        format('select public.invite_homie(%L, %L, %L)', target_home, 'c@example.test', 'HOMIE'),
        'NOT_ADMIN'
    );
end;
$$;

select public.rls_check_login('00000000-0000-0000-0000-00000000000c', 'c@example.test');

do $$
begin
    assert (select count(*) from public.homes) = 0, 'stranger sees no homes';
    assert (select count(*) from public.tasks) = 0, 'stranger sees no tasks';
    assert (select count(*) from public.home_members) = 0, 'stranger sees no members';
    assert (select count(*) from public.invitations) = 0, 'stranger sees no invitations';
end;
$$;

select public.rls_check_login('00000000-0000-0000-0000-00000000000a', 'a@example.test');

do $$
declare
    target_home uuid := current_setting('rls_check.home')::uuid;
begin
    perform public.rls_check_expect(
        format('update public.home_members set role = %L where user_id = %L',
            'HOMIE', '00000000-0000-0000-0000-00000000000a'),
        'LAST_ADMIN'
    );
    perform public.rls_check_expect(
        format('delete from public.home_members where user_id = %L',
            '00000000-0000-0000-0000-00000000000a'),
        'LAST_ADMIN'
    );
    perform public.rls_check_expect(
        format('select public.invite_homie(%L, %L, %L)', target_home, ' B@example.TEST', 'HOMIE'),
        'ALREADY_MEMBER'
    );
    perform public.save_tasks(
        target_home,
        '[{"what": "First", "assignee_kind": "EVERYONE", "recurrence": {}, "next_date": "2026-10-17"},
          {"what": "Second", "assignee_kind": "PEOPLE",
           "assignee_ids": ["00000000-0000-0000-0000-00000000000b"],
           "recurrence": {}, "next_date": "2026-10-18"}]'::jsonb
    );
    assert (select count(*) from public.tasks) = 2, 'first batch saved';
    perform public.save_tasks(
        target_home,
        '[{"what": "Third", "assignee_kind": "UNASSIGNED", "recurrence": {}, "next_date": "2026-10-19"}]'::jsonb
    );
    assert (select count(*) from public.tasks) = 1, 'second batch replaces first';
    assert (select what from public.tasks) = 'Third', 'only second batch remains';
    perform public.save_tasks(target_home, '[]'::jsonb);
    assert (select count(*) from public.tasks) = 0, 'empty array clears tasks';
    update public.home_members set role = 'ADMIN'
    where user_id = '00000000-0000-0000-0000-00000000000b';
    update public.home_members set role = 'HOMIE'
    where user_id = '00000000-0000-0000-0000-00000000000a';
    assert (select role from public.home_members
            where user_id = '00000000-0000-0000-0000-00000000000a') = 'HOMIE',
        'admin can demote when another admin remains';
end;
$$;

rollback;
