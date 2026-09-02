# ipie-service-template

The approved starting point for every iPIE backend microservice. Ships a full vertical slice — User CRUD against PostgreSQL — so the layering and cross-cutting wiring are visible end to end, not merely described.

Extracted from the `ipie-platform-mca` monorepo. It builds against the platform as **published
artifacts** — there is no `project(':...')` dependency and no `includeBuild` anywhere here.

## Starting a new service

Use the script in `ipie-platform-mca`:

    ./new-service.sh ipie-casefile-service

It copies this repository's tracked files, renames everything that carries the template's name — the
directory, the base package `in.gov.ipie.service.template` → `in.gov.ipie.service.<name>`, the
directory tree that mirrors it, `ServiceTemplateApplication`, `rootProject.name`,
`ArchitectureTest`'s `BASE_PACKAGE` and the database name in `application.yml` — then verifies no
reference survived and that the platform version this service pins is actually resolvable on the
machine. It refuses to overwrite an existing directory, and it does not create the repository,
commit or push.

The rename is five things across a hundred-odd files, which is why it is a script. Missing
`BASE_PACKAGE` leaves the ArchUnit rules asserting nothing; missing the database name leaves two
services quietly sharing one schema. Neither fails loudly.

Doing it by hand is still the documented fallback: rename the directory, `rootProject.name`, the
base package, the application class, `ArchitectureTest.BASE_PACKAGE`, and the datasource name.
Nothing about the layering changes.

## Examples

`src/main/java/in/gov/ipie/service/template/examples/` carries one `*Example` class per
`ipie-common-libs` module, showing how that module is called from a controller or a service. They
are reference material, never wired into a real request flow — read them when adopting a module,
and delete the ones this service does not need.

`SERVICE_CLASS_REFERENCE.md` lists all of them, and says which carry their own tests. Where a
module is also used in this template's real User/Document business logic, that call site is the
better guide; the same document maps each module to where it is demonstrated.

Since 2026-08-09 a new service starts from a clone of this repository rather than by copying a module out of the monorepo and adding it to `settings.gradle`.

## Platform dependency

One property in `gradle.properties` fixes the version:

    ipiePlatformVersion=0.1.0

which pins `in.gov.ipie:ipie-parent` (the version BOM), `in.gov.ipie:ipie-common-libs` (shared
libraries, plus its test-fixtures variant carrying the ArchUnit rules and Testcontainers base
classes) and `in.gov.ipie:ipie-build-conventions` (the `ipie.*` convention plugins, which also
carry the Checkstyle and SpotBugs configuration).

Bumping it is a deliberate act — that is the trade the extraction buys. This service is no longer
dragged by every platform change, but it must choose when to take one. Automate the bump as a pull
request gated on this repository's own CI. **Never** a dynamic `1.+` or `-SNAPSHOT` range: builds
stop being reproducible, and one bad platform commit would reach every service unchecked.

The shared test fixtures are consumed with `testFixtures("in.gov.ipie:ipie-common-libs:…")`, not a
`:test-fixtures` classifier. A classifier fetches the jar but bypasses Gradle Module Metadata
variant selection, so Testcontainers and `spring-boot-starter-test` would not come with it and the
first integration test would fail with `NoClassDefFoundError`.

## Resolving the platform artifacts

They live in GitHub Packages under `ipie-cms/ipie-platform-mca`. That registry authenticates every
read, whether or not the repository is public, so reads need credentials:

- **Locally** — set `ipie.packages.user` and `ipie.packages.token` in `~/.gradle/gradle.properties`
  (never in this repository), or publish the platform to Maven Local, which is checked first.
  Publishing the platform locally needs two commands, not one: `./gradlew publishToMavenLocal` in
  `ipie-platform-mca` misses `ipie-build-conventions`, because that is an `includeBuild` and the
  root task does not reach into it. Follow it with
  `./gradlew -p ipie-build-conventions publishToMavenLocal`, or the `ipie.*` plugins fail to
  resolve at settings evaluation and the error names the plugin rather than the cause.
- **In CI** — a workflow's default `GITHUB_TOKEN` is scoped to *its own* repository and cannot read
  a private package owned by another one. Grant that access explicitly in the package settings, or
  supply a PAT.

## Local stack

`docker-compose.yml` in `ipie-platform-mca` starts the infrastructure — Postgres, Keycloak,
RabbitMQ, Redis, Elasticsearch, MailHog and the observability stack. It no longer builds any
service. Start it there, then run this service against it.

## Standards

`MASTER_CODE_STANDARDS.md` is committed here so you can check your work without leaving the
repository. It is generated into `.docx` alongside; edit the `.md`, never the `.docx`.

## Build

    ./gradlew check      # tests, ArchUnit, Checkstyle, SpotBugs
    ./gradlew bootJar
