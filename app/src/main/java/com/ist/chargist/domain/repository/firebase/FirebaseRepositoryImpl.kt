package com.ist.chargist.domain.repository.firebase

import com.hocel.assetmanager.utils.DispatcherProvider
import com.ist.chargist.domain.DatabaseRepository
import com.ist.chargist.domain.DeviceInfoProvider
import com.ist.chargist.domain.ImageRepository
import javax.inject.Inject

class FirebaseRepositoryImpl @Inject constructor(
    private val deviceInfo: DeviceInfoProvider,
    private val imageRepository: ImageRepository,
    dispatcherProvider: DispatcherProvider
) : DatabaseRepository {

}