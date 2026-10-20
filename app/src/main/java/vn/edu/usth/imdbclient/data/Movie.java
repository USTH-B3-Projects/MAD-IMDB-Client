package vn.edu.usth.imdbclient.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Entity(tableName = "favorite_movies")
public class Movie {
    @PrimaryKey public int id;
    @NonNull public String title = "";
    public String posterPath = "";
    public String backdropPath = "";
    public String releaseDate = "";
    public String overview = "";
    public String genres = "";
    public String cast = "";
    public String director = "";
    public int runtime;
    public double voteAverage;
    @ColumnInfo(defaultValue = "0") public long savedAt;

    public static Movie fromJson(JSONObject json) {
        Movie movie = new Movie();
        movie.id = json.optInt("id");
        movie.title = text(json, "title");
        movie.posterPath = text(json, "poster_path");
        movie.backdropPath = text(json, "backdrop_path");
        movie.releaseDate = text(json, "release_date");
        movie.overview = text(json, "overview");
        movie.runtime = Math.max(0, json.optInt("runtime"));
        double rating = json.optDouble("vote_average", 0);
        movie.voteAverage = Double.isFinite(rating) ? Math.max(0, Math.min(10, rating)) : 0;
        JSONArray genres = json.optJSONArray("genres");
        if (genres != null) movie.genres = names(genres, "name", 8);
        JSONObject credits = json.optJSONObject("credits");
        if (credits != null) {
            movie.cast = names(credits.optJSONArray("cast"), "name", 12);
            JSONArray crew = credits.optJSONArray("crew");
            List<String> directors = new ArrayList<>();
            if (crew != null) for (int i = 0; i < crew.length(); i++) {
                JSONObject person = crew.optJSONObject(i);
                if (person != null && "Director".equals(person.optString("job"))) {
                    String name = text(person, "name");
                    if (!name.isEmpty()) directors.add(name);
                }
            }
            movie.director = String.join(", ", directors);
        }
        return movie;
    }

    private static String text(JSONObject json, String field) {
        return json.isNull(field) ? "" : json.optString(field, "").trim();
    }

    private static String names(JSONArray list, String field, int limit) {
        List<String> names = new ArrayList<>();
        if (list != null) for (int i = 0; i < Math.min(limit, list.length()); i++) {
            JSONObject item = list.optJSONObject(i);
            if (item != null && !text(item, field).isEmpty()) names.add(text(item, field));
        }
        return String.join(", ", names);
    }

    public String year() { return releaseDate != null && releaseDate.length() >= 4 ? releaseDate.substring(0, 4) : ""; }
    public String rating() { return String.format(Locale.getDefault(), "%.1f", voteAverage); }
    public String imageUrl(boolean backdrop) {
        String path = backdrop ? backdropPath : posterPath;
        return path != null && path.startsWith("/") ? "https://image.tmdb.org/t/p/" + (backdrop ? "w780" : "w500") + path : "";
    }
}
