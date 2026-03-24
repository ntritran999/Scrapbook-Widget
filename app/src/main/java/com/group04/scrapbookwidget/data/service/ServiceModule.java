package com.group04.scrapbookwidget.data.service;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import retrofit2.Retrofit;

@Module
@InstallIn(SingletonComponent.class)
public class ServiceModule {
    private final String BASE_URL = "http://10.0.2.2:3000/api/v1";
    @Singleton
    @Provides
    public GroupService provideGroupService() {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .build()
                .create(GroupService.class);
    }

    @Singleton
    @Provides
    public MessageService provideMessageService() {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .build()
                .create(MessageService.class);
    }
    @Singleton
    @Provides
    public ScrapbookService provideScrapbookService() {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .build()
                .create(ScrapbookService.class);
    }
    @Singleton
    @Provides
    public TemplateService provideTemplateService() {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .build()
                .create(TemplateService.class);
    }
    @Singleton
    @Provides
    public UserService provideUserService() {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .build()
                .create(UserService.class);
    }
}
