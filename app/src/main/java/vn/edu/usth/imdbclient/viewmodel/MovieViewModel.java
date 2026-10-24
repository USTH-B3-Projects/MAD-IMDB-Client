package vn.edu.usth.imdbclient.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import vn.edu.usth.imdbclient.MovieApplication;
import vn.edu.usth.imdbclient.data.Movie;
import vn.edu.usth.imdbclient.data.MovieDataSource;
import vn.edu.usth.imdbclient.data.MovieDataSource.Callback;
import vn.edu.usth.imdbclient.data.MovieDataSource.Failure;
import vn.edu.usth.imdbclient.data.MoviePage;

public class MovieViewModel extends AndroidViewModel {
    public final MutableLiveData<ListState> home = new MutableLiveData<>(ListState.empty());
    public final MutableLiveData<ListState> search = new MutableLiveData<>(ListState.empty());
    public final MutableLiveData<Movie> detail = new MutableLiveData<>();
    public final MutableLiveData<Failure> detailError = new MutableLiveData<>();
    public final MutableLiveData<Boolean> detailLoading = new MutableLiveData<>(false);
    public final MutableLiveData<Feedback> feedback = new MutableLiveData<>();
    public final MutableLiveData<Set<Integer>> busyIds = new MutableLiveData<>(Collections.emptySet());
    public final LiveData<List<Movie>> favorites;
    public final LiveData<Failure> favoritesError;
    public final SavedStateHandle saved;
    public final SharedPreferences preferences;
    private final MovieDataSource repository;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Object homeTag = new Object(), searchTag = new Object(), detailTag = new Object();
    private Runnable pendingSearch;
    private int searchGeneration;
    private int detailId = -1;

    public static final class Feedback {
        public enum Kind { ADDED, DUPLICATE, REMOVED, RESTORED, STORAGE_ERROR }
        public final Kind kind;
        public final List<Movie> removed;
        /** The movie just added, so Undo can take it back out; null for every other feedback. */
        public final Movie added;
        public Feedback(Kind kind, List<Movie> removed) { this(kind, removed, null); }
        public Feedback(Kind kind, List<Movie> removed, Movie added) { this.kind = kind; this.removed = removed; this.added = added; }
        public boolean canUndo() { return added != null || !removed.isEmpty(); }
    }

    public MovieViewModel(Application application, SavedStateHandle saved) {
        super(application);
        this.saved = saved;
        repository = ((MovieApplication) application).repository;
        preferences = application.getSharedPreferences("settings", 0);
        favorites = repository.favorites();
        favoritesError = repository.favoritesError();
        if (saved.get("query") == null) saved.set("query", "");
        if (!query().isEmpty()) setQuery(query());
    }

    public void loadHome(boolean retry) {
        ListState state = home.getValue();
        if (state.loading || (!retry && (state.page > 0 || state.error != null))) return;
        if (state.movies.isEmpty() && state.page > 0) state = ListState.empty();
        home.setValue(state.loading());
        repository.popular(state.page + 1, homeTag, pageCallback(home, () -> true));
    }

    public String query() { return saved.get("query"); }
    public void setQuery(String value) {
        String query = value.trim();
        saved.set("query", query);
        int generation = ++searchGeneration;
        repository.cancel(searchTag);
        if (pendingSearch != null) main.removeCallbacks(pendingSearch);
        search.setValue(query.isEmpty() ? ListState.empty() : ListState.empty().loading());
        if (!query.isEmpty()) {
            pendingSearch = () -> {
                saveHistory(query);
                repository.search(query, 1, searchTag, pageCallback(search, () -> generation == searchGeneration));
            };
            main.postDelayed(pendingSearch, 400);
        }
    }
    public void loadMore(boolean homeScreen, boolean retry) {
        MutableLiveData<ListState> target = homeScreen ? home : search;
        ListState state = target.getValue();
        if (state.loading || !state.hasMore() || (!retry && state.error != null)) return;
        if (homeScreen) { loadHome(true); return; }
        if (query().isEmpty()) return;
        int generation = searchGeneration;
        target.setValue(state.loading());
        repository.search(query(), state.page + 1, searchTag, pageCallback(target, () -> generation == searchGeneration));
    }
    private interface Current { boolean isCurrent(); }
    private Callback<MoviePage> pageCallback(MutableLiveData<ListState> target, Current current) {
        return new Callback<MoviePage>() {
            @Override public void success(MoviePage value) {
                if (current.isCurrent()) target.setValue(target.getValue().received(value));
            }
            @Override public void failure(Failure error) {
                if (current.isCurrent()) target.setValue(target.getValue().failed(error));
            }
        };
    }

