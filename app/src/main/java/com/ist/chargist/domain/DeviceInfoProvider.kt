package com.ist.chargist.domain

interface DeviceInfoProvider {

    fun hasInternetConnection(): Boolean
}