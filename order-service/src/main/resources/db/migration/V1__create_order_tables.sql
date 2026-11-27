create sequence order_id_seq start with 1 increment by 50;
create sequence order_item_id_seq start with 1 increment by 50;

create table orders
(
    id bigint default nextval('order_id_seq') not null,
    order_number text not null unique ,
    username text not null ,
    customer_name text not null ,
    customer_email text not null ,
    customer_phone text not null ,
    delivery_address_line1 text not null ,
    delivery_address_line2 text,
    delivery_address_city text not null ,
    delivery_address_state text not null ,
    delivery_address_zip_code text not null ,
    delivery_address_country text not null ,
    status text not null check ( status in ('NEW','SHIPPED','DELIVERED','CANCELLED') ),
    comments text,
    created_at timestamptz not null ,
    updated_at timestamptz not null ,
    version bigint not null default 0,
    primary key (id)
);

create table order_items
(
    id bigint default nextval('order_item_id_seq') not null,
    code text not null ,
    name text not null ,
    price numeric(10,2) not null check ( price >= 0 ),
    quantity integer not null check ( quantity > 0 ),
    primary key (id),
    version bigint not null default 0,
    order_id bigint not null references orders(id)
);

create index idx_orders_username on orders(username);
create index idx_order_items_order_id on order_items(order_id);
