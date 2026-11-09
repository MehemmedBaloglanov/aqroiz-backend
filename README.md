# AqroIz Backend

Meyvə-tərəvəz ixracçıları və topdayıcılar üçün tedarük, partiya izlənməsi, yük sənədləri və fermer ödənişləri sistemi.

Bu fayl layihənin **qaydalar kitabıdır**. Burada yazılan hər qərarın səbəbi var. Qərarı dəyişmək olar, amma əvvəl bu faylda dəyişdirilməli və “Qərarlar jurnalı” bölməsinə yeni sətir əlavə edilməlidir.

Gündəlik iş vəziyyəti `PROGRESS.md` faylındadır.

---

## 1. Texnologiya steki

| Komponent | Versiya | Qeyd |
| --- | --- | --- |
| Java | 21 | Microsoft OpenJDK 21 |
| Spring Boot | 4.1.2 | 3.5 xətti 30.06.2026-da açıq mənbə dəstəyini itirib, ona görə 4.x |
| Spring Framework | 7.0.x | Spring Boot 4 ilə gəlir |
| Spring Security | 7.1.x | |
| Spring Modulith | 2.1.1 | Yalnız `starter-core` və `starter-test` |
| Hibernate ORM | 7.4.x | |
| Flyway | 12.x | |
| PostgreSQL | 17 (Docker, `postgres:17-alpine`) | |
| Jackson | 3.x | Paket adı `tools.jackson`, annotasiyalar hələ `com.fasterxml.jackson.annotation` |
| Build | Maven (Maven Wrapper ilə: `mvnw`) | |
| Frontend | Ayrı repo: `aqroiz-frontend` (React + Vite, sonra) | |

### Spring Boot 4 üzrə vacib fərqlər (3.x ilə qarışdırmamaq üçün)

- Starter-lər modullara bölünüb: `spring-boot-starter-webmvc` (əvvəlki `-web`), `spring-boot-starter-flyway` (Flyway üçün mütləqdir, yalnız `flyway-core` kifayət deyil), `spring-boot-starter-security-oauth2-resource-server`.
- Hər modulun öz test starter-i var: `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test` və s.
- Testcontainers artefaktlarının adı dəyişib: `testcontainers-postgresql`, `testcontainers-junit-jupiter`.
- `PostgreSQLContainer` yeni paketdədir və generic deyil: `org.testcontainers.postgresql.PostgreSQLContainer` (köhnə: `org.testcontainers.containers.PostgreSQLContainer<?>`).
- Jackson 3: `ObjectMapper` əvəzinə `JsonMapper` (paket `tools.jackson.databind`). `ProblemDetail`-in əlavə sahələri (`code`, `timestamp`) JSON-da yuxarı səviyyədə yazılır — yoxlanılıb.
- `@WebMvcTest` paketi: `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` — yoxlanılıb.
- 422 statusu: `HttpStatus.UNPROCESSABLE_CONTENT` (köhnə `UNPROCESSABLE_ENTITY` Spring 7-də deprecated); testdə `status().isUnprocessableContent()`.
- Köhnəlmiş API-ləri tapmaq: `.\mvnw clean test-compile "-Dmaven.compiler.showDeprecation=true" | Select-String "deprecat"` — yalnız `[debug deprecation parameters ...]` sətirləri çıxmalıdır, `[WARNING]` yox.
- IntelliJ-də köhnəlmiş metod mövzudan asılı olaraq üstündən xətlə **və ya nöqtəli xətlə** göstərilə bilər — həmişə siçanı üzərinə gətirib mesajı oxuyun.
- Avtokonfiqurasiya sinifləri yeni paketlərdədir (məsələn, `org.springframework.boot.flyway.autoconfigure`).
- Spring Boot 3.x üçün yazılmış nümunə kodu birbaşa köçürməzdən əvvəl yoxlayın.

---

## 2. Lokal mühit

### Tələblər

- JDK 21
- Docker Desktop — **həmişə açıq olmalıdır** (lokal baza və Testcontainers testləri üçün). Settings → General → “Start Docker Desktop when you sign in”
- IntelliJ IDEA
- Git
- pgAdmin 4 (bazaya baxmaq üçün, istəyə görə)

### Bilinən problem: yerli PostgreSQL servisi

Kompüterdə Windows üçün quraşdırılmış PostgreSQL (`postgresql-x64-18` servisi) 5432 portunu tutur və tətbiq Docker-dəki baza əvəzinə ona qoşulur (`password authentication failed for user "aqroiz"` xətası). Həll: servis dayandırılıb və əl ilə rejimə keçirilib.

```cmd
:: Administrator kimi
net stop postgresql-x64-18
sc config postgresql-x64-18 start= demand
```

Yoxlama: `netstat -ano | findstr :5432` yalnız Docker prosesini (`com.docker.backend.exe`) göstərməlidir.

### Bazanı qaldırmaq

```powershell
docker compose up -d
docker compose ps        # STATUS: healthy
```

