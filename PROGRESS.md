# PROGRESS

Bu fayl işin gündəliyidir: harada qaldıq, nə hazırdır, növbəti addım nədir. Qaydalar və qərarlar `README.md`-dədir.

---

## Cari vəziyyət

- **Son tamamlanan:** Mərhələ 4 — `identity` modulu (4.1–4.7).
- **Növbəti addım:** Mərhələ 5 — `catalog` modulu, Addım 5.1 (V2 miqrasiyası).
- **Bazada cədvəl:** `flyway_schema_history`, `tenants`, `users`.
- **Lokal işə salma:** IntelliJ Run Configuration → Active profiles: `local` (demo hesablar README-də).
- **Tətbiq işə düşür:** Bəli (`Started AqroizBackendApplication`).
- **Testlər:** 55 test (`.\mvnw clean test` → BUILD SUCCESS).
- **Yoxlanmalı:** Sessiya 1-in son commit-inin GitHub-a düşdüyü (`git status`, `git log --oneline`, `git ls-files`).

---

## Növbəti addım: `catalog` modulu

Məqsəd: hər şirkətin məhsulları (xurma, alma, pomidor...), sortları (I, II, III) və gündəlik alış qiymətləri. Qəbz (`procurement`) qiyməti buradan götürüb dondurulacaq.

1. V2 miqrasiyası: `products`, `daily_prices` (tarixçə ilə, üzərinə yazılmır).
2. Entity-lər, repository-lər, testlər (qiymətin “verilən tarixdə qüvvədə olan” seçimi).
3. `CatalogService` + endpoint-lər: məhsullar (siyahı, yaratma), qiymətlər (cari siyahı, təyin etmə) — `CurrentUser` və rollar ilə.
4. Demo məhsullar və qiymətlər (`CatalogDevDataLoader`).
5. Postman: `02 Kataloq`.

## Köhnə plan: `identity` modulu (tamamlandı)

Kiçik addımlara bölünəcək, hər biri ayrıca təsdiqlə:

1. ✅ Postman qərarı (S1: repoda fayl) və `postman/` qovluğu.
2. ✅ Flyway `V1__identity.sql`: `tenants`, `users` cədvəlləri.
3. ✅ Entity-lər (`Tenant`, `UserAccount`) və repository-lər + Testcontainers ilə repository testi.
4. ✅ Şifrələmə (`PasswordEncoder`) və JWT (imzalama, yoxlama, `JwtProperties`).
5. ✅ Öz `SecurityFilterChain`: stateless, JWT, CSRF söndürülür, 401/403 də `ProblemDetail` formatında.
6. ✅ `POST /api/auth/login`, `GET /api/auth/me` + Postman + demo istifadəçilər (8-ci bənd buraya birləşdirildi).
7. ✅ `CurrentUser` (shared) — tenant və rol token-dən.

## Köhnə plan: `shared` modulu (tamamlandı)

Hər alt addım ayrıca edilir, istifadəçi təsdiq etdikdən sonra növbətiyə keçilir.

1. ✅ `az.aqroiz.shared` paketi və `package-info.java` (Modulith üçün “açıq modul”), `ModularityTests`.
2. ✅ `TimeConfig`: Asia/Baku zonası ilə `Clock` bean-i; `application.yaml`-a `hibernate.jdbc.time_zone: UTC`.
3. ✅ `BusinessRuleException`, `NotFoundException`.
4. ✅ `GlobalExceptionHandler`: `ProblemDetail` + `code` sahəsi, Azərbaycan dilində mesajlar.
5. Yoxlama: tətbiq işə düşür, test keçir, commit.

Qeyd: cari istifadəçi (`CurrentUser`) və sənəd nömrələri (`NumberingService`) `identity` və ilk miqrasiyadan sonra əlavə olunacaq, çünki onlar JWT-yə və `document_counters` cədvəlinə bağlıdır.

---

## Açıq suallar

| # | Sual | Nə vaxt lazımdır |
| --- | --- | --- |
| S2 | JWT gizli açarı prod-da necə saxlanacaq? | Deploy-dan əvvəl |
| S3 | SMS provayderi hansı olacaq (yerli və ya WhatsApp Business API)? | `notifications` mərhələsi |
| S4 | Şəxsi məlumatların Azərbaycanda saxlanma tələbi (server yeri) | Deploy-dan əvvəl, hüquqşünasla |
| S5 | Repo nə vaxt private ediləcək? | İlk pilot müştəridən əvvəl |

