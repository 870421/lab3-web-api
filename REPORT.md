## What I specified

Before completing the tests, I identified the expected HTTP properties of each method.

- `POST /employees` is not safe and not idempotent. I would know the test passed if two identical POST requests created two different employees with different ids.
- `GET /employees/{id}` is safe and idempotent. I would know the test passed if repeated GET requests returned the same result and no repository write method was called.
- `GET /employees` is safe and idempotent. I would know the test passed if repeated requests returned the same list and did not modify the repository.
- `PUT /employees/{id}` is not safe but idempotent. I would know the test passed if both requests stored the employee at the same id and the second request did not create a new id.
- `DELETE /employees/{id}` is not safe but idempotent. I would know the test passed if both identical DELETE requests returned `204 No Content` and `deleteById(1)` was called twice.

No bonus task was included.

## What I changed

I completed `ControllerTests.kt` by replacing the TODO blocks with MockK setups and verifications for the five HTTP operations.

For POST, I configured `save()` to return id `1` on the first call and id `2` on the second call. I also verified that `save()` was called twice and that no read or delete method was used.

For the individual GET, I configured `findById(1)` to return Mary and `findById(2)` to return empty. I verified that both ids were requested twice and that no modifying repository method was called.

For PUT, I configured `findById(1)` to return empty first and the employee on the second call. I also configured `save()` to always store Tom with id `1`.

For DELETE, I configured `deleteById(1)` with `justRun` so it could be called twice without returning a value.

For the collection GET, I configured `findAll()` to always return the same list with Mary.

I also added the required MockK imports and simple comments to explain the test setup and verification.

## Technical decisions

For POST, I used `andThenAnswer` so the two identical requests received different generated ids. This shows that POST is not idempotent because repeating the request creates a new resource. I also verified that `save()` was called twice, so POST is not safe.

For the individual GET, I used fixed answers for `findById(1)` and `findById(2)` because the repository state does not change during the test. I verified that no `save()` or `deleteById()` call was made, which shows that GET is safe. Repeating the requests returned the same results, so GET is idempotent.

For PUT, I used `Optional.empty()` for the first `findById(1)` call and the stored employee for the second one. Both saves use the same id from the URI. This shows that PUT is idempotent because repeating the request does not create a new id, but it is not safe because it writes with `save()`.

For DELETE, I used `justRun` because `deleteById()` returns no value. The two identical DELETE requests both return `204 No Content` and call `deleteById(1)`. This matches the expected idempotent behaviour of DELETE. It is not safe because it performs a delete operation.

For the collection GET, I used `findAll()` returning the same list for both requests. I verified that no save, delete, or single-employee read was made. This shows that the collection GET is safe and idempotent.

I rejected MockK blocks that did not match the expected repository calls. For POST, I did not use the "no modifying methods were called" block because POST must call `save()`. For PUT, I did not use the setup where `save()` returns a new id on every call because both PUT requests must use the same id. For the collection GET, I did not use a verification that sets `findAll()` to zero because this endpoint must call `findAll()`. For the individual GET, I did not use the collection verification because it sets `findById()` to zero, while this endpoint must read an employee by id.

## How I verified

According to the lab guide, the starter project fails because the five tests contain TODO blocks. I completed those blocks before running the test suite. After completing them, the tests passed successfully on the first run.

I ran:

```bash
./gradlew test
```

The five controller tests completed successfully.

For POST, I checked that `save()` was called twice and that the two identical requests returned different ids. This confirmed that POST is not idempotent. I also checked that no read or delete methods were called.

For the individual GET, I checked that `findById(1)` and `findById(2)` were each called twice. The repeated requests returned the same results, and no modifying repository methods were called. This confirmed that GET is safe and idempotent.

For PUT, I checked that `findById(1)` was called twice and that both requests saved Tom with id `1`. The first request returned `201 Created` and the second returned `200 OK`, but both used the same resource id. This confirmed that PUT is idempotent but not safe.

For DELETE, I checked that `deleteById(1)` was called twice and that both requests returned `204 No Content`. I also verified that no save or read operations were performed. This confirmed the expected idempotent and non-safe behaviour.

For the collection GET, I checked that `findAll()` was called twice and that both requests returned the same list. I also verified that no save, delete, or single-employee read was performed. This confirmed that the operation is safe and idempotent.

I then ran:

```bash
./gradlew check
```

and the result was:

```text
BUILD SUCCESSFUL
```

Finally, I formatted and checked the Kotlin code with:

```bash
./gradlew ktlintFormat
./gradlew ktlintCheck
```

Both commands completed successfully. `ktlintFormat` displayed warnings related to `sun.misc.Unsafe` in the Kotlin compiler, but they did not cause any build or test failure.

The final checks were:

```text
./gradlew test        -> BUILD SUCCESSFUL
./gradlew check       -> BUILD SUCCESSFUL
./gradlew ktlintCheck -> BUILD SUCCESSFUL
```

## AI disclosure

- **Tools / skills:** Claude Code and ChatGPT.

- **Purpose:** Claude Code was used as assistive support while completing individual MockK setup and verification blocks in `ControllerTests.kt`, and to review specific test decisions. ChatGPT was used to explain safe and idempotent HTTP behaviour, review each completed test, improve simple English comments, check the relation between the MockK blocks and the lab guide, and help draft and review `REPORT.md`.

- **Representative prompts:** Examples include asking for help with one test at a time, such as the POST, GET, PUT, DELETE, or collection GET test; asking whether a specific MockK setup or `verify(exactly = ...)` block correctly demonstrated safety or idempotency; and asking for help writing and correcting individual sections of the report.

- **Affected files/sections:** AI assistance affected `src/test/kotlin/es/unizar/webeng/lab3/ControllerTests.kt` and `REPORT.md`.

- **Validation steps:** I ran `./gradlew test`, `./gradlew check`, `./gradlew ktlintFormat`, and `./gradlew ktlintCheck`. I checked that all five tests passed and reviewed the repository calls and verification counts for each HTTP method.

- **Citations:** No external code snippets were adapted. The MockK blocks and expected behaviour were based on the provided Lab 3 guide.

- **Human-reviewed:** I reviewed the completed tests and comments, checked the expected behaviour of each HTTP method, ran the test and build commands myself, and kept or changed the suggested text and code after reviewing it.