package vn.edu.usth.imdbclient.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import vn.edu.usth.imdbclient.R;
import vn.edu.usth.imdbclient.data.Movie;
import vn.edu.usth.imdbclient.viewmodel.MovieViewModel;

final class MovieAdapter extends BaseAdapter {
    private final MainActivity activity;
    private final MovieViewModel model;
    private final boolean home;
    private final boolean compact;
    private final boolean removable;
    private List<Movie> movies = Collections.emptyList();
    private Set<Integer> selection;
    private Runnable selectionChanged;
    MovieAdapter(MainActivity activity, MovieViewModel model, boolean home, boolean compact, boolean removable) {
        this.activity = activity; this.model = model; this.home = home; this.compact = compact; this.removable = removable;
    }
    void submit(List<Movie> movies) { this.movies = new ArrayList<>(movies); notifyDataSetChanged(); }
    void selection(Set<Integer> ids, Runnable changed) { selection = ids; selectionChanged = changed; notifyDataSetChanged(); }
    @Override public int getCount() { return movies.size(); }
    @Override public Movie getItem(int position) { return movies.get(position); }
    @Override public long getItemId(int position) { return getItem(position).id; }
    @Override public boolean hasStableIds() { return true; }
    @Override public View getView(int position, View recycled, ViewGroup parent) {
        View row = recycled == null ? LayoutInflater.from(activity).inflate(R.layout.item_movie, parent, false) : recycled;
        if (recycled == null) {
            Ui.fonts(row);
            if (home) row.setPadding(Ui.dp(activity, 20), 0, Ui.dp(activity, 20), 0);
            if (compact) {
                ImageView poster = row.findViewById(R.id.poster);
                ViewGroup.LayoutParams params = poster.getLayoutParams();
                params.width = Ui.dp(activity, 54); params.height = Ui.dp(activity, 80);
                poster.setLayoutParams(params);
                ((View) poster.getParent()).setPadding(Ui.dp(activity, 12), Ui.dp(activity, 10), Ui.dp(activity, 12), Ui.dp(activity, 10));
            }
        }
        Movie movie = getItem(position);
        Ui.text(row, R.id.movie_title, movie.title);
        Ui.text(row, R.id.movie_meta, Ui.metadata(activity, movie));
        String rating = activity.getString(R.string.rating_value, movie.rating());
        if (movie.runtime > 0) rating = activity.getString(R.string.runtime, movie.runtime) + " · " + rating;
        Ui.text(row, R.id.movie_rating, rating);
        Ui.image(row.findViewById(R.id.poster), movie.imageUrl(false), activity.getString(R.string.poster_description, movie.title));
        boolean saved = model.isFavorite(movie.id);
        ImageButton favorite = row.findViewById(R.id.favorite_action);
        favorite.setImageResource(saved ? R.drawable.ic_heart_saved : R.drawable.ic_heart);
        favorite.setContentDescription(activity.getString(removable ? R.string.remove_favorite : R.string.add_favorite) + ": " + movie.title);
        favorite.setEnabled(!model.busyIds.getValue().contains(movie.id) && model.favorites.getValue() != null && model.favoritesError.getValue() == null);
        favorite.setOnClickListener(v -> {
            if (removable) model.removeSelected(Collections.singletonList(movie));
            else model.addFavorite(movie);
        });
        favorite.setVisibility(selection == null ? View.VISIBLE : View.GONE);
        CheckBox checkbox = row.findViewById(R.id.movie_select);
        checkbox.setVisibility(selection == null ? View.GONE : View.VISIBLE);
        checkbox.setOnCheckedChangeListener(null);
        checkbox.setChecked(selection != null && selection.contains(movie.id));
        checkbox.setContentDescription(activity.getString(R.string.select_movie, movie.title));
        checkbox.setOnCheckedChangeListener((button, checked) -> {
            if (selection == null) return;
            if (checked) selection.add(movie.id); else selection.remove(movie.id);
            selectionChanged.run();
        });
        row.setOnClickListener(v -> {
            if (selection == null) activity.openMovie(movie);
            else checkbox.setChecked(!checkbox.isChecked());
        });
        return row;
    }
}
