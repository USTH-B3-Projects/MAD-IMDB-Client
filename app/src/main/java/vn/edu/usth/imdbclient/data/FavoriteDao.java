package vn.edu.usth.imdbclient.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.List;

@Dao
public abstract class FavoriteDao {
    @Query("SELECT * FROM favorite_movies ORDER BY savedAt DESC, id DESC")
    public abstract List<Movie> getAll();

    @Query("SELECT COUNT(*) FROM favorite_movies WHERE id = :id")
    public abstract int contains(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void save(Movie movie);

    @Query("DELETE FROM favorite_movies WHERE id = :id")
    public abstract void delete(int id);

    @Query("DELETE FROM favorite_movies WHERE id IN (:ids)")
    public abstract void deleteIds(List<Integer> ids);

    @Transaction
    public boolean add(Movie movie) {
        if (contains(movie.id) != 0) {
            return false;
        }
        movie.savedAt = System.currentTimeMillis();
        save(movie);
        return true;
    }

    @Transaction
    public void restore(List<Movie> movies) {
        for (Movie movie : movies) {
            save(movie);
        }
    }
}
