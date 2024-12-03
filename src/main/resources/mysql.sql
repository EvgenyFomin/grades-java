create table user (id bigint auto_increment primary key, name varchar(50), code bigint);

create procedure generate_rows()
begin
    SET @p1 = 1;
    label1: LOOP
        insert into user (name, code) values ('generate-rows', @p1);
        SET @p1 = @p1 + 1;
        IF @p1 < 30000 THEN
            ITERATE label1;
        END IF;
        LEAVE label1;
    END LOOP label1;
end;

create procedure generate_rows2()
begin
    SET @p1 = 1;
    label1: LOOP
        insert into user (name, code) values ('generate-rows2', @p1);
        SET @p1 = @p1 + 1;
        IF @p1 < 30000 THEN
            ITERATE label1;
        END IF;
        LEAVE label1;
    END LOOP label1;
end;

create procedure generate_rows3()
begin
    SET @p1 = 1;
    label1: LOOP
        insert into user (name, code) values ('generate-rows3', @p1);
        SET @p1 = @p1 + 1;
        IF @p1 < 30000 THEN
            ITERATE label1;
        END IF;
        LEAVE label1;
    END LOOP label1;
end;

call generate_rows();
call generate_rows2();
call generate_rows3();

drop procedure generate_rows;
drop procedure generate_rows2;
drop procedure generate_rows3;

create view v_avg as
select name, avg(code), count(*) from user group by name;

select * from v_avg;

create temporary table avg_calc as select name, avg(code) avg from user group by name;
select * from user where name in (select name from avg_calc where avg < 10000);
select * from user where name in (select name from avg_calc where avg >= 1000);
drop temporary table avg_calc;

create procedure sqr_out(IN a int, OUT ret int)
begin
    select a * a into ret;
end;

create procedure sqr_in_out(INOUT a int)
begin
    select a * a into a;
end;

SET GLOBAL log_bin_trust_function_creators = 1;

create function sqr_func(a int) returns double deterministic contains sql
begin
    return a * a;
end;

select * from user;

drop function sqr_func;
select sqr_func(20);

create trigger plus_last_code before insert on user
    for each row
begin
    SET NEW.code = NEW.code + (select code from user order by id desc limit 1);
end;

create trigger wrong_plus_last_code before insert on user for each row
begin
    SET @pls = (select code from user order by id desc limit 1);
end;

insert into user (name, code) VALUES ('zxcv', 1 + @pls);

drop trigger plus_last_code;
drop trigger wrong_plus_last_code;