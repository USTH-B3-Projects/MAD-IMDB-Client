# Current architecture and main sequences

These diagrams describe the implemented Java/XML application. They supersede
the earlier planning PNGs, which used separate screen ViewModels and separate
favorite/detail model names that were never implemented.

## MVVM and data flow

```mermaid
flowchart LR
    subgraph client ["Android UI"]
        screens["MainActivity and Fragments"]
        xml["XML resources"]
    end
    subgraph gateway ["Presentation state"]
        viewModel["Shared MovieViewModel"]
    end
    subgraph service ["Data access"]
        repository["MovieRepository"]
        imageLoader["Volley ImageLoader"]
    end
    subgraph datastore ["On-device storage"]
        room["Room FavoriteDao"]
        preferences["SharedPreferences"]
    end
    subgraph external ["External service"]
        tmdb["TMDB API and images"]
    end

    xml -->|"Inflates"| screens
    screens -->|"Actions and observers"| viewModel
    viewModel -->|"MovieDataSource"| repository
    viewModel -->|"Theme and history"| preferences
    repository -->|"Worker executor"| room
    repository -.->|"MovieRepository: HTTPS requests"| tmdb
    screens -->|"Poster requests"| imageLoader
    imageLoader -.->|"ImageLoader: HTTPS images"| tmdb
```

`MovieViewModel` is activity-scoped. `LiveData` drives loading, success, empty,
error, retry, feedback, busy-ID, and favorites states. `SavedStateHandle` keeps
the active tab, query, selection, sorting, and scroll positions across
recreation. Volley performs network and parsing work off the main thread; the
repository serializes Room work on one background executor.

## Search sequence

```mermaid
sequenceDiagram
    title Debounced movie search
    participant User
    participant SearchFragment
    participant MovieViewModel
    participant MovieRepository
    participant TMDB

    User->>SearchFragment: Enter title
    SearchFragment->>MovieViewModel: setQuery(title)
    MovieViewModel->>MovieRepository: cancel previous request
    MovieViewModel->>MovieRepository: search after 400 ms
    MovieRepository->>TMDB: GET /search/movie
    TMDB-->>MovieRepository: JSON page
    MovieRepository-->>MovieViewModel: MoviePage callback
    MovieViewModel-->>SearchFragment: Publish ListState
    SearchFragment-->>User: Render results or state
```

Generation IDs ignore obsolete callbacks, and the current `ListState` is
retained rather than re-requested during ordinary Activity recreation.

## Add favorite sequence

```mermaid
sequenceDiagram
    title Add a favorite without toggling
    participant User
    participant MovieScreen
    participant MovieViewModel
    participant MovieRepository
    participant FavoriteDao

    User->>MovieScreen: Add to Favourites
    MovieScreen->>MovieViewModel: addFavorite(movie)
    MovieViewModel->>MovieRepository: add(movie)
    MovieRepository->>FavoriteDao: transactional add(movie)
    FavoriteDao-->>MovieRepository: added or duplicate
    MovieRepository-->>MovieViewModel: Refresh favorites and result
    MovieViewModel-->>MovieScreen: Saved or Already saved feedback
```

A duplicate add never deletes or replaces the stored row. Explicit single or
batch removal is available only from Favorites, and successful removal carries
the removed movies in feedback so Undo can restore them.

## Remove favorite sequence

```mermaid
sequenceDiagram
    title Explicit favorite removal and undo
    participant User
    participant FavoritesFragment
    participant MovieViewModel
    participant MovieRepository
    participant FavoriteDao

    User->>FavoritesFragment: Remove saved movie
    FavoritesFragment->>MovieViewModel: removeSelected(movies)
    MovieViewModel->>MovieRepository: remove(movies)
    MovieRepository->>FavoriteDao: deleteIds(ids)
    FavoriteDao-->>MovieRepository: Delete complete
    MovieRepository-->>MovieViewModel: Refresh favorites
    MovieViewModel-->>FavoritesFragment: Removed feedback with Undo
    User->>FavoritesFragment: Undo
    FavoritesFragment->>MovieViewModel: undo()
    MovieViewModel->>MovieRepository: restore(movies)
    MovieRepository->>FavoriteDao: save(movies)
```