| Parametr | Dəyər (yalnız lokal) |
| --- | --- |
| Host | `localhost` |
| Port | `5432` |
| Baza | `aqroiz` |
| İstifadəçi | `aqroiz` |
| Şifrə | `aqroiz` |

Bazanı sıfırdan yaratmaq (bütün məlumat silinir):

```powershell
docker compose down -v
docker compose up -d
```

PostgreSQL şifrəni yalnız volume ilk dəfə yaradılanda oxuyur. `compose.yaml`-da şifrəni dəyişsəniz, `down -v` etmədən yeni şifrə işləməyəcək.

### Tətbiqi işə salmaq

IntelliJ-də `AqroizBackendApplication` və ya:

```powershell
./mvnw spring-boot:run
```

Uğur: logda `Started AqroizBackendApplication`.

### `local` profili və demo hesablar

IntelliJ: Run Configuration → **Active profiles: `local`** (və ya VM options: `-Dspring.profiles.active=local`). Logda: `The following 1 profile is active: "local"`.

`application-local.yaml` → `aqroiz.dev-data.enabled: true`. Hər modulun öz demo loader-i var (`IdentityDevDataLoader` və s.), bazada məlumat varsa heç nə etmir. Demo məlumat **heç vaxt** Flyway miqrasiyası ilə yazılmır (miqrasiyalar prod-da da işləyir).

| E-poçt | Rol | Şifrə |
| --- | --- | --- |
| `rehber@demo.aqroiz.az` | OWNER | `Demo-12345` |
| `menecer@demo.aqroiz.az` | MANAGER | `Demo-12345` |
| `qebulcu@demo.aqroiz.az` | RECEIVER | `Demo-12345` |
| `muhasib@demo.aqroiz.az` | ACCOUNTANT | `Demo-12345` |

Şirkət: Demo Meyvə İxrac MMC. Testlər və prod `local` profilini aktivləşdirmir.

Layihə əmrləri (`.\mvnw ...`, `git ...`, `docker compose ...`) **yalnız IntelliJ terminalında**, layihə qovluğunda icra olunur. Administrator pəncərəsi yalnız Windows servisləri üçündür.

Testlər: `.\mvnw test`. Fayl adı dəyişəndə və ya resurs silinəndə: `.\mvnw clean test`.

### Konfiqurasiya və mühit dəyişənləri

`application.yaml` lokal dəyərləri standart kimi saxlayır, prod-da mühit dəyişənləri ilə əvəz olunur:

| Dəyişən | Standart | Mənası |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/aqroiz` | Baza ünvanı |
| `DB_USER` | `aqroiz` | Baza istifadəçisi |
| `DB_PASSWORD` | `aqroiz` | Baza şifrəsi |

Tətbiqin öz parametrləri (`aqroiz.*`):

| Parametr | Standart | Mənası |
| --- | --- | --- |
| `aqroiz.time.business-zone` | `Asia/Baku` | Biznes tarixlərinin hesablandığı zona. Səhv ad yazılsa, tətbiq işə düşmür |
| `aqroiz.security.jwt.secret` | `${AQROIZ_JWT_SECRET:dev-only-...}` | JWT imza açarı, ən azı 32 bayt. Repodakı dəyər **yalnız lokal** üçündür; prod-da mühit dəyişəni ilə |
| `aqroiz.security.jwt.ttl` | `PT12H` | Tokenin ömrü |
| `aqroiz.security.jwt.issuer` | `aqroiz` | Tokenin `iss` claim-i; başqa issuer-in tokeni rədd edilir |

Gizli dəyərlər (`.env`) heç vaxt repoya düşmür. Repo hazırda **public**-dir.

---

## 3. Arxitektura

### Modular monolit

Bir Spring Boot tətbiqi, bir baza, daxildə aydın sərhədli modullar. Mikroservislər **bilərəkdən** seçilməyib: MVP mərhələsində əlavə mürəkkəblik, vaxt və server xərci yaradır, müştəriyə görünən fayda vermir. Modullar elə qurulur ki, lazım gələrsə gələcəkdə ayrıca servisə çıxarıla bilsin.

Spring Modulith sərhədləri testlə qoruyur: `ApplicationModules.of(AqroizBackendApplication.class).verify()`.

### Paket quruluşu

Əsas paket: **`az.aqroiz`**. Hər modul onun birbaşa alt paketidir.

```
az.aqroiz
├── AqroizBackendApplication.java
├── shared/            ← ümumi infrastruktur (açıq modul)
├── identity/          ← tenant, istifadəçilər, rollar, giriş (JWT)
├── catalog/           ← məhsullar, sortlar, gündəlik qiymətlər
├── farmers/           ← fermer reyestri
├── procurement/       ← qəbul qəbzləri
├── lots/              ← partiyalar
├── shipments/         ← yüklər
├── settlements/       ← avans, ödəniş, fermer balansı
├── documents/         ← PDF, Excel, CSV, e-qaimə
├── notifications/     ← SMS/WhatsApp
└── reports/           ← hesabatlar
```

### Modul daxili qayda

```
az.aqroiz.farmers
├── FarmerService.java      ← API: digər modullar YALNIZ bunlardan istifadə edir
├── FarmerView.java         ← API: digər modullara qaytarılan məlumat (record)
├── FarmerData.java         ← API: daxil olan məlumat (record)
└── internal/               ← modulun daxili hissəsi, başqa modul toxunmur
    ├── Farmer.java         (JPA entity)
    ├── FarmerRepository.java
    └── FarmerController.java
