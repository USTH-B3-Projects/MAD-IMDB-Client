package vn.edu.usth.imdbclient;

import android.app.Application;
import android.graphics.Bitmap;
import android.util.LruCache;
import androidx.room.Room;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import vn.edu.usth.imdbclient.data.FavoriteDatabase;
import vn.edu.usth.imdbclient.data.MovieDataSource;
import vn.edu.usth.imdbclient.data.MovieRepository;

public class MovieApplication extends Application {
    public MovieDataSource repository;
    public ImageLoader images;

    @Override public void onCreate() {
        super.onCreate();
        RequestQueue queue = Volley.newRequestQueue(this);
        FavoriteDatabase database = Room.databaseBuilder(this, FavoriteDatabase.class, "favorites.db")
                .addMigrations(FavoriteDatabase.FROM_1_TO_2).build();
        repository = new MovieRepository(queue, database.favorites());
        int cacheSize = (int) Math.min(Runtime.getRuntime().maxMemory() / 8, 24 * 1024 * 1024);
        LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(cacheSize) {
            @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount(); }
        };
        images = new ImageLoader(queue, new ImageLoader.ImageCache() {
            @Override public Bitmap getBitmap(String key) { return cache.get(key); }
            @Override public void putBitmap(String key, Bitmap value) { cache.put(key, value); }
        });
    }
}
