package vn.edu.usth.imdbclient;

import androidx.room.Room;
import android.content.Context;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Arrays;
import vn.edu.usth.imdbclient.data.FavoriteDao;
import vn.edu.usth.imdbclient.data.FavoriteDatabase;
import vn.edu.usth.imdbclient.data.Movie;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class FavoritesDatabaseTest {
    @Test public void addDuplicateBatchRemoveAndUndoAreAtomicAndDeduplicated() {
        FavoriteDatabase database = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().getTargetContext(), FavoriteDatabase.class).build();
        try {
            FavoriteDao dao = database.favorites();
            Movie first = new Movie(); first.id = 1; first.title = "First";
            Movie second = new Movie(); second.id = 2; second.title = "Second";
            assertTrue(dao.add(first)); assertFalse(dao.add(first));
            assertEquals(1, dao.getAll().size());
            dao.delete(first.id); assertEquals(0, dao.getAll().size());
            first.savedAt = 100; second.savedAt = 200;
            dao.restore(Arrays.asList(first, first, second));
            assertEquals(2, dao.getAll().size());
            assertEquals(2, dao.getAll().get(0).id);
            dao.deleteIds(Arrays.asList(1, 2)); assertEquals(0, dao.getAll().size());
            dao.restore(Arrays.asList(first, second)); assertEquals(2, dao.getAll().size());
        } finally { database.close(); }
    }
    @Test public void schemaUpgradePreservesPreviouslySavedMovies() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name = "favorites_migration_test.db";
        context.deleteDatabase(name);
        SupportSQLiteOpenHelper helper = new FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(new SupportSQLiteOpenHelper.Callback(1) {
                    @Override public void onCreate(SupportSQLiteDatabase database) {
                        database.execSQL("CREATE TABLE favorite_movies (id INTEGER NOT NULL PRIMARY KEY, title TEXT NOT NULL, posterPath TEXT, backdropPath TEXT, releaseDate TEXT, overview TEXT, genres TEXT, `cast` TEXT, director TEXT, runtime INTEGER NOT NULL, voteAverage REAL NOT NULL)");
                        database.execSQL("INSERT INTO favorite_movies (id,title,runtime,voteAverage) VALUES (99,'Saved before upgrade',120,8.5)");
                    }
                    @Override public void onUpgrade(SupportSQLiteDatabase database, int oldVersion, int newVersion) { }
                }).build());
        helper.getWritableDatabase(); helper.close();
        FavoriteDatabase database = Room.databaseBuilder(context, FavoriteDatabase.class, name)
                .addMigrations(FavoriteDatabase.FROM_1_TO_2).build();
        try {
            assertEquals("Saved before upgrade", database.favorites().getAll().get(0).title);
            assertEquals(0, database.favorites().getAll().get(0).savedAt);
        } finally { database.close(); context.deleteDatabase(name); }
    }
}
