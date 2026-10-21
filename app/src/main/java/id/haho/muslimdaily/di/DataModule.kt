package id.haho.muslimdaily.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.haho.muslimdaily.data.content.AssetContentSource
import id.haho.muslimdaily.data.content.ContentSource
import id.haho.muslimdaily.data.db.ContentDatabase
import id.haho.muslimdaily.data.db.UserDatabase
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    /** Content is rebuilt from assets, so a destructive migration loses nothing. */
    @Provides
    @Singleton
    fun contentDatabase(@ApplicationContext context: Context): ContentDatabase =
        Room.databaseBuilder(context, ContentDatabase::class.java, "content.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    /** User data: schema changes must ship real migrations. */
    @Provides
    @Singleton
    fun userDatabase(@ApplicationContext context: Context): UserDatabase =
        Room.databaseBuilder(context, UserDatabase::class.java, "user.db").build()

    @Provides
    fun clock(): Clock = Clock.systemDefaultZone()

    @Provides
    fun contentSource(assets: AssetContentSource): ContentSource = assets
}
