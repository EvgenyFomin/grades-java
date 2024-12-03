create table "user" (id serial primary key, name varchar(50), code bigint);

do $$
BEGIN
    for i in 1..3000000 by 1
        loop
insert into "user" (name, code) values ('do-for', i);
end loop;
END;
$$;

create or replace procedure gen_codes()
language plpgsql
as $$
    declare
        i int := 1;
begin
    while i <= 3000000
        loop
insert into "user" (name, code) values ('pl/pgSql-gen_codes-while', i);
i := i + 1;
end loop;
end;
$$;

create or replace procedure gen_codes_2()
language SQL
as $$
insert into "user" (name, code) select 'pl/SQL-gen_codes2-generate_series', floor(random() * 301) from generate_series(1, 3000000);
$$;

call gen_codes();
call gen_codes_2();

create materialized view mv_avg as
select name, avg(code), count(*) from "user" group by name;

create view v_avg as
select name, avg(code), count(*) from "user" group by name;

select * from v_avg;
select * from mv_avg;

insert into "user" (name, code) values ('new-value', 1000);

select * from v_avg;
select * from mv_avg;

refresh materialized view mv_avg;

select * from mv_avg;

create temporary table avg_calc as select name, avg(code) avg from "user" group by name;
select * from "user" where name in (select name from avg_calc where avg < 1000);
select * from "user" where name in (select name from avg_calc where avg >= 1000);
drop table avg_calc;

do $$
    declare
        foo text;
bar text := 'World';
begin
    foo := 'Hello';
raise notice '%, %', foo, bar;
end;
$$;

do $$
       <<outer_block>>
    declare
    bar text := 'Hello';
begin
    <<inner_block>>
        declare
            bar text := 'World';
begin
    raise notice '%, %', outer_block.bar, inner_block.bar;
raise notice 'Дефолтное значение - %', bar;
end;
end;
$$;

create function sqr_in(IN a int) returns numeric
    as $$
begin
    return a * a;
end;
$$ language plpgsql;

create function sqr_out(IN a int, OUT ret int)
    as $$
begin
    ret := a * a;
end;
$$ language plpgsql;

create function sqr_in_out(INOUT a int)
    as $$
begin
    a := a * a;
end;
$$ language plpgsql;

select sqr_in(3);
select sqr_out(3);
select sqr_in_out(3);

drop function sqr_in;
drop function sqr_out;
drop function sqr_in_out;

create function plus_last_code_func() returns trigger as
    $$
        declare
            last_code int;
begin
    last_code := (select "user".code from "user" order by id desc limit 1);
NEW.code := NEW.code + last_code;
return NEW;
end;
$$ language plpgsql;

create trigger plus_last_code before insert on "user"
    for each row execute function plus_last_code_func();

select "user".code from "user" order by id desc limit 1;
insert into "user" (name, code) VALUES ('zxcv', 1);
select "user".code from "user" order by id desc limit 1;
select * from "user" where name = 'zxcv' order by id desc limit 1;

drop trigger "plus_last_code" on "user";
drop function plus_last_code_func;
