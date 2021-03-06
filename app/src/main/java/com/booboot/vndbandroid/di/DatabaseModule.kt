package com.booboot.vndbandroid.di

import android.app.Application
import com.booboot.vndbandroid.dao.MyObjectBox
import dagger.Module
import dagger.Provides
import io.objectbox.BoxStore
import javax.inject.Singleton

/**
 * Module that provides application-scoped data stores, i.e. stores that can cache data as long as the app lives.
 * Especially useful to replace static fields, or to retain data when the configuration changes: in a Presenter,
 *
 * @Inject a store, set its data when it is fetched, and get it after configuration changes to retrieve it.
 */
@Module
internal class DatabaseModule {
    @Provides
    @Singleton
    fun boxStore(application: Application): BoxStore = MyObjectBox.builder().androidContext(application).build()
}