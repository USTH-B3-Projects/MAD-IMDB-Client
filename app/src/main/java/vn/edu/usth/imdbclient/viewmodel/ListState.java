package vn.edu.usth.imdbclient.viewmodel;

import vn.edu.usth.imdbclient.data.Movie;
import vn.edu.usth.imdbclient.data.MovieDataSource.Failure;
import vn.edu.usth.imdbclient.data.MoviePage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ListState {
    public final List<Movie> movies;
    public final boolean loading;
    public final Failure error;
    public final int page;
    public final int totalPages;

    public ListState(List<Movie> movies, boolean loading, Failure error, int page, int totalPages) {
        this.movies = Collections.unmodifiableList(new ArrayList<>(movies));
        this.loading = loading;
        this.error = error;
        this.page = page;
        this.totalPages = totalPages;
    }
    public static ListState empty() {
        return new ListState(Collections.emptyList(), false, null, 0, 1);
    }

    public ListState loading() {
        return new ListState(movies, true, null, page, totalPages);
    }

    public ListState failed(Failure failure) {
        return new ListState(movies, false, failure, page, totalPages);
    }

    public ListState received(MoviePage response) {
        List<Movie> merged = new ArrayList<>(movies);
        Set<Integer> ids = new HashSet<>();
        for (Movie movie : merged) {
            ids.add(movie.id);
        }
        for (Movie movie : response.movies) {
            if (ids.add(movie.id)) {
                merged.add(movie);
            }
        }
        return new ListState(merged, false, null, response.page, response.totalPages);
    }

    public boolean hasMore() {
        return page < totalPages;
    }
}
