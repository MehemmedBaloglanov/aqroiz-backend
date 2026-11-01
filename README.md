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
- Jackson 3: `ObjectMapper` əvəzinə `JsonMapper` (paket `tools.jackson.databind`).
- Avtokonfiqurasiya sinifləri yeni paketlərdədir (məsələn, `org.springframework.boot.flyway.autoconfigure`).
- Spring Boot 3.x üçün yazılmış nümunə kodu birbaşa köçürməzdən əvvəl yoxlayın.

---

## 2. Lokal mühit

### Tələblər

- JDK 21
- Docker Desktop
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

### Konfiqurasiya və mühit dəyişənləri

`application.yaml` lokal dəyərləri standart kimi saxlayır, prod-da mühit dəyişənləri ilə əvəz olunur:

| Dəyişən | Standart | Mənası |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/aqroiz` | Baza ünvanı |
| `DB_USER` | `aqroiz` | Baza istifadəçisi |
| `DB_PASSWORD` | `aqroiz` | Baza şifrəsi |

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

- ID: `UUID`, tətbiqdə yaradılır (`UUID.randomUUID()`), bazadan gözlənilmir.
- JPA üçün `protected` boş konstruktor; yaradılma statik fabrik metodu və ya mənalı konstruktorla.
- `public` setter yoxdur. Vəziyyət yalnız biznes metodları ilə dəyişir (`confirm()`, `reverse(...)`).
- Biznes qaydası entity-nin içində yoxlanır və pozulanda `BusinessRuleException` atılır.
- Paralel dəyişikliklər üçün `@Version` (optimistic locking).
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
- Cari vaxt həmişə inject olunan `Clock` vasitəsilə alınır, `Instant.now()` birbaşa çağırılmır (testlərdə vaxtı idarə etmək üçün).

### Xətalar

- API xətaları RFC 9457 `ProblemDetail` formatında, əlavə `code` sahəsi ilə.
- `404` tapılmadı, `400` validasiya, `401` giriş, `403` icazə, `409` konflikt, `422` biznes qaydası.

---

## 5. API qaydaları

- Bütün endpoint-lər `/api/...` ilə başlayır.
- Resurs adları cəm halında: `/api/farmers`, `/api/receipts`.
- Əməliyyatlar alt yol ilə: `POST /api/receipts/{id}/reverse`.
- Tarixlər ISO-8601: `2026-11-01`, `2026-11-01T10:15:30Z`.
- Siyahılar səhifələnir (`page`, `size`).
- Oflayn sahə tətbiqindən gələn yaratma sorğuları idempotentdir (`clientId`).
- Hər yeni və ya dəyişən endpoint `postman/` qovluğundakı kolleksiyaya əlavə edilir. Frontend API-ni oradan öyrənir.

---

## 6. Verilənlər bazası qaydaları

- Sxemi **yalnız Flyway** dəyişir. `ddl-auto: validate` — Hibernate yalnız uyğunluğu yoxlayır.
- Miqrasiyalar: `src/main/resources/db/migration/V{nömrə}__{izah}.sql` (iki alt xətt). Məsələn: `V1__identity.sql`.
- **Tətbiq olunmuş miqrasiya faylı heç vaxt dəyişdirilmir.** Düzəliş yeni miqrasiya ilə edilir.
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
- Modul sərhədləri: Spring Modulith `verify()` testi.
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
| 3 | `shared`: xəta formatı, `Clock`, cari istifadəçi, sənəd nömrələri | ⏳ |
| 4 | `identity`: tenant, istifadəçilər, rollar, JWT ilə giriş | ⏳ |
| 5 | `catalog`: məhsullar, sortlar, gündəlik qiymətlər | ⏳ |
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
| 2026-11-01 | Repo public | Claude-un linkdən yoxlaya bilməsi; gizli dəyərlər və müştəri məlumatları heç vaxt repoya düşmür |

---

## 12. Claude üçün təlimat

Bu layihə Claude ilə birlikdə, addım-addım yazılır. Claude kodu söhbətdə yazır, istifadəçi onu IntelliJ-də yaradır, işə salır və nəticəni göndərir. Hər yeni sessiyada Claude:

1. Əvvəlcə bu `README.md`-ni, sonra `PROGRESS.md`-ni oxuyur.
2. Burada yazılmış qərarlara zidd kod yazmır. Qərarı dəyişmək lazımdırsa, əvvəl səbəbini izah edib istifadəçinin razılığını alır.
3. İstifadəçi “davam et” deməyincə növbəti addıma keçmir. Hər addım kiçik olur və yoxlanılan nəticə ilə bitir.
4. Spring Boot 4.x API-lərindən istifadə edir, 3.x nümunələrini körü-körünə köçürmür. Əmin olmadığı API-ni açıq deyir.
5. Kodun istifadəçinin kompüterində kompilyasiya olunduğunu güman etmir: hər addımın sonunda işə salma və ya test nəticəsi istəyir.
6. Hər sessiyanın sonunda `PROGRESS.md`-ni yeniləməyi təklif edir, yeni qərarları bu faylın 11-ci bölməsinə əlavə edir.