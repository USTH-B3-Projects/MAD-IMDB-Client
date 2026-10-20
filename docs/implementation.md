# Java/XML implementation and course constraints

The repository initially contained only README, planning documents, and diagrams.
There was no Android source, package name, Gradle build, persistence, or API client to preserve.

## Proposed structure (recorded before implementation)

```
app/src/main/
  java/vn/edu/usth/imdbclient/
    MovieApplication.java
    ui/           MainActivity, screen Fragments, list and pager adapters
    viewmodel/    MovieViewModel, screen state
    data/         Movie models, TMDB requests, Repository, Room favourites
  res/
    layout/       screen layouts, reusable search/state/movie rows
    values/       strings, dimensions, colors, light/dark styles
    drawable/     XML surfaces and downloaded Figma icons as bitmaps
  assets/         Inter font and license
app/src/test/      JSON and paging/state logic tests
app/src/androidTest/  Room and native-screen flow tests
```

## Course API mapping

Course PDFs were located at `C:/Users/ADMIN/Desktop/Course/Mobile application`.
References below use the printed slide number (PDF pages may differ due to incremental slides).

| Implementation | Course source |
| --- | --- |
| Activities/Fragments, XML inflation, standard widgets | `1._Layout.pdf`, slides 8–13 |
| FrameLayout layers, LinearLayout weights, RelativeLayout | `1._Layout.pdf`, slides 15–23; `2._RelativeLayout_and_ViewPagerFile.pdf`, slides 25–30 |
| ScrollView/ListView, original ViewPager and adapters | `2._RelativeLayout_and_ViewPagerFile.pdf`, slides 31–41 |
| Strings, colors, dimensions, styles, drawable bitmaps/XML | `3._Values__DrawablesFile_p2.pdf`, slides 48–59 |
| Bundled fonts/assets, popup menus and menu resources | `4._raw_and_menuFile_p2.pdf`, slides 67–79, 85–92 |
| Gesture/touch handling and runtime LayoutParams | `Advanced_Touch_InputsFile.pdf`, slides 6–9, 14–26 |
| Main/worker thread separation, Handler | `Threads__TasksFile.pdf`, slides 9–18 |
| Volley queue, callbacks, image requests and JSON parsing | `Networking_Volley__Data_representationFile.pdf`, slides 33–52 |

AndroidX AppCompat/Fragment/ViewPager are the maintained equivalents of the support-library classes shown in the course. There is no Compose, Kotlin source, WebView, ConstraintLayout, Material UI, or third-party UI framework. Lifecycle ViewModel/LiveData and Room belong to the permitted architecture/data layer. Only ordinary widgets and XML resource styling compose the interface. Rounded surfaces approximate the Figma shadows; no custom drawing/shader UI is used.

## Figma targets

File `V3sRPtZtIOTvfpqxZmUX2u`, page `0:1` (the file has one page).
Home `53:521`, Search idle `104:2450`, Search results `133:2981`, Details `133:3044`,
Favourites `67:951`, Settings `67:987`, About `2017:2603`, plus light/dark and loading/empty/error variants.
412×917 reference frames, Inter typography, background #F7F5EF / #1E211D,
surface #FFFFFF / #2B2F29, gold #FFCD34, horizontal gutter 20dp, actions 48dp.

The static example data includes TV programmes. The implementation uses the required TMDB movie endpoints and live titles/posters rather than treating the examples as application data.

## Setup

Use Android Studio's bundled JDK (17+), SDK 36.1, and the Gradle wrapper.
Add `TMDB_API_KEY=your_tmdb_v3_api_key` to untracked `local.properties`, or set the environment variable of the same name. Do not commit that file. A missing key is a recoverable configuration error, never a fake successful movie list.
Build: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
Device tests: `./gradlew :app:connectedDebugAndroidTest`.

Credentials injected into BuildConfig are excluded from Git but can be extracted from an APK; this client uses a TMDB application key, not a user secret.

## Screens and interactions

Home uses the original ViewPager for the five-poster showcase, with adjacent posters,
page indicators, discover cards and a recycling, paginated popular-movie list.
Search has suggestions, locally stored recent queries, a 400ms debounce, request
cancellation, pagination, placeholders, empty results and retry. Details show
release date, rating, runtime, overview, genres, director and cast when returned by TMDB.
Unavailable fields use explicit placeholders rather than invented values.

Favourites support persistent add/remove, duplicate-add feedback, sorting,
multi-selection, batch removal and undo. Add actions never toggle or delete an
existing favorite; removal is explicit on the Favorites screen. Selection survives
Activity recreation; its action footer replaces the bottom navigation and feedback
cannot cover the removal controls. Settings persist
light/dark appearance, confirm clearing search history without deleting favourites,
and navigate to app information and TMDB attribution. A simple XML launch background
replaces the design's animated launch concept.

