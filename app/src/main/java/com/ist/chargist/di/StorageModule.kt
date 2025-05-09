package com.hocel.assetmanager.di

import com.ist.chargist.domain.ImageRepository
import com.ist.chargist.domain.repository.firebase.FirebaseStorageImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {

    @Singleton
    @Binds
    abstract fun provideImageRepository(storageRepositoryImpl: FirebaseStorageImpl): ImageRepository
}