## Texniki borc və qeydlər

| # | Qeyd | Nə vaxt |
| --- | --- | --- |
| T1 | ~~`Using generated security password`~~ | ✅ Bağlandı (4.5) |
| T2 | Mockito “self-attaching” xəbərdarlığı (gələcək JDK-larda qadağan) | Mockito istifadəsinə başlayanda `pom.xml`-də agent kimi qoşulacaq |
| T3 | Maven-in `flyway-community-db-support ... 401` xəbərdarlığı | Zərərsiz, toxunmuruq |
| T4 | Boş `static` və `templates` qovluqları | İstəyə bağlı silinə bilər |
| T5 | ~~`GlobalExceptionHandlerTests`-də `@WithMockUser` və `csrf()`~~ | ✅ Bağlandı (4.5) |
| T6 | `Annotation processing is enabled...` kompilyator məlumatı | Zərərsiz; lazım olsa `pom.xml`-də prosessorlar açıq göstəriləcək |
| T7 | JWT açarının dev dəyəri repodadır | Prod profilində dev açarla işə düşməyi qadağan edən yoxlama (deploy-dan əvvəl) |
| T8 | Refresh token yoxdur, token 12 saat | Pilotdan sonra qiymətləndirmək |
| T9 | Giriş cəhdlərinin sayı məhdudlaşdırılmayıb (brute-force) | Deploy-dan əvvəl |
| T10 | Hesab bağlananda token bitənə qədər (12 saat) digər endpoint-lərdə işləyir; yalnız `/me` dərhal yoxlayır | T8 ilə birlikdə həll (qısa ömürlü token + refresh) |

---

## Sessiya jurnalı

### 2026-11-01 — Sessiya 1: Layihənin təməli

**Edilənlər:**

1. IntelliJ ilə Spring Boot 4.1.2 layihəsi yaradıldı: Maven, Java 21, group `az.aqroiz`, artifact `aqroiz-backend`, paket `az.aqroiz`, YAML konfiqurasiya.
2. Asılılıqlar seçildi: webmvc, validation, security, oauth2-resource-server, data-jpa, flyway, postgresql, actuator, modulith, testcontainers, configuration-processor.
3. `pom.xml` təmizləndi: Modulith-in `starter-jpa`, `actuator`, `runtime`, `observability-*` asılılıqları silindi; boş metadata blokları silindi; asılılıqlar qruplara ayrıldı.
4. `compose.yaml`: PostgreSQL 17-alpine, baza/istifadəçi/şifrə `aqroiz`, UTC, healthcheck, volume `aqroiz-pgdata`.
5. `application.yaml`: datasource (mühit dəyişənləri ilə), `open-in-view: false`, `ddl-auto: validate`, port 8080.
6. `HELP.md` silindi; `.gitignore` və `.gitattributes` (LF qaydası) yazıldı.
7. GitHub reposu yaradıldı: https://github.com/MehemmedBaloglanov/aqroiz-backend (public), branch `master` → `main` dəyişdirildi, `master` silindi.
8. README.md və PROGRESS.md yaradıldı.

**Rastlaşılan problemlər və həlləri:**

| Problem | Səbəb | Həll |
| --- | --- | --- |
| `Failed to configure a DataSource: 'url' attribute is not specified` | Konfiqurasiya hələ yazılmamışdı | `application.yaml` yazıldı |
| `password authentication failed for user "aqroiz"` (tətbiqdə) | Yerli Windows PostgreSQL 18 servisi də 5432-də dinləyirdi | `postgresql-x64-18` servisi dayandırıldı, `start= demand` edildi |
| `password authentication failed` (pgAdmin-də) | Şifrə səhv yazılmışdı | Düzəldildi |
| `refusing to delete the current branch: refs/heads/master` | GitHub-da default branch `master` idi | Settings → Default branch → `main` |

**Yoxlanılanlar:**

- `docker compose ps` → `healthy`
- `select version();` → PostgreSQL 17.11
- Tətbiq: `Started AqroizBackendApplication in 4.896 seconds`
- `git branch -a` → yalnız `main` və `origin/main`
- `resources/db/migration` lokalda iç-içə qovluq kimi mövcuddur (boş olduğu üçün repoda görünmür)