## Assets and implementation files

The Figma icons and six light/dark state illustrations were downloaded and rasterized
intact into `res/drawable-nodpi`; the native app does not load temporary Figma URLs.
Three Figma poster images are bundled only for the small Settings previews (and test
fixtures). Actual movie posters come from TMDB through Volley's asynchronous image
loader, with a bounded 24MiB memory cache and the queue's disk cache. Inter is bundled
in `assets/inter.ttf` with its license.

Added files: root Gradle configuration/wrapper; `app/build.gradle`, manifest;
`MovieApplication`; `data/` models, Java data-source interface, Volley Repository,
Room DAO/database and migration; `viewmodel/` state; `ui/` Activity, six screen
Fragments, adapters and shared resource/image helpers; XML layouts, menu, styles,
colours, dimensions, drawables and assets; unit/device tests and Room schemas.
Updated files: `.gitignore`, `README.md`, the data contract, requirements summary,
and current Mermaid architecture/sequence diagrams. The implementation intentionally
uses `Movie` as both the TMDB model and Room favorite entity so loaded detail data
remains available offline without conversion or a destructive schema migration.
No original Android implementation existed to preserve.

## Validation (16 October 2026)

Command executed with Android Studio's JDK:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug
```

- Debug APK built successfully.
- 9 JVM tests passed: JSON/null parsing, credits, valid image hosts, paging,
  de-duplication, immutable state and next-page error retention.
- 3 device tests passed on Pixel_10_Pro, Android 17: the complete native screen flow;
  real Room add/duplicate/batch-remove/restore ordering; and the v1-to-v2 SQLite migration retaining
  saved movies. The screen-flow test also exercises light/dark changes, actual
  landscape/portrait rotation, selection recreation, debounce, obsolete callbacks,
  missing/empty/error states and retry. It checks that recreation does not duplicate
  successful requests and that a storage error blocks unsafe favorite writes.
- Lint: 0 errors, 34 warnings. Warnings include pinned dependency/target-SDK updates,
  nested layouts, decorative miniature text, RTL padding and unused
  resources. Gradle/Activity APIs also have deprecation notices; these were not hidden
  with a lint baseline or broad suppression.
- `git diff --check` passed; `local.properties` is ignored; production source contains
  no Kotlin, Compose, WebView, ConstraintLayout, Material UI, or temporary Figma URLs.

The emulator's restored snapshot initially produced black app surfaces. A fresh boot
with the software GPU renderer resolved this; production hardware acceleration was
not disabled. Captures use a temporary 412x917dp viewport with animation scales off
for stable comparison. They are saved locally in the ignored
`output/screenshots/screenshots/` directory: Home loading/success, Details portrait/
landscape, Favourites saved/empty/selection, Settings light/dark/confirmation, About
dark, and Search idle/loading/results/empty/error dark. Reference PNGs are in
`output/screenshots/figma-*.png`. Live production-path captures are
`output/screenshots/live-home-2026-10-16.png`,
`output/screenshots/live-details-final-2026-10-16.png`, and
`output/screenshots/live-favorites-final-2026-10-16.png`. The viewport and animation
overrides are restored after capture. These are visual/manual comparisons, not
pixel-diff assertions.

## Fidelity limits and remaining unverified behavior

Colours, Inter typography, main spacing, carousel proportions, bottom tabs, state
illustrations and rounded native XML surfaces were compared with Figma screenshots.
The implementation deliberately uses supported course widgets: native CheckBox
appearance, a simplified selection toolbar, small XML Settings previews, loading
placeholders and shadows are approximations. Genre chips are limited to two with
ellipsis. Native system bars/insets differ from the static Figma status-bar drawings;
long movie titles and extra ratings/full dates reflow the Details layout.

The label “Top 5 this week” is retained from Figma, but currently displays the first
five results from TMDB popular movies, not a separate weekly-trending ranking.
The Home filter icon opens Search; no filter dialog was specified. Screenshot
fixtures deliberately reuse three posters and do not represent real movie metadata.

A locally configured, ignored TMDB key was available for this pass. The production
debug APK loaded live popular titles/posters, returned a live “Interstellar” search,
opened live Spider-Man details with backdrop, genres, runtime and overview, saved it,
reported a duplicate add without removing it, and showed exactly `1 saved film` in
Favorites. The key value was not printed or logged. Automated UI-flow tests still use
a fake data source for deterministic loading/error/retry states, while Room tests use
a real local database. A live network-failure/timeout was not forced. There was no
physical-device/min-SDK-26 run, hardware scrolling/memory benchmark, release-store
validation, exhaustive large-font/TalkBack audit, or pixel-diff assertion.