```

- Modulun kök paketindəki `public` tiplər onun **API**-sidir.
- `internal` alt paketi başqa modullar üçün qadağandır (Modulith testi pozuntunu tutur).
- Bir modul başqa modulun entity-sini və repository-sini heç vaxt birbaşa istifadə etmir.
- Modullar arası dövri asılılıq qadağandır.
- Modullar arası əlaqə: sinxron üçün başqa modulun servisi, asinxron üçün tətbiq hadisəsi (event).

---

## 4. Kod qaydaları

### Ümumi

- **Lombok istifadə olunmur.** DTO-lar üçün Java `record`, entity-lər üçün əl ilə yazılmış getter-lər.
- **Constructor injection.** `@Autowired` sahələrə yazılmır.
- Kodda identifikatorlar (sinif, metod, dəyişən, cədvəl adları) **ingiliscədir**. İstifadəçiyə gedən mesajlar (xətalar, validasiya) **Azərbaycan dilindədir**. Şərhlər Azərbaycan dilində ola bilər.
- Faylların sətir sonu **LF**-dir (`.gitattributes`). IntelliJ: Settings → Editor → Code Style → Line separator → Unix and macOS (\n).

### Entity-lər

**Qaydanın yeri:** obyektin yalnız öz məlumatına baxan qayda → **entity**-də (e-poçtun normallaşdırılması, adın uzunluğu, status keçidləri). Bazaya, başqa obyektlərə və ya Spring bean-lərinə ehtiyac duyan qayda → **servis**-də (unikallıq yoxlaması, şifrə heşləmə, cari vaxt, icazələr, tranzaksiya, hadisələr). Entity-lər JPA annotasiyaları ilə birlikdə domen obyektidir (ayrıca JPA modeli yoxdur — MVP üçün bilərəkdən seçilib).

- ID: `UUID`, tətbiqdə yaradılır (`UUID.randomUUID()`), bazadan gözlənilmir.
- JPA üçün `protected` boş konstruktor; yaradılma statik fabrik metodu və ya mənalı konstruktorla.
- `public` setter yoxdur. Vəziyyət yalnız biznes metodları ilə dəyişir (`confirm()`, `reverse(...)`).
- Biznes qaydası entity-nin içində yoxlanır və pozulanda `BusinessRuleException` atılır.
- Paralel dəyişikliklər üçün `@Version` (optimistic locking). Tip **`Long`** (`long` yox): ID-ni tətbiq yaratdığı üçün Spring Data yeni obyekti versiyanın `null` olmasından tanıyır; `long` olsa, `persist` əvəzinə `merge` çağırılar (lazımsız `SELECT` və başqa obyekt qaytarılır).
- Vaxt entity-yə parametr kimi ötürülür (`Instant now`); entity `Clock`-u özü çağırmır.
- `equals`/`hashCode` ID-yə görə, digər obyektin ID-si **getter ilə** (`other.getId()`) — Hibernate proxy-lərinə görə.
- Başqa aqreqata istinad sadə `UUID` sahəsi ilə (`tenantId`), `@ManyToOne` yox; əlaqəni bazada xarici açar qoruyur.
- Mətnləri kiçiltmək/böyütmək həmişə `Locale.ROOT` ilə: Azərbaycan/Türk dilində `"I".toLowerCase()` → `"ı"`.
- Biznes qaydası pozulanda `BusinessRuleException` (422); yalnız proqramçı səhvində mümkün olan hallarda `IllegalArgumentException`/`NullPointerException` (500).
- Hər biznes cədvəlində `tenant_id` var. Hər sorğu `tenant_id` ilə filtrlənir.

### Rəqəmlər

- Pul və çəki: **`BigDecimal`**, heç vaxt `double`/`float`.
- Pul: 2 onluq, `RoundingMode.HALF_UP`.
- Kq üzrə qiymət: 4 onluq.
- Çəki: kq, bazada `numeric(12,2)`.
- Valyuta: AZN (hələlik tək valyuta).

### Vaxt

- Bazada vaxt **UTC**-də saxlanılır (`timestamptz`, Java-da `Instant`).
- Biznes tarixləri (qiymət günü, qəbul günü) **Asia/Baku** zonasında hesablanır (`LocalDate`).
- Cari vaxt həmişə inject olunan `Clock` vasitəsilə alınır, `Instant.now()`, `LocalDate.now()` birbaşa çağırılmır (testlərdə vaxtı idarə etmək üçün). `clock.instant()` → UTC an, `LocalDate.now(clock)` → Bakı günü.
- `Clock` bean-i: `az.aqroiz.shared.TimeConfig`, parametr: `TimeProperties` (`aqroiz.time.business-zone`).
- Hibernate bazaya vaxtı UTC ilə yazır: `spring.jpa.properties.hibernate.jdbc.time_zone: UTC`.
- Səbəb: UTC 21:30 (1 noyabr) = Bakı 01:30 (2 noyabr). Səhv zona qəbzə səhv günün qiymətini tətbiq edər.

### Xətalar

Bütün xəta sinifləri: `az.aqroiz.shared.error`.

| İstisna / vəziyyət | HTTP | `code` |
| --- | --- | --- |
| `NotFoundException` | 404 | `NOT_FOUND` |
| `BusinessRuleException(code, message)` | 422 | modulun öz kodu (məs., `NET_NOT_POSITIVE`) |
| `@Valid` validasiya xətası | 400 | `VALIDATION_FAILED` + `errors[]` |
| Oxunmayan JSON | 400 | `MALFORMED_REQUEST` |
| Parametrin tipi səhvdir (`/x/abc` əvəzinə rəqəm) | 400 | `TYPE_MISMATCH` |
| Token yoxdur (`AuthenticationException`) | 401 | `UNAUTHORIZED` |
| Token saxtadır və ya müddəti bitib | 401 | `TOKEN_INVALID` |
| Girişdə səhv e-poçt və ya şifrə (`BadCredentialsException`) | 401 | `INVALID_CREDENTIALS` |
| İstifadəçi hesabı bağlanıb (`DisabledException`) | 401 | `ACCOUNT_DISABLED` |
| Şirkətin girişi dayandırılıb (`LockedException`) | 401 | `TENANT_SUSPENDED` |
| `AccessDeniedException` | 403 | `FORBIDDEN` |
| Spring MVC-nin digər xətaları (405, 415...) | müvafiq | `HTTP_{status}` |
| Gözlənilməz xəta | 500 | `INTERNAL_ERROR` — detal yalnız logda, cavabda yox |

Cavab formatı (RFC 9457 `ProblemDetail`):

```json
{
  "type": "about:blank",
  "title": "Yanlış sorğu",
  "status": 400,
  "detail": "Bəzi sahələr düzgün doldurulmayıb.",
  "instance": "/api/farmers",
  "code": "VALIDATION_FAILED",
  "timestamp": "2026-11-02T16:20:00Z",
  "errors": [ { "field": "fullName", "message": "boş ola bilməz" } ]
}
```

- 400 — sorğu formaca səhvdir; 422 — formaca düzgündür, biznes qaydası qəbul etmir.
- Başqa tenant-ın obyektinə müraciət 404 qaytarır (403 yox) — obyektin mövcudluğu açıqlanmır.
- Frontend qərarı mətnə yox, `code`-a görə verir.
- Validasiya mesajları Azərbaycan dilində: `src/main/resources/ValidationMessages.properties`. Ad **dəqiq** belə olmalıdır (sonda `s`), əks halda Hibernate Validator faylı görmür və ingiliscə mesaj qaytarır. Fayl UTF-8-dir (IntelliJ: Settings → Editor → File Encodings → properties: UTF-8, native-to-ascii söndürülüb).

---

## 5. API qaydaları

- Bütün endpoint-lər `/api/...` ilə başlayır.
- Resurs adları cəm halında: `/api/farmers`, `/api/receipts`.
- Əməliyyatlar alt yol ilə: `POST /api/receipts/{id}/reverse`.
- Tarixlər ISO-8601: `2026-11-01`, `2026-11-01T10:15:30Z`.
- Siyahılar səhifələnir (`page`, `size`).
- Oflayn sahə tətbiqindən gələn yaratma sorğuları idempotentdir (`clientId`).
- Hər yeni və ya dəyişən endpoint `postman/` qovluğundakı kolleksiyaya əlavə edilir. Frontend API-ni oradan öyrənir.

### Endpoint-lər

| Metod | Ünvan | Giriş | Rol | Modul |
| --- | --- | --- | --- | --- |
| `GET` | `/actuator/health` | Açıq | — | Spring |
| `POST` | `/api/auth/login` | Açıq | — | identity |
| `GET` | `/api/auth/me` | JWT | hamısı | identity |

### Postman

| Fayl | Məzmun |
| --- | --- |
| `postman/AqroIz.postman_collection.json` | Bütün sorğular, qovluqlar üzrə (`00 Sistem`, `01 Auth`, ...) |
| `postman/AqroIz-local.postman_environment.json` | `baseUrl` = `http://localhost:8080`, `token` (boş, `secret`) |

