package vn.edu.usth.imdbclient.ui;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Scroller;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import vn.edu.usth.imdbclient.R;
import vn.edu.usth.imdbclient.data.Movie;
import vn.edu.usth.imdbclient.viewmodel.ListState;

public class HomeFragment extends ScreenFragment {
    private static final float SIDE_SCALE = .75f;               // size of the posters beside the centred one
    private static final int POSTER_GAP_DP = 12;                // gap between the centred poster and its neighbors
    private static final int POSTER_HEIGHT_DP = 280;
    private static final int POSTER_TOP_DP = 10;                // pushes the posters down to balance the space above and below
    private static final long AUTO_ADVANCE_MILLIS = 4000;       // how long each movie stays in the centre
    private static final int AUTO_SCROLL_MILLIS = 700;          // how long the slide to the next movie takes
    private boolean slowScroll;
    private ListView list;
    private View header;
    private ViewPager pager;
    private MovieAdapter adapter;
    private Button footer;
    private boolean expanded;   // See all swaps the two discover cards for the full movie list; Hide swaps them back
    private List<Movie> featured = Collections.emptyList();
    private boolean restored;
    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle bundle) {
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
        expanded = Boolean.TRUE.equals(model.saved.get("homeExpanded"));
        footer.setOnClickListener(v -> model.loadMore(true, true));
        adapter = new MovieAdapter(host(), model, true, false, false);
        list.setAdapter(adapter);
        pager = header.findViewById(R.id.featured_pager);
        pager.post(() -> {
            if (getView() == null) {
                return;
            }
            int padding = Math.max(0, (pager.getWidth() - Ui.dp(requireContext(), 222)) / 2);
            pager.setPadding(padding, Ui.dp(requireContext(), POSTER_TOP_DP), padding, 0);
            pager.getLayoutParams().height = Ui.dp(requireContext(), POSTER_HEIGHT_DP + 16);
            pager.requestLayout();
        });
        installShowcaseScroller();
        // Style the pages from their real on-screen position while scrolling and after every layout (at rest).
        pager.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> styleShowcase());
        pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float offset, int offsetPixels) {
                styleShowcase();
            }

            @Override
            public void onPageSelected(int position) {
                int index = ((FeaturedAdapter) pager.getAdapter()).movieIndex(position);
                model.saved.set("featured", index);
                featuredMetadata(index);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_DRAGGING) {
                    slowScroll = false;
                    stopAutoAdvance();
                    return;
                }
                if (state != ViewPager.SCROLL_STATE_IDLE || featured.size() < 2) {
                    return;
                }
                if (pager.getCurrentItem() == 0) {
                    pager.setCurrentItem(featured.size(), false);
                } else if (pager.getCurrentItem() == featured.size() + 1) {
                    pager.setCurrentItem(1, false);
                }
                slowScroll = false;
                scheduleAutoAdvance();
            }
        });
        header.findViewById(R.id.see_all).setOnClickListener(v -> setExpanded(!expanded));
        list.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int first, int count, int total) {
                if (expanded && count > 0 && first > 0 && first + count >= total - 2) {
                    model.loadMore(true, false);
                }
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
        if (empty && state.error != null) {
            Ui.error(root, state.error, () -> model.loadHome(true));
        } else if (empty && !state.loading) {
            Ui.state(root, R.string.empty_home, R.string.empty_home_body, R.attr.movieStateSearch, R.string.retry, () -> model.loadHome(true));
        }
        if (empty) {
            return;
        }
        if (featured.isEmpty()) {
            featured = new ArrayList<>(state.movies.subList(0, Math.min(5, state.movies.size())));
            pager.setAdapter(new FeaturedAdapter(host(), featured));
            Integer selected = model.saved.get("featured");
            int index = Math.min(selected == null ? 0 : selected, featured.size() - 1);
            pager.setCurrentItem(featured.size() > 1 ? index + 1 : 0, false);
            featuredMetadata(index);
            scheduleAutoAdvance();
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
        showList(state);
        if (!restored) {
            Parcelable scroll = model.saved.get("homeScroll");
            if (scroll != null) {
                list.onRestoreInstanceState(scroll);
            }
            restored = true;
        }
    }

    /** Positions every page from its offset to the center: scale, fade and pull the side posters in so the gap stays constant. */
    private void setExpanded(boolean value) {
        expanded = value;
        model.saved.set("homeExpanded", value);
        ListState state = model.home.getValue();
        if (state == null) {
            return;
        }
        showList(state);
        if (!value) {
            list.post(() -> {
                if (list != null) {
                    list.setSelection(0);
                }
            });
        }
    }

    /** Collapsed: the two discover cards. Expanded: the movie list takes their place. */
    private void showList(ListState state) {
        adapter.submit(expanded ? state.movies : Collections.<Movie>emptyList());
        ((Button) header.findViewById(R.id.see_all)).setText(expanded ? R.string.hide : R.string.see_all);
        ((View) header.findViewById(R.id.discover_1).getParent()).setVisibility(expanded ? View.GONE : View.VISIBLE);
        header.setPadding(header.getPaddingLeft(), header.getPaddingTop(), header.getPaddingRight(), expanded ? 0 : Ui.dp(requireContext(), 40));
        footer.setVisibility(expanded && state.hasMore() ? View.VISIBLE : View.GONE);
        footer.setEnabled(!state.loading);
        footer.setText(state.loading ? R.string.loading : state.error != null ? R.string.retry : R.string.more);
    }

    private void styleShowcase() {
        if (pager == null) {
            return;
        }
        int client = pager.getMeasuredWidth() - pager.getPaddingLeft() - pager.getPaddingRight();
        if (client <= 0) {
            return;
        }
        for (int i = 0; i < pager.getChildCount(); i++) {
            View page = pager.getChildAt(i);
            float centre = page.getLeft() - pager.getScrollX() + page.getWidth() / 2f;
            styleShowcasePage(page, (centre - pager.getWidth() / 2f) / client);
        }
    }

    private void styleShowcasePage(View page, float position) {
        ImageView poster = page.findViewById(R.id.poster);
        if (poster == null || poster.getWidth() == 0) {
            return;
        }
        int height = Ui.dp(page.getContext(), POSTER_HEIGHT_DP);
        if (poster.getLayoutParams().height != height) {
            poster.getLayoutParams().height = height;
            poster.requestLayout();
        }
        float distance = Math.min(1, Math.abs(position));
        float scale = 1 - (1 - SIDE_SCALE) * distance;
        // Center-to-center distance that leaves exactly POSTER_GAP_DP between a full-size and a side poster.
        float pull = page.getWidth() - poster.getWidth() * (1 + SIDE_SCALE) / 2 - Ui.dp(page.getContext(), POSTER_GAP_DP);
        poster.setScaleX(scale);
        poster.setScaleY(scale);
        poster.setTranslationX(-Math.signum(position) * distance * pull);
        poster.setAlpha(1 - .3f * distance);
    }

    /** Makes the automatic slide last AUTO_SCROLL_MILLIS; a swipe by hand keeps ViewPager's normal speed. */
    private void installShowcaseScroller() {
        try {
            Field field = ViewPager.class.getDeclaredField("mScroller");
            field.setAccessible(true);
            field.set(pager, new Scroller(requireContext(), new DecelerateInterpolator(1.5f)) {
                @Override
                public void startScroll(int startX, int startY, int dx, int dy, int duration) {
                    super.startScroll(startX, startY, dx, dy, slowScroll ? AUTO_SCROLL_MILLIS : duration);
                }
            });
        } catch (ReflectiveOperationException | RuntimeException ignored) { /* keep ViewPager's default scroller */ }
    }

    private final Runnable autoAdvance = () -> {
        if (pager == null) {
            return;
        }
        slowScroll = true;
        pager.setCurrentItem(pager.getCurrentItem() + 1, true);
    };

    private void scheduleAutoAdvance() {
        stopAutoAdvance();
        if (pager != null && featured.size() > 1 && isResumed()) {
            pager.postDelayed(autoAdvance, AUTO_ADVANCE_MILLIS);
        }
    }

    private void stopAutoAdvance() {
        if (pager != null) {
            pager.removeCallbacks(autoAdvance);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        scheduleAutoAdvance();
    }

    @Override
    public void onPause() {
        stopAutoAdvance();
        super.onPause();
    }

    private void featuredMetadata(int position) {
        if (featured.isEmpty()) {
            return;
        }
        Movie movie = featured.get(position);
        Ui.text(header, R.id.featured_title, movie.title);
        String information = movie.runtime > 0 ? getString(R.string.runtime, movie.runtime) : getString(R.string.rating_value, movie.rating());
        Ui.text(header, R.id.featured_meta, Ui.metadata(requireContext(), movie) + " / " + information);
        LinearLayout dots = header.findViewById(R.id.pager_indicators);
        dots.removeAllViews();
        for (int i = 0; i < featured.size(); i++) {
            TextView dot = new TextView(requireContext());
            dot.setText("●");
            dot.setTextSize(10);
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
