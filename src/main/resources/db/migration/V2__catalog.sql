-- =====================================================================
-- V2: catalog modulu — məhsullar və onların qiymətləri
-- =====================================================================

-- ---------------------------------------------------------------------
-- products: şirkətin aldığı məhsullar (Xurma, Alma, Pomidor...)
-- ---------------------------------------------------------------------
create table products (
                          id          uuid         primary key,
                          tenant_id   uuid         not null,
                          name        varchar(100) not null,
                          active      boolean      not null default true,
                          created_at  timestamptz  not null,
                          updated_at  timestamptz  not null,
                          version     bigint       not null default 0,

                          constraint fk_products_tenant foreign key (tenant_id) references tenants (id),
    -- Aşağıdakı birləşik xarici açar üçün lazımdır (id onsuz da unikaldır)
                          constraint uq_products_id_tenant unique (id, tenant_id)
);

-- Eyni şirkətdə "Xurma" və "xurma" iki ayrı məhsul ola bilməz
create unique index uq_products_tenant_name on products (tenant_id, lower(name));

comment on table products is 'Şirkətin aldığı məhsullar. Çəki həmişə kiloqramla.';
comment on column products.name is 'Məhsulun adı. Şirkət daxilində böyük-kiçik hərfdən asılı olmayaraq unikaldır.';
comment on column products.active is 'false — yeni qəbzlərdə seçilmir, amma köhnə qəbzlərdə qalır. Silinmir.';

-- ---------------------------------------------------------------------
-- product_prices: məhsulun sort üzrə kiloqram qiyməti, tarixçə ilə
-- ---------------------------------------------------------------------
create table product_prices (
                                id              uuid           primary key,
                                tenant_id       uuid           not null,
                                product_id      uuid           not null,
                                grade           varchar(5)     not null,
                                price_per_kg    numeric(10, 4) not null,
                                effective_from  timestamptz    not null,
                                created_at      timestamptz    not null,
                                created_by      uuid           not null,

    -- Qiymət və məhsul MÜTLƏQ eyni şirkətə aiddir
                                constraint fk_product_prices_product foreign key (product_id, tenant_id)
                                    references products (id, tenant_id),
                                constraint fk_product_prices_created_by foreign key (created_by) references users (id),
                                constraint ck_product_prices_grade check (grade in ('I', 'II', 'III')),
                                constraint ck_product_prices_positive check (price_per_kg > 0)
);

-- Qəbz üçün “qüvvədə olan qiymət” axtarışı məhz bu sıra ilə gedir
create index ix_product_prices_lookup
    on product_prices (tenant_id, product_id, grade, effective_from desc, created_at desc);

comment on table product_prices is 'Qiymət tarixçəsi. Sətirlər heç vaxt dəyişdirilmir və silinmir: yeni qiymət = yeni sətir.';
comment on column product_prices.grade is 'Sort: I, II, III.';
comment on column product_prices.price_per_kg is 'Fermerdən alış qiyməti, AZN/kq, 4 onluq dəqiqliklə.';
comment on column product_prices.effective_from is 'Qiymətin qüvvəyə mindiyi an (UTC). Qəbz öz vaxtında qüvvədə olan ən son qiyməti götürür.';
comment on column product_prices.created_by is 'Qiyməti daxil edən istifadəçi (audit).';