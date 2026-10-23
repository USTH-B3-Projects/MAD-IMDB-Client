# IMDB Client Data Contract

## Intentional Model Design

The application uses one `Movie` class for TMDB list responses, detail responses,
and the Room `favorite_movies` entity. A separate `FavoriteMovie` or
`MovieDetails` transfer model is unnecessary here: saving the same object avoids
conversion code and keeps already-loaded details available offline. This is the
intentional implementation and does not require a database migration.

## TMDB Mapping

| TMDB JSON field | `Movie` field | Java type | Missing or invalid value |
|---|---|---|---|
| `id` | `id` | `int` | Item rejected when non-positive |
| `title` | `title` | `String` | Item rejected when empty |
| `poster_path` | `posterPath` | `String` | Empty; placeholder shown |
| `backdrop_path` | `backdropPath` | `String` | Empty; poster may be used |
| `release_date` | `releaseDate` | `String` | Empty; “Not available” shown |
| `overview` | `overview` | `String` | Empty; “Not available” shown |
| `runtime` | `runtime` | `int` | Clamped to zero |
| `vote_average` | `voteAverage` | `double` | Clamped to 0–10 |
| `genres[].name` | `genres` | comma-separated `String` | Empty; at most the first 8 names are kept |
| `credits.cast[].name` | `cast` | comma-separated `String` | Empty; at most the first 12 names are kept |
| `credits.crew[job=Director].name` | `director` | comma-separated `String` | Empty |

Paginated `/movie/popular` and `/search/movie` responses map `page`,
`total_pages`, and valid `results` into `MoviePage`. The client caps TMDB paging
at page 500 and removes repeated movie IDs while merging pages.

Image URLs are accepted only for TMDB paths beginning with `/`:

- Poster: `https://image.tmdb.org/t/p/w500{posterPath}`
- Backdrop: `https://image.tmdb.org/t/p/w780{backdropPath}`

## Room Contract

Room database `favorites.db`, schema version 2, stores the `Movie` entity in
`favorite_movies`.

| Column | SQLite / Java type | Constraint or purpose |
|---|---|---|
| `id` | INTEGER / `int` | Primary key, duplicate prevention |
| `title` | TEXT / `String` | Not null |
| `posterPath`, `backdropPath` | TEXT / `String` | Image paths |
| `releaseDate`, `overview` | TEXT / `String` | Display metadata |
| `genres`, `cast`, `director` | TEXT / `String` | Flattened detail metadata |
| `runtime` | INTEGER / `int` | Display metadata |
| `voteAverage` | REAL / `double` | Display metadata |
| `savedAt` | INTEGER / `long` | Recent-first ordering; added in v2 |

All DAO access is dispatched through the repository's single worker executor.
`add()` is transactional: it inserts and timestamps a new ID, but returns
`false` without changing the row when that ID already exists. Removal is a
separate `delete`/`deleteIds` operation. `restore()` supports undo, and the
repository refreshes the observable favorites list after successful writes.

## UI and Failure Contract

| Scenario | Required behavior |
|---|---|
| New favourite | Save once and show “Saved to Favourites” |
| Duplicate add | Keep the existing row and show “Already in Favourites” |
| Explicit removal | Remove from Favorites and offer Undo |
| Empty favourites | Show “No favourite movies yet” and a browse action |
| Storage error | Keep the current UI safe, disable writes, and offer Retry |
| Missing movie fields | Show “Not available” or the image placeholder |
| Empty search | Show “No movies found” |
| Network/API failure | Show an informative state and Retry |

The API key is injected from ignored local configuration into `BuildConfig`.
It is never logged. As with any client-side API credential, it remains
extractable from a built APK, so use a key created only for this project and regenerate it if it leaks.
