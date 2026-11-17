# Tickets

Tickets for the NavVis mobile challenge, in the order they'll be worked on. Each one maps to a branch cut from `develop`. Several small tickets can share a branch when they belong to the same layer.

See `assumptions.md` for the rules behind parsing, pairing and sorting, and `system-design.html` for the class names.

## Screen states

The state tickets (NAV-12 to NAV-15) use these definitions.

| State | When | What the user sees |
|---|---|---|
| Loading | No rows yet and the numbers stream is still fetching | A centered progress indicator with "Loading numbers…" |
| Empty | The numbers stream finished and there are no valid rows | A message explaining there's nothing to show, with the skipped count if any |
| Success, in progress | At least one row is shown and a stream is still fetching | The list, with a small progress bar at the top |
| Success, done | Both streams finished and at least one row is shown | The list, with a footer giving the totals |
| Error | The numbers stream failed before any row arrived | A full-screen error with the reason and a Retry button |

A stream that fails after rows are already shown does not replace the list. It shows a banner above the list instead (NAV-15).

Rows have their own states too: no result, pending, result, and error (NAV-11).

## Layer setup

### NAV-1 Set up the project

Branch: `feature/project-setup`

Add the libraries the app needs: OkHttp, kotlinx.serialization, coroutines, and lifecycle ViewModel for Compose. For tests, add coroutines-test. Add `mock` and `real` product flavors that set `BuildConfig.USE_MOCK` and the base URL. Create the `ui`, `domain`, `data` and `di` packages and an empty `AppContainer`.

Done when:
- `mockDebug` and `realDebug` both build and launch
- The app shows a placeholder screen
- The sample `ExampleUnitTest` and `ExampleInstrumentedTest` are removed

### NAV-2 Fetch and decode one chunk

Branch: `feature/data-layer`
Depends on: NAV-1

Add `StreamApi` with `fetchNumbers()` and `fetchInputs()`. Each call does one GET and returns a list of raw JSON values. Decode element by element: a string, a decimal or an object becomes an invalid entry, and the other values in the chunk are kept. A missing key, a value that isn't an array, or a body that isn't JSON fails the request with a typed error.

Done when:
- Both endpoints decode the examples from the challenge
- HTTP errors, timeouts and bad bodies come back as typed errors, not crashes

Tests: decoding a chunk with mixed valid and invalid elements, and a body with the wrong key.

### NAV-3 Stream chunks with retry

Branch: `feature/data-layer`
Depends on: NAV-2

Add `StreamRepository` with `numbers()` and `inputs()`, each returning a `Flow` of stream events (chunk received, finished, failed). It requests chunks in a loop until it gets an empty array. Timeouts, 5xx, 429 and bad bodies are retried with exponential backoff and jitter, up to 3 attempts. After that the flow emits a failure and stops. Collecting again resumes from the last index received.

Done when:
- Both flows finish on an empty array
- A failure after retries gives a failure event with the reason
- Cancelling the collector stops the requests

Tests: stops on an empty array; retries then succeeds; gives up after the last attempt; resumes from the right index.

### NAV-4 Basic mock endpoints

Branch: `feature/data-layer`
Depends on: NAV-2

Add `MockInterceptor` and register it in OkHttp for the `mock` flavor only. For now it serves one happy-path scenario: around 200 numbers and 200 inputs, in chunks of random size, with a short random delay, then empty arrays. More scenarios come in NAV-16.

Done when:
- The `mock` build gets data with no network connection
- The `real` build calls `navvis.com` and gets a 404

### NAV-5 Parse numbers

Branch: `feature/domain-layer`
Depends on: NAV-1

Add `NumberParser`. It turns a raw value into section index, item value and result flag, or marks it invalid. Invalid means null, outside 0 to 255, not an integer, or an item value of 6 or more. Map indexes to "SECTION 1" to "SECTION 4" and "Item 1" to "Item 6".

Done when:
- All six example numbers from the challenge parse to the expected values

