# IMDB Client - Android Movie Discovery Application

## IMDB Data Contract

### 1. Data Models
- Movie
- MovieDetails
- FavoriteMovie

### 2. Data Types & JSON Mapping

| OMDb Field | Java Field | Data Type | Model |
|---|---|---|---|
| imdbID | imdbID | String | Movie, MovieDetails |
| Title | title | String | Movie, MovieDetails |
| Year | year | String | Movie, MovieDetails |
| Poster | poster | String | Movie, MovieDetails |
| Type | type | String | Movie |
| Genre | genre | String | MovieDetails |
| imdbRating | imdbRating | String | MovieDetails |
| Plot | plot | String | MovieDetails |
| Actors | actors | String | MovieDetails |
| Runtime | runtime | String | MovieDetails |
| Director | director | String | MovieDetails |

`FavoriteMovie` will retrieve corresponding field from Movie/MovieDetails when user add to Favorites.

API Response Structure:
| JSON Field | Description |
|---|---|
| Search | Array of matching movies |
| totalResults | Total number of matching results |
| Response | "True" or "False" |
| Error | Error description when the request fails |

### 3. Local Databse Design
App uses **Room Database** to store some favorite movies locally on device.

Table `favorite_movies`:
| | Data Type | Constraint |
|---|---|---|
| imdbID | String | PRIMARY KEY, NOT NULL |
| title | String | NOT NULL |
| year | String | |
| poster | String | |

Database operations:
| Operation | Description |
|---|---|
| insertFavorite() | Add a movie to Favorites |
| getAllFavorites() | Retrieve all saved favorite movies |
| deleteFavorite() | Remove a movie from Favorites |
| isFavorite() | Check whether a movie is already saved |

### 4. Data Validation & Error Handling

| Scenario | Expected Behavior |
|---|---|
| Missing movie information | Display "N/A" or an appropriate placeholder |
| Missing poster | Display a default poster image |
| No search results | Display "No movies found." |
| API request failure | Display an error message with a Retry option |
| Duplicate favorite movie | Prevent duplicate entries |
| Empty Favorites list | Display "No favorite movies yet." |
| Local database error | Display an appropriate error message |

The application checks the OMDb API response status before processing movie data.