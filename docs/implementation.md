# Java/XML Implementation and Course Constraints

This document records how the app maps to the course material, the design targets, and the validation log. Setup and build commands are in the README.

## Course API Mapping

The course PDFs are the Mobile Application Development slides. References below use the printed slide number (PDF pages may differ due to incremental slides).

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

AndroidX AppCompat/Fragment/ViewPager are the maintained equivalents of the support-library classes shown in the course. Lifecycle ViewModel/LiveData and Room belong to the permitted architecture/data layer. Rounded surfaces approximate the Figma shadows; no custom drawing/shader UI is used. The full list of excluded frameworks is in the [README](../README.md#structure-and-responsibilities).

## Figma Targets

Figma file: [open in Figma](https://www.figma.com/design/V3sRPtZtIOTvfpqxZmUX2u) (file key `V3sRPtZtIOTvfpqxZmUX2u`, page `0:1`; the file has one page).
Home `53:521`, Search idle `104:2450`, Search results `133:2981`, Details `133:3044`,
Favourites `67:951`, Settings `67:987`, About `2017:2603`, plus light/dark and loading/empty/error variants.
412×917 reference frames, Inter typography, background #F7F5EF / #1E211D,
surface #FFFFFF / #2B2F29, gold #FFCD34, horizontal gutter 20dp, actions 48dp.
 
The static example data includes TV programmes. The implementation uses the required TMDB movie endpoints and live titles/posters rather than treating the examples as application data.

## Setup

See [Run locally in the README](../README.md#run-locally) for the JDK, the TMDB key and the build commands. Device tests need a running emulator or connected device.

## Screens and Interactions

Behavior per screen (Home, Search, Details, Favourites, Settings, state handling) is described in [Behavior and Data in the README](../README.md#behavior-and-data). Implementation notes specific to the design:
 
- Home's showcase uses the original ViewPager with adjacent posters and page indicators. The centred poster is full size and the side posters are scaled down with a fixed gap. The showcase advances automatically (4s per movie, 0.7s slide) and pauses while the user drags it or Home is not on screen. 
- Home shows two discover cards under "More to discover". "See all" swaps them for the pagninated movie list and "Hide" swaps them back; the choice survives rotation.
- In Favourites, the selection action footer replaces the bottom navigation, and feedback cannot cover the removal controls.
- A simple XML launch background replaces the design's animated launch concept.

## Assets

The Figma icons and six light/dark state illustrations were downloaded and rasterized
intact into `res/drawable-nodpi`; the native app does not load temporary Figma URLs.
Three Figma poster images are bundled only for the small Settings previews (and test
fixtures). Actual movie posters come from TMDB through Volley's asynchronous image
loader, with a bounded 24MiB memory cache and the queue's disk cache. Inter is bundled
in `assets/inter.ttf` with its license.

## Validation (recorded run, 16 October 2026)

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

Visual comparison against Figma used a temporary 412x917dp viewport with animation scales off
for stable captures; the overrides were restored afterwards. Captures covered Home loading/success,
Details portrait/landscape, Favourites saved/empty/selection, Settings light/dark/confirmation,
About dark, and Search idle/loading/results/empty/error dark, plus live production-path
captures of Home, Details and Favourites. The screenshots are kept locally and are not in
this repository. These are visual/manual comparisons, not pixel-diff assertions.
 
Troubleshooting: if the emulator's restored snapshot shows black app surfaces, do a cold
boot with the software GPU renderer. Production hardware acceleration is not disabled.

## Fidelity Limits and Remaining Unverified Behavior

Colours, Inter typography, main spacing, carousel proportions, bottom tabs, state
illustrations and rounded native XML surfaces were compared with Figma screenshots.
The implementation deliberately uses supported course widgets: native CheckBox
appearance, a simplified selection toolbar, small XML Settings previews, loading
placeholders and shadows are approximations. Genre chips are limited to two with
ellipsis. Native system bars/insets differ from the static Figma status-bar drawings;
long movie titles and extra ratings/full dates reflow the Details layout.

The known functional limits ("Top 5 this week" label, Home filter icon) are listed under Known limits in the README. Screenshot fixtures deliberately reuse three posters and do not represent real movie metadata.

A locally configured, ignored TMDB key was available for the live check. The production
debug APK loaded live popular titles/posters, returned a live "Interstellar" search,
opened live Spider-Man details with backdrop, genres, runtime and overview, saved it,
reported a duplicate add without removing it, and showed exactly `1 saved film` in
Favorites. The key value was not printed or logged. Automated UI-flow tests still use
a fake data source for deterministic loading/error/retry states, while Room tests use
a real local database. A live network-failure/timeout was not forced. There was no
physical-device/min-SDK-26 run, hardware scrolling/memory benchmark, release-store
validation, exhaustive large-font/TalkBack audit, or pixel-diff assertion.
