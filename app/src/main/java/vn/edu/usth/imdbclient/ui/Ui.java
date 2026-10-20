package vn.edu.usth.imdbclient.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import java.lang.ref.WeakReference;
import vn.edu.usth.imdbclient.MovieApplication;
import vn.edu.usth.imdbclient.R;
import vn.edu.usth.imdbclient.data.Movie;
import vn.edu.usth.imdbclient.data.MovieDataSource.Failure;

final class Ui {
    private static Typeface inter;
    static int color(Context context, int attribute) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attribute, value, true);
        return value.data;
    }
    static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
    static void fonts(View view) {
        if (inter == null) inter = Typeface.createFromAsset(view.getContext().getAssets(), "inter.ttf");
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            boolean bold = text.getTypeface() != null && text.getTypeface().isBold();
            text.setTypeface(inter);
            text.setFontVariationSettings(bold ? "'wght' 600" : "'wght' 400");
        }
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) fonts(((ViewGroup) view).getChildAt(i));
    }
    static void text(View root, int id, String value) { ((TextView) root.findViewById(id)).setText(value); }
    static String available(Context context, String value) { return value == null || value.isEmpty() ? context.getString(R.string.not_available) : value; }
    static String metadata(Context context, Movie movie) {
        String year = available(context, movie.year());
        return movie.genres == null || movie.genres.isEmpty() ? year : context.getString(R.string.movie_metadata, movie.genres, year);
    }
    static void image(ImageView view, String url, String description) {
        view.setContentDescription(description);
        if (url.equals(view.getTag(R.id.image_url))) return;
        cancelImage(view);
        view.setImageDrawable(null);
        view.setTag(R.id.image_url, url);
        view.setClipToOutline(true);
        if (url.isEmpty()) return;
        WeakReference<ImageView> target = new WeakReference<>(view);
        MovieApplication application = (MovieApplication) view.getContext().getApplicationContext();
        ImageLoader.ImageContainer request = application.images.get(url, new ImageLoader.ImageListener() {
            @Override public void onResponse(ImageLoader.ImageContainer response, boolean immediate) {
                ImageView current = target.get();
                if (current != null && url.equals(current.getTag(R.id.image_url)) && response.getBitmap() != null) current.setImageBitmap(response.getBitmap());
            }
            @Override public void onErrorResponse(VolleyError error) { }
        }, Math.max(view.getWidth(), dp(view.getContext(), 208)), dp(view.getContext(), 380), ImageView.ScaleType.CENTER_CROP);
        view.setTag(R.id.image_request, request);
    }
    private static void cancelImage(ImageView view) {
        Object request = view.getTag(R.id.image_request);
        if (request instanceof ImageLoader.ImageContainer) ((ImageLoader.ImageContainer) request).cancelRequest();
        view.setTag(R.id.image_request, null);
        view.setTag(R.id.image_url, null);
    }
    static void cancelImages(View root) {
        if (root instanceof ImageView) cancelImage((ImageView) root);
        if (root instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++) cancelImages(((ViewGroup) root).getChildAt(i));
    }
    static void state(View root, int title, int body, int icon, int action, Runnable click) {
        View panel = root.findViewById(R.id.state_panel);
        panel.setVisibility(View.VISIBLE);
        ((TextView) panel.findViewById(R.id.state_title)).setText(title);
        ((TextView) panel.findViewById(R.id.state_body)).setText(body);
        TypedValue drawable = new TypedValue();
        panel.getContext().getTheme().resolveAttribute(icon, drawable, true);
        ((ImageView) panel.findViewById(R.id.state_icon)).setImageResource(drawable.resourceId);
        Button button = panel.findViewById(R.id.state_action);
        button.setVisibility(action == 0 ? View.GONE : View.VISIBLE);
        if (action != 0) button.setText(action);
        button.setOnClickListener(v -> { if (click != null) click.run(); });
    }
    static void error(View root, Failure failure, Runnable retry) {
        int title = failure == Failure.STORAGE ? R.string.favorites_unavailable : failure == Failure.CONFIGURATION ? R.string.configuration_error
                : failure == Failure.UNAVAILABLE ? R.string.unavailable : R.string.network_error;
        int body = failure == Failure.STORAGE ? R.string.storage_error_body : failure == Failure.CONFIGURATION ? R.string.configuration_error_body
                : failure == Failure.UNAVAILABLE ? R.string.unavailable_body : R.string.network_error_body;
        state(root, title, body, R.attr.movieStateError, R.string.retry, retry);
    }
}
