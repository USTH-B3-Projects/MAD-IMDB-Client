# IMDB Client - Android Movie Discovery Application

## IMDB Data Contract

### 1. Data Models
- Movie
- MovieDetails
- FavoriteMovie

### 2. Data Types & JSON Mapping

| TMDB JSON Field | App Field | Data Type | Model |
|---|---|---|---|
| id | id | int | Movie, MovieDetails, FavoriteMovie |
| title | title | String | Movie, MovieDetails, FavoriteMovie |
| release_date | releaseDate | String | Movie, MovieDetails, FavoriteMovie |
| poster_path | posterPath | String | Movie, MovieDetails, FavoriteMovie |
| vote_average | voteAverage | double | Movie, MovieDetails, FavoriteMovie |
| backdrop_path | backdropPath | String | MovieDetails |
| overview | overview | String | Movie, MovieDetails |
| runtime | runtime | int | MovieDetails |
| genres | genres | List<Genre> | MovieDetails |
| credits.cast | cast | List<CastMember> | MovieDetails |

`FavoriteMovie` will retrieve corresponding fields from Movie/MovieDetails when user adds to Favorites.

Image Resolution Base URLs:
- Poster: `https://image.tmdb.org/t/p/w500{poster_path}`
- Backdrop: `https://image.tmdb.org/t/p/w780{backdrop_path}`

API Paginated Response Structure (`/movie/popular`, `/search/movie`):
| JSON Field | Description |
|---|---|
| page | Current page number |
| results | Array of matching `Movie` objects |
| total_pages | Total number of result pages |
| total_results | Total count of results |

### 3. Local Databse Design
App uses **Room Database** to store favorite movies locally on device.

Table `favorite_movies`:
| Column | Data Type | Constraint |
|---|---|---|
| id | Integer | PRIMARY KEY, NOT NULL |
| title | String | NOT NULL |
| releaseDate | String | |
| posterPath | String | |
| voteAverage | Real / Double | |

Database operations:
| Operation | Description |
|---|---|
| insertFavorite() | Add a movie to Favorites |
| getAllFavorites() | Retrieve all saved favorite movies (as LiveData/Flow) |
| deleteFavorite() | Remove a movie from Favorites |
| isFavorite(id) | Check whether a movie is already saved |

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

The application handles TMDB API HTTP status codes and error responses before processing movie data.