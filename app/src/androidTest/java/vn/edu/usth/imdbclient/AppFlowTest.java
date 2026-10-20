package vn.edu.usth.imdbclient;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import vn.edu.usth.imdbclient.data.Movie;
import vn.edu.usth.imdbclient.data.MovieDataSource;
import vn.edu.usth.imdbclient.data.MoviePage;
import vn.edu.usth.imdbclient.ui.MainActivity;
import vn.edu.usth.imdbclient.viewmodel.MovieViewModel;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AppFlowTest {
    private final MovieApplication application = (MovieApplication) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();

    @Test public void nativeScreensFavoritesUndoThemeAndRotation() throws Exception {
        FixtureRepository fixture = new FixtureRepository();
        MovieDataSource original = application.repository;
        ImageLoader originalImages = application.images;
        boolean originalDark = application.getSharedPreferences("settings", 0).getBoolean("dark", false);
        String originalHistory = application.getSharedPreferences("settings", 0).getString("history", "");
        RequestQueue imageQueue = Volley.newRequestQueue(application);
        Bitmap[] posters = {BitmapFactory.decodeResource(application.getResources(), R.drawable.preview_center),
                BitmapFactory.decodeResource(application.getResources(), R.drawable.preview_right),
                BitmapFactory.decodeResource(application.getResources(), R.drawable.preview_left)};
        application.repository = fixture;
        application.images = new ImageLoader(imageQueue, new ImageLoader.ImageCache() {
            @Override public Bitmap getBitmap(String key) { return posters[key.contains("/2.jpg") ? 1 : key.contains("/5.jpg") ? 2 : 0]; }
            @Override public void putBitmap(String key, Bitmap bitmap) { }
        });
        application.getSharedPreferences("settings", 0).edit().putBoolean("dark", false).putString("history", "").commit();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            screenshot("home-loading-light");
            await(scenario, activity -> activity.findViewById(R.id.featured_title) != null && ((TextView) activity.findViewById(R.id.featured_title)).length() > 0);
            screenshot("home-light");
            scenario.onActivity(activity -> {
                assertEquals(1, fixture.popularCalls);
                activity.findViewById(R.id.featured_title).performClick();
                activity.openMovie(fixture.movies.get(0));
            });
            await(scenario, activity -> activity.findViewById(R.id.detail_scroll) != null && activity.findViewById(R.id.detail_scroll).getVisibility() == View.VISIBLE);
            screenshot("details-light");
            scenario.onActivity(activity -> {
                assertTrue(((TextView) activity.findViewById(R.id.detail_overview)).getText().toString().contains("Peter Parker"));
                activity.findViewById(R.id.detail_favorite).performClick();
                assertEquals(1, fixture.saved.getValue().size());
                activity.findViewById(R.id.detail_favorite).performClick();
                assertEquals(1, fixture.saved.getValue().size());
                assertEquals(activity.getString(R.string.already_saved), ((TextView) activity.findViewById(R.id.feedback_text)).getText().toString());
                assertEquals(activity.getString(R.string.add_favorite), ((Button) activity.findViewById(R.id.detail_favorite)).getText().toString());
            });
            scenario.recreate();
            await(scenario, activity -> activity.findViewById(R.id.detail_title) != null && ((TextView) activity.findViewById(R.id.detail_title)).length() > 0);
            assertEquals(1, fixture.detailCalls);
            scenario.onActivity(activity -> activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
            await(scenario, activity -> activity.getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE && activity.findViewById(R.id.detail_title) != null);
            screenshot("details-landscape");
            scenario.onActivity(activity -> activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
            await(scenario, activity -> activity.getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT && activity.findViewById(R.id.detail_title) != null);
            assertEquals(1, fixture.detailCalls);
            scenario.onActivity(activity -> activity.showTab("favorites"));
            await(scenario, activity -> activity.findViewById(R.id.saved_count) != null && ((TextView) activity.findViewById(R.id.saved_count)).getText().toString().startsWith("1"));
            screenshot("favorites-saved-light");
            scenario.onActivity(activity -> {
                ListView list = activity.findViewById(R.id.movie_list);
                list.getChildAt(0).findViewById(R.id.favorite_action).performClick();
                assertEquals(0, fixture.saved.getValue().size());
                activity.findViewById(R.id.feedback_undo).performClick();
                assertEquals(1, fixture.saved.getValue().size());
                activity.findViewById(R.id.select_films).performClick();
            });
            await(scenario, activity -> activity.findViewById(R.id.selection_tools).getVisibility() == View.VISIBLE);
            scenario.onActivity(activity -> {
                ListView list = activity.findViewById(R.id.movie_list);
                list.getChildAt(0).findViewById(R.id.movie_select).performClick();
                assertEquals(View.GONE, activity.findViewById(R.id.bottom_navigation).getVisibility());
                assertEquals(View.GONE, activity.findViewById(R.id.feedback_bar).getVisibility());
            });
            screenshot("favorites-selection-light");
            scenario.recreate();
            await(scenario, activity -> activity.findViewById(R.id.selection_tools) != null && activity.findViewById(R.id.selection_tools).getVisibility() == View.VISIBLE);
            scenario.onActivity(activity -> {
                assertEquals(View.GONE, activity.findViewById(R.id.bottom_navigation).getVisibility());
                activity.findViewById(R.id.remove_selected).performClick();
            });
            await(scenario, activity -> activity.findViewById(R.id.state_panel).getVisibility() == View.VISIBLE);
            screenshot("favorites-empty-light");
            scenario.onActivity(activity -> fixture.storageError.setValue(MovieDataSource.Failure.STORAGE));
            scenario.onActivity(activity -> {
                new ViewModelProvider(activity).get(MovieViewModel.class).addFavorite(fixture.movies.get(0));
                assertEquals(0, fixture.saved.getValue().size());
            });
            await(scenario, activity -> ((TextView) activity.findViewById(R.id.state_title)).getText().toString().equals(activity.getString(R.string.favorites_unavailable)));
            scenario.onActivity(activity -> activity.findViewById(R.id.state_action).performClick());
            await(scenario, activity -> ((TextView) activity.findViewById(R.id.state_title)).getText().toString().equals(activity.getString(R.string.empty_favorites)));
            scenario.onActivity(activity -> activity.showTab("settings"));
            await(scenario, activity -> activity.findViewById(R.id.appearance_dark) != null);
            screenshot("settings-light");
            scenario.onActivity(activity -> activity.findViewById(R.id.clear_history).performClick());
            screenshot("settings-confirmation-light");
            scenario.onActivity(activity -> activity.findViewById(R.id.confirmation_clear).performClick());
            scenario.onActivity(activity -> activity.findViewById(R.id.appearance_dark).performClick());
            await(scenario, activity -> activity.findViewById(R.id.selected_dark) != null && activity.findViewById(R.id.selected_dark).getVisibility() == View.VISIBLE);
            screenshot("settings-dark");
            scenario.onActivity(activity -> activity.findViewById(R.id.about_app).performClick());
            await(scenario, activity -> activity.findViewById(R.id.app_version) != null);
            screenshot("about-dark");
            scenario.onActivity(activity -> activity.showTab("search"));
            await(scenario, activity -> activity.findViewById(R.id.suggestion_list) != null && activity.findViewById(R.id.suggestion_list).getVisibility() == View.VISIBLE);
            screenshot("search-idle-dark");
            scenario.onActivity(activity -> ((EditText) activity.findViewById(R.id.query)).setText("Spider-Man"));
            screenshot("search-loading-dark");
            await(scenario, activity -> activity.findViewById(R.id.movie_list).getVisibility() == View.VISIBLE);
            screenshot("search-results-dark");
            scenario.recreate();
            await(scenario, activity -> activity.findViewById(R.id.query) != null && ((EditText) activity.findViewById(R.id.query)).getText().toString().equals("Spider-Man"));
            assertEquals(1, fixture.searchCalls);
            scenario.onActivity(activity -> ((EditText) activity.findViewById(R.id.query)).setText("no-such-film"));
            await(scenario, activity -> activity.findViewById(R.id.state_panel).getVisibility() == View.VISIBLE);
            screenshot("search-empty-dark");
            scenario.onActivity(activity -> {
                fixture.searchCallbacks.get("Spider-Man").success(new MoviePage(fixture.movies, 1, 1));
                assertTrue(new ViewModelProvider(activity).get(MovieViewModel.class).search.getValue().movies.isEmpty());
            });
            scenario.onActivity(activity -> ((EditText) activity.findViewById(R.id.query)).setText("network-error"));
            await(scenario, activity -> ((TextView) activity.findViewById(R.id.state_title)).getText().toString().equals(activity.getString(R.string.network_error)));
            screenshot("search-error-dark");
            scenario.onActivity(activity -> activity.findViewById(R.id.state_action).performClick());
            await(scenario, activity -> fixture.searchCalls == 4);
            scenario.onActivity(activity -> {
                ((EditText) activity.findViewById(R.id.query)).setText("S");
                ((EditText) activity.findViewById(R.id.query)).setText("Sp");
                ((EditText) activity.findViewById(R.id.query)).setText("Spider-Man");
            });
            await(scenario, activity -> activity.findViewById(R.id.movie_list).getVisibility() == View.VISIBLE);
            assertEquals(5, fixture.searchCalls);
            assertEquals("Spider-Man", fixture.lastQuery);
        } finally {
            application.repository = original;
            application.images = originalImages;
            imageQueue.stop();
            application.getSharedPreferences("settings", 0).edit().putBoolean("dark", originalDark).putString("history", originalHistory).commit();
        }
    }

    private void await(ActivityScenario<MainActivity> scenario, Predicate<MainActivity> condition) throws Exception {
        long deadline = System.currentTimeMillis() + 8000;
        AtomicBoolean ready = new AtomicBoolean();
        do {
            scenario.onActivity(activity -> ready.set(condition.test(activity)));
            if (ready.get()) return;
            Thread.sleep(50);
        } while (System.currentTimeMillis() < deadline);
        fail("Screen did not reach the expected state within 8 seconds");
    }
    private void screenshot(String name) throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        // The view state can be ready before SurfaceFlinger presents its next frame.
        Thread.sleep(350);
        Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        int visibleSamples = 0;
        for (int y = 1; y <= 3; y++) for (int x = 1; x <= 3; x++) {
            if ((bitmap.getPixel(bitmap.getWidth() * x / 4, bitmap.getHeight() * y / 4) & 0x00ffffff) != 0) visibleSamples++;
        }
        assertTrue("Emulator returned a black app surface", visibleSamples >= 3);
        File directory = new File(application.getExternalFilesDir(null), "screenshots");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        try (FileOutputStream stream = new FileOutputStream(new File(directory, name + ".png"))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
        }
        bitmap.recycle();
    }

    // Fixtures are only in the test APK; production screens always use TMDB and Room.
    private static final class FixtureRepository implements MovieDataSource {
        final List<Movie> movies = new ArrayList<>();
        final MutableLiveData<List<Movie>> saved = new MutableLiveData<>(new ArrayList<>());
        final MutableLiveData<Failure> storageError = new MutableLiveData<>();
        final Handler main = new Handler(Looper.getMainLooper());
        final Map<Object, Integer> generations = new HashMap<>();
        final Map<String, Callback<MoviePage>> searchCallbacks = new HashMap<>();
        int popularCalls, searchCalls, detailCalls;
        String lastQuery;
        FixtureRepository() throws Exception {
            String[] titles = {"Spider-Man: Brand New Day", "Insidious: Out of the Further", "Phi Phong: The Blood Demon", "The Counterfeit Case of Ultra 30", "Maxo: Forbidden Ritual"};
            for (int i = 0; i < titles.length; i++) {
                Movie movie = Movie.fromJson(new JSONObject("{\"id\":" + (i + 1) + ",\"title\":\"" + titles[i] + "\",\"release_date\":\"2026-07-31\",\"poster_path\":\"/" + (i + 1) + ".jpg\",\"runtime\":150,\"vote_average\":8.2,\"overview\":\"A forgotten Peter Parker lives alone as a full-time Spider-Man until a powerful new enemy emerges.\",\"genres\":[{\"name\":\"Action\"},{\"name\":\"Adventure\"}],\"credits\":{\"cast\":[{\"name\":\"Tom Holland\"}],\"crew\":[{\"name\":\"Destin Daniel Cretton\",\"job\":\"Director\"}]}}"));
                movies.add(movie);
            }
        }
        private void deliver(Object tag, int delay, Runnable action) {
            int generation = generations.getOrDefault(tag, 0);
            main.postDelayed(() -> { if (generation == generations.getOrDefault(tag, 0)) action.run(); }, delay);
        }
        @Override public void popular(int page, Object tag, Callback<MoviePage> callback) {
            popularCalls++; deliver(tag, 2000, () -> callback.success(new MoviePage(movies, page, 1)));
        }
        @Override public void search(String query, int page, Object tag, Callback<MoviePage> callback) {
            searchCallbacks.put(query, callback);
            searchCalls++; lastQuery = query;
            deliver(tag, 600, () -> {
                if (query.equals("network-error")) callback.failure(Failure.NETWORK);
                else callback.success(new MoviePage(query.equals("no-such-film") ? Collections.emptyList() : movies, page, 1));
            });
        }
        @Override public void details(int id, Object tag, Callback<Movie> callback) {
            detailCalls++; deliver(tag, 600, () -> callback.success(movies.get(id - 1)));
        }
        @Override public void cancel(Object tag) { generations.put(tag, generations.getOrDefault(tag, 0) + 1); }
        @Override public LiveData<List<Movie>> favorites() { return saved; }
        @Override public LiveData<Failure> favoritesError() { return storageError; }
        @Override public void refreshFavorites() { storageError.setValue(null); }
        @Override public void add(Movie movie, Callback<Boolean> callback) {
            List<Movie> values = new ArrayList<>(saved.getValue());
            boolean duplicate = values.stream().anyMatch(value -> value.id == movie.id);
            if (!duplicate) values.add(movie);
            saved.setValue(values); callback.success(!duplicate);
        }
        @Override public void remove(List<Movie> movies, Callback<Void> callback) {
            List<Movie> values = new ArrayList<>(saved.getValue());
            values.removeIf(value -> movies.stream().anyMatch(movie -> movie.id == value.id));
            saved.setValue(values); callback.success(null);
        }
        @Override public void restore(List<Movie> movies, Callback<Void> callback) {
            List<Movie> values = new ArrayList<>(saved.getValue());
            for (Movie movie : movies) if (values.stream().noneMatch(value -> value.id == movie.id)) values.add(movie);
            saved.setValue(values); callback.success(null);
        }
    }
}
