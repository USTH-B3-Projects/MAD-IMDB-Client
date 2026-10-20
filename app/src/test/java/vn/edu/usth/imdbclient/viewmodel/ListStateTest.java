package vn.edu.usth.imdbclient.viewmodel;

import org.junit.Test;
import java.util.Arrays;
import vn.edu.usth.imdbclient.data.Movie;
import vn.edu.usth.imdbclient.data.MoviePage;
import vn.edu.usth.imdbclient.data.MovieDataSource.Failure;
import static org.junit.Assert.*;

public class ListStateTest {
    private Movie movie(int id) { Movie movie = new Movie(); movie.id = id; movie.title = "Film " + id; return movie; }
    @Test public void mergesPagesWithoutDuplicatesAndLeavesOldStateUnchanged() {
        ListState first = ListState.empty().received(new MoviePage(Arrays.asList(movie(1), movie(2)), 1, 2));
        ListState second = first.loading().received(new MoviePage(Arrays.asList(movie(2), movie(3)), 2, 2));
        assertEquals(2, first.movies.size()); assertEquals(3, second.movies.size());
        assertEquals(3, second.movies.get(2).id); assertFalse(second.loading); assertFalse(second.hasMore());
    }
    @Test public void nextPageFailureKeepsLoadedMoviesAndCanBeRetried() {
        ListState first = ListState.empty().received(new MoviePage(Arrays.asList(movie(1)), 1, 4));
        ListState error = first.loading().failed(Failure.NETWORK);
        assertEquals(1, error.movies.size()); assertEquals(1, error.page); assertTrue(error.hasMore());
        assertEquals(Failure.NETWORK, error.error);
        assertNull(error.loading().error); assertTrue(error.loading().loading);
    }
    @Test(expected = UnsupportedOperationException.class) public void publishedListsCannotBeMutated() {
        ListState.empty().movies.add(movie(1));
    }
}
