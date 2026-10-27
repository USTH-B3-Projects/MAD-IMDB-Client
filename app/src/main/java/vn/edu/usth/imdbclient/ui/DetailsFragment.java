package vn.edu.usth.imdbclient.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import vn.edu.usth.imdbclient.R;
import vn.edu.usth.imdbclient.data.Movie;

public class DetailsFragment extends ScreenFragment {
    private int movieId;
    public DetailsFragment() { super(R.layout.fragment_details); }
    static DetailsFragment create(int id) {
        DetailsFragment fragment = new DetailsFragment();
        Bundle arguments = new Bundle(); arguments.putInt("id", id); fragment.setArguments(arguments); return fragment;
    }
    @Override public void onViewCreated(@NonNull View root, @Nullable Bundle bundle) {
        super.onViewCreated(root, bundle);
        movieId = requireArguments().getInt("id");
        root.findViewById(R.id.detail_back).setOnClickListener(v -> host().onBackPressed());
        root.findViewById(R.id.detail_favorite).setOnClickListener(v -> addFavorite());
        model.detail.observe(getViewLifecycleOwner(), movie -> {
            if (movie != null && movie.id == movieId) bind(root, movie);
        });
        model.detailLoading.observe(getViewLifecycleOwner(), loading -> {
            root.findViewById(R.id.loading_panel).setVisibility(loading ? View.VISIBLE : View.GONE);
            if (loading) {
                root.findViewById(R.id.state_panel).setVisibility(View.GONE);
                root.findViewById(R.id.detail_scroll).setVisibility(View.GONE);
            }
        });
        model.detailError.observe(getViewLifecycleOwner(), error -> {
            if (error != null) Ui.error(root, error, () -> model.loadDetails(movieId, true));
        });
        model.favorites.observe(getViewLifecycleOwner(), movies -> favoriteButton(root));
        model.favoritesError.observe(getViewLifecycleOwner(), error -> favoriteButton(root));
        model.busyIds.observe(getViewLifecycleOwner(), ids -> favoriteButton(root));
        model.loadDetails(movieId, false);
    }
    private void bind(View root, Movie movie) {
        root.findViewById(R.id.state_panel).setVisibility(View.GONE);
        root.findViewById(R.id.detail_scroll).setVisibility(View.VISIBLE);
        Ui.text(root, R.id.detail_title, movie.title);
        String[] genres = Ui.available(requireContext(), movie.genres).split(", ");
        Ui.text(root, R.id.detail_genres, genres[0]);
        root.findViewById(R.id.detail_genres_second).setVisibility(genres.length > 1 ? View.VISIBLE : View.GONE);
        if (genres.length > 1) Ui.text(root, R.id.detail_genres_second, genres[1]);
        Ui.text(root, R.id.detail_date, getString(R.string.release_date) + ": " + Ui.available(requireContext(), movie.releaseDate));
        Ui.text(root, R.id.detail_runtime, movie.runtime > 0 ? getString(R.string.runtime, movie.runtime) : getString(R.string.not_available));
        Ui.text(root, R.id.detail_rating, getString(R.string.rating_value, movie.rating()));
        Ui.text(root, R.id.detail_overview, Ui.available(requireContext(), movie.overview));
        Ui.text(root, R.id.detail_director, Ui.available(requireContext(), movie.director));
        Ui.text(root, R.id.detail_cast, Ui.available(requireContext(), movie.cast));
        ImageView hero = root.findViewById(R.id.hero);
        Ui.image(hero, movie.imageUrl(true).isEmpty() ? movie.imageUrl(false) : movie.imageUrl(true), getString(R.string.poster_description, movie.title));
        favoriteButton(root);
        Integer savedId = model.saved.get("detailScrollId");
        Integer scroll = model.saved.get("detailScroll");
        if (savedId != null && savedId == movieId && scroll != null) root.findViewById(R.id.detail_scroll).post(() -> {
            if (getView() != null) ((ScrollView) root.findViewById(R.id.detail_scroll)).scrollTo(0, scroll);
        });
    }
    private void addFavorite() {
        Movie movie = model.detail.getValue();
        if (movie != null && movie.id == movieId && !model.isFavorite(movieId)) model.addFavorite(movie);
    }
    private void favoriteButton(View root) {
        boolean saved = model.isFavorite(movieId);
        boolean busy = model.busyIds.getValue().contains(movieId) || model.favorites.getValue() == null || model.favoritesError.getValue() != null;
        Button button = root.findViewById(R.id.detail_favorite);
        button.setText(saved ? R.string.already_saved : R.string.add_favorite);
        button.setAlpha(saved ? .6f : 1f);
        button.setEnabled(!busy);
    }
    @Override public void onDestroyView() {
        model.saved.set("detailScrollId", movieId);
        model.saved.set("detailScroll", ((ScrollView) requireView().findViewById(R.id.detail_scroll)).getScrollY());
        super.onDestroyView();
    }
}