- Kolleksiya səviyyəsində `Bearer {{token}}`; giriş tələb etməyən sorğularda `noauth`.
- Hər sorğunun `pm.test` yoxlaması var.
- Postman dəyişiklikləri fayla özü yazmır: kolleksiya → ⋯ → **More → Export collection → Other** → Collection v2.1 → adı dəqiq `AqroIz.postman_collection.json` (Postman `AqroIz API...` təklif edir) → fayl üzərinə yazılır → commit.
- `01 Auth → giris` sorğusu tokeni `{{token}}` mühit dəyişəninə yazır; qalan sorğular onu kolleksiyadan miras alır. Əvvəl həmişə login.
- Sınaq üçün sorğunu dəyişəndə tabı **Don't save** ilə bağlayın; yeni ssenari üçün **Save As** ilə ayrıca sorğu yaradın.
- Mühit faylına heç vaxt real şifrə və token yazılmır.
- `/actuator/health` Spring Boot tərəfindən standart olaraq açıqdır (giriş tələb etmir).

---

## 6. Verilənlər bazası qaydaları

- Sxemi **yalnız Flyway** dəyişir. `ddl-auto: validate` — Hibernate yalnız uyğunluğu yoxlayır.
- Miqrasiyalar: `src/main/resources/db/migration/V{nömrə}__{izah}.sql` (böyük `V`, **iki** alt xətt). Hər modul öz miqrasiyasını yazır və öz cədvəllərinə cavabdehdir.
- **Push olunmuş miqrasiya faylı heç vaxt dəyişdirilmir.** Düzəliş yeni miqrasiya ilə edilir. İstisna: fayl hələ push olunmayıbsa, düzəldib lokal bazanı sıfırlamaq olar (`docker compose down -v`).
- ID-lər `uuid`, bazada `default` yoxdur — tətbiq yaradır (oflayn sahə tətbiqi üçün vacibdir).
- `created_at`/`updated_at` üçün `default now()` yoxdur — vaxtı tətbiq `Clock` ilə yazır.
- Siyahı tipli dəyərlər (rol, status): `varchar` + `check`, PostgreSQL `enum` yox (dəyişdirmək asandır). Java-da `enum`.
- Məhdudiyyət adları: `pk_`, `fk_`, `ck_`, `uq_`, `ix_` prefiksləri ilə — xəta mesajında hansı qaydanın pozulduğu görünsün.
- Cədvəl və vacib sütunlar üçün `comment on` (Azərbaycan dilində) — pgAdmin-də görünür.
- Silmək əvəzinə söndürmək (`active = false`), tarixçə qorunur.

