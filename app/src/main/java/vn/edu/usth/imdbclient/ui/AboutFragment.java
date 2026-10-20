package vn.edu.usth.imdbclient.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import vn.edu.usth.imdbclient.BuildConfig;
import vn.edu.usth.imdbclient.R;

public class AboutFragment extends ScreenFragment {
    public AboutFragment() { super(R.layout.fragment_about); }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle bundle) {
        super.onViewCreated(view, bundle);
        view.findViewById(R.id.about_back).setOnClickListener(v -> host().onBackPressed());
        ((TextView) view.findViewById(R.id.app_version)).setText(getString(R.string.version, BuildConfig.VERSION_NAME));
    }
}
