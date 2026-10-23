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
        room["Room FavouriteDao"]
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

The debounce timer lives in `MovieViewModel`: each new query cancels the previous
request and pending timer, and the query is saved to the recent-search history
only when the timer fires. Generation IDs ignore obsolete callbacks, and the
current `ListState` is retained rather than re-requested during ordinary
Activity recreation.

## Add Favourite Sequence

```mermaid
sequenceDiagram
    title Add a favourite without toggling
    participant User
    participant MovieScreen
    participant MovieViewModel
    participant MovieRepository
    participant FavouriteDao

    User->>MovieScreen: Add to Favourites
    MovieScreen->>MovieViewModel: addFavourite(movie)
    MovieViewModel->>MovieRepository: add(movie)
    MovieRepository->>FavouriteDao: transactional add(movie)
    FavouriteDao-->>MovieRepository: added or duplicate
    MovieRepository-->>MovieViewModel: Refresh favourites and result
    MovieViewModel-->>MovieScreen: Saved or Already saved feedback
```

A duplicate add never deletes or replaces the stored row. Explicit single or
batch removal is available only from Favourites, and successful removal carries
the removed movies in feedback so Undo can restore them.

## Remove Favourite Sequence

```mermaid
sequenceDiagram
    title Explicit favourite removal and undo
    participant User
    participant FavouritesFragment
    participant MovieViewModel
    participant MovieRepository
    participant FavouriteDao

    User->>FavouritesFragment: Remove saved movie
    FavouritesFragment->>MovieViewModel: removeSelected(movies)
    MovieViewModel->>MovieRepository: remove(movies)
    MovieRepository->>FavouriteDao: deleteIds(ids)
    FavouriteDao-->>MovieRepository: Delete complete
    MovieRepository-->>MovieViewModel: Refresh favourites
    MovieViewModel-->>FavouritesFragment: Removed feedback with Undo
    User->>FavouritesFragment: Undo
    FavouritesFragment->>MovieViewModel: undo()
    MovieViewModel->>MovieRepository: restore(movies)
    MovieRepository->>FavouriteDao: restore(movies)
```