### Miqrasiyalar

| Versiya | Fayl | Məzmun |
| --- | --- | --- |
| V1 | `V1__identity.sql` | `tenants`, `users` |
- Cədvəl və sütun adları `snake_case`, cədvəllər cəm halında: `purchase_receipts`.
- Məhdudiyyətlər bazada da yazılır: `not null`, `check`, `unique`, `foreign key`.
- pgAdmin yalnız **baxmaq** üçündür. Bazada əl ilə cədvəl yaratmaq, dəyişmək, silmək qadağandır.

---

## 7. Biznes qaydaları (domen invariantları)

Bu qaydalar kodda və testlərdə qorunmalıdır. Status sütunu hansının artıq həyata keçirildiyini göstərir.

| # | Qayda | Status |
| --- | --- | --- |
| B1 | Tara = yeşik sayı × yeşik başına tara (tenant ayarı), tam kq-a yuvarlaqlaşdırılır | Planlaşdırılıb |
| B2 | Net çəki = brüt − tara, sıfırdan böyük olmalıdır | Planlaşdırılıb |
| B3 | Qəbzin qiyməti qəbul anında gündəlik qiymətdən götürülür və qəbzdə dondurulur | Planlaşdırılıb |
| B4 | Təsdiqlənmiş qəbz dəyişdirilmir; səhv olarsa səbəb yazılaraq ləğv edilir | Planlaşdırılıb |
| B5 | Partiyaya salınmış qəbz ləğv edilə bilməz | Planlaşdırılıb |
| B6 | Bir partiyada yalnız eyni məhsul və sort olur | Planlaşdırılıb |
| B7 | Partiyanın çəkisi ona daxil olan qəbzlərin net çəkilərinin cəminə bərabərdir | Planlaşdırılıb |
| B8 | Fermer balansı = təsdiqlənmiş qəbzlərin məbləği − avanslar − ödənişlər | Planlaşdırılıb |
| B9 | Fermer razılığı olmadan qeydiyyata alınmır | Planlaşdırılıb |
| B10 | Qəbz nömrələri tenant daxilində ardıcıl və boşluqsuzdur (Q-000001) | Planlaşdırılıb |
| B11 | Hər tenant yalnız öz məlumatını görür | Planlaşdırılıb |

### Təhlükəsizlik