Tests: the six examples; the edges 0 and 255; null, -1, 256, 1.5; an item value of 6.

### NAV-6 Compute results

Branch: `feature/domain-layer`
Depends on: NAV-1

Add `ResultComputer`. It validates the input (0 to 65535, integer, not null) and returns the last two digits as a zero-padded string. Run it on `Dispatchers.Default` with parallelism limited to the number of CPU cores. Add a stand-in delay of a few milliseconds with an occasional slow outlier, a 2 second timeout, and a cache keyed by input value.

Done when:
- 4123 gives "23" and 1 gives "01"
- Invalid inputs and timeouts return a typed error

Tests: formatting; invalid input; the timeout path; the second call for the same input hits the cache.

### NAV-7 Group and sort rows

Branch: `feature/domain-layer`
Depends on: NAV-5

Add `SectionListBuilder`. It takes the current rows and returns sections in index order, hiding empty ones. Within a section, rows sort by item value. For the same item value, the order is: no result, results ascending, pending, errors. Rows that are otherwise equal keep their arrival order.

Done when:
- The output for the challenge's example matches the layout in the challenge

Tests: section order; the tie-break order above; stable order for equal rows.

### NAV-8 ViewModel and UI state

Branch: `feature/ui-layer`
Depends on: NAV-3, NAV-7

Add `StreamViewModel` and `UiState`. `UiState` models the screen states in the table above. The ViewModel starts both streams in `viewModelScope`, keeps rows in an index-keyed store, and exposes one `StateFlow<UiState>`. Build it with `AppContainer` through a ViewModel factory.

Done when:
- The ViewModel survives rotation without restarting the streams
- Leaving the screen cancels the streams

### NAV-9 Basic list screen

Branch: `feature/ui-layer`
Depends on: NAV-8

Add `StreamScreen`. It collects `UiState` with `collectAsStateWithLifecycle` and draws section headers and rows in a `LazyColumn`, using stable keys. Rows show the item title and, when present, `result=NN` on the right. Keep the styling plain for now; NAV-19 handles the visual design.

Done when:
- The mock build shows a grouped, sorted list matching the challenge's example layout

## Features

### NAV-10 Show rows as they arrive

Branch: `feature/incremental-list`
Depends on: NAV-9

Rebuild the list as chunks arrive and results finish, but no more than once every 100 ms. Build the list on `Dispatchers.Default` and only publish it on the main thread.

Done when:
- Rows appear while streams are still running
- Scrolling stays smooth with the large-dataset scenario (checked in NAV-17)

### NAV-11 Pair inputs and show results

Branch: `feature/computation-results`
Depends on: NAV-6, NAV-10

Pair number `i` with input `i`. Rows with the result flag get one of these states:

- Pending: input not here yet, or still computing. Shows a small spinner.
- Result: shows `result=NN`.
- Error: shows a short reason in the error color ("missing input", "invalid input", "timed out").

A row becomes "missing input" when the inputs stream finishes before reaching its index. Rows without the result flag never show an error.

Done when:
- Every state above can be seen in the mock build
- Rows move to their sorted place when their result arrives

### NAV-12 Loading states

Branch: `feature/screen-states`
Depends on: NAV-10

Add the loading states:

- Loading: centered progress indicator with "Loading numbers…" before the first row
- Success, in progress: a thin progress bar at the top of the list while either stream is still fetching
- Pending rows from NAV-11 are the per-row loading state

Done when:
- The progress bar disappears once both streams finish

### NAV-13 Empty states

Branch: `feature/screen-states`
Depends on: NAV-12

Add the empty states:

- The numbers stream returns an empty array on the first request: "No numbers available."
- All numbers received were invalid: "No valid numbers. N values were skipped."

Done when:
- Both cases can be reproduced with mock scenarios (NAV-16)

### NAV-14 Success state

Branch: `feature/screen-states`
Depends on: NAV-12

