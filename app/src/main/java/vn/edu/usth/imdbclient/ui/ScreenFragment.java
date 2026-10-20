package vn.edu.usth.imdbclient.ui;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import vn.edu.usth.imdbclient.viewmodel.MovieViewModel;

public abstract class ScreenFragment extends Fragment {
    protected MovieViewModel model;
    protected ScreenFragment(int layout) { super(layout); }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        model = new ViewModelProvider(requireActivity()).get(MovieViewModel.class);
        Ui.fonts(view);
    }
    protected MainActivity host() { return (MainActivity) requireActivity(); }
    @Override public void onDestroyView() {
        if (getView() != null) Ui.cancelImages(getView());
        super.onDestroyView();
    }
}
