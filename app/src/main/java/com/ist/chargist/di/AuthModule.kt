package com.ist.chargist.di

import com.ist.chargist.domain.AuthenticationRepository
import com.ist.chargist.domain.repository.firebase.FirebaseAuthImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Singleton
    @Binds
    abstract fun provideAuthenticationRepository(authRepositoryImpl: FirebaseAuthImpl): AuthenticationRepository
}