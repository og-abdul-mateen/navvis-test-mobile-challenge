# Assumptions

The challenge leaves some requirements undefined on purpose. This file lists the assumptions made for this solution and the reasoning behind each one.

## 1. Platform and scope

### 1.1 Android only

The solution targets **Android only**. It is written in **Kotlin** with **Jetpack Compose** and follows Android's recommended practices: a layered architecture with a ViewModel, Kotlin coroutines and Flow, and unidirectional data flow.

**Why:** the position I'm applying for is for Android, and the task needs around 1 day of work. Therefore, I'm focusing on the Android app for now only.

### 1.2 Third-party libraries are allowed on Android

The "no 3rd-party frameworks" rule is listed under iOS only. The Android app therefore uses a few well-established libraries where they make the code clearer, such as OkHttp and kotlinx.serialization. It does not rely on libraries for the core logic (parsing, pairing, sorting).
**Why:** this is standard practice on Android across most apps, and the core logic is what the challenge is really testing.

## 2. Endpoints and mocking

### 2.1 The endpoints are not live, so they are mocked

Both endpoints currently return **HTTP 404**:

- `https://navvis.com/numbers.json`
- `https://navvis.com/inputs.json`

**Why:** this matches the challenge, which says the service is "still in development" and asks for mocked endpoints. The 404 looks intentional, so the app ships with a mock implementation. When running against the real endpoints, the 404 responses are handled as server errors and shown in the UI.

### 2.2 The mock replaces the network layer, not the app logic

The mock is an OkHttp interceptor that returns fake HTTP responses. The real networking, JSON decoding, retry and error-handling code runs in both modes.

Switching between mock and real is done with a build setting (a `BuildConfig` flag set per build variant), so no code change is needed.

The mock offers several scenarios to exercise the app:

- normal data delivered in chunks
- random truncation of responses
- `null` and out-of-range values
- server errors and timeouts
- streams of different lengths
- a large dataset for performance testing

**Why:** mocking at the network level tests the most production code. Scenarios let us show behaviour under real-world failures, not only the happy path.

## 3. Fetching the streams

### 3.1 Each request returns the next chunk

Each request to an endpoint returns the **next chunk** of values; the server keeps track of position. The app keeps a running index per endpoint, so a value's index is its position across all chunks received so far. An empty array means that endpoint is finished.

**Why:** this is the most natural reading of "return values available at the time of each request" together with "an empty array means fetching is done".

**Known risk:** if the server advances its position and the response is then lost, those values are gone. For the real API, an `offset` query parameter would make requests safe to retry.

### 3.2 The two endpoints are independent

Both endpoints are fetched at the same time, each in its own coroutine. Each finishes on its own schedule. Requests are sent back to back with no fixed polling delay.

## 4. Parsing numbers

### 4.1 Bit layout

Each number is one byte, read as `R IIIII SS`:

| Bits | Meaning |
|---|---|
| 0–1 (`SS`) | Section index (0–3 → SECTION 1–4) |
| 2–6 (`IIIII`) | Item value |
| 7 (`R`) | Result bit |

Checked against the example: `150 = 0b1_00101_10` gives SECTION 3, Item 6, result shown. All six example numbers parse as valid.

### 4.2 What makes a number invalid

The item field has 5 bits (0–31), but there are only 6 item titles (0–5). A number is **invalid** if:

- it is `null`
- it is outside 0–255
- it is not an integer
- its item value is 6 or higher

### 4.3 Invalid numbers are not listed

"Each **valid** number shall be shown" is read to mean invalid numbers are left out of the list. A small banner will show how many numbers were skipped.

### 4.4 Duplicates

The same number appearing more than once is shown as a separate row each time. Each value in the stream is its own entry.

## 5. Pairing numbers with inputs

Input `i` belongs to number `i`. For a row whose result bit is set:

| Situation | Row shows |
|---|---|
| Input `i` has arrived and is valid | The result |
| Input `i` has not arrived yet | Pending |
| The inputs stream ended before reaching `i` | Error: missing input |
| Input `i` is `null`, outside 0–65535, or not an integer | Error: invalid input |

Other rules:

- If the result bit is **not** set, the input is never used, so a missing or invalid input is **not** an error for that row.
- Inputs beyond the last number are ignored.

## 6. Computing results

### 6.1 The stand-in algorithm

The result is the last two digits of the input (`input % 100`), shown with two digits: `03`, `23`.

### 6.2 Running the computation

- A result is only computed when the row's result bit is set.
- The computation runs off the main thread on `Dispatchers.Default`, with a limit on how many run at once.
- The mock version adds a few milliseconds of simulated CPU work, with occasional slow outliers.
- A computation that takes too long times out and the row shows an error.
- Results are cached by input value, because the same input always gives the same result.

**Why:** the challenge says the computation can take a few milliseconds with outliers. The UI must stay smooth, and one slow value must not hold up the rest of the list.

## 7. Display and sorting

### 7.1 Sections

Sections are sorted by index (SECTION 1 to SECTION 4). Sections with no rows are hidden, as in the challenge's example.

### 7.2 Rows within a section

Rows are sorted by item value. Rows with the same item value are sorted in this order:

1. No result (result bit not set)
2. Results, in ascending numeric order
3. Pending
4. Errors

Rows that are otherwise equal keep their arrival order.

**Why:** the example puts "Item 4" (no result) before "Item 4 result=03". Pending and error rows have no value to sort by, so they go last.

### 7.3 Incremental updates

The list updates as each chunk arrives and as each result finishes. Updates are grouped together briefly, so a large stream does not redraw the screen for every single value.

## 8. Handling server errors

- **Tolerant decoding:** a single bad element (for example a string or a decimal) is treated as invalid without discarding the rest of the chunk. A wrong JSON key or a value that is not an array fails only that request.
- **Retries:** timeouts, HTTP 5xx, HTTP 429 and malformed responses are retried with exponential backoff and random jitter, up to a fixed number of attempts.
- **After retries run out:** the endpoint shows an error state with a **Retry** button. Retrying continues from where fetching stopped, not from the beginning.
- **Lifecycle:** fetching runs in the ViewModel's scope. It survives screen rotation and is cancelled cleanly when the screen is closed.