**Addım 3.1 (eyni gün, davamı):**

1. `az.aqroiz.shared` paketi və `package-info.java` yaradıldı: `@ApplicationModule(type = OPEN)`.
2. `ModularityTests` yazıldı: `verify()` və modulların siyahısı. Çıxış: `# Shared (open)`.
3. start.spring.io-nun yaratdığı test faylları araşdırıldı:
   - `AqroizBackendApplicationTests` saxlanıldı (tüstü testi, Testcontainers ilə).
   - `TestAqroizBackendApplication` silindi (lokal işə salmanın ikinci yolu çaşqınlıq yaradardı).
   - `TestcontainersConfiguration`: `postgres:latest` → `postgres:17-alpine` (sabit `POSTGRES_IMAGE`).
4. IntelliJ-də yeni fayllar üçün sətir sonu LF təyin edildi.
5. `.\mvnw test` → `Tests run: 3, Failures: 0, Errors: 0` → BUILD SUCCESS.

**Rastlaşılan problemlər:**

| Problem | Səbəb | Həll |
| --- | --- | --- |
| Silinməli fayl əvəzinə `AqroizBackendApplicationTests` silindi | Adlar oxşardır (`TestAqroiz...` / `...Tests`) | Fayl yenidən yaradıldı, `TestAqroizBackendApplication` silindi |
| `.\mvnw : The term '.\mvnw' is not recognized` | Əmr `C:\Windows\system32`-də işə salınmışdı | Layihə əmrləri yalnız IntelliJ terminalında (layihə qovluğunda) |
| Claude GitHub-dan köhnə (keşlənmiş) səhifə aldı | GitHub keşi və avtomatik girişə qadağalar | Repo vəziyyəti `git status/log/ls-files` çıxışı ilə yoxlanır |

**Növbəti sessiyada ilk iş:** `git status`, `git log --oneline`, `git ls-files` nəticəsini Claude-a göndərmək, sonra Addım 3.2.

### 2026-11-02 — Sessiya 2: `shared` modulunun tamamlanması

**Addım 3.2 — Vaxt idarəsi:**

1. `application.yaml`: `spring.jpa.properties.hibernate.jdbc.time_zone: UTC`, `aqroiz.time.business-zone: Asia/Baku`.
2. `TimeProperties` (record, `@ConfigurationProperties("aqroiz.time")`, standart `Asia/Baku`).
3. `TimeConfig`: `Clock.system(ZoneId.of(...))` bean-i.
4. `TimeConfigTests` (4 test, `ApplicationContextRunner`): standart zona, dəyişdirilə bilən zona, səhv zonada tətbiq işə düşmür, UTC 21:30 = Bakıda növbəti gün.

**Addım 3.3 — Xətaların vahid idarəsi:**

1. `az.aqroiz.shared.error`: `ErrorCodes`, `BusinessRuleException`, `NotFoundException`, `FieldViolation`, `GlobalExceptionHandler`.
2. `ValidationMessages.properties` — Azərbaycan dilində validasiya mesajları.
3. `GlobalExceptionHandlerTests` (7 test, `@WebMvcTest`, test üçün xüsusi controller).

**Rastlaşılan problemlər:**

| Problem | Səbəb | Həll |
| --- | --- | --- |
| `expected: Asia/Tashkent but was: Asia/Baku` | Testdə gözlənilən dəyər kopyalanarkən qarışmışdı | Test düzəldildi; tətbiq düzgün işləyirdi |
| `invalidZonePreventsStartup` testi silinmişdi | Logdakı `Unknown time-zone ID: Asia/Bakuu` xəta sanılmışdı | Test qaytarıldı. Bu WARN gözləniləndir |
| `Could not find a valid Docker environment` | Docker Desktop açıq deyildi | Docker açıldı; avtomatik başlama tövsiyə olundu |
| Validasiya mesajları ingiliscə (`must not be blank`) | Fayl adı `ValidationMessage.properties` idi (sonda `s` yox) | `ValidationMessages.properties` olaraq dəyişdirildi, `mvnw clean test` |
| Cavabda `Yalnış sorğu` | Yazı səhvi | `Yanlış sorğu`; testə `$.title` yoxlaması əlavə olundu |
| Commit mesajı formata uyğun deyildi (`refactor PROGRESS and README files`) | — | Göndərilmiş tarixçə dəyişdirilmədi; növbəti commit-lərdə Conventional Commits |

