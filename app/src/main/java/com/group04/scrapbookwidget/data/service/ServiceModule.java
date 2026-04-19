package com.group04.scrapbookwidget.data.service;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.group04.scrapbookwidget.data.realtime.GroupRealtimeSocketClient;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

@Module
@InstallIn(SingletonComponent.class)
public class ServiceModule {
    private final String BASE_URL = "http://192.168.1.5:3000/api/v1/";

    @Provides
    @Named("baseUrl")
    public String provideBaseUrl() {
        return BASE_URL;
    }

    @Singleton
    @Provides
    public FirebaseAuth provideFirebaseAuth() {
        return FirebaseAuth.getInstance();
    }

    @Singleton
    @Provides
    public OkHttpClient provideOkHttpClient(FirebaseAuth firebaseAuth) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request originalRequest = chain.request();

                        // 1. Skip if it's the Google Auth endpoint
                        // 2. Skip if the request already has an Authorization header (e.g. from manual SSE call)
                        if (originalRequest.url().toString().contains("auth/google") || 
                            originalRequest.header("Authorization") != null) {
                            return chain.proceed(originalRequest);
                        }

                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            try {
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
    public Retrofit provideRetrofit(OkHttpClient okHttpClient, @Named("baseUrl") String baseUrl) {
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    @Singleton
    @Provides
    public AuthService provideAuthService(Retrofit retrofit) {
        return retrofit.create(AuthService.class);
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

    @Singleton
    @Provides
    public BackgroundImageService provideBackgroundImageService(Retrofit retrofit) {
        return retrofit.create(BackgroundImageService.class);
    }

    @Singleton
    @Provides
    public GroupRealtimeSocketClient provideGroupRealtimeSocketClient(
            OkHttpClient okHttpClient,
            FirebaseAuth firebaseAuth,
            @Named("baseUrl") String baseUrl
    ) {
        return new GroupRealtimeSocketClient(okHttpClient, firebaseAuth, baseUrl);
    }

}
