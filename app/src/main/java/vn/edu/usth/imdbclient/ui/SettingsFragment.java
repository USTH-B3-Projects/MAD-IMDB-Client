package vn.edu.usth.imdbclient.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import vn.edu.usth.imdbclient.R;

public class SettingsFragment extends ScreenFragment {
    public SettingsFragment() { super(R.layout.fragment_settings); }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle bundle) {
        super.onViewCreated(view, bundle);
        boolean dark = model.preferences.getBoolean("dark", false);
        view.findViewById(R.id.appearance_light).setBackgroundResource(dark ? R.drawable.appearance_light : R.drawable.appearance_selected_light);
        view.findViewById(R.id.appearance_dark).setBackgroundResource(dark ? R.drawable.appearance_selected_dark : R.drawable.appearance_dark);
        view.findViewById(R.id.selected_light).setVisibility(dark ? View.INVISIBLE : View.VISIBLE);
        view.findViewById(R.id.selected_dark).setVisibility(dark ? View.VISIBLE : View.INVISIBLE);
        view.findViewById(R.id.appearance_light).setSelected(!dark);
        view.findViewById(R.id.appearance_dark).setSelected(dark);
        ((TextView) view.findViewById(R.id.appearance_status)).setText(dark ? R.string.dark_selected : R.string.light_selected);
        view.findViewById(R.id.appearance_light).setOnClickListener(v -> host().changeAppearance(false));
        view.findViewById(R.id.appearance_dark).setOnClickListener(v -> host().changeAppearance(true));
        view.findViewById(R.id.clear_history).setOnClickListener(v -> host().confirmation(true));
        view.findViewById(R.id.about_app).setOnClickListener(v -> host().openAbout());
    }
}