- Şifrələr `PasswordEncoderFactories.createDelegatingPasswordEncoder()` ilə heşlənir (`{bcrypt}...` formatı); eyni şifrə hər dəfə fərqli heş verir. Şifrənin özü heç vaxt saxlanılmır və loga yazılmır.
- Giriş JWT ilə, HS256. Claim-lər: `iss`, `sub` (istifadəçi ID), `tenantId`, `role`, `iat`, `exp`. Tokenə şifrə, e-poçt kimi məlumat yazılmır (məzmun şifrələnmir).
- Token yoxlaması: imza, bitmə vaxtı (bizim `Clock` ilə, 30 san. tolerantlıq), issuer.
- Bean-lər: `az.aqroiz.identity.internal.JwtConfig`; token yaradan: `TokenService`. Claim adları: `az.aqroiz.shared.security.JwtClaims`.

**Təhlükəsizlik qaydaları** (`identity.internal.SecurityConfig`):

| Ünvan | Giriş |
| --- | --- |
| `GET /actuator/health`, `/actuator/health/**` | Açıq |
| `POST /api/auth/login` | Açıq |
| `/error` | Açıq (əks halda əsl xəta 401 ilə gizlənər) |
| Qalan hər şey | JWT tələb olunur |

- Stateless (sessiya yoxdur), login forması, Basic auth, logout söndürülüb.
- **CSRF söndürülüb**, çünki token cookie-də yox, `Authorization` başlığındadır. Token gələcəkdə cookie-yə köçürülsə, CSRF **mütləq** qaytarılmalıdır.
- `role` claim-i → `ROLE_<ROL>` icazəsi; metodlarda `@PreAuthorize("hasRole('OWNER')")` (`@EnableMethodSecurity`).
- Filtr səviyyəsindəki 401/403 `ProblemDetailSecurityHandler` vasitəsilə `GlobalExceptionHandler`-ə ötürülür — format bir yerdədir. 401-də `WWW-Authenticate: Bearer` başlığı.

**Giriş qaydaları** (`AuthService`):

- Mövcud olmayan e-poçt və səhv şifrə **eyni** cavabı alır (“E-poçt və ya şifrə yanlışdır.”).
- E-poçt tapılmayanda da saxta heşlə yoxlama aparılır — cavab vaxtı e-poçtun mövcudluğunu açıqlamasın.
- Bağlanmış hesab və dayandırılmış şirkət yalnız şifrə düzgün olandan sonra bildirilir.
- 72 baytdan uzun şifrə dərhal rədd edilir (bcrypt yalnız 72 baytı nəzərə alır).
- Uğurlu girişdə `last_login_at` yazılır (dirty checking, `save()` yoxdur).

**Cari istifadəçi və tenant** (`az.aqroiz.shared.security`):

- Controller metodunda parametr: `CurrentUser currentUser` (`userId`, `tenantId`, `role`).
- **`tenantId` yalnız `CurrentUser`-dən götürülür — heç vaxt URL-dən, sorğu parametrindən və ya JSON-dan.**
- Servislər `CurrentUser`-i özləri oxumur; controller `tenantId`-ni parametr kimi ötürür.
- `CurrentUser.role` `String`-dir (`shared` → `identity` asılılığı dövri asılılıq yaradardı).

### Rollar

| Rol | İcazə |
| --- | --- |
| `OWNER` | Hər şey |
| `MANAGER` | Qəbul, partiya, yük, qiymət, ləğv |
| `RECEIVER` | Sahədə qəbul, fermer qeydiyyatı |
| `ACCOUNTANT` | Ödənişlər, hesabatlar, sənədlər |
| `VIEWER` | Yalnız baxış |

---

## 8. Testlər

- Domen qaydaları: adi unit testlər (Spring konteksti olmadan).
- Repository və API: Testcontainers ilə real PostgreSQL.
- Modul sərhədləri: Spring Modulith `verify()` testi (`ModularityTests`).
- Tətbiqin bütövlükdə işə düşməsi: `AqroizBackendApplicationTests` (Testcontainers bazası ilə).
- Testcontainers PostgreSQL imici `compose.yaml`-dakı ilə **eyni** olmalıdır (`TestcontainersConfiguration.POSTGRES_IMAGE`). Versiya dəyişəndə hər ikisi birlikdə dəyişdirilir.
- Lokal inkişaf üçün yeganə işə salma yolu: `compose.yaml` + `AqroizBackendApplication`. Testlər isə həmişə Testcontainers ilə işləyir və lokal bazaya toxunmur.
- Bütün testlər: `.\mvnw test` (IntelliJ terminalında, layihə qovluğunda).
- **`@WebMvcTest` həmişə `controllers = ...` ilə** yazılır: əks halda bütün controller-ləri yükləyir, onların servisləri isə kontekstdə olmur.
- `@WebMvcTest` bütün `WebMvcConfigurer`-ləri yükləyir, adi `@Component`-ləri yükləmir — `WebMvcConfigurer`-lərə bean inject etməyin.
- Yalnız xəta formatını yoxlayan testlərdə təhlükəsizlik filtrləri söndürülür: `@AutoConfigureMockMvc(addFilters = false)`.
- Təhlükəsizlik testləri: `@SpringBootTest` + `@AutoConfigureMockMvc` + real JWT (`TokenService`).
- `SecurityContextHolder`-ə toxunan testlərdə `@AfterEach SecurityContextHolder.clearContext()`.
- `@Transactional` test + `MockMvc`: sorğu test tranzaksiyası daxilində işləyir, sonda geri qaytarılır.
- Boot 4 paketləri: `org.springframework.boot.webmvc.test.autoconfigure.{WebMvcTest, AutoConfigureMockMvc}` — yoxlanılıb.
- Hər biznes qaydası (B1–B11) ən azı bir testlə qorunur.

