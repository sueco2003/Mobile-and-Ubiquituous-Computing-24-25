package com.ist.chargist.di

import com.hocel.assetmanager.di.AppFeatures
import com.hocel.assetmanager.di.Feature
import com.hocel.assetmanager.di.FeatureOption
import com.hocel.assetmanager.utils.DispatcherProvider
import com.ist.chargist.domain.DatabaseRepository
import com.ist.chargist.domain.DeviceInfoProvider
import com.ist.chargist.domain.ImageRepository
import com.ist.chargist.domain.repository.firebase.FirebaseRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabaseRepository(
        appFeatures: AppFeatures,
        deviceInfo: DeviceInfoProvider,
        imageRepository: ImageRepository,
        dispatcherProvider: DispatcherProvider,
    ): DatabaseRepository {
        if (appFeatures.contains(Feature.Database(FeatureOption.DatabaseType.Firebase))) {
            return FirebaseRepositoryImpl(
                deviceInfo = deviceInfo,
                dispatcherProvider = dispatcherProvider
            )
        }
        throw IllegalArgumentException("Invalid Database Feature Value")
    }
}