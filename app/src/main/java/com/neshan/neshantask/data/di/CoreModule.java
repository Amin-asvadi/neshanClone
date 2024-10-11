package com.neshan.neshantask.data.di;

import com.neshan.neshantask.data.network.ApiClient;
import com.neshan.neshantask.data.network.RetrofitConfig;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

import javax.inject.Singleton;

/**
 * Provides class instances to be used in other modules
 */
@Module
@InstallIn(SingletonComponent.class)
public class CoreModule {

    // Some singleton classes like database helpers, API service helper classes, preferences helper classes
    // will be provided here

    @Provides
    @Singleton
    public ApiClient provideApiClient(RetrofitConfig retrofitConfig) {
        retrofitConfig.initialize();
        return retrofitConfig.createService(ApiClient.class);
    }
}
