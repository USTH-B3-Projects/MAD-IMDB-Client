# IMDB Client - Android Movie Discovery Application

_based on standard SDLC Model_

## Phase 1. Project Planning
### 1.1. Problem Statement
Android App for client to **search, view movie's info, and add to favourites** through external API.

### 1.2. Objectives
- Develop a native Android app for user to browse, search, view movie's info.
- Support local favourites management.
- Integrate with a RESTful movie API.

### 1.3. Project Scope
- Movie list
- Search
- Movie details
- Favourites
- Settings
- API integration.

**Out of scope:** 
- User Authentication
- User Accounts
- Cloud Synchronization
- Custom Backend

---
## Phase 2. Requirement Analysis
### 2.1. Functional Requirements (FR)
**MUST HAVE:**
- Display list of popular movies
- Search movie by title
- View movie detailed info, poster, ratings, and release dates

**MORE CAN DO:**
- Add/Remove movie to/from favourites
- View favourite movie list
- Access app Settings/About

### 2.2. Non-functional Requirements (NFR)

| Category | Requirement | Specification & Target |
| :--- | :--- | :--- |
| **Usability** | Intuitive navigation & accessible controls | • All primary sections (Home, Search, Favorites, Settings) reachable within **≤ 2 taps** from anywhere in the app.<br>• Core interactive buttons provide a touch area of at least **48x48 dp**. |
| **Performance** | Responsive UI & API conservation | • All network and database operations run asynchronously off the main UI thread to prevent UI freezing (zero ANRs).<br>• Loading indicators displayed within **≤ 100ms** of any user action.<br>• Client renders data within **≤ 500ms** of receiving API response (overall display target **≤ 2.0s** under typical network conditions).<br>• Search input delayed (debounced by ~400ms) to prevent unnecessary API queries.<br>• Movie poster images loaded and cached asynchronously without lagging list scrolling. |
| **Reliability** | Safe error handling & offline feedback | • Network requests enforce a **10-second timeout**.<br>• Network failures, timeouts, and empty results display informative UI feedback ("Retry" option, "No results found") instead of terminating/crashing the app. |
| **Compatibility** | Standard Android device support | • **Minimum SDK:** API 26 (Android 8.0).<br>• **Target SDK:** API 34+ (Android 14).<br>• Designed for phone form factors in portrait orientation. |
| **Security** | API key protection | • TMDB API key excluded from public VCS; stored locally (e.g., via `local.properties` or environment variables) and injected at build time via `BuildConfig`. |
| **Maintainability** | Architectural separation & state persistence | • Strict separation between UI, business logic, and data sources (MVVM + Repository).<br>• Screen rotation preserves the current user state without re-fetching data from the API. |

### 2.3. Use Case Analysis
- Use Case Diagram: [Use Case Diagram](use-case-diagram.png)

- Use Case Description:
    | Field | Description |
    | --- | --- |
    | ID, Name | UC-01: Browse Featured Movies |
    | Actor | Guest User |
    | Precondition | App is launched, user can access the Home screen |
    | Postcondition | The available featured movies are displayed with their posters, titles, and ratings. |
    | Main Flow | 1. User opens the Home screen.<br>2. System retrieves the featured movie list from the API.<br>3. System displays the featured showcase and two discover cards.<br>4. User taps See all.<br>5. System replaces the cards with the full movie list.<br>6. User scrolls through the movie list. |
    | Alternative Flow | No featured Movies available: The system displays a message "No movies available." | 

    | Field | Description |
    | --- | --- |
    | ID, Name | UC-02: Search Movies |
    | Actor | Guest User |
    | Precondition | App is launched, user can access the Search screen |
    | Postcondition | The matching movies are displayed with their posters, titles, and ratings |
    | Main Flow | 1. User opens the Search screen.<br>2. User enters movie's title in the search bar.<br>3. System displays the matching movies are displayed with their posters, titles, and ratings. |
    | Alternative Flow | No matching movies<br>→ The system displays a message such as "No movies found." | 

    | Field | Description |
    | --- | --- |
    | ID, Name | UC-03: View Movie Details |
    | Actor | Guest User |
    | Precondition | App is launched, user can access a movie list or search results. |
    | Postcondition | The selected movie's details are displayed, including poster, title, rating, release date, runtime, genres, summary, director, and cast. |
    | Main Flow | 1. User selects a movie from the Home screen or search results.<br>2. System navigates to the Movie Details screen<br>3. System retrieves the selected movie's info<br>4. System displays its poster, title, rating, release date, runtime, genres, summary, director, and cast. |
    | Alternative Flow | Movie Info Unavailable<br>→ The system displays a message such as "Unavailable movie details." |

    | Field | Description |
    | --- | --- |
    | ID, Name | UC-04: Manage Favourites |
    | Actor | Guest User |
    | Precondition | App is launched, user can access Movie Details or the Favourites screen. |
    | Postcondition | The Favourites list is updated and saved locally. |
    | Main Flow | 1. User selects a movie from the Home screen or search results.<br>2. System displays the Movie Details screen.<br>3. User adds the selected movie to Favourites.<br>4. System saves the movie to the Favourites list.<br>5. System displays confirmation message.<br>6. User opens the favourites screen.<br>7. System displays the saved movies. |
    | Alternative Flow | **Remove Favourite:** User removes a movie from Favourites. System updates the list.<br>**Undo after adding:** Right after a movie is added, the notification offers Undo; selecting it removes that movie again and displays a notification.<br>**Undo after removal:** After a movie is removed from Favourites, the notification offers Undo; selecting it restores the removed movie.<br>**Movie already in Favourites:** System prevents the duplicate without removing the saved movie. On the Movie Details screen, the button reads "Already in Favourites" and does nothing; elsewhere a notification is displayed (no Undo is offered).<br>**Empty Favourites list:** System displays "No favourite movies yet". |

    | Field | Description |
    | --- | --- |
    | ID, Name | UC-05: View Settings |
    | Actor | Guest User |
    | Precondition | App is launched, user can access Settings screen. |
    | Postcondition | The selected app preferences are updated and saved locally. |
    | Main Flow | 1. User opens the Settings screen.<br>2. System displays the available settings<br>3. User selects a setting (eg: Theme).<br>4. User changes the selected preference.<br>5. System saves the updated preference and applies the change. |
    | Alternative Flow | **View App Info:** User selects About App. System displays app info and version.<br>**No Changes:** User exits Settings without changing any preferences. |

