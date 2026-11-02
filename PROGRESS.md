# PROGRESS

Bu fayl işin gündəliyidir: harada qaldıq, nə hazırdır, növbəti addım nədir. Qaydalar və qərarlar `README.md`-dədir.

---

## Cari vəziyyət

- **Son tamamlanan:** Mərhələ 3 — `shared` modulu (3.1 modul və testlər, 3.2 vaxt idarəsi, 3.3 xəta formatı).
- **Növbəti addım:** Mərhələ 4 — `identity` modulu (tenant, istifadəçilər, rollar, JWT ilə giriş, ilk Flyway miqrasiyası, ilk Postman sorğuları).
- **Tətbiq işə düşür:** Bəli (`Started AqroizBackendApplication`).
- **Testlər:** 14 test (`.\mvnw clean test` → BUILD SUCCESS).
- **Bazada cədvəl:** Yoxdur (Flyway miqrasiyası hələ yoxdur).
- **Yoxlanmalı:** Sessiya 1-in son commit-inin GitHub-a düşdüyü (`git status`, `git log --oneline`, `git ls-files`).

---

## Növbəti addım: `identity` modulu

Kiçik addımlara bölünəcək, hər biri ayrıca təsdiqlə:

1. Postman qərarı (S1) və `postman/` qovluğu.
2. Flyway `V1__identity.sql`: `tenants`, `users` cədvəlləri.
3. Entity-lər və repository-lər + Testcontainers ilə repository testi.
4. Şifrələmə (`PasswordEncoder`) və JWT (imzalama, yoxlama, `JwtProperties`).
5. Öz `SecurityFilterChain`: stateless, JWT, CSRF söndürülür, 401/403 də `ProblemDetail` formatında.
6. `POST /api/auth/login`, `GET /api/auth/me` + Postman.
7. `CurrentUser` (shared) — tenant və rol token-dən.
8. Dev üçün ilkin məlumat (demo tenant və istifadəçilər).

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
| S1 | Postman: lokal kolleksiya faylı, yoxsa Postman Workspace? (Claude-un tövsiyəsi: repoda fayl) | `identity` mərhələsinin ilk addımı |
| S2 | JWT gizli açarı prod-da necə saxlanacaq? | Deploy-dan əvvəl |
| S3 | SMS provayderi hansı olacaq (yerli və ya WhatsApp Business API)? | `notifications` mərhələsi |
| S4 | Şəxsi məlumatların Azərbaycanda saxlanma tələbi (server yeri) | Deploy-dan əvvəl, hüquqşünasla |
| S5 | Repo nə vaxt private ediləcək? | İlk pilot müştəridən əvvəl |

## Texniki borc və qeydlər

| # | Qeyd | Nə vaxt |
| --- | --- | --- |
| T1 | `Using generated security password` — Spring Security-nin standart konfiqurasiyası | `identity`-də öz konfiqurasiyamızla yox olacaq |
| T2 | Mockito “self-attaching” xəbərdarlığı (gələcək JDK-larda qadağan) | Mockito istifadəsinə başlayanda `pom.xml`-də agent kimi qoşulacaq |
| T3 | Maven-in `flyway-community-db-support ... 401` xəbərdarlığı | Zərərsiz, toxunmuruq |
| T4 | Boş `static` və `templates` qovluqları | İstəyə bağlı silinə bilər |
| T5 | `GlobalExceptionHandlerTests`-də `@WithMockUser` və `csrf()` | `identity`-dən sonra sadələşəcək |

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