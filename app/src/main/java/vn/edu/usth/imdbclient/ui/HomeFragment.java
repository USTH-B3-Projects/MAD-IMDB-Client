package vn.edu.usth.imdbclient.ui;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import vn.edu.usth.imdbclient.R;
import vn.edu.usth.imdbclient.data.Movie;
import vn.edu.usth.imdbclient.viewmodel.ListState;

public class HomeFragment extends ScreenFragment {
    private ListView list;
    private View header;
    private ViewPager pager;
    private MovieAdapter adapter;
    private Button footer;
    private List<Movie> featured = Collections.emptyList();
    private boolean restored;
    public HomeFragment() { super(R.layout.fragment_home); }
    @Override public void onViewCreated(@NonNull View root, @Nullable Bundle bundle) {
        super.onViewCreated(root, bundle);
        // The home search opens the dedicated Search screen, preserving its query.
        View query = root.findViewById(R.id.query);
        query.setFocusable(false);
        query.setClickable(true);
        query.setOnClickListener(v -> host().showTab("search"));
        root.findViewById(R.id.clear_query).setVisibility(View.GONE);
        list = root.findViewById(R.id.movie_list);
        header = LayoutInflater.from(requireContext()).inflate(R.layout.header_home, list, false);
        Ui.fonts(header);
        list.addHeaderView(header, null, false);
        footer = (Button) LayoutInflater.from(requireContext()).inflate(R.layout.list_footer, list, false);
        Ui.fonts(footer);
        list.addFooterView(footer, null, false);
        footer.setOnClickListener(v -> model.loadMore(true, true));
        adapter = new MovieAdapter(host(), model, true, false, false);
        list.setAdapter(adapter);
        pager = header.findViewById(R.id.featured_pager);
        pager.post(() -> {
            if (getView() == null) return;
            int padding = Math.max(0, (pager.getWidth() - Ui.dp(requireContext(), 222)) / 2);
            pager.setPadding(padding, 0, padding, 0);
        });
        pager.setPageTransformer(false, (page, position) -> {
            ImageView poster = page.findViewById(R.id.poster);
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) poster.getLayoutParams();
            int top = Ui.dp(page.getContext(), 16 * Math.min(1, Math.abs(position)));
            if (params.topMargin != top) { params.topMargin = top; poster.setLayoutParams(params); }
            poster.setAlpha(1 - .15f * Math.min(1, Math.abs(position)));
        });
        pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override public void onPageSelected(int position) {
                int index = ((FeaturedAdapter) pager.getAdapter()).movieIndex(position);
                model.saved.set("featured", index); featuredMetadata(index);
            }
            @Override public void onPageScrollStateChanged(int state) {
                if (state != ViewPager.SCROLL_STATE_IDLE || featured.size() < 2) return;
                if (pager.getCurrentItem() == 0) pager.setCurrentItem(featured.size(), false);
                else if (pager.getCurrentItem() == featured.size() + 1) pager.setCurrentItem(1, false);
            }
        });
        header.findViewById(R.id.see_all).setOnClickListener(v -> list.setSelection(1));
        list.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView view, int scrollState) { }
            @Override public void onScroll(AbsListView view, int first, int count, int total) {
                if (count > 0 && first > 0 && first + count >= total - 2) model.loadMore(true, false);
            }
        });
        model.home.observe(getViewLifecycleOwner(), state -> render(root, state));
        model.favorites.observe(getViewLifecycleOwner(), movies -> adapter.notifyDataSetChanged());
        model.favoritesError.observe(getViewLifecycleOwner(), error -> adapter.notifyDataSetChanged());
        model.busyIds.observe(getViewLifecycleOwner(), ids -> adapter.notifyDataSetChanged());
        model.loadHome(false);
    }
    private void render(View root, ListState state) {
        boolean empty = state.movies.isEmpty();
        list.setVisibility(empty ? View.GONE : View.VISIBLE);
        root.findViewById(R.id.state_panel).setVisibility(View.GONE);
        root.findViewById(R.id.loading_panel).setVisibility(empty && state.loading ? View.VISIBLE : View.GONE);
        if (empty && state.error != null) Ui.error(root, state.error, () -> model.loadHome(true));
        else if (empty && !state.loading) Ui.state(root, R.string.empty_home, R.string.empty_home_body, R.attr.movieStateSearch, R.string.retry, () -> model.loadHome(true));
        if (empty) return;
        if (featured.isEmpty()) {
            // shortcut: the Figma weekly showcase uses the first five popular movies; add weekly trending if required.
            featured = new ArrayList<>(state.movies.subList(0, Math.min(5, state.movies.size())));
            pager.setAdapter(new FeaturedAdapter(host(), featured));
            Integer selected = model.saved.get("featured");
            int index = Math.min(selected == null ? 0 : selected, featured.size() - 1);
            pager.setCurrentItem(featured.size() > 1 ? index + 1 : 0, false);
            featuredMetadata(index);
            for (int i = 1; i <= 2; i++) {
                Movie movie = state.movies.get(Math.min(i, state.movies.size() - 1));
                int poster = i == 1 ? R.id.discover_poster_1 : R.id.discover_poster_2;
                int title = i == 1 ? R.id.discover_title_1 : R.id.discover_title_2;
                int meta = i == 1 ? R.id.discover_meta_1 : R.id.discover_meta_2;
                Ui.image(header.findViewById(poster), movie.imageUrl(false), getString(R.string.poster_description, movie.title));
                Ui.text(header, title, movie.title);
                Ui.text(header, meta, Ui.metadata(requireContext(), movie));
                header.findViewById(i == 1 ? R.id.discover_1 : R.id.discover_2).setOnClickListener(v -> host().openMovie(movie));
            }
        }
        adapter.submit(state.movies);
        footer.setVisibility(state.hasMore() ? View.VISIBLE : View.GONE);
        footer.setEnabled(!state.loading);
        footer.setText(state.loading ? R.string.loading : state.error != null ? R.string.retry : R.string.more);
        if (!restored) {
            Parcelable scroll = model.saved.get("homeScroll");
            if (scroll != null) list.onRestoreInstanceState(scroll);
            restored = true;
        }
    }
    private void featuredMetadata(int position) {
        if (featured.isEmpty()) return;
        Movie movie = featured.get(position);
        Ui.text(header, R.id.featured_title, movie.title);
        String information = movie.runtime > 0 ? getString(R.string.runtime, movie.runtime) : getString(R.string.rating_value, movie.rating());
        Ui.text(header, R.id.featured_meta, Ui.metadata(requireContext(), movie) + " / " + information);
        LinearLayout dots = header.findViewById(R.id.pager_indicators);
        dots.removeAllViews();
        for (int i = 0; i < featured.size(); i++) {
            TextView dot = new TextView(requireContext());
            dot.setText("●"); dot.setTextSize(10);
            dot.setTextColor(i == position ? requireContext().getColor(R.color.gold) : Ui.color(requireContext(), R.attr.movieBorder));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(Ui.dp(requireContext(), 12), Ui.dp(requireContext(), 12));
            dots.addView(dot, params);
        }
    }
    @Override public void onDestroyView() {
        model.saved.set("homeScroll", list.onSaveInstanceState());
        super.onDestroyView();
        pager.setAdapter(null);
        list = null; header = null; pager = null; adapter = null; footer = null;
        featured = Collections.emptyList(); restored = false;
    }
}
