package com.aiassistant.di

import android.content.Context
import androidx.room.Room
import com.aiassistant.data.api.GroqApi
import com.aiassistant.data.local.AppDatabase
import com.aiassistant.data.local.HistoryDao
import com.aiassistant.data.repository.AIRepositoryImpl
import com.aiassistant.data.repository.HistoryRepositoryImpl
import com.aiassistant.data.repository.SettingsRepositoryImpl
import com.aiassistant.domain.repository.AIRepository
import com.aiassistant.domain.repository.HistoryRepository
import com.aiassistant.domain.repository.SettingsRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .setLenient()
        .create()
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }
    
    @Provides
    @Singleton
    fun provideGroqApi(client: OkHttpClient, gson: Gson): GroqApi {
        return Retrofit.Builder()
            .baseUrl("https://api.groq.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(GroqApi::class.java)
    }
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ai_assistant.db"
        ).build()
    }
    
    @Provides
    @Singleton
    fun provideHistoryDao(database: AppDatabase): HistoryDao {
        return database.historyDao()
    }
    
    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository = SettingsRepositoryImpl(context)
    
    @Provides
    @Singleton
    fun provideAIRepository(
        groqApi: GroqApi,
        settingsRepository: SettingsRepository,
        gson: Gson
    ): AIRepository = AIRepositoryImpl(groqApi, settingsRepository, gson)
    
    @Provides
    @Singleton
    fun provideHistoryRepository(
        historyDao: HistoryDao
    ): HistoryRepository = HistoryRepositoryImpl(historyDao)
}
