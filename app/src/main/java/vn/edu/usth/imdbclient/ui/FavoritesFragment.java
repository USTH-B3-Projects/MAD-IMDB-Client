package vn.edu.usth.imdbclient.ui;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import vn.edu.usth.imdbclient.R;
import vn.edu.usth.imdbclient.data.Movie;

public class FavoritesFragment extends ScreenFragment {
    private ListView list;
    private MovieAdapter adapter;
    private List<Movie> movies = Collections.emptyList();
    private final Set<Integer> selected = new HashSet<>();
    private boolean selecting, restored;
    public FavoritesFragment() { super(R.layout.fragment_favorites); }
    @Override public void onViewCreated(@NonNull View root, @Nullable Bundle bundle) {
        super.onViewCreated(root, bundle);
        Boolean savedSelecting = model.saved.get("selecting");
        selecting = Boolean.TRUE.equals(savedSelecting);
        ArrayList<Integer> ids = model.saved.get("selection");
        if (ids != null) selected.addAll(ids);
        list = root.findViewById(R.id.movie_list);
        adapter = new MovieAdapter(host(), model, false, false, true);
        list.setAdapter(adapter);
        root.findViewById(R.id.add_film).setOnClickListener(v -> host().showTab("search"));
        root.findViewById(R.id.select_films).setOnClickListener(v -> {
            model.feedback.setValue(null);
            selecting = true; controls(root);
        });
        root.findViewById(R.id.cancel_selection).setOnClickListener(v -> cancelSelection());
        root.findViewById(R.id.remove_selected).setOnClickListener(v -> {
            List<Movie> removed = new ArrayList<>();
            for (Movie movie : movies) if (selected.contains(movie.id)) removed.add(movie);
            model.removeSelected(removed);
            cancelSelection();
        });
        root.findViewById(R.id.sort_films).setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(requireContext(), v);
            menu.getMenuInflater().inflate(R.menu.sort, menu.getMenu());
            menu.setOnMenuItemClickListener(item -> {
                model.saved.set("sort", item.getItemId()); render(root, model.favorites.getValue()); return true;
            });
            menu.show();
        });
        model.favorites.observe(getViewLifecycleOwner(), saved -> render(root, saved));
        model.favoritesError.observe(getViewLifecycleOwner(), error -> render(root, model.favorites.getValue()));
        model.busyIds.observe(getViewLifecycleOwner(), busy -> { adapter.notifyDataSetChanged(); controls(root); });
        controls(root);
    }
    private void render(View root, List<Movie> saved) {
        if (model.favoritesError.getValue() != null) {
            root.findViewById(R.id.favorites_loading).setVisibility(View.GONE);
            list.setVisibility(View.GONE);
            Ui.error(root, model.favoritesError.getValue(), () -> model.refreshFavorites());
            return;
        }
        root.findViewById(R.id.state_panel).setVisibility(View.GONE);
        root.findViewById(R.id.favorites_loading).setVisibility(saved == null ? View.VISIBLE : View.GONE);
        if (saved == null) return;
        movies = new ArrayList<>(saved);
        Integer sort = model.saved.get("sort");
        if (sort != null && sort == R.id.sort_title) movies.sort(Comparator.comparing(movie -> movie.title, String.CASE_INSENSITIVE_ORDER));
        else if (sort != null && sort == R.id.sort_rating) movies.sort(Comparator.comparingDouble((Movie movie) -> movie.voteAverage).reversed());
        Set<Integer> savedIds = new HashSet<>();
        for (Movie movie : movies) savedIds.add(movie.id);
        selected.retainAll(savedIds);
        adapter.submit(movies);
        boolean empty = movies.isEmpty();
        root.findViewById(R.id.favorites_divider).setVisibility(empty ? View.GONE : View.VISIBLE);
        list.setVisibility(empty ? View.GONE : View.VISIBLE);
        root.findViewById(R.id.state_panel).setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            selecting = false;
            Ui.state(root, R.string.empty_favorites, R.string.empty_favorites_body, R.attr.movieStateFavorite,
                    R.string.browse, () -> host().showTab("home"));
        }
        controls(root);
        if (!empty && !restored) {
            Parcelable scroll = model.saved.get("favoriteScroll");
            if (scroll != null) list.onRestoreInstanceState(scroll);
            restored = true;
        }
    }
    private void controls(View root) {
        model.saved.set("selecting", selecting);
        model.saved.set("selection", new ArrayList<>(selected));
        root.findViewById(R.id.favorite_tools).setVisibility(!movies.isEmpty() ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.selection_tools).setVisibility(selecting ? View.VISIBLE : View.GONE);
        host().selectionMode(selecting);
        ((TextView) root.findViewById(R.id.favorites_heading)).setText(selecting ? R.string.select_films : R.string.favorites);
        root.findViewById(R.id.remove_selected).setEnabled(!selected.isEmpty() && model.busyIds.getValue().isEmpty());
        int count = selecting ? selected.size() : movies.size();
        ((TextView) root.findViewById(R.id.saved_count)).setText(getResources().getQuantityString(
                selecting ? R.plurals.selected_count : R.plurals.saved_count, count, count));
        adapter.selection(selecting ? selected : null, () -> controls(getView()));
    }
    public boolean cancelSelection() {
        if (!selecting) return false;
        selecting = false; selected.clear(); controls(requireView()); return true;
    }
    @Override public void onDestroyView() {
        model.saved.set("favoriteScroll", list.onSaveInstanceState());
        host().selectionMode(false);
        super.onDestroyView();
        list = null; adapter = null; movies = Collections.emptyList(); selected.clear(); restored = false;
    }
}