When both streams finish, show a footer under the list: "X numbers shown, Y skipped, Z results." Skipped numbers are counted while streaming and shown in the footer at the end.

Done when:
- The totals match the mock data

### NAV-15 Error states and retry

Branch: `feature/stream-errors`
Depends on: NAV-12

Add the error states:

- Numbers stream fails before any row: full-screen error with the reason and a Retry button.
- Either stream fails after rows are shown: a banner above the list ("Couldn't load more inputs") with a Retry button. Rows already shown stay.

Retry resumes from the last index received. Errors are shown in plain words ("Server error (503)", "No connection"), not as exception names.

Done when:
- The `real` build shows the full-screen error for the 404
- Retrying in the mock build after a failure finishes the stream without duplicate rows

### NAV-16 Mock scenarios

Branch: `feature/mock-scenarios`
Depends on: NAV-4

Add the remaining mock scenarios: random truncation, nulls and out-of-range values, bad elements (strings, decimals), server errors (5xx, 429), timeouts, inputs shorter than numbers, inputs longer than numbers, empty numbers, all-invalid numbers, and a large dataset (100,000 numbers). Add a debug menu in the top app bar of the `mock` build to pick a scenario and restart.

Done when:
- Each scenario can be picked from the app and shows the expected state

### NAV-17 Performance check

Branch: `feature/performance`
Depends on: NAV-16

Run the large-dataset scenario on a mid-range device or emulator. Record the time to first row, the time until all data is loaded, and frame timing while scrolling. Fix whatever shows up. Write the numbers down for the README.

Done when:
- No frozen frames while streaming 100,000 numbers
- The results are recorded with the device used

### NAV-18 README and evidence

Branch: `feature/docs`
Depends on: NAV-17

Write the README: how to run each flavor, how to pick a scenario, the architecture (link to `system-design.html`), assumptions (link to `assumptions.md`), shortcuts taken and how to close them, test summary, performance numbers, screenshots of each screen state, and how AI was used.

Done when:
- Someone new can clone, build and run the app using only the README

## Design and delivery

### NAV-19 Match NavVis design and colors

Branch: `feature/navvis-theme`
Depends on: NAV-20

Apply the NavVis colors and typography from `brand_guidelines.html` to the Material 3 theme. Cover light and dark mode, the top app bar, section headers, rows, the error color and the progress indicators. Use only colors and type, not the NavVis logo.

Done when:
- Every screen state matches the brand guidelines in light and dark mode
- Text contrast meets WCAG AA

### NAV-20 Brand guidelines file

Branch: `feature/navvis-theme`

Write `brand_guidelines.html`, the design system for the app. Base it on NavVis's public website and materials. It lists the color tokens (light and dark, mapped to Material 3 roles), the typography scale, spacing, and how each component looks: section header, row, pending row, error row, banner, empty state and footer. Note anywhere the values are a best guess.

Done when:
- Every value used in NAV-19 is defined in the file

### NAV-21 Fastlane

Branch: `feature/fastlane`
Depends on: NAV-1

Add Fastlane with these lanes:

- `test`: runs unit tests for both flavors
- `lint`: runs Android lint
- `build_mock` and `build_real`: build release APKs
- `screenshots`: captures screenshots of each screen state for the README (optional if time runs short)

Done when:
- Each lane runs locally from a clean checkout
- The README explains how to run them

### NAV-22 CI/CD

Branch: `feature/ci`
Depends on: NAV-21

Add GitHub Actions workflows that call the Fastlane lanes, so CI and local runs behave the same.

- On every PR to `develop`: lint and unit tests
- On merge to `develop`: build both flavors and upload the APKs as artifacts
- On merge to `main`: build release APKs and attach them to a GitHub release

Cache Gradle and Ruby gems to keep runs short. Keep the workflow files readable, since NavVis developers will maintain CI themselves.

Done when:
- A PR to `develop` shows passing checks
- A merge to `main` produces a release with both APKs
