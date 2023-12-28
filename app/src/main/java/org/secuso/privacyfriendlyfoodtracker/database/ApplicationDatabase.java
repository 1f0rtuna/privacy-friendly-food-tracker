/*
This file is part of Privacy friendly food tracker.

Privacy friendly food tracker is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Privacy friendly food tracker is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Privacy friendly food tracker.  If not, see <https://www.gnu.org/licenses/>.
*/
package org.secuso.privacyfriendlyfoodtracker.database;

import androidx.room.migration.Migration;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;
import net.sqlcipher.database.SupportFactory;

import org.secuso.privacyfriendlyfoodtracker.database.converter.DateConverter;
import org.secuso.privacyfriendlyfoodtracker.helpers.KeyGenHelper;

/**
 * Database singleton.
 *
 * @author Andre Lutz
 */
@Database(entities = {ConsumedEntries.class, Product.class}, version = 3, exportSchema = true)
@TypeConverters({DateConverter.class})
public abstract class ApplicationDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "consumed_entries_database";

    public abstract ConsumedEntriesDao getConsumedEntriesDao();

    public abstract ProductDao getProductDao();

    public abstract ConsumedEntrieAndProductDao getConsumedEntriesAndProductDao();

    private static ApplicationDatabase sInstance;

    private final MutableLiveData<Boolean> mIsDatabaseCreated = new MutableLiveData<>();

    public static ApplicationDatabase getInstance(final Context context) throws Exception {
        if (sInstance == null) {
            synchronized (ApplicationDatabase.class) {
                if (sInstance == null) {
                    SupportFactory factory = new SupportFactory(SQLiteDatabase.getBytes(KeyGenHelper.getSecretKeyAsChar(context)), new SQLiteDatabaseHook() {
                        @Override
                        public void preKey(SQLiteDatabase database) {
                        }

                        @Override
                        public void postKey(SQLiteDatabase database) {
                            database.rawExecSQL("PRAGMA cipher_compatibility = 3;");
                        }
                    });

                    sInstance = Room.databaseBuilder(context.getApplicationContext(), ApplicationDatabase.class, DATABASE_NAME)
                            .openHelperFactory(factory)
                            .allowMainThreadQueries()
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    try {
                                        ApplicationDatabase database = ApplicationDatabase.getInstance(context);
                                        // notify that the database was created and it's ready to be used
                                        database.setDatabaseCreated();
                                    } catch (Exception e) {
                                        Log.e("ApplicationDatabase", e.getMessage());
                                    }
                                }
                            })
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3).build();

                }
            }
        }
        return sInstance;
    }

    /*
    Migrate the database so it contains columns with carbs,fat,satfat etc.
     */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            String[] newFields = new String[]{"carbs","sugar", "protein", "fat","satFat"};
            for(String field : newFields) {
                database.execSQL("ALTER TABLE Product "
                            + " ADD COLUMN "+ field +" REAL NOT NULL default 0");
            }
        }
    };

    /***
     * Migrate database so it also contains micro nutriments.
     */
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            String[] newFields = new String[]{"salt", "fiber", "vitaminA_retinol", "betaCarotin", "vitaminD", "vitaminE", "vitaminK", "thiamin_B1", "riboflavin_B2", "niacin", "vitaminB6", "folat", "pantothenacid", "biotin", "cobalamin_B12", "vitaminC", "natrium", "chlorid", "kalium", "calcium", "phosphor", "magnesium", "eisen", "jod", "fluorid", "zink", "selen", "kupfer", "mangan", "chrom", "molybdaen"};
            for(String field : newFields) {
                database.execSQL("ALTER TABLE Product "
                            + " ADD COLUMN "+ field +" REAL NOT NULL default 0");
            }
        }
    };




    private void setDatabaseCreated() {
        mIsDatabaseCreated.postValue(true);
    }

    /**
     * Indicates if the database is created.
     *
     * @return if database is created
     */
    public LiveData<Boolean> getDatabaseCreated() {
        return mIsDatabaseCreated;
    }
}
