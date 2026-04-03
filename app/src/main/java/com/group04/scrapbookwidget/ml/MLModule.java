package com.group04.scrapbookwidget.ml;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Hilt module for providing ML-related dependencies.
 * Provides FaceEmbeddingManager as a singleton to ensure it's initialized only once.
 */
@Module
@InstallIn(SingletonComponent.class)
public class MLModule {
    
    @Singleton
    @Provides
    public FaceEmbeddingManager provideFaceEmbeddingManager(@ApplicationContext Context context) {
        FaceEmbeddingManager manager = new FaceEmbeddingManager(context);
        return manager;
    }
}
