package com.group04.scrapbookwidget.data.service;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
@InstallIn(SingletonComponent.class)
public class ServiceModule {
    private final String BASE_URL = "http://10.0.2.2:3000/api/v1/";

    @Singleton
    @Provides
    public FirebaseAuth provideFirebaseAuth() {
        return FirebaseAuth.getInstance();
    }

    @Singleton
    @Provides
    public OkHttpClient provideOkHttpClient(FirebaseAuth firebaseAuth) {
        return new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request originalRequest = chain.request();
                        FirebaseUser user = firebaseAuth.getCurrentUser();

                        if (user != null) {
                            try {
                                // Retrofit/OkHttp calls run on background threads, 
                                // so we can safely block to get the token.
                                GetTokenResult tokenResult = Tasks.await(user.getIdToken(false));
                                String token = tokenResult.getToken();

                                if (token != null) {
                                    Request authenticatedRequest = originalRequest.newBuilder()
                                            .header("Authorization", "Bearer " + token)
                                            .build();
                                    return chain.proceed(authenticatedRequest);
                                }
                            } catch (ExecutionException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        
                        return chain.proceed(originalRequest);
                    }
                })
                .build();
    }

    @Singleton
    @Provides
    public Retrofit provideRetrofit(OkHttpClient okHttpClient) {
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    @Singleton
    @Provides
    public GroupService provideGroupService(Retrofit retrofit) {
        return retrofit.create(GroupService.class);
    }

    @Singleton
    @Provides
    public MessageService provideMessageService(Retrofit retrofit) {
        return retrofit.create(MessageService.class);
    }

    @Singleton
    @Provides
    public ScrapbookService provideScrapbookService(Retrofit retrofit) {
        return retrofit.create(ScrapbookService.class);
    }

    @Singleton
    @Provides
    public TemplateService provideTemplateService(Retrofit retrofit) {
        return retrofit.create(TemplateService.class);
    }

    @Singleton
    @Provides
    public UserService provideUserService(Retrofit retrofit) {
        return retrofit.create(UserService.class);
    }

    @Singleton
    @Provides
    public WidgetService provideWidgetService(Retrofit retrofit) {
        return retrofit.create(WidgetService.class);
    }
}