**Yoxlanılanlar:**

- `.\mvnw test` → `Tests run: 14` (gözlənilən; son nəticəni Claude-a göndərmək)
- `GlobalExceptionHandlerTests` → 7/7 keçir
- Jackson 3 `ProblemDetail` sahələrini yuxarı səviyyədə yazır

**Növbəti sessiyada ilk iş:** `git log --oneline` (3.3 commit-inin olduğunu yoxlamaq), S1 qərarı, sonra `identity`.

### 2026-11-03 — Sessiya 3: `identity` başlanğıcı

**Addım 4.1 — Postman:**

1. S1 qərarı: kolleksiya repoda fayl kimi.
2. `postman/AqroIz.postman_collection.json` (`00 Sistem → Sağlamlıq yoxlaması`, kolleksiya səviyyəsində `Bearer {{token}}`).
3. `postman/AqroIz-local.postman_environment.json` (`baseUrl`, `token`).
4. Import, mühit seçimi, sorğu: **200 OK**, `{"status":"UP","groups":["liveness","readiness"]}`, testlər 2/2.

**Addım 4.2 — V1 miqrasiyası:**

1. `src/main/resources/db/migration/V1__identity.sql`: `tenants` (status, VÖEN unikal, `version`), `users` (rol `check`, e-poçt kiçik hərflə və unikal, `active`, `last_login_at`, `version`), `ix_users_tenant`, Azərbaycan dilində `comment on`.
2. `.\mvnw test` → 14 test, BUILD SUCCESS (miqrasiya Testcontainers-in təmiz bazasında tətbiq olundu).

**Rastlaşılan problemlər:**

| Problem | Səbəb | Həll |
| --- | --- | --- |
| Claude `health` üçün 401 gözləyirdi, 200 gəldi | Spring Boot actuator `health`-i standart olaraq açıq saxlayır | Claude-un proqnozu səhv idi; nəticə düzgündür |
| GitHub səhifəsi köhnə keşdən gəlir | Claude-un veb alətinin keşi | `raw.githubusercontent.com` linkləri və ya mətnin yapışdırılması (README, Claude üçün təlimat 7) |

**Növbəti sessiyada ilk iş:** Docker-i açmaq; lokal bazada V1-in tətbiq olunduğunu yoxlamaq (pgAdmin: `tenants`, `users`, `flyway_schema_history`); sonra Addım 4.3.

### 2026-11-09 — Sessiya 4: `identity` entity-ləri və JWT

**Addım 4.3 — Entity-lər:**

1. `az.aqroiz.identity.Role` (açıq API); `internal`: `TenantStatus`, `Tenant`, `UserAccount`, `TenantRepository`, `UserAccountRepository`.
2. Entity qaydaları: setter yoxdur, fabrik metodları (`Tenant.register`, `UserAccount.create`), `@Version Long`, vaxt parametrlə, `Locale.ROOT` ilə e-poçt.
3. Testlər: `TenantTest` (5), `UserAccountTest` (5), `IdentityRepositoryTests` (4, Testcontainers: e-poçt unikallığı, xarici açar, VÖEN unikallığı).
4. `TestcontainersConfiguration` `public` edildi (başqa paketlərdən istifadə üçün).

**Köhnəlmə düzəlişi:** `HttpStatus.UNPROCESSABLE_ENTITY` → `UNPROCESSABLE_CONTENT`, testdə `isUnprocessableContent()`.

**Addım 4.4 — Şifrələmə və JWT:**

1. `application.yaml`: `aqroiz.security.jwt.secret/ttl/issuer`.
2. `JwtProperties` (record, açar ≥ 32 bayt, ttl > 0), `JwtConfig` (`PasswordEncoder`, `SecretKey`, `JwtEncoder`, `JwtDecoder` — bizim `Clock` və issuer yoxlaması ilə), `IssuedToken`, `TokenService`.
3. Testlər: `JwtPropertiesTest` (2), `JwtTokenTests` (4: claim-lər, müddət bitməsi, başqa açar, şifrə heşi).
4. `.\mvnw clean test` → 34 test, BUILD SUCCESS. Postman `health` → 200 (`JwtDecoder`-dən sonra da açıq qaldı).

**Rastlaşılan problemlər:**

