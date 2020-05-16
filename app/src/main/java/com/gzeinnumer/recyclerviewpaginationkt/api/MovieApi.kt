@file:Suppress("NOT_A_FUNCTION_LABEL_WARNING")

package com.gzeinnumer.recyclerviewpaginationkt.api

import android.content.Context
import com.gzeinnumer.recyclerviewpaginationkt.utils.NetworkUtil.hasNetwork
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class MovieApi {

    companion object{

        private var retrofit: Retrofit? = null

        private val CACHE_SIZE = 10 * 1024 * 1024.toLong() // 10MB Cache size

        private fun buildClient(context: Context): OkHttpClient {

            val REWRITE_CACHE_CONTROL_INTERCEPTOR =
                Interceptor { chain: Interceptor.Chain ->
                    val originalResponse = chain.proceed(chain.request())
                    if (hasNetwork(context)) {
                        val maxAge = 60 // read from cache for 1 minute
                        return@Interceptor originalResponse.newBuilder().header("Cache-Control", "public, max-age=$maxAge").build()
                    } else {
                        val maxStale = 60 * 60 * 24 * 28 // tolerate 4-weeks stale
                        return@Interceptor originalResponse.newBuilder().header("Cache-Control", "public, only-if-cached, max-stale=$maxStale").build()
                    }
                }

            // Create Cache
            val cache = Cache(context.cacheDir, CACHE_SIZE)
            return OkHttpClient.Builder()
                .addNetworkInterceptor(REWRITE_CACHE_CONTROL_INTERCEPTOR)
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .cache(cache)
                .build()
        }

        fun getClient(context: Context): Retrofit? {
            if (retrofit == null) {
                retrofit = Retrofit.Builder()
                    .client(buildClient(context))
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl("https://api.themoviedb.org/3/")
                    .build()
            }
            return retrofit
        }
    }
}