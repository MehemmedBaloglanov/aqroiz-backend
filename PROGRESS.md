# PROGRESS

Bu fayl işin gündəliyidir: harada qaldıq, nə hazırdır, növbəti addım nədir. Qaydalar və qərarlar `README.md`-dədir.

---

## Cari vəziyyət

- **Son tamamlanan:** Yol xəritəsinin 2-ci mərhələsi (README.md və PROGRESS.md).
- **Növbəti addım:** Yol xəritəsinin 3-cü mərhələsi — `shared` modulu.
- **Tətbiq işə düşür:** Bəli (`Started AqroizBackendApplication`).
- **Testlər:** Hələ yazılmayıb.
- **Bazada cədvəl:** Yoxdur (Flyway miqrasiyası hələ yoxdur).

---

## Növbəti addım: `shared` modulu (dəqiq plan)

Hər alt addım ayrıca edilir, istifadəçi təsdiq etdikdən sonra növbətiyə keçilir.

1. `az.aqroiz.shared` paketi və `package-info.java` (Modulith üçün “açıq modul”).
2. `TimeConfig`: Asia/Baku zonası ilə `Clock` bean-i; `application.yaml`-a `hibernate.jdbc.time_zone: UTC`.
3. `BusinessRuleException`, `NotFoundException`.
4. `GlobalExceptionHandler`: `ProblemDetail` + `code` sahəsi, Azərbaycan dilində mesajlar.
5. Modulith yoxlama testi: `ModularityTests`.
6. Yoxlama: tətbiq işə düşür, test keçir, commit.

Qeyd: cari istifadəçi (`CurrentUser`) və sənəd nömrələri (`NumberingService`) `identity` və ilk miqrasiyadan sonra əlavə olunacaq, çünki onlar JWT-yə və `document_counters` cədvəlinə bağlıdır.

---

## Açıq suallar

| # | Sual | Nə vaxt lazımdır |
| --- | --- | --- |
| S1 | Postman: lokal kolleksiya faylı, yoxsa Postman Workspace? | `identity` mərhələsində ilk endpoint-dən əvvəl |
| S2 | JWT gizli açarı prod-da necə saxlanacaq? | Deploy-dan əvvəl |
| S3 | SMS provayderi hansı olacaq (yerli və ya WhatsApp Business API)? | `notifications` mərhələsi |
| S4 | Şəxsi məlumatların Azərbaycanda saxlanma tələbi (server yeri) | Deploy-dan əvvəl, hüquqşünasla |
| S5 | Repo nə vaxt private ediləcək? | İlk pilot müştəridən əvvəl |

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