package vn.edu.usth.imdbclient.ui;

import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import vn.edu.usth.imdbclient.R;
import vn.edu.usth.imdbclient.viewmodel.ListState;

public class SearchFragment extends ScreenFragment {
    private EditText query;
    private TextWatcher watcher;
    private ListView list;
    private ListView suggestions;
    private MovieAdapter adapter;
    private Button footer;
    private boolean restored;
    public SearchFragment() { super(R.layout.fragment_search); }
    @Override public void onViewCreated(@NonNull View root, @Nullable Bundle bundle) {
        super.onViewCreated(root, bundle);
        query = root.findViewById(R.id.query);
        query.setText(model.query());
        query.setSelection(query.length());
        watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                restored = false;
                model.saved.set("searchScroll", null);
                model.setQuery(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        };
        query.addTextChangedListener(watcher);
        query.setOnEditorActionListener((text, action, event) -> {
            if (action != EditorInfo.IME_ACTION_SEARCH) return false;
            model.saveQueryToHistory();
            host().hideKeyboard(); return true;
        });
        root.findViewById(R.id.clear_query).setOnClickListener(v -> query.setText(""));
        list = root.findViewById(R.id.movie_list);
        suggestions = root.findViewById(R.id.suggestion_list);
        footer = (Button) LayoutInflater.from(requireContext()).inflate(R.layout.list_footer, list, false);
        Ui.fonts(footer);
        list.addFooterView(footer, null, false);
        footer.setOnClickListener(v -> model.loadMore(false, true));
        adapter = new MovieAdapter(host(), model, false, true, false);
        list.setAdapter(adapter);
        list.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView view, int state) { }
            @Override public void onScroll(AbsListView view, int first, int count, int total) {
                if (count > 0 && first + count >= total - 2) model.loadMore(false, false);
            }
        });
        model.search.observe(getViewLifecycleOwner(), state -> render(root, state));
        model.favorites.observe(getViewLifecycleOwner(), movies -> adapter.notifyDataSetChanged());
        model.favoritesError.observe(getViewLifecycleOwner(), error -> adapter.notifyDataSetChanged());
        model.busyIds.observe(getViewLifecycleOwner(), ids -> adapter.notifyDataSetChanged());
    }
    private void render(View root, ListState state) {
        boolean idle = model.query().isEmpty();
        boolean empty = state.movies.isEmpty();
        list.setVisibility(!idle && !empty ? View.VISIBLE : View.GONE);
        suggestions.setVisibility(idle ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.state_panel).setVisibility(View.GONE);
        root.findViewById(R.id.loading_panel).setVisibility(!idle && empty && state.loading ? View.VISIBLE : View.GONE);
        TextView heading = root.findViewById(R.id.results_heading);
        heading.setVisibility(!idle && empty && !state.loading ? View.GONE : View.VISIBLE);
        heading.setTextSize(idle ? 22 : 18);
        android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) root.findViewById(R.id.search_field).getLayoutParams();
        params.topMargin = Ui.dp(requireContext(), idle ? 14 : 22);
        root.findViewById(R.id.search_field).setLayoutParams(params);
        if (idle) {
            List<String> recent = model.history();
            List<String> values = recent.isEmpty() ? Arrays.asList(getResources().getStringArray(R.array.default_suggestions)) : recent;
            heading.setText(recent.isEmpty() ? R.string.suggestions : R.string.recent_searches);
            suggestions.setAdapter(new ArrayAdapter<String>(requireContext(), R.layout.item_suggestion, values) {
                @NonNull @Override public View getView(int position, @Nullable View recycled, @NonNull android.view.ViewGroup parent) {
                    View view = super.getView(position, recycled, parent); Ui.fonts(view); return view;
                }
            });
            suggestions.setOnItemClickListener((parent, view, position, id) -> {
                query.setText(values.get(position)); query.setSelection(query.length());
                model.saveQueryToHistory(); host().hideKeyboard();
            });
            return;
        }
        heading.setText(R.string.results);
        if (empty && state.error != null) Ui.error(root, state.error, () -> model.loadMore(false, true));
        else if (empty && !state.loading) Ui.state(root, R.string.empty_search, R.string.empty_search_body, R.attr.movieStateSearch, 0, null);
        adapter.submit(state.movies);
        footer.setVisibility(state.hasMore() ? View.VISIBLE : View.GONE);
        footer.setEnabled(!state.loading);
        footer.setText(state.loading ? R.string.loading : state.error != null ? R.string.retry : R.string.more);
        if (!empty && !restored) {
            Parcelable scroll = model.saved.get("searchScroll");
            if (scroll != null) list.onRestoreInstanceState(scroll);
            restored = true;
        }
    }
    @Override public void onDestroyView() {
        model.saved.set("searchScroll", list.onSaveInstanceState());
        query.removeTextChangedListener(watcher);
        super.onDestroyView();
        query = null; watcher = null; list = null; suggestions = null; adapter = null; footer = null; restored = false;
    }
}
