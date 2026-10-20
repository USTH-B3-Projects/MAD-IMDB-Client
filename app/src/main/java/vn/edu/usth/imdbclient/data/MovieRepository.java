package vn.edu.usth.imdbclient.data;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import vn.edu.usth.imdbclient.BuildConfig;

public final class MovieRepository implements MovieDataSource {
    private final RequestQueue queue;
    private final FavoriteDao dao;
    private final ExecutorService disk = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final MutableLiveData<List<Movie>> favorites = new MutableLiveData<>();
    private final MutableLiveData<Failure> favoritesError = new MutableLiveData<>();

    public MovieRepository(RequestQueue queue, FavoriteDao dao) {
        this.queue = queue;
        this.dao = dao;
        refreshFavorites();
    }

    @Override public void popular(int page, Object tag, Callback<MoviePage> callback) {
        request("movie/popular", "", page, tag, MoviePage::fromJson, callback);
    }
    @Override public void search(String query, int page, Object tag, Callback<MoviePage> callback) {
        request("search/movie", query, page, tag, MoviePage::fromJson, callback);
    }
    @Override public void details(int id, Object tag, Callback<Movie> callback) {
        request("movie/" + id, "", 0, tag, json -> {
            Movie movie = Movie.fromJson(json);
            if (movie.id != id || movie.title.isEmpty()) throw new IllegalArgumentException("Invalid movie");
            return movie;
        }, callback);
    }

    private interface Parser<T> { T parse(JSONObject json) throws Exception; }

    private <T> void request(String path, String query, int page, Object tag, Parser<T> parser, Callback<T> callback) {
        if (BuildConfig.TMDB_API_KEY.trim().isEmpty()) {
            main.post(() -> callback.failure(Failure.CONFIGURATION));
            return;
        }
        Uri.Builder url = Uri.parse("https://api.themoviedb.org/3/" + path).buildUpon()
                .appendQueryParameter("api_key", BuildConfig.TMDB_API_KEY)
                .appendQueryParameter("language", "en-US");
        if (page > 0) url.appendQueryParameter("page", String.valueOf(page));
        if (!query.isEmpty()) url.appendQueryParameter("query", query).appendQueryParameter("include_adult", "false");
        if (page == 0) url.appendQueryParameter("append_to_response", "credits");
        // Parsing happens on Volley's worker, before delivery to the main-thread callback.
        Request<T> request = new Request<T>(Request.Method.GET, url.toString(), error -> {
            int status = error.networkResponse == null ? 0 : error.networkResponse.statusCode;
            callback.failure(status == 401 || status == 403 ? Failure.CONFIGURATION
                    : status == 404 || error instanceof ParseError ? Failure.UNAVAILABLE : Failure.NETWORK);
        }) {
            @Override protected Response<T> parseNetworkResponse(NetworkResponse response) {
                try {
                    JSONObject json = new JSONObject(new String(response.data, StandardCharsets.UTF_8));
                    return Response.success(parser.parse(json), HttpHeaderParser.parseCacheHeaders(response));
                } catch (Exception error) { return Response.error(new ParseError(error)); }
            }
            @Override protected void deliverResponse(T value) { callback.success(value); }
        };
        request.setTag(tag);
        request.setRetryPolicy(new DefaultRetryPolicy(10_000, 0, 1));
        queue.add(request);
    }

    @Override public void cancel(Object tag) { queue.cancelAll(tag); }
    @Override public LiveData<List<Movie>> favorites() { return favorites; }
    @Override public LiveData<Failure> favoritesError() { return favoritesError; }
    @Override public void refreshFavorites() { disk.execute(this::refreshOnWorker); }
    private void refreshOnWorker() {
        try {
            List<Movie> movies = dao.getAll();
            main.post(() -> { favoritesError.setValue(null); favorites.setValue(movies); });
        } catch (RuntimeException error) { main.post(() -> favoritesError.setValue(Failure.STORAGE)); }
    }
    @Override public void add(Movie movie, Callback<Boolean> callback) {
        disk.execute(() -> {
            try {
                boolean added = dao.add(movie);
                refreshOnWorker();
                main.post(() -> callback.success(added));
            } catch (RuntimeException error) { main.post(() -> callback.failure(Failure.STORAGE)); }
        });
    }
    @Override public void remove(List<Movie> movies, Callback<Void> callback) {
        List<Integer> ids = new ArrayList<>();
        for (Movie movie : movies) ids.add(movie.id);
        write(() -> dao.deleteIds(ids), callback);
    }
    @Override public void restore(List<Movie> movies, Callback<Void> callback) {
        write(() -> dao.restore(movies), callback);
    }
    private void write(Runnable action, Callback<Void> callback) {
        disk.execute(() -> {
            try { action.run(); refreshOnWorker(); main.post(() -> callback.success(null)); }
            catch (RuntimeException error) { main.post(() -> callback.failure(Failure.STORAGE)); }
        });
    }
}
