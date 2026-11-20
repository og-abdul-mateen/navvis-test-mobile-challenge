# Performance

Measured on 2026-11-20 with the "100,000 numbers" mock scenario: 100,000 numbers and 100,000 inputs, sent in chunks of up to 500 values every 10–50 ms.

## How it was measured

- Build: `mockBenchmark`. It uses the release settings but is signed with the debug key so it installs. Debug builds of Compose are much slower than release builds, so they aren't used for numbers.
- Timings: the app logs three milestones under the `StreamTiming` tag. They are the first rows on screen, both streams finished, and all results computed.
- Frames: `adb shell dumpsys gfxinfo` is reset when the scenario is picked. While the data streams in, a script flings the list up and down every 2 seconds.
- Each build was run 3 times. The tables show every run, because single runs vary a lot.

## Results on a phone

Sony Xperia 1 IV (SO-51C), 8 cores, display at 120 Hz. At 120 Hz a frame has 8.3 ms.

| Build | Janky frames | 90th percentile frame | 99th percentile frame |
|---|---|---|---|
| Before (one `item()` per row) | 13.8%, 10.1%, 10.1% | 34–44 ms | 53–121 ms |
| After (`items()` per section) | 1.1%, 0.8%, 2.2% | 15–20 ms | 31–46 ms |
| After, with R8 | 6.8%, 0.7%, 1.3% | 17–21 ms | 28–34 ms |

| Milestone | Before | After |
|---|---|---|
| First rows on screen | 0.25 s | 0.25 s |
| Both streams finished | 17.0–18.7 s | 14.8–17.6 s |
| All results computed | 101.5–104.3 s | 86.0–96.1 s |

## What changed

The list used to call `item()` once for every row. With 100,000 rows that means 100,000 calls on the main thread every time the list updates, up to 10 times a second. It now calls `items()` once per section, which only looks at the rows on screen. This removed most of the jank.

R8 made no clear difference to frame times, since its runs overlap with the runs without it. It shrinks the APK from 8.9 MB to 1.3 MB, so it's on for release builds anyway.

## Why "all results" takes over a minute

Most of that time is the simulated algorithm, not the app. About half the rows need a result, which is roughly 50,000 computations. Each takes 1–5 ms, and about 1 in 100 is an outlier of 50 ms to 3 s. At most 7 run at once on this phone (cores minus one), and the outliers make up most of the total. Rows, spinners and scrolling stay responsive the whole time, and results fill in as they finish.

## Notes on the emulator

The first emulator runs used software rendering (SwiftShader), and frames were slow even for 200 idle rows. Numbers from that setup say more about the emulator than about the app. With `-gpu host` the emulator used the Mac's GPU, and the trend matched the phone. Janky frames went from 0.7% to 0.3%, and the 99th percentile from 31 ms to 21 ms. All results took about 185 s there, because the emulator has 4 cores, so 3 computations run at once.
