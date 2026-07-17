# QueueCTL

CLI-based Background Job Queue System — a tiny Sidekiq/RabbitMQ for shell commands.

## Features

- **Job Queue**: Enqueue shell commands as jobs with unique IDs
- **Parallel Workers**: Multiple workers execute jobs concurrently
- **Retry with Exponential Backoff**: Failed jobs retry automatically (delay = base^attempt)
- **Dead Letter Queue**: Jobs exceeding max retries move to DLQ
- **Full Persistence**: SQLite database — everything survives restarts
- **Priority Queue**: Higher priority jobs are processed first
- **Delayed Jobs**: Schedule jobs for future execution via `runAt`
- **Worker Heartbeat**: Workers send periodic heartbeats for health tracking
- **Stale Lock Cleanup**: Automatic recovery of jobs locked by crashed workers
- **Job Timeout**: Commands exceeding timeout are killed
- **Execution Statistics**: Success rate, failure rate, average execution time
- **Cross-Platform**: Works on Windows, Linux, and macOS

## Tech Stack

| Component       | Technology          |
|----------------|---------------------|
| Language        | Java 17             |
| Build           | Maven               |
| CLI Framework   | Picocli 4.7.5       |
| JSON            | Jackson 2.16.1      |
| Database        | SQLite (JDBC)       |
| Logging         | SLF4J + Logback     |
| Testing         | JUnit 5             |
| Utilities       | Apache Commons IO   |

## Architecture

```
queuectl/
├── src/main/java/com/queuectl/
│   ├── Main.java                  # Application entry point
│   ├── cli/
│   │   ├── commands/              # Picocli CLI commands
│   │   │   ├── QueueCTLCommand.java   # Root command
│   │   │   ├── EnqueueCommand.java    # Enqueue jobs
│   │   │   ├── WorkerCommand.java     # Worker management
│   │   │   ├── ListCommand.java       # List jobs
│   │   │   ├── StatusCommand.java     # System status
│   │   │   ├── DlqCommand.java        # Dead Letter Queue
│   │   │   ├── ConfigCommand.java     # Configuration
│   │   │   └── HelpCommand.java       # Help guide
│   │   └── config/
│   │       └── AppConfig.java     # Application config holder
│   ├── entity/                    # Domain entities
│   │   ├── Job.java
│   │   ├── JobState.java          # State enum
│   │   ├── WorkerInfo.java
│   │   ├── ConfigEntry.java
│   │   └── JobLog.java
│   ├── repository/                # Data access (SQL only)
│   │   ├── JobRepository.java
│   │   ├── WorkerRepository.java
│   │   ├── ConfigRepository.java
│   │   └── LogRepository.java
│   ├── service/                   # Business logic
│   │   ├── JobService.java
│   │   ├── WorkerService.java
│   │   ├── RetryService.java
│   │   ├── DlqService.java
│   │   ├── ConfigService.java
│   │   └── StatusService.java
│   ├── worker/                    # Worker runtime
│   │   ├── Worker.java
│   │   ├── WorkerFactory.java
│   │   └── WorkerManager.java
│   ├── executor/
│   │   └── CommandExecutor.java   # ProcessBuilder wrapper
│   ├── scheduler/
│   │   └── Scheduler.java         # Background maintenance
│   ├── lock/
│   │   └── JobLockManager.java    # Transaction-based locking
│   ├── db/
│   │   └── DatabaseManager.java   # SQLite connection + schema
│   ├── exception/                 # Custom exceptions
│   │   ├── DuplicateJobException.java
│   │   ├── JobNotFoundException.java
│   │   ├── ConfigurationException.java
│   │   ├── WorkerException.java
│   │   └── DatabaseException.java
│   └── utils/                     # Utility classes
│       ├── ClockUtil.java
│       ├── JsonUtil.java
│       ├── ConsolePrinter.java
│       ├── RetryCalculator.java
│       └── CommandValidator.java
├── src/main/resources/
│   └── logback.xml
├── src/test/java/com/queuectl/    # Test suite
│   ├── TestBase.java
│   ├── repository/JobRepositoryTest.java
│   ├── service/JobServiceTest.java
│   ├── service/DlqServiceTest.java
│   ├── service/ConfigServiceTest.java
│   ├── executor/CommandExecutorTest.java
│   └── integration/IntegrationTest.java
└── pom.xml
```

## State Machine

