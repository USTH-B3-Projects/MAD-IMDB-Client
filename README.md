# IMDB-client
(for Mobile Application Development course)

The native Android app is implemented in **Java and XML** under `app/`.
See [implementation notes](docs/implementation.md) for the course API mapping,
project structure, Figma targets, and validation details.

To run it, open this directory in Android Studio and add a TMDB v3 API key as
`TMDB_API_KEY` in your untracked `local.properties` file (or environment).
Use the Gradle wrapper to build and test:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

The app uses Volley for TMDB requests and cached images, Room for local favourites,
and Java ViewModel/LiveData for lifecycle-aware state. Its frontend uses the
course's Activities/Fragments, standard widgets, XML resources, original ViewPager,
and recycling ListView. No API key is included in the repository.
