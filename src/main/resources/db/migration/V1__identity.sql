-- =====================================================================
-- V1: identity modulu — şirkətlər (tenant) və istifadəçilər
-- =====================================================================

-- ---------------------------------------------------------------------
-- tenants: sistemdən istifadə edən şirkət (ixracçı, topdayıcı)
-- ---------------------------------------------------------------------
create table tenants (
                         id          uuid         primary key,
                         name        varchar(200) not null,
                         tax_id      varchar(20),
                         status      varchar(20)  not null default 'ACTIVE',
                         created_at  timestamptz  not null,
                         updated_at  timestamptz  not null,
                         version     bigint       not null default 0,

                         constraint ck_tenants_status check (status in ('ACTIVE', 'SUSPENDED')),
                         constraint uq_tenants_tax_id unique (tax_id)
);

comment on table tenants is 'Sistemdən istifadə edən şirkətlər. Hər biznes cədvəlində tenant_id var.';
comment on column tenants.tax_id is 'VÖEN. Boş ola bilər (pilot mərhələsində), amma dolu olanda unikal olmalıdır.';
comment on column tenants.status is 'ACTIVE — işləyir; SUSPENDED — girişi dayandırılıb (ödəniş yoxdur və s.).';
comment on column tenants.version is 'Optimistic locking: eyni anda iki dəyişikliyin bir-birinin üzərinə yazmasının qarşısı.';

-- ---------------------------------------------------------------------
-- users: şirkətin işçiləri
-- ---------------------------------------------------------------------
create table users (
                       id             uuid         primary key,
                       tenant_id      uuid         not null,
                       email          varchar(254) not null,
                       full_name      varchar(120) not null,
                       phone          varchar(20),
                       password_hash  varchar(255) not null,
                       role           varchar(20)  not null,
                       active         boolean      not null default true,
                       last_login_at  timestamptz,
                       created_at     timestamptz  not null,
                       updated_at     timestamptz  not null,
                       version        bigint       not null default 0,

                       constraint fk_users_tenant foreign key (tenant_id) references tenants (id),
                       constraint ck_users_role check (role in ('OWNER', 'MANAGER', 'RECEIVER', 'ACCOUNTANT', 'VIEWER')),
                       constraint ck_users_email_lowercase check (email = lower(email)),
                       constraint uq_users_email unique (email)
);

create index ix_users_tenant on users (tenant_id);

comment on table users is 'Şirkət işçiləri. Hər istifadəçi bir tenant-a aiddir və bir rolu var.';
comment on column users.email is 'Giriş adı. Həmişə kiçik hərflərlə saxlanılır, bütün sistemdə unikaldır.';
comment on column users.password_hash is 'Şifrənin heş-i ({bcrypt}... formatında). Şifrənin özü heç vaxt saxlanılmır.';
comment on column users.role is 'OWNER, MANAGER, RECEIVER, ACCOUNTANT, VIEWER — README-dəki rol cədvəlinə baxın.';
comment on column users.active is 'false — istifadəçi silinmir, sadəcə girişi bağlanır (tarixçə qorunur).';