| Problem | Səbəb | Həll |
| --- | --- | --- |
| `TestcontainersConfiguration is not public` | Təlimatın 6.1 bəndi icra olunmamışdı | IntelliJ quick fix: Make public |
| `GlobalExceptionHandler.java uses or overrides a deprecated API` | Spring 7-də `UNPROCESSABLE_ENTITY` köhnəlib | `UNPROCESSABLE_CONTENT` |
| Claude köhnəlmiş metodun üstündən xətlə görünəcəyini demişdi | IntelliJ mövzusunda nöqtəli xətlə göstərilirdi | Siçanla üzərinə gəlib mesajı oxumaq |
| GitHub səhifəsi yenə köhnə keş | Claude-un veb aləti | `git status/log/ls-files`, SHA ilə raw linklər və ya ZIP (`git archive`) |

**Növbəti sessiyada ilk iş:** Docker-i açmaq, `git log --oneline -3`, sonra Addım 4.5.

### 2026-11-09 — Sessiya 5: `identity` modulunun tamamlanması

**Addım 4.5 — Təhlükəsizlik konfiqurasiyası:** `SecurityConfig` (açıq endpoint-lər, stateless, CSRF söndürülüb, `role` → `ROLE_*`, `@EnableMethodSecurity`), `ProblemDetailSecurityHandler`, `TOKEN_INVALID` kodu. `GlobalExceptionHandlerTests` sadələşdi (`addFilters = false`). `SecurityIntegrationTests` (7). Postman: `Tokensiz sorğu` (401). Logda `generated security password` yoxdur.

**Addım 4.6 — Giriş:** `AuthService` (eyni cavab, saxta heş, 72 bayt, hesab/şirkət statusu, `last_login_at`), `AuthController` (`POST /api/auth/login`, `GET /api/auth/me`), yeni kodlar (`INVALID_CREDENTIALS`, `ACCOUNT_DISABLED`, `TENANT_SUSPENDED`), `application-local.yaml`, `IdentityDevDataLoader` (4 demo istifadəçi). `AuthIntegrationTests` (10). Postman: `01 Auth` (giriş, mən kiməm, səhv şifrə). `expiresAt` Jackson 3-də ISO formatında gəlir (yoxlanılıb).

**Addım 4.7 — `CurrentUser`:** `shared.security`: `JwtClaims`, `CurrentUser`, `CurrentUserProvider`, `CurrentUserWebConfig` (argument resolver). `TokenService`, `SecurityConfig`, `AuthController` buna keçirildi. `CurrentUserProviderTests` (3) + 1 inteqrasiya testi. Postman-da sübut: qəbulçu tokeni ilə `/me` → Murad Kərimov, `RECEIVER`.

**Nəticə:** 55 test, BUILD SUCCESS.

**Rastlaşılan problemlər:**

| Problem | Səbəb | Həll |
| --- | --- | --- |
| Export iki fayl yaratdı (`AqroIz API.postman_collection.json`) | Postman faylı kolleksiyanın adı ilə saxlayır | Köhnəni silmək, yenini `AqroIz.postman_collection.json` adlandırmaq; IntelliJ-in “not safe to delete” xəbərdarlığında Refactor Anyway |
| `git status`: `deleted` + `untracked` eyni fayl | IntelliJ silməni stage edib, yeni fayl hələ yox | `git add .` → `modified` |
| `GlobalExceptionHandlerTests` 7 test düşdü (“failure threshold exceeded”) | `@WebMvcTest` yeni `AuthController`-i yükləyirdi, `AuthService` isə kontekstdə yox idi | `@WebMvcTest(controllers = TestController.class)` |
| Səhv şifrə ssenarisi əsas `giris` sorğusunda dəyişdirilmişdi | — | Save As ilə ayrıca sorğu, əsas sorğu Don't save |
| Qəbulçu ilə giriş `Giriş — səhv şifrə` sorğusunda edilmişdi, token mühitə yazılmadı | O sorğunun skripti `token`-i yazmır | Rol dəyişikliyi `giris` sorğusu ilə yoxlanıldı |
| `CurrentUser` nümunə kodu (`FarmerController`) layihəyə yazılmalıdırmı? | Nümunə idi | Yalnız gələcək `farmers` modulu üçün |

**Növbəti sessiyada ilk iş:** Docker, `git log --oneline -3`, Addım 5.1.