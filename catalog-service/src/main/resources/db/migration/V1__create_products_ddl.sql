create table products
(
    id          bigint generated always as identity not null,
    code        text                                not null unique,
    name        text                                not null,
    description text,
    image_url   text,
    price       numeric                             not null,
    primary key (id)
);