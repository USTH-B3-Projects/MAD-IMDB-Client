# IMDB Client - Android Movie Discovery Application

_based on standard SDLC Model_
---
## Phase 1. Project Planning
### 1.1. Problem Statement
- Android App for client to **search, view movie's info, and add to favorites** through external API.

### 1.2. Objectives
- Develop a native Android app for user to browse, search, view movie's info.
- Support local favorites management.
- Integrate with a RESTful movie API.

### 1.3. Project Scope (HIGHLY ATTENTION)
- Movie list
- Search
- Movie details
- Favorites
- Settings
- API integration.

**ATTENTION - OUT OF SCOPE:** 
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
- Add/Remove movie to/from favorites
- View favorite movie list
- Access app Settings/About

### 2.2. Non-functional Requirements (NFR)

| NFR | Requirement |
| --- | --- |
| Usability | Clear navigation, main functions are easy to find |
| Performance | Display loading and do not lock UI when calling API |
| Reliability | Handle API timeout, lost network, empty results |
| Compatibility | Run on emulator and selected Android versions |
| Security | Do not hardcode API key into public repository |
| Maintainability | Separate UI, networking and data logic |

### 2.3. Use Case Analysis
- Use Case Diagram: [Use Case Diagram](use-case-diagram.png)

- Use Case Description:
    | Field | Description |
    | --- | --- |
    | ID, Name | UC-01: Browse Featured Movies |
    | Actor | Guest User |
    | Precondition | App is launched, user can access the Home screen |
    | Postcondition | The available featured movies are displayed with their posters, titles, and ratings. |
    | Main Flow | 1. User opens the Home screen.<br>2. System retrieves the featured movie list from the API.<br>3. System displays the available movies.<br>4. User scrolls through the movie list. |
    | Alternative Flow | No featured Movies available: The system displays a message "No movies available." | 

    | Field | Description |
    | --- | --- |
    | ID, Name | UC-02: Search Movies |
    | Actor | Guest User |
    | Precondition | App is launched, user can access the Search screen |
    | Postcondition | The matching movies are displayed with their posters, titles, and ratings |
    | Main Flow | 1. User opens the Search screen.<br>2. User enter movie's title in the search bar.<br>3. System displays the matching movies are displayed with their posters, titles, and ratings. |
    | Alternative Flow | No matching movies<br>→ The system displays a message such as "No movies found." | 

    | Field | Description |
    | --- | --- |
    | ID, Name | UC-03: View Movie Details |
    | Actor | Guest User |
    | Precondition | App is launched, user can access a movie list or search results. |
    | Postcondition | The selected movie's details are displayed, including poster, title, rating, summary, and casts. |
    | Main Flow | 1. User selects a movie from the Home screen or search results.<br>2. System navigates to the Movie Details screen<br>3. System retrieves the selected movie's info<br>4. System displays its poster, title, ratings, summary, and actors. |
    | Alternative Flow | Movie Info Unavailable<br>→ The system displays a message such as "Unavailable movie details." |

    | Field | Description |
    | --- | --- |
    | ID, Name | UC-04: Manage Favorites |
    | Actor | Guest User |
    | Precondition | App is launched, user can access Movie Details or the Favorites screen. |
    | Postcondition | The Favorites list is updated and saved locally. |
    | Main Flow | 1. User selects a movie from the Home screen or search results.<br>2. System displays the Movie Details screen.<br>3. User adds the selected movie to Favorites.<br>4. System saves the movie to the Favorites list.<br>5. System displays confirmation message.<br>6. User opens the favorites screen.<br>7. System displays the saved movies. |
    | Alternative Flow | **Remove Favorite:** User removes a movie from Favorites. System updates the list<br>**Movie already in Favorites:** System prevents duplicate and displays duplicate notificatio message<br>**Empty Favorites list:** System displays "No favorite movies yet". |

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
Using MVVM (Model - View - ViewModel) model + Repository Pattern: [MVVM](mvv-model.png)
- **View:** Interface, display info and receive user interactions. It includes:
    * MainActivity/HomeFragment
    * SearchFragment
    * MovieDetailFragment
    * XML layout files

- **ViewModel:** Logic of interface, intermediate between View and Repository. It roles:
    * Receive user actions from View
    * Request data from Repository
    * Manage `Loading`, `Success`, `Error` state
    * Provide data for View to display

- **Model:** Represent data structures app uses. eg: 
    ```
        public class Movie {
            int id;
            String title;
            String overview;
            String posterPath;
            String releaseDate;
            double rating;
        }
    ```

- **Repository:** Data access management, provide an abstraction between ViewModel and data sources.
| Data Source | Role |
|---|---|
| Movie API (OMDb) | GET movie list, search, details |
| Database | Store and retrieve favorite movies |

- **Data Flow:** View → ViewModel → Repository → API / Local Database

### 3.2. Sequence Diagram for MAIN feature
- UC-02: [Search Movies](uc02-sequence-diagram.png)
- UC-04: [Manage Favorites](uc04-sequence-diagram.png)

### 3.3. UI/UX Design
_na_

### 3.4. API Design
#### a. API Endpoints
- **[OMDb API](https://www.omdbapi.com):** Provides Movie Details
- **Authentication:** API Key
- **Response Format:** JSON

| Use Case | Request | Data Source |
|---|---|---|
| UC-01 Browse Featured Movies | `GET ?i={imdbID}` for each movie | OMDb |
| UC-02 Search Movies | `GET ?s={title}&type=movie` | OMDb |
| UC-03 View Movie Details | `GET ?i={imdbID}&plot=full` | OMDb |
| UC-04 Manage Favorites | Insert / Select / Delete | Room Database |
| UC-05 View Settings | Read / Write Preferences | SharedPreferences |

#### b. API Request & Response Design
| Use Case | Response |
| --- | --- |
| UC-01 | Title, Year, Poster, imdbRating, imdbID |
| UC-02 | Title, Year, imdbID, Type, Poster |
| UC-03 | Title, Year, imdbID, Poster, Genre, imdbRating, Plot, Actors, Runtime, Director |

### 3.5. Data Design
| Model | Fields | Purpose |
|---|---|---|
| Movie | imdbID, Title, Year, Type, Poster | Browse/Search Movies |
| MovieDetails | imdbID, Title, Year, Poster, Genre, imdbRating, Plot, Actors, Runtime, Director | View Movie Details |
| FavoriteMovie | imdbID, Title, Year, Poster | Store Favorites locally |

Detailed data types and database schema: [Data Contract](data-contract.md)