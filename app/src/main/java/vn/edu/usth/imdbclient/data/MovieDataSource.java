package vn.edu.usth.imdbclient.data;

import androidx.lifecycle.LiveData;
import java.util.List;

public interface MovieDataSource {
    enum Failure { CONFIGURATION, NETWORK, UNAVAILABLE, STORAGE }
    interface Callback<T> {
        void success(T value);
        void failure(Failure reason);
    }
    void popular(int page, Object tag, Callback<MoviePage> callback);
    void search(String query, int page, Object tag, Callback<MoviePage> callback);
    void details(int id, Object tag, Callback<Movie> callback);
    void cancel(Object tag);
    LiveData<List<Movie>> favorites();
    LiveData<Failure> favoritesError();
    void refreshFavorites();
    void add(Movie movie, Callback<Boolean> callback);
    void remove(List<Movie> movies, Callback<Void> callback);
    void restore(List<Movie> movies, Callback<Void> callback);
}
