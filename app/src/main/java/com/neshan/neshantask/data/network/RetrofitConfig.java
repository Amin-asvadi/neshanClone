package com.neshan.neshantask.data.network;

import android.util.Pair;

import androidx.multidex.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.neshan.neshantask.data.AppConfig;
import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RetrofitConfig {

    private Retrofit.Builder builder;
    private OkHttpClient.Builder httpClientBuilder;

    @Inject
    public RetrofitConfig() {
        initialize();
    }

    public void initialize() {
        // Initialize the HttpClient builder
        httpClientBuilder = getHttpClientBuilder();
        // Add interceptors
        httpClientBuilder.addInterceptor(getMainInterceptor());
        httpClientBuilder.addInterceptor(getLogger());
    }

    private boolean isLogEnabled() {
        return BuildConfig.DEBUG;
    }

    public String getBaseUrl() {
        return AppConfig.API_URL;
    }

    /**
     * Initialize Retrofit Builder
     */
    public Retrofit.Builder getRetrofitBuilder() {
        if (builder == null) {
            builder = new Retrofit.Builder()
                    .baseUrl(getBaseUrl())
                    .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                    .addConverterFactory(getConverterFactory())
                    .client(httpClientBuilder.build());
        }
        return builder;
    }

    /**
     * Initialize OkHttpClient instance and setup configurations
     */
    public OkHttpClient.Builder getHttpClientBuilder() {
        return new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .writeTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS);
    }

    /**
     * Initialize main interceptor
     */
    public Interceptor getMainInterceptor() {
        return new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request.Builder requestBuilder = chain.request().newBuilder();

                // Add required headers
                List<Pair<String, String>> headers = getHeaders();
                if (headers != null) {
                    for (Pair<String, String> header : headers) {
                        requestBuilder.addHeader(header.first, header.second);
                    }
                }

                Request request = requestBuilder.build();
                Response response = chain.proceed(request);
                onResponse(response);

                return response;
            }
        };
    }

    /**
     * Initialize logger interceptor
     */
    public Interceptor getLogger() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(isLogEnabled() ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);
        return loggingInterceptor;
    }

    /**
     * Initialize converter factory
     */
    public Converter.Factory getConverterFactory() {
        Gson gson = new GsonBuilder().setLenient().create();
        return GsonConverterFactory.create(gson);
    }

    /**
     * Override this method to do actions before upper layer observers are triggered.
     */
    public void onResponse(Response response) {
        // Handle response actions here
    }

    /**
     * Override this method and initialize your headers if necessary
     */
    public List<Pair<String, String>> getHeaders() {
        List<Pair<String, String>> headers = new ArrayList<>();
        headers.add(new Pair<>("Api-Key", "service.4310373c08f042dfa5bd0e39ff6a6be1"));
        return headers;
    }

    /**
     * Create Service for a retrofit interface
     */
    public <T> T createService(Class<T> serviceClass) {
        Retrofit retrofit = getRetrofitBuilder().build();
        return retrofit.create(serviceClass);
    }
}
