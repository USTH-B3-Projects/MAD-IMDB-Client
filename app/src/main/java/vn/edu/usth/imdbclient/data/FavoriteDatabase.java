package vn.edu.usth.imdbclient.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Movie.class}, version = 2, exportSchema = true)
public abstract class FavoriteDatabase extends RoomDatabase {
    public static final Migration FROM_1_TO_2 = new Migration(1, 2) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE favorite_movies ADD COLUMN savedAt INTEGER NOT NULL DEFAULT 0");
        }
    };
    public abstract FavoriteDao favorites();
}
