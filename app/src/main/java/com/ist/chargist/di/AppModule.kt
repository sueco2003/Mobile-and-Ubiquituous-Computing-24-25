package com.ist.chargist.di

import android.content.Context
import com.ist.chargist.domain.repository.android.DeviceInfoProviderImpl
import com.hocel.assetmanager.utils.DefaultDispatcherProvider
import com.hocel.assetmanager.utils.DispatcherProvider
import com.ist.chargist.ChargISTApp
import com.ist.chargist.domain.DeviceInfoProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideApplication(@ApplicationContext app: Context): ChargISTApp {
        return app as ChargISTApp
    }

    @Singleton
    @Provides
    fun provideDeviceInfo(@ApplicationContext app: Context): DeviceInfoProvider = DeviceInfoProviderImpl(app)

    @Provides
    fun provideDefaultDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()
}