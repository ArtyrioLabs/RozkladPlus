package com.mkr.schedule.data.api

import com.mkr.schedule.BuildConfig
import com.mkr.schedule.util.LogCollector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val packetInterceptor = Interceptor { chain ->
            val request = chain.request()
            val startNanos = System.nanoTime()

            LogCollector.log(
                "ApiPacket",
                "--> ${request.method} ${request.url}"
            )

            try {
                val response = chain.proceed(request)
                val tookMs = (System.nanoTime() - startNanos) / 1_000_000
                LogCollector.log(
                    "ApiPacket",
                    "<-- ${response.code} ${request.method} ${request.url} (${tookMs}ms)"
                )
                response
            } catch (e: IOException) {
                val tookMs = (System.nanoTime() - startNanos) / 1_000_000
                LogCollector.log(
                    "ApiPacket",
                    "<-- HTTP FAILED ${request.method} ${request.url} (${tookMs}ms): ${e.message}"
                )
                throw e
            }
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(packetInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    fun createRetrofitForBaseUrl(baseUrl: String, okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideMkrApiService(retrofit: Retrofit): MkrApiService {
        return retrofit.create(MkrApiService::class.java)
    }
}