```
                    ┌──────────┐
                    │  PENDING │
                    └────┬─────┘
                         │ worker claims
                    ┌────▼─────┐
                    │PROCESSING│
                    └──┬───┬───┘
                  ok   │   │  fail
                ┌──────┘   └──────┐
         ┌──────▼──────┐    ┌─────▼────┐
         │  COMPLETED  │    │  FAILED   │
         └─────────────┘    └──┬────┬──┘
                          < max│    │> max
                         retries│    │retries
                    ┌──────▼───┐│ ┌──▼───┐
                    │  PENDING ││ │ DEAD │  (DLQ)
                    │ (retry)  ││ └──────┘
                    └──────────┘│
                                │
                         dlq retry
                         ┌──────▼──────┐
                         │   PENDING   │
                         └─────────────┘
```

## Retry Mechanism

Exponential backoff with configurable base:

```
delay = base^attempt
```

| Base | Attempt 1 | Attempt 2 | Attempt 3 |
|------|-----------|-----------|-----------|
| 2    | 2s        | 4s        | 8s        |
| 3    | 3s        | 9s        | 27s       |
| 5    | 5s        | 25s       | 125s      |

## Worker Lifecycle

1. Worker registers in the `workers` table with a unique ID and PID
2. Worker enters polling loop at configured interval (default: 1000ms)
3. Each cycle: claim a job via transaction → execute → update status
4. Worker sends heartbeat every 5 seconds
5. On shutdown: finishes current job, then marks status as STOPPED

## How Locking Works

SQLite transactions prevent duplicate job execution:

```sql
BEGIN IMMEDIATE;   -- Acquires write lock
SELECT * FROM jobs WHERE state='PENDING' AND locked=0 ...;
UPDATE jobs SET locked=1, worker_id=?, state='PROCESSING' WHERE id=? AND locked=0;
COMMIT;
```

- `BEGIN IMMEDIATE` acquires a RESERVED lock immediately, preventing other writers
- The `WHERE locked=0` clause ensures only one worker claims each job
- If the UPDATE affects 0 rows, the transaction is rolled back
- Stale locks from crashed workers are cleaned up by the scheduler

## CLI Examples

### Enqueue a job
```bash
java -jar queuectl.jar enqueue '{"id":"job1","command":"echo hello"}'
java -jar queuectl.jar enqueue '{"id":"job2","command":"ls -la","priority":10}'
java -jar queuectl.jar enqueue '{"id":"job3","command":"sleep 5","maxRetries":5}'
```

### Start workers
```bash
java -jar queuectl.jar worker start              # 1 worker
java -jar queuectl.jar worker start --count 5    # 5 workers
```

### Stop workers
```bash
java -jar queuectl.jar worker stop
```

### List jobs
```bash
java -jar queuectl.jar list                      # all jobs
java -jar queuectl.jar list --state pending      # filter by state
```

### System status
```bash
java -jar queuectl.jar status
```

### Dead Letter Queue
```bash
java -jar queuectl.jar dlq list                  # show dead jobs
java -jar queuectl.jar dlq retry job1            # retry a dead job
```

### Configuration
```bash
java -jar queuectl.jar config get                # show config
java -jar queuectl.jar config set max-retries 5
java -jar queuectl.jar config set backoff-base 3
java -jar queuectl.jar config set timeout 600
java -jar queuectl.jar config set poll-interval 500
```

### Help
```bash
java -jar queuectl.jar help
java -jar queuectl.jar --help
```

## Building

```bash
mvn clean package
java -jar target/queuectl-1.0.0.jar
```

## Testing

```bash
mvn test                      # run all 53 tests
```

Test coverage:
- **JobRepositoryTest** (13 tests): CRUD, claim, lock, DLQ operations
- **JobServiceTest** (12 tests): enqueue, claim, complete, fail, retry, DLQ
- **DlqServiceTest** (7 tests): list, retry, purge dead jobs
- **ConfigServiceTest** (10 tests): get, set, validation, aliases
- **CommandExecutorTest** (5 tests): execution, exit codes, errors
- **IntegrationTest** (6 tests): end-to-end flows, persistence, concurrency

## Design Decisions

1. **No Spring Boot**: Manual DI keeps the application lightweight and fast-starting
2. **SQLite**: Zero-configuration database, perfect for single-node queue systems
3. **Clean Architecture**: Clear separation between CLI, services, repositories, and workers
4. **Picocli**: Type-safe CLI parsing with auto-generated help
5. **ProcessBuilder**: Cross-platform command execution with output capture
6. **Manual DI**: Services construct their dependencies explicitly, no framework magic

## Future Improvements

- [ ] Web dashboard for monitoring
- [ ] Job progress reporting
- [ ] Cron-based scheduling
- [ ] Job dependencies (DAG)
- [ ] Rate limiting per command type
- [ ] Job output streaming
- [ ] REST API alongside CLI
- [ ] Job tags and filtering
- [ ] Batch enqueue/dequeue
- [ ] Horizontal scaling with distributed locking