---

## 9. Git

- Əsas branch: **`main`**.
- Commit mesajları: [Conventional Commits](https://www.conventionalcommits.org/).
  - `feat:` yeni funksiya
  - `fix:` səhv düzəlişi
  - `refactor:` davranışı dəyişməyən dəyişiklik
  - `test:` testlər
  - `docs:` sənədlər
  - `chore:` texniki iş (asılılıq, konfiqurasiya)
- Hər commit-dən əvvəl tətbiq işə düşməli və testlər keçməlidir.

---

## 10. Backend yol xəritəsi

| # | Mərhələ | Status |
| --- | --- | --- |
| 1 | Layihə skeleti, Docker PostgreSQL, Git | ✅ Hazır |
| 2 | README.md, PROGRESS.md | ✅ Hazır |
| 3 | `shared`: modul, `Clock`, xəta formatı | ✅ Hazır (`CurrentUser` və `NumberingService` `identity`-dən sonra) |
| 4 | `identity`: tenant, istifadəçilər, rollar, JWT ilə giriş, `CurrentUser` | ✅ Hazır |
| 5 | `catalog`: məhsullar, sortlar, gündəlik qiymətlər | 🔄 Növbəti |
| 6 | `farmers`: fermer reyestri | ⏳ |
| 7 | `procurement`: qəbul, ləğv, idempotent sahə sorğuları | ⏳ |
| 8 | `lots`: partiyalar | ⏳ |
| 9 | `shipments`: yüklər | ⏳ |
| 10 | `settlements`: avans, ödəniş, balans | ⏳ |
| 11 | `documents`: PDF, CSV | ⏳ |
| 12 | `notifications`: SMS (Modulith hadisələri + `starter-jpa`) | ⏳ |
| 13 | `reports`: marja, rayonlar, sortlar | ⏳ |
| 14 | OpenAPI/Swagger, Postman kolleksiyasının tamamlanması | ⏳ |

---

## 11. Qərarlar jurnalı

| Tarix | Qərar | Səbəb |
| --- | --- | --- |
| 2026-11-01 | Modular monolit, mikroservis yox | MVP üçün sürət və sadəlik; modullar sonra ayrıla bilər |
| 2026-11-01 | Backend və frontend ayrı repolarda | Müstəqil build və deploy |
| 2026-11-01 | Əvvəl backend tam, Postman ilə test, sonra frontend | İstifadəçinin seçimi |
| 2026-11-01 | Spring Boot 4.1.2 | 3.5 açıq mənbə dəstəyini itirib |
| 2026-11-01 | Java 21 | Kompüterdəki JDK; LTS |
| 2026-11-01 | Paket `az.aqroiz` | Qısa və oxunaqlı |
| 2026-11-01 | Lombok yoxdur | Gizli kod, JPA ilə `@Data` riskləri |
| 2026-11-01 | Modulith-dən yalnız `starter-core` və `starter-test` | `starter-jpa` `event_publication` cədvəli tələb edir; hadisələrə keçəndə miqrasiya ilə əlavə olunacaq |
| 2026-11-01 | PostgreSQL 17 Docker-də, port 5432 | 18-in Docker volume quruluşu fərqlidir; yerli PG 18 servisi dayandırıldı |
| 2026-11-01 | `ddl-auto: validate`, sxem yalnız Flyway ilə | Prod bazasının təsadüfən dəyişməsinin qarşısını almaq |
| 2026-11-01 | `open-in-view: false` | Gizli lazy loading və performans problemlərinin qarşısı |
| 2026-11-01 | YAML konfiqurasiya | İç-içə parametrlər üçün oxunaqlı |
| 2026-11-01 | Sətir sonu LF | Linux server; Windows-da işləsək də repoda vahid format |
| 2026-11-01 | `shared` modulu `OPEN` tipdədir | İnfrastruktur hamıya açıqdır; biznes modulları heç vaxt `OPEN` olmur |
| 2026-11-01 | `TestAqroizBackendApplication` silindi | Lokal işə salmanın iki yolu fərqli bazalara qoşulub çaşqınlıq yaradardı |
| 2026-11-01 | Testlərdə `postgres:17-alpine`, `latest` yox | Test və real mühit eyni versiyada olmalıdır |
| 2026-11-02 | Vaxt üçün yeganə mənbə inject olunan `Clock` (Asia/Baku), bazada UTC | Qəbzin günü və qiyməti düzgün hesablansın, testlərdə vaxt idarə olunsun |
| 2026-11-02 | Xətalar RFC 9457 `ProblemDetail` + `code` + `timestamp` | Standart format; frontend koda görə qərar verir |
| 2026-11-02 | 422 biznes qaydası üçün, 400 format xətası üçün | Frontend üçün fərqli davranış |
| 2026-11-02 | `GlobalExceptionHandler` `ResponseEntityExceptionHandler`-dən miras alır | Spring MVC-nin bütün xətaları da eyni formatda |
| 2026-11-02 | 500-də daxili detal cavaba düşmür | Təhlükəsizlik |
| 2026-11-03 | Postman kolleksiyası repoda fayl kimi | API dəyişiklikləri kodla birlikdə commit olunur; Claude da görə bilir |
| 2026-11-03 | E-poçt bütün sistemdə unikal, həmişə kiçik hərflə | Girişdə şirkət seçilmir; `Ali@` və `ali@` iki istifadəçi olmasın |
| 2026-11-03 | Bir istifadəçi = bir tenant = bir rol | MVP üçün sadəlik |
| 2026-11-03 | Tenant ayarları (məs., yeşik tarası) öz modullarında | Hər modul öz cədvəllərinə cavabdehdir |
| 2026-11-09 | Domen qaydaları entity-də, infrastruktur tələb edənlər servisdə; JPA annotasiyaları domen sinfində | DDD-nin əsas faydası, iki qat kod olmadan |
| 2026-11-09 | İstifadəçi entity-si `UserAccount` (`User` yox) | Spring Security-nin `User` sinfi ilə ad toqquşmasının qarşısı |
| 2026-11-09 | JWT HS256, ömür 12 saat, refresh token sonra | Tokeni verən və yoxlayan eyni tətbiqdir; qəbulçu iş günü ərzində təkrar girmir |
| 2026-11-09 | JWT açarı < 32 bayt olsa tətbiq işə düşmür | HS256 standartı; səhv konfiqurasiya erkən aşkarlansın |
| 2026-11-09 | Öz `SecurityFilterChain`; standart olaraq hər şey bağlı | Açıq endpoint-lər açıq şəkildə sadalanır |
| 2026-11-09 | CSRF söndürülüb | Token başlıqdadır, cookie yoxdur |
| 2026-11-09 | 401/403 `HandlerExceptionResolver` vasitəsilə `GlobalExceptionHandler`-ə | Vahid format, kod təkrarı yoxdur |
| 2026-11-09 | Login-də səhv e-poçt və səhv şifrə eyni cavab + saxta heş | E-poçt sadalama hücumunun qarşısı |
| 2026-11-09 | Demo məlumat `local` profili və modul loader-ləri ilə, miqrasiya ilə yox | Demo şifrəli istifadəçilər prod-a düşməsin |
| 2026-11-09 | `tenantId` yalnız tokendən (`CurrentUser`) | Bir şirkətin başqa şirkətin məlumatını görməsinin qarşısı |
| 2026-11-01 | Repo public | Claude-un linkdən yoxlaya bilməsi; gizli dəyərlər və müştəri məlumatları heç vaxt repoya düşmür |

---

## 12. Claude üçün təlimat

Bu layihə Claude ilə birlikdə, addım-addım yazılır. Claude kodu söhbətdə yazır, istifadəçi onu IntelliJ-də yaradır, işə salır və nəticəni göndərir. Hər yeni sessiyada Claude:

1. Əvvəlcə bu `README.md`-ni, sonra `PROGRESS.md`-ni oxuyur.
2. Burada yazılmış qərarlara zidd kod yazmır. Qərarı dəyişmək lazımdırsa, əvvəl səbəbini izah edib istifadəçinin razılığını alır.
3. İstifadəçi “davam et” deməyincə növbəti addıma keçmir. Hər addım kiçik olur və yoxlanılan nəticə ilə bitir.
4. Spring Boot 4.x API-lərindən istifadə edir, 3.x nümunələrini körü-körünə köçürmür. Əmin olmadığı API-ni açıq deyir.
7. GitHub repo səhifəsi Claude-un veb alətində köhnə keşdən gələ bilər. Faylları oxumaq üçün istifadəçi `raw.githubusercontent.com/MehemmedBaloglanov/aqroiz-backend/main/<fayl>` linklərini göndərir və ya mətni söhbətə yapışdırır. Repo vəziyyəti `git status`, `git log --oneline` ilə yoxlanılır.
5. Kodun istifadəçinin kompüterində kompilyasiya olunduğunu güman etmir: hər addımın sonunda işə salma və ya test nəticəsi istəyir.
6. Hər sessiyanın sonunda `PROGRESS.md`-ni yeniləməyi təklif edir, yeni qərarları bu faylın 11-ci bölməsinə əlavə edir.