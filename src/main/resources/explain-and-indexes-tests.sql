# Scheme for tests

# create table if not exists person
# (
#     id               bigint auto_increment
#         primary key,
#     created          datetime                  not null,
#     creator          varchar(256)              not null,
#     company_id       bigint                    not null,
#     firstname        varchar(128)              null,
#     lastname         varchar(128)              null,
#     secondname       varchar(128)              null,
#     displayname      varchar(256)              not null,
#     displayname_en   varchar(256) default ''   not null,
#     displayPosition  varchar(500)              null,
#     sex              char                      not null,
#     birthday         date                      null,
#     info             varchar(2000)             null,
#     ipaddress        varchar(64)               null,
#     isdeleted        int          default 0    not null,
#     department_id    bigint                    null,
#     department       varchar(256)              null,
#     displayShortName varchar(128)              null,
#     isfired          int          default 0    not null,
#     relations        varchar(128)              null,
#     old_id           bigint                    null,
#     locale           varchar(8)   default 'ru' null,
#     firedate         datetime                  null,
#     inn              varchar(16)               null,
#     istech           int          default 0    not null
# )
#     collate = utf8mb3_unicode_ci;
#
# create table if not exists case_comment
# (
#     ID                        bigint auto_increment
#         primary key,
#     CREATED                   datetime    default CURRENT_TIMESTAMP not null,
#     CASE_ID                   bigint                                not null,
#     AUTHOR_ID                 bigint                                not null,
#     COMMENT_TEXT              longtext                              null,
#     time_elapsed              bigint                                null,
#     remote_id                 varchar(64)                           null,
#     remote_link_id            bigint                                null,
#     original_author_name      varchar(64)                           null,
#     original_author_full_name varchar(256)                          null,
#     time_elapsed_type         int                                   null,
#     privacy_type              varchar(64) default 'PUBLIC'          not null,
#     constraint FK_CSCOMM_AUTHOR
#         foreign key (AUTHOR_ID) references person (id)
# )
#     collate = utf8mb4_unicode_ci;
#
# create index ix_casecomment_ap
#     on case_comment (AUTHOR_ID);


# drop index case_comment_time_elapsed_type_index on case_comment;
# explain select * from case_comment where time_elapsed_type = 1;
# create index case_comment_time_elapsed_type_index on case_comment (time_elapsed_type desc);
# explain select * from case_comment where time_elapsed_type = 1;
# explain select * from case_comment where time_elapsed_type > 1;
# explain select * from case_comment where time_elapsed_type > 0;
# explain select * from (select MAX(с.time_elapsed_type) from case_comment с) as ccM;
# explain select MAX(с.time_elapsed_type) from case_comment с;


# drop index case_comment_name_index on case_comment;
# explain select * from case_comment where original_author_name = "Фомин";
# create index case_comment_name_index on case_comment (original_author_name,original_author_full_name);
# explain select * from case_comment where original_author_name = "Фомин";
# explain select * from case_comment where original_author_name like "Фомин%";
# explain select * from case_comment where original_author_name like "Фомин";
# explain select * from case_comment where original_author_name like "%Фомин" or original_author_name like "Фомин%";
# explain select * from case_comment where original_author_name = "Фомин" or original_author_name = "Фомин";
# explain select * from case_comment having ID = 3;
# explain select * from case_comment where original_author_name like "Фомин%";
# explain select * from case_comment where original_author_full_name = "Фомин";
# explain update case_comment set COMMENT_TEXT = "sadf" where original_author_name = "Фомин";
# explain select * from case_comment where original_author_name = "Фомин Евгений" and original_author_full_name = "Фомин Евгений";
# explain select * from case_comment where original_author_full_name = "Фомин Евгений" and original_author_name = "Фомин Евгений";
# explain select * from case_comment inner join person p on case_comment.AUTHOR_ID = p.id where p.id = 3;
# explain analyse select * from case_comment inner join person p on case_comment.AUTHOR_ID = p.id where p.id = 3;

# types:
# all - поиск по всей таблице
# index - поиск по всей таблице, но в порядке индекса
# range - используется индекс. Начинается в определенной точке и возвращает значения, пока утверждение верно
# ref - используется индекс. Возврат строк, соответствующих одному значению
# eq_ref - считывание одной строки по первичному/уникальному ключу
# system - константное (единственное) значение
# NULL - обращение к таблице не требуется. Есть вся необходимая инфа