package vn.edu.usth.imdbclient.ui;

import android.os.Bundle;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import vn.edu.usth.imdbclient.R;
import vn.edu.usth.imdbclient.data.Movie;
import vn.edu.usth.imdbclient.viewmodel.MovieViewModel;

public class MainActivity extends AppCompatActivity {
    private MovieViewModel model;
    private String tab = "home";

    @Override protected void onCreate(Bundle state) {
        boolean dark = getSharedPreferences("settings", 0).getBoolean("dark", false);
        setTheme(dark ? R.style.Theme_Movies_Dark : R.style.Theme_Movies_Light);
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        Ui.fonts(findViewById(R.id.app_root));
        model = new ViewModelProvider(this).get(MovieViewModel.class);
        String restored = model.saved.get("tab");
        if (restored != null) tab = restored;
        int[] buttons = {R.id.nav_home, R.id.nav_search, R.id.nav_favorites, R.id.nav_settings};
        String[] tabs = {"home", "search", "favorites", "settings"};
        for (int i = 0; i < buttons.length; i++) {
            String destination = tabs[i];
            findViewById(buttons[i]).setOnClickListener(v -> showTab(destination));
        }
        findViewById(R.id.feedback_undo).setOnClickListener(v -> model.undo());
        findViewById(R.id.feedback_dismiss).setOnClickListener(v -> model.feedback.setValue(null));
        findViewById(R.id.confirmation_cancel).setOnClickListener(v -> confirmation(false));
        findViewById(R.id.confirmation_clear).setOnClickListener(v -> {
            model.clearHistory(); confirmation(false);
        });
        model.feedback.observe(this, feedback -> {
            View bar = findViewById(R.id.feedback_bar);
            bar.setVisibility(feedback == null ? View.GONE : View.VISIBLE);
            if (feedback == null) return;
            int message = feedback.kind == MovieViewModel.Feedback.Kind.ADDED ? R.string.saved
                    : feedback.kind == MovieViewModel.Feedback.Kind.DUPLICATE ? R.string.already_saved
                    : feedback.kind == MovieViewModel.Feedback.Kind.REMOVED ? R.string.removed
                    : feedback.kind == MovieViewModel.Feedback.Kind.RESTORED ? R.string.restored : R.string.storage_error;
            ((TextView) findViewById(R.id.feedback_text)).setText(message);
            findViewById(R.id.feedback_undo).setVisibility(feedback.removed.isEmpty() ? View.GONE : View.VISIBLE);
            bar.announceForAccessibility(getString(message));
        });
        if (state == null) showTab(tab); else updateNavigation();
    }

    public void showTab(String destination) {
        hideKeyboard();
        confirmation(false);
        FragmentManager manager = getSupportFragmentManager();
        manager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        Fragment current = manager.findFragmentById(R.id.screen_container);
        boolean same = destination.equals(tab) && current != null && !(current instanceof DetailsFragment) && !(current instanceof AboutFragment);
        tab = destination;
        model.saved.set("tab", tab);
        if (!"favorites".equals(tab)) model.feedback.setValue(null);
        if (!same) {
            Fragment screen = "search".equals(tab) ? new SearchFragment() : "favorites".equals(tab)
                    ? new FavoritesFragment() : "settings".equals(tab) ? new SettingsFragment() : new HomeFragment();
            manager.beginTransaction().replace(R.id.screen_container, screen).commit();
        }
        updateNavigation();
    }
    public void openMovie(Movie movie) {
        hideKeyboard();
        model.loadDetails(movie.id, false);
        getSupportFragmentManager().beginTransaction().replace(R.id.screen_container, DetailsFragment.create(movie.id)).addToBackStack("details").commit();
    }
    public void openAbout() {
        getSupportFragmentManager().beginTransaction().replace(R.id.screen_container, new AboutFragment()).addToBackStack("about").commit();
    }
    public void changeAppearance(boolean dark) {
        if (model.preferences.getBoolean("dark", false) == dark) return;
        model.preferences.edit().putBoolean("dark", dark).apply();
        recreate();
    }
    public void selectionMode(boolean selecting) {
        findViewById(R.id.bottom_navigation).setVisibility(selecting ? View.GONE : View.VISIBLE);
        FrameLayout.LayoutParams bar = (FrameLayout.LayoutParams) findViewById(R.id.feedback_bar).getLayoutParams();
        bar.bottomMargin = Ui.dp(this, selecting ? 88 : 16);
        findViewById(R.id.feedback_bar).setLayoutParams(bar);
    }
    public void confirmation(boolean visible) {
        findViewById(R.id.confirmation).setVisibility(visible ? View.VISIBLE : View.GONE);
        int importance = visible ? View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS : View.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
        findViewById(R.id.screen_container).setImportantForAccessibility(importance);
        findViewById(R.id.bottom_navigation).setImportantForAccessibility(importance);
        findViewById(R.id.bottom_navigation).setEnabled(!visible);
        for (int id : new int[]{R.id.nav_home, R.id.nav_search, R.id.nav_favorites, R.id.nav_settings}) findViewById(id).setEnabled(!visible);
        if (visible) findViewById(R.id.confirmation_cancel).requestFocus();
    }
    public void hideKeyboard() {
        View focused = getCurrentFocus();
        if (focused != null) {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focused.getWindowToken(), 0);
            focused.clearFocus();
        }
    }
    private void updateNavigation() {
        int[] icons = {R.id.nav_icon_home, R.id.nav_icon_search, R.id.nav_icon_favorites, R.id.nav_icon_settings};
        int[] labels = {R.id.nav_label_home, R.id.nav_label_search, R.id.nav_label_favorites, R.id.nav_label_settings};
        int[] active = {R.drawable.ic_home, R.drawable.ic_search_active, R.drawable.ic_heart_active, R.drawable.ic_settings_active};
        int[] idle = {R.drawable.ic_home_idle, R.drawable.ic_search, R.drawable.ic_heart, R.drawable.ic_settings};
        String[] tabs = {"home", "search", "favorites", "settings"};
        for (int i = 0; i < icons.length; i++) {
            boolean selected = tab.equals(tabs[i]);
            ImageView image = findViewById(icons[i]);
            image.setImageResource(selected ? active[i] : idle[i]);
            image.setColorFilter(selected ? getColor(R.color.gold) : Ui.color(this, R.attr.movieMuted));
            ((TextView) findViewById(labels[i])).setTextColor(Ui.color(this, selected ? R.attr.movieText : R.attr.movieMuted));
            ((TextView) findViewById(labels[i])).setFontVariationSettings(selected ? "'wght' 600" : "'wght' 400");
            ((View) image.getParent()).setSelected(selected);
        }
    }
    @Override public void onBackPressed() {
        if (findViewById(R.id.confirmation).getVisibility() == View.VISIBLE) { confirmation(false); return; }
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.screen_container);
        if (current instanceof FavoritesFragment && ((FavoritesFragment) current).cancelSelection()) return;
        if (getSupportFragmentManager().getBackStackEntryCount() == 0 && !"home".equals(tab)) { showTab("home"); return; }
        super.onBackPressed();
    }
}
