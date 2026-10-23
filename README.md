# IMDB Client

A native Android movie discovery app built with Java, XML, Volley, Room and the TMDB API for the Mobile Application Development course at USTH.

This is an unofficial student project and is not affiliated with IMDb. Movie data comes from TMDB: this product uses the TMDB API but is not endorsed or certified by TMDB.

## Requirements
 
| Item | Version |
|---|---|
| Android | minSdk 26 (Android 8.0), targetSdk 34, compileSdk 36.1; phones in portrait orientation |
| Language | Java 17 source level, XML layouts (no Kotlin, no Compose) |
| Build | Gradle 9.5.0 (wrapper), Android Gradle Plugin 9.3.2 |
| Gradle daemon JVM | JDK 25, pinned in `gradle/gradle-daemon-jvm.properties`; Gradle downloads it on the first run if it is not installed (network access required) |
| Libraries | AppCompat, ViewPager, Lifecycle (ViewModel, LiveData, SavedState), Volley, Room |
| API key | A TMDB **v3 API key**. Create one under [TMDB account settings, API](https://www.themoviedb.org/settings/api) and copy the "API Key", not the "API Read Access Token" |
 
The brief behind these requirements is in [docs/brief.md](docs/brief.md).

## Run locally
 
1. Open this directory in Android Studio and install the Android SDK platform 36.1 when prompted.
2. Add your key to `local.properties` (it is ignored by Git), or set an environment variable of the same name:
```properties
   TMDB_API_KEY=your_tmdb_v3_api_key
```
 
3. Run the `app` configuration on an emulator or device, or build from the command line.

The key is read at build time, so rebuild after you change it.

Command line, Windows (PowerShell):
 
```powershell
# Only needed if `java` is not on PATH; Android Studio ships a JDK in `jbr`
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```
 
Command line, macOS and Linux:
 
```bash
# First time only: the wrapper is stored in Git without the executable bit
chmod +x gradlew
# If `java` is not on PATH, point JAVA_HOME at Android Studio's bundled JDK, e.g. on macOS
# export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug
```
 
The key is injected into `BuildConfig`, never committed and never logged. A missing key is a recoverable configuration error shown in the UI, never a fake movie list. Like any client-side key, it can be extracted from a built APK, so use a key created only for this project and regenerate it if it leaks.

## Structure and Responsibilities

The app uses MVVM with the Repository pattern. Fragments render state, one shared ViewModel holds presentation state, the repository is the only data-access entry point, and TMDB and Room are the two data sources. See [architecture.md](docs/architecture.md) for the diagrams.

```text
app/src/main/
  java/vn/edu/usth/imdbclient/
    MovieApplication.java      Application setup
    ui/                        MainActivity, six screen Fragments, list and pager adapters, image/resource helpers
    viewmodel/                 MovieViewModel (activity-scoped), screen state
    data/                      Movie model, MovieDataSource, Volley MovieRepository, Room DAO/database/migration
  res/
    layout/                    Screen layouts, reusable search/state/movie rows
    values/                    Strings, dimensions, colors, light/dark styles
    drawable/, drawable-nodpi/ XML surfaces, Figma icons and state illustrations as bitmaps
  assets/                      Inter font and license
app/src/test/                  JVM tests: JSON parsing, paging, de-duplication, state
app/src/androidTest/           Device tests: Room, migration, native screen flow
docs/                          Brief, architecture, data contract, implementation notes
```

Layer responsibilities:

- **View**: `MainActivity` plus Home, Search, Details, Favorites, Settings and About Fragments inflate XML layouts, observe `LiveData` and forward user actions.
- **ViewModel**: the activity-scoped `MovieViewModel` drives loading, success, empty, error, retry, feedback, busy-ID and favorites states. `SavedStateHandle` keeps the active tab, query, selection, sorting and scroll positions across recreation.
- **Repository**: `MovieRepository` wraps Volley requests and JSON parsing (off the main thread) and serializes all Room work on one background executor.
- **Model**: a single `Movie` class serves as the TMDB list/detail model and the Room `favorite_movies` entity (intentional; see the [data-contract.md](docs/data-contract.md)).

Frontend constraints follow the course material: Activities/Fragments, standard widgets, XML resources, the original `ViewPager` and a recycling `ListView`. There is no Compose, Kotlin source, WebView, ConstraintLayout, Material UI or third-party UI framework. The course-slide mapping is in [implementation.md](docs/implementation.md).

## Behavior and Data

- **Home**: `ViewPager` showcase of five posters, discover cards and a paginated popular-movie list.
- **Search**: suggestions, locally stored recent queries, 400 ms debounce, cancellation of the previous request, pagination, and empty/error/retry states. Obsolete callbacks are ignored through generation IDs.
- **Details**: release date, rating, runtime, overview, genres, director and cast when TMDB returns them. Missing fields show "Not available" or an image placeholder, never invented values.
- **Favorites**: persistent add/remove in Room, sorting, multi-selection, batch removal and Undo. Add never toggles: a duplicate add keeps the stored row and shows "Already in Favourites". Removal is explicit and only available from Favorites.
- **Settings**: light/dark theme and search-history clearing (favorites are untouched) stored in `SharedPreferences`, plus About and TMDB attribution.
- **State**: rotation and Activity recreation retain the current `ListState` without re-requesting data. Selection survives recreation.
- **Reliability**: network requests use a 10-second timeout; failures show an informative state with Retry. A storage error disables favorite writes and offers Retry.
- **Images**: posters load through Volley's `ImageLoader` with a memory cache capped at 24 MiB plus the queue's disk cache.

Out of scope: user authentication, user accounts, cloud synchronization and a custom backend.

## External API and Storage

TMDB is used through these requests (base URL `https://api.themoviedb.org/3/`, authenticated with the `api_key` query parameter):

| Use case | Request | Source |
|---|---|---|
| UC-01 Browse featured movies | `GET /movie/popular` | TMDB |
| UC-02 Search movies | `GET /search/movie?query={title}&page={page}` | TMDB |
| UC-03 View movie details | `GET /movie/{movie_id}?append_to_response=credits` | TMDB |
| UC-04 Manage favorites | Insert / select / delete | Room (`favorites.db`, schema v2) |
| UC-05 Settings | Read / write preferences | SharedPreferences |

Poster images use `https://image.tmdb.org/t/p/w500{path}` and backdrops use `.../w780{path}`. The client caps TMDB paging at page 500 and removes repeated movie IDs when merging pages. Field mapping, clamping rules, the Room schema and the UI failure contract are in the [data-contract.md](docs/data-contract.md).

## Validation
 
```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest   # needs a running emulator or connected device
```
 
On Windows, use `.\gradlew.bat` instead of `./gradlew`.
 
- JVM tests (`app/src/test`) cover JSON and null parsing, credits, image hosts, paging, de-duplication and immutable state.
- Device tests (`app/src/androidTest`) cover the native screen flow against a fake data source (theme change, rotation, selection recreation, debounce, empty/error states, retry), Room favorites against a real local database, and the schema v1-to-v2 migration.
- Lint runs without a baseline or broad suppression, so warnings stay visible.
- Screenshot comparisons against Figma are visual and manual, not pixel-diff assertions.

The results of the last recorded run (device, Android version, test and lint counts) are in [implementation.md](docs/implementation.md).

## Documentation

| Document | Content |
|---|---|
| [docs/brief.md](docs/brief.md) | SDLC brief: scope, requirements, use cases, design |
| [docs/architecture.md](docs/architecture.md) | Current MVVM diagram and search/add/remove sequences |
| [docs/data-contract.md](docs/data-contract.md) | TMDB mapping, Room schema, UI and failure behavior |
| [docs/implementation.md](docs/implementation.md) | Course API mapping, Figma targets, assets, validation log |

The diagrams in `architecture.md` match the implemented app. `mvv-model.png` and the two sequence-diagram PNGs in `docs/` are planning-phase drawings.

## Known limits

- "Top 5 this week" (kept from Figma) shows the first five TMDB popular movies, not a separate weekly-trending ranking.
- The Home filter icon only opens Search; no filter dialog was specified.
- Native widgets approximate the Figma design (CheckBox appearance, simplified selection toolbar, shadows, status bar); this is deliberate to stay within course widgets.
- Not yet verified: a live network failure/timeout, a physical-device or minimum-SDK-26 run, scrolling/memory benchmarks, a large-font/TalkBack audit, and release-store validation.
- The API key remains extractable from an APK (see Run locally).

## Credits
 
- Movie data and images: [The Movie Database (TMDB)](https://www.themoviedb.org/). This product uses the TMDB API but is not endorsed or certified by TMDB.
- Typeface: Inter, bundled with its license in `app/src/main/assets/`.