---
## Phase 3. System Design
### 3.1. Architecture Design
Using MVVM (Model - View - ViewModel) with the Repository Pattern: [current architecture and sequences](architecture.md)
- **View:** Interface, display info and receive user interactions. It includes:
    * MainActivity/HomeFragment
    * SearchFragment
    * DetailsFragment
    * FavouritesFragment
    * SettingsFragment
    * AboutFragment
    * XML layout files

- **ViewModel:** The activity-scoped `MovieViewModel` is the interface between all screens and the repository. Its roles are:
    * Receive user actions from View
    * Request data from Repository
    * Manage loading, success, empty, error, retry, feedback and favourites states
    * Provide data for View to display

- **Model:** A single `Movie` class represents TMDB list/detail data and the Room favourite entity. Fields are listed in [Data Design](#35-data-design) and the [data-contract.md](data-contract.md).

- **Repository:** Data access management, provide an abstraction between ViewModel and data sources.

    | Data Source | Role |
    |---|---|
    | Movie API (TMDB) | GET movie list, search, details |
    | Database | Store and retrieve favourite movies |

- **Data Flow:** View → shared `MovieViewModel` → `MovieRepository` → TMDB / Room. See the [current architecture](architecture.md#mvvm-and-data-flow).

### 3.2. Sequence Diagrams for Main Features
- UC-02: [Search Movies](architecture.md#search-sequence)
- UC-04: [Add Favourite](architecture.md#add-favourite-sequence) and [Remove Favourite](architecture.md#remove-favourite-sequence)

### 3.3. UI/UX Design
The screens follow a Figma design: [open in Figma](https://www.figma.com/design/V3sRPtZtIOTvfpqxZmUX2u). It covers Home, Search, Details, Favourites, Settings and About in light and dark mode, with loading, empty and error variants. Frame IDs and design tokens are in [implementation.md](implementation.md#figma-targets).

### 3.4. API Design
#### a. API Endpoints
- **[TMDB API](https://developer.themoviedb.org):** The Movie Database API
- **Base URL:** `https://api.themoviedb.org/3/`
- **Image Base URL:** `https://image.tmdb.org/t/p/w500`
- **Authentication:** TMDB v3 API key sent as the `api_key` query parameter (the v4 Bearer token is not used)
- **Response Format:** JSON

| Use Case | Request | Data Source |
|---|---|---|
| UC-01 Browse Featured Movies | `GET /movie/popular` | TMDB |
| UC-02 Search Movies | `GET /search/movie?query={title}&page={page}` | TMDB |
| UC-03 View Movie Details | `GET /movie/{movie_id}?append_to_response=credits` | TMDB |
| UC-04 Manage Favourites | Insert / Select / Delete | Room Database |
| UC-05 View Settings | Read / Write Preferences | SharedPreferences |

#### b. API Request & Response Design
| Use Case | Response Key Fields |
| --- | --- |
| UC-01 | id, title, poster_path, vote_average, release_date |
| UC-02 | id, title, poster_path, vote_average, release_date |
| UC-03 | id, title, poster_path, backdrop_path, genres, vote_average, overview, runtime, release_date, credits (cast & crew) |

### 3.5. Data Design
| Model | Fields | Purpose |
|---|---|---|
| Movie | id, title, posterPath, backdropPath, genres, voteAverage, overview, runtime, releaseDate, cast, director, savedAt | Browse/search/detail data and the intentional Room favourite entity |

Detailed data types and database schema: [data-contract.md](data-contract.md)
