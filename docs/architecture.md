# Current Architecture and Main Sequences

These diagrams describe the implemented Java/XML application. They supersede
the earlier planning PNGs, which used separate screen ViewModels and separate
favourite/detail model names that were never implemented.

## MVVM and Data Flow

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
error, retry, feedback, busy-ID, and favourites states. `SavedStateHandle` keeps
the active tab, query, selection, sorting, and scroll positions across
recreation. Volley performs network and parsing work off the main thread; the
repository serializes Room work on one background executor.

## Search Sequence

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
    Note over MovieViewModel: Restart 400 ms debounce (main-thread Handler)
    MovieViewModel->>MovieRepository: search(query, page 1) after the delay
    MovieRepository->>TMDB: GET /search/movie
    TMDB-->>MovieRepository: JSON page
    MovieRepository-->>MovieViewModel: MoviePage callback
    MovieViewModel-->>SearchFragment: Publish ListState
    SearchFragment-->>User: Render results or state
```

The debounce timer lives in `MovieViewModel`: each new query cancels the previous request and pending timer. The query is saved to the recent-search history only when the user confirms it (Search key, picking a suggestion, or opening a result), so partial text typed while searching is never stored. Generation IDs ignore obsolete callbacks, and the current `ListState` is retained rather than re-requested during ordinary Activity recreation.

## Add Favourite Sequence

```mermaid
sequenceDiagram
    title Add a favourite without toggling
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
    MovieRepository-->>MovieViewModel: Refresh favourites and result
    MovieViewModel-->>MovieScreen: Saved (with Undo) or Already saved feedback
    opt User taps Undo within 4 s
        User->>MovieScreen: Undo
        MovieScreen->>MovieViewModel: undo()
        MovieViewModel->>MovieRepository: remove([movie])
        MovieRepository->>FavoriteDao: deleteIds(ids)
        MovieViewModel-->>MovieScreen: Removed feedback (no Undo)
    end
```

A duplicate add never deletes or replaces the stored row. A successful add
carries the added movie in feedback, so Undo can take it out again. Explicit
single or batch removal is available only from Favourites, and successful
removal carries the removed movies in feedback so Undo can restore them.
On the Details screen, the button already reads "Already in Favourites" for a saved movie and does not call `addFavorite`, so no duplicate feedback appears there.

## Remove Favourite Sequence

```mermaid
sequenceDiagram
    title Explicit favourite removal and undo
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
    MovieRepository-->>MovieViewModel: Refresh favourites
    MovieViewModel-->>FavoritesFragment: Removed feedback with Undo
    User->>FavoritesFragment: Undo
    FavoritesFragment->>MovieViewModel: undo()
    MovieViewModel->>MovieRepository: restore(movies)
    MovieRepository->>FavoriteDao: restore(movies)
```
