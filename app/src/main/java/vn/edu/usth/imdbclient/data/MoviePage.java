package vn.edu.usth.imdbclient.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public final class MoviePage {
    public final List<Movie> movies;
    public final int page;
    public final int totalPages;

    public MoviePage(List<Movie> movies, int page, int totalPages) {
        this.movies = movies;
        this.page = page;
        this.totalPages = Math.min(500, Math.max(page, totalPages));
    }

    public static MoviePage fromJson(JSONObject json) throws JSONException {
        JSONArray results = json.getJSONArray("results");
        List<Movie> movies = new ArrayList<>();
        for (int i = 0; i < results.length(); i++) {
            JSONObject item = results.optJSONObject(i);
            if (item == null) continue;
            Movie movie = Movie.fromJson(item);
            if (movie.id > 0 && !movie.title.isEmpty()) movies.add(movie);
        }
        return new MoviePage(movies, Math.max(1, json.optInt("page", 1)), json.optInt("total_pages", 1));
    }
}
