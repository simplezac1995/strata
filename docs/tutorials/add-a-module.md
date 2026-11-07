# Tutorial: add your first module

> Goal: a CRUD module with an entity, repository, service and controller that is auto-configured by the starter.

## 1. Copy the golden path

```bash
cd strata/examples
cp -r strata-module-example my-domain
```

The example is a complete reactor module; rename the artifact/package and add it to `strata/examples/pom.xml` (and to
`strata/pom.xml` module list for the product reactor). Keep the api/biz split:

```
my-domain/
  my-domain-api/   entity, DTO, service port
  my-domain-biz/   controller, app service, repository, mapper
    src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

## 2. Entity and repository

```java
@Entity
@Table(name = "biz_note")
public class Note {
    @Id
    private Long id;          // snowflake ids via IdGenerator, never auto-increment
    private String title;
    private String body;
    private Long createdAt;
    // getters/setters
}

public interface NoteRepository extends JpaRepository<Note, Long> {}
```

Add the entity/repository package to your module's auto-configuration (or place them under a package that the starter
already scans if you are extending the system domain):

```java
@AutoConfiguration
@EntityScan(basePackages = "dev.strata.mydomain.api.entity")
@EnableJpaRepositories(basePackages = "dev.strata.mydomain.biz.repository")
public class MyDomainAutoConfiguration {}
```

## 3. Service and controller

```java
@Service
public class NoteAppService {
    private final NoteRepository repository;

    public NoteAppService(NoteRepository repository) { this.repository = repository; }

    @Transactional
    public Note create(String title, String body) {
        Note note = new Note();
        note.setId(IdGenerator.nextId());
        note.setTitle(title);
        note.setBody(body);
        return repository.save(note);
    }

    @Transactional(readOnly = true)
    public List<Note> list() { return repository.findAll(); }
}

@RestController
@RequestMapping("/api/v1/notes")
public class NoteController {
    private final NoteAppService service;

    public NoteController(NoteAppService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("@permissions.has('notes:note:list')")
    public Result<List<NoteDto>> list() {
        return Result.ok(service.list().stream().map(NoteDto::from).toList());
    }

    @PostMapping
    @PreAuthorize("@permissions.has('notes:note:create')")
    public Result<NoteDto> create(@RequestBody CreateNoteRequest request) {
        return Result.ok(NoteDto.from(service.create(request.title(), request.body())));
    }
}
```

## 4. Migration

Add a Flyway migration after the framework ones — the framework already ships `V1__strata_framework.sql` and
`V2__idempotency_record.sql` in `classpath:db/strata-framework`, and Flyway versions are global across locations, so
start at `V3` (never edit released migrations):

```sql
-- src/main/resources/db/migration/V3__create_biz_note.sql
create table biz_note (
  id bigint primary key,
  title varchar(128) not null,
  body text,
  created_at bigint not null
);
```

## 5. Test it

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
class NoteApiTest {
    @Autowired TestRestTemplate rest;
    // POST /api/v1/notes with a JWT, assert 200 and the returned id
}
```

Every module in this repository runs `mvn verify` with tests (JDK 25 is the only supported build baseline); add at
least one controller-level test.

## 6. Permissions

Register the authority string in the RBAC seed (or through the admin UI): `notes:note:list`, `notes:note:create`,
`notes:note:update`, `notes:note:delete`. The naming convention is `module:entity:action`.
