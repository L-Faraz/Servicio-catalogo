create table sync_status (
    id bigint primary key,
    state varchar(20) not null,
    last_error varchar(2000),
    updated_at timestamp with time zone not null
);

insert into sync_status (id, state, updated_at) values (1, 'IDLE', now());

alter table professionals drop column email;
