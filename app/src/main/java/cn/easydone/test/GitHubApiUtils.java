package cn.easydone.test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.GsonConverterFactory;
import retrofit2.Retrofit;
import retrofit2.RxJavaCallAdapterFactory;

/**
 * Created by Android Studio
 * User: Ailurus(ailurus@foxmail.com)
 * Date: 2016-01-07
 * Time: 08:59
 */
public class GitHubApiUtils {

    private static GitHubApiUtils mInstance;
    private GitHubApi gitHubApi;

    private GitHubApiUtils() {
        /* JSON 解析 */
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .client(okHttpClient())
                .build();

        gitHubApi = retrofit.create(GitHubApi.class);
    }

    /* 单例 */
    public static GitHubApiUtils getInstance() {
        if (mInstance == null) {
            synchronized (GitHubApiUtils.class) {
                if (mInstance == null) {
                    mInstance = new GitHubApiUtils();
                }
            }
        }
        return mInstance;
    }

    public GitHubApi getGitHubApi() {
        return gitHubApi;
    }

    private OkHttpClient okHttpClient() {
        OkHttpClient client = new OkHttpClient();

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        /*List<Interceptor> interceptors = new ArrayList<>();
        interceptors.add(logging);
        interceptors.add(headerInterceptor);

        client.interceptors().addAll(interceptors);*/

        client.newBuilder().addInterceptor(logging).addInterceptor(headerInterceptor);

        return client;
    }

    /* header */
    Interceptor headerInterceptor = chain -> {
        Request original = chain.request();

        Request request = original.newBuilder()
                .addHeader("User-Agent", "Test")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("Authorization", "")
                .method(original.method(), original.body())
                .build();

        return chain.proceed(request);
    };
}
