package vn.edu.usth.imdbclient.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import java.util.List;
import vn.edu.usth.imdbclient.R;
import vn.edu.usth.imdbclient.data.Movie;

final class FeaturedAdapter extends PagerAdapter {
    private final MainActivity activity;
    private final List<Movie> movies;

    FeaturedAdapter(MainActivity activity, List<Movie> movies) {
        this.activity = activity;
        this.movies = movies;
    }

    @Override
    public int getCount() {
        return movies.size() > 1 ? movies.size() + 2 : movies.size();
    }

    int movieIndex(int position) {
        return movies.size() > 1 ? (position + movies.size() - 1) % movies.size() : 0;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }
    @NonNull @Override public Object instantiateItem(@NonNull ViewGroup container, int position) {
        View view = LayoutInflater.from(activity).inflate(R.layout.item_featured, container, false);
        Movie movie = movies.get(movieIndex(position));
        ImageView poster = view.findViewById(R.id.poster);
        Ui.image(poster, movie.imageUrl(false), activity.getString(R.string.poster_description, movie.title));
        poster.setOnClickListener(v -> activity.openMovie(movie));
        container.addView(view);
        return view;
    }
    @Override public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        View view = (View) object;
        Ui.cancelImages(view);
        container.removeView(view);
    }
}
