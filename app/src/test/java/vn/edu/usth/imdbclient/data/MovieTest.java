package vn.edu.usth.imdbclient.data;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class MovieTest {
    @Test public void nullablePersistedFieldsAreSafeAfterMigration() {
        Movie movie = new Movie(); movie.releaseDate = null; movie.posterPath = null; movie.backdropPath = null;
        assertEquals("", movie.year()); assertEquals("", movie.imageUrl(false)); assertEquals("", movie.imageUrl(true));
    }
    @Test public void parsesDetailsAndCredits() throws Exception {
        Movie movie = Movie.fromJson(new JSONObject("{\"id\":42,\"title\":\"A Film\",\"release_date\":\"2026-10-15\",\"runtime\":120,\"vote_average\":8.2,\"poster_path\":\"/poster.jpg\",\"genres\":[{\"name\":\"Drama\"}],\"credits\":{\"cast\":[{\"name\":\"Actor One\"}],\"crew\":[{\"name\":\"Director One\",\"job\":\"Director\"},{\"name\":\"Writer\",\"job\":\"Writer\"}]}}"));
        assertEquals(42, movie.id);
        assertEquals("2026", movie.year());
        assertEquals("Drama", movie.genres);
        assertEquals("Actor One", movie.cast);
        assertEquals("Director One", movie.director);
        assertTrue(movie.imageUrl(false).endsWith("/w500/poster.jpg"));
    }
    @Test public void nullOptionalFieldsBecomePlaceholdersWithoutLiteralNull() throws Exception {
        Movie movie = Movie.fromJson(new JSONObject("{\"id\":1,\"title\":\"Film\",\"poster_path\":null,\"release_date\":null,\"runtime\":-2,\"credits\":null}"));
        assertEquals("", movie.posterPath);
        assertEquals("", movie.year());
        assertEquals("", movie.cast);
        assertEquals("", movie.imageUrl(false));
        assertEquals(0, movie.runtime);
        assertEquals(0, movie.voteAverage, 0);
    }
    @Test public void imagePathCannotReplaceTheImageHost() throws Exception {
        Movie movie = Movie.fromJson(new JSONObject("{\"poster_path\":\"https://untrusted.example/p.jpg\",\"vote_average\":999}"));
        assertEquals("", movie.imageUrl(false));
        assertEquals(10, movie.voteAverage, 0);
    }
    @Test public void skipsInvalidRowsAndCapsTmdbPagination() throws Exception {
        MoviePage page = MoviePage.fromJson(new JSONObject("{\"page\":1,\"total_pages\":2000,\"results\":[null,{\"id\":0,\"title\":\"Invalid\"},{\"id\":1,\"title\":\"\"},{\"id\":2,\"title\":\"Valid\"}]}"));
        assertEquals(1, page.movies.size());
        assertEquals(2, page.movies.get(0).id);
        assertEquals(500, page.totalPages);
    }
    @Test(expected = org.json.JSONException.class) public void malformedPageIsAnErrorRatherThanEmptySuccess() throws Exception {
        MoviePage.fromJson(new JSONObject("{\"status_message\":\"Invalid API key\"}"));
    }
}
