# IMDB-client

A native Android movie discovery app for the Mobile Application Development course, built with Java, XML, Volley, Room and the TMDB API.

## Run locally

Open this directory in Android Studio (bundled JDK 17+, SDK 36.1) and use the Gradle wrapper. A TMDB v3 API key is required:

```properties
# local.properties (untracked) -- or set an environment variable of the same name
TMDB_API_KEY=your_tmdb_v3_api_key
```

Build and test:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

Requirements from the [brief](docs/brief.md): minimum SDK 26 (Android 8.0), target SDK 34+, designed for phones in portrait orientation. The device tests need a running emulator or connected device.

The key is injected into `BuildConfig` at build time and is never committed or logged. A missing key is a recoverable configuration error shown in the UI, never a fake movie list. Like any client-side key, it can be extracted from a built APK, so use a restricted TMDB application key.

## Structure and responsibilities

The app uses MVVM with the Repository pattern. Fragments render state, one shared ViewModel holds presentation state, the repository is the only data-access entry point, and TMDB and Room are the two data sources. See [architecture.md](docs/architecture.md) for the diagrams.

```text
app/src/main/
  java/vn/edu/usth/imdbclient/
    MovieApplication.java     Application setup
    ui/                       MainActivity, six screen Fragments, list and pager adapters, image/resource helpers
    viewmodel/                MovieViewModel (activity-scoped), screen state
    data/                     Movie model, MovieDataSource, Volley MovieRepository, Room DAO/database/migration
  res/
    layout/                   Screen layouts, reusable search/state/movie rows
    values/                   Strings, dimensions, colours, light/dark styles
    drawable/, drawable-nodpi/ XML surfaces, Figma icons and state illustrations as bitmaps
  assets/                     Inter font and licence
app/src/test/                 JVM tests: JSON parsing, paging, de-duplication, state
app/src/androidTest/          Device tests: Room, migration, native screen flow
```

Layer responsibilities:

- **View**: `MainActivity` plus Home, Search, Details, Favorites, Settings and About Fragments inflate XML layouts, observe `LiveData` and forward user actions.
- **ViewModel**: the activity-scoped `MovieViewModel` drives loading, success, empty, error, retry, feedback, busy-ID and favorites states. `SavedStateHandle` keeps the active tab, query, selection, sorting and scroll positions across recreation.
- **Repository**: `MovieRepository` wraps Volley requests and JSON parsing (off the main thread) and serializes all Room work on one background executor.
- **Model**: a single `Movie` class serves as the TMDB list/detail model and the Room `favorite_movies` entity (intentional; see the [data contract](docs/data-contract.md)).

Frontend constraints follow the course material: Activities/Fragments, standard widgets, XML resources, the original `ViewPager` and a recycling `ListView`. There is no Compose, Kotlin source, WebView, ConstraintLayout, Material UI or third-party UI framework. The course-slide mapping is in [implementation.md](docs/implementation.md).

## Behavior and data

- **Home**: `ViewPager` showcase of five posters, discover cards and a paginated popular-movie list.
- **Search**: suggestions, locally stored recent queries, 400 ms debounce, cancellation of the previous request, pagination, and empty/error/retry states. Obsolete callbacks are ignored through generation IDs.
- **Details**: release date, rating, runtime, overview, genres, director and cast when TMDB returns them. Missing fields show "Not available" or an image placeholder, never invented values.
- **Favorites**: persistent add/remove in Room, sorting, multi-selection, batch removal and Undo. Add never toggles: a duplicate add keeps the stored row and shows "Already in Favourites". Removal is explicit and only available from Favorites.
- **Settings**: light/dark theme and search-history clearing (favorites are untouched) stored in `SharedPreferences`, plus About and TMDB attribution.
- **State**: rotation and Activity recreation retain the current `ListState` without re-requesting data. Selection survives recreation.
- **Reliability**: network requests use a 10-second timeout; failures show an informative state with Retry. A storage error disables favorite writes and offers Retry.
- **Images**: posters load through Volley's `ImageLoader` with a bounded 24 MiB memory cache plus the queue's disk cache.

Out of scope: user authentication, user accounts, cloud synchronization and a custom backend.

## External API and storage

TMDB is used through these requests (base URL `https://api.themoviedb.org/3/`):

| Use case | Request | Source |
|---|---|---|
| UC-01 Browse featured movies | `GET /movie/popular` | TMDB |
| UC-02 Search movies | `GET /search/movie?query={title}&page={page}` | TMDB |
| UC-03 View movie details | `GET /movie/{movie_id}?append_to_response=credits` | TMDB |
| UC-04 Manage favorites | Insert / select / delete | Room (`favorites.db`, schema v2) |
| UC-05 Settings | Read / write preferences | SharedPreferences |

Poster images use `https://image.tmdb.org/t/p/w500{path}` and backdrops use `.../w780{path}`. The client caps TMDB paging at page 500 and removes repeated movie IDs when merging pages. Field mapping, clamping rules, the Room schema and the UI failure contract are in the [data contract](docs/data-contract.md).

## Validation

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug
```

Latest recorded run (16 October 2026):

- Debug APK built successfully.
- 9 JVM tests passed (JSON/null parsing, credits, image hosts, paging, de-duplication, immutable state, next-page error retention).
- 3 device tests passed on Pixel_10_Pro, Android 17: complete native screen flow (theme change, rotation, selection recreation, debounce, empty/error states, retry), Room add/duplicate/batch-remove/restore ordering, and the v1-to-v2 migration.
- Lint: 0 errors, 34 warnings (dependency updates, nested layouts, RTL padding, unused resources, deprecations). No baseline or broad suppression is used.
- A live check with a local TMDB key loaded popular movies, searched "Interstellar", opened details, saved a movie and confirmed the duplicate add did not remove it.

UI-flow tests use a fake data source for deterministic states; Room tests use a real local database. Screenshot comparisons against Figma are visual and manual, not pixel-diff assertions. Details are in [implementation.md](docs/implementation.md).

## Documentation

| Document | Content |
|---|---|
| [docs/brief.md](docs/brief.md) | SDLC brief: scope, requirements, use cases, design |
| [docs/architecture.md](docs/architecture.md) | Current MVVM diagram and search/add/remove sequences |
| [docs/data-contract.md](docs/data-contract.md) | TMDB mapping, Room schema, UI and failure behavior |
| [docs/implementation.md](docs/implementation.md) | Course API mapping, Figma targets, assets, validation log |

The Mermaid diagrams in `architecture.md` supersede the earlier planning PNGs.

## Known limits

- "Top 5 this week" (kept from Figma) shows the first five TMDB popular movies, not a separate weekly-trending ranking.
- The Home filter icon only opens Search; no filter dialog was specified.
- Native widgets approximate the Figma design (CheckBox appearance, simplified selection toolbar, shadows, status bar); this is deliberate to stay within course widgets.
- Not yet verified: a live network failure/timeout, a physical-device or minimum-SDK-26 run, scrolling/memory benchmarks, a large-font/TalkBack audit, and release-store validation.
- The API key remains extractable from an APK (see Run locally).

