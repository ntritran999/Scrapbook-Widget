package com.group04.scrapbookwidget.data.repository;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public abstract class RepositoryModule {
    @Singleton
    @Binds
//    public abstract IScrapbookRepository bindScrapbookRepository(ScrapbookRepository scrapbookRepository);
    public abstract IScrapbookRepository bindScrapbookRepository(MockScrapbookRepository mockScrapbookRepository);

    @Singleton
    @Binds
    public abstract IWidgetRepository bindWidgetRepository(WidgetRepository widgetRepository);

    @Singleton
    @Binds
    public abstract IUserRepository bindUserRepository(MockUserRepo mockUserRepo);
}