    public void loadDetails(int id, boolean retry) {
        if (id == detailId && !retry) return;
        repository.cancel(detailTag);
        detailId = id;
        saved.set("movieId", id);
        detail.setValue(null);
        detailError.setValue(null);
        detailLoading.setValue(true);
        repository.details(id, detailTag, new Callback<Movie>() {
            @Override public void success(Movie movie) {
                if (detailId != id) return;
                detail.setValue(movie);
                detailLoading.setValue(false);
            }
            @Override public void failure(Failure error) {
                if (detailId != id) return;
                detailError.setValue(error);
                detailLoading.setValue(false);
            }
        });
    }
    public boolean isFavorite(int id) {
        List<Movie> movies = favorites.getValue();
        if (movies != null) for (Movie movie : movies) if (movie.id == id) return true;
        return false;
    }
    public void refreshFavorites() { repository.refreshFavorites(); }
    public void addFavorite(Movie movie) {
        if (favorites.getValue() == null || favoritesError.getValue() != null) {
            feedback.setValue(new Feedback(Feedback.Kind.STORAGE_ERROR, Collections.emptyList())); return;
        }
        if (busyIds.getValue().contains(movie.id)) return;
        busy(movie.id, true);
        repository.add(movie, new Callback<Boolean>() {
            @Override public void success(Boolean added) {
                busy(movie.id, false);
                feedback.setValue(added
                        ? new Feedback(Feedback.Kind.ADDED, Collections.emptyList(), movie)
                        : new Feedback(Feedback.Kind.DUPLICATE, Collections.emptyList()));
            }
            @Override public void failure(Failure reason) {
                busy(movie.id, false);
                feedback.setValue(new Feedback(Feedback.Kind.STORAGE_ERROR, Collections.emptyList()));
            }
        });
    }
    public void removeSelected(List<Movie> movies) {
        if (movies.isEmpty() || !busyIds.getValue().isEmpty()) return;
        for (Movie movie : movies) busy(movie.id, true);
        repository.remove(movies, new Callback<Void>() {
            @Override public void success(Void ignored) {
                busyIds.setValue(Collections.emptySet());
                feedback.setValue(new Feedback(Feedback.Kind.REMOVED, new ArrayList<>(movies)));
            }
            @Override public void failure(Failure reason) {
                busyIds.setValue(Collections.emptySet());
                feedback.setValue(new Feedback(Feedback.Kind.STORAGE_ERROR, Collections.emptyList()));
            }
        });
    }
    public void undo() {
        Feedback value = feedback.getValue();
        if (value == null || !value.canUndo()) return;
        feedback.setValue(null);
        if (value.added != null) {
            // Undo of an add: take the just-saved movie out again (shown as a plain "Removed" message).
            repository.remove(Collections.singletonList(value.added), new Callback<Void>() {
                @Override public void success(Void ignored) { feedback.setValue(new Feedback(Feedback.Kind.REMOVED, Collections.emptyList())); }
                @Override public void failure(Failure reason) { feedback.setValue(new Feedback(Feedback.Kind.STORAGE_ERROR, Collections.emptyList())); }
            });
            return;
        }
        repository.restore(value.removed, new Callback<Void>() {
            @Override public void success(Void ignored) { feedback.setValue(new Feedback(Feedback.Kind.RESTORED, Collections.emptyList())); }
            @Override public void failure(Failure reason) { feedback.setValue(new Feedback(Feedback.Kind.STORAGE_ERROR, Collections.emptyList())); }
        });
    }
    private void busy(int id, boolean busy) {
        Set<Integer> ids = new HashSet<>(busyIds.getValue());
        if (busy) ids.add(id); else ids.remove(id);
        busyIds.setValue(ids);
    }
    public List<String> history() {
        String stored = preferences.getString("history", "");
        return stored.isEmpty() ? Collections.emptyList() : Arrays.asList(stored.split("\n"));
    }
    private void saveHistory(String query) {
        List<String> history = new ArrayList<>(history());
        history.remove(query);
        history.add(0, query.replace('\n', ' '));
        if (history.size() > 8) history = history.subList(0, 8);
        preferences.edit().putString("history", String.join("\n", history)).apply();
    }
    public void clearHistory() { preferences.edit().remove("history").apply(); }
    @Override protected void onCleared() {
        main.removeCallbacksAndMessages(null);
        repository.cancel(homeTag); repository.cancel(searchTag); repository.cancel(detailTag);
    }
}