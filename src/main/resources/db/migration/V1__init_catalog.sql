create table categories (
    id bigint primary key,
    name varchar(255) not null,
    description varchar(1000),
    enabled boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table professionals (
    id bigint primary key,
    first_name varchar(255) not null,
    last_name varchar(255) not null,
    email varchar(255),
    category_id bigint references categories(id),
    enabled boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_professionals_category on professionals(category_id);

create table weekly_schedules (
    id bigint primary key,
    professional_id bigint references professionals(id),
    day_of_week varchar(10) not null,
    start_time time not null,
    end_time time not null,
    slot_duration_minutes integer not null,
    enabled boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_weekly_schedules_professional on weekly_schedules(professional_id);

create table catalog_versions (
    id bigint primary key,
    current_version bigint not null,
    last_synced_at timestamp with time zone
);

create table processed_catalog_events (
    event_id varchar(255) primary key,
    processed_at timestamp with time zone not null
);
