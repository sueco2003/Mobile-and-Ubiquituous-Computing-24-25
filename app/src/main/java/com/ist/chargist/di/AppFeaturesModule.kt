package com.hocel.assetmanager.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppFeaturesModule {

    @Singleton
    @Provides
    fun provideAppFeatures(): AppFeatures = AppFeatures().also { Timber.i("Initializing the app with $it") }
}

class AppFeatures(
    /**
     * Map of the current features in the app along with their values.
     */
    private val features: List<Feature> = listOf(
        Feature.Database(FeatureOption.DatabaseType.Firebase),
    )
) {

    /**
     * Returns True if the app contains the given [feature]
     */
    fun contains(feature: Feature): Boolean = features.contains(feature)

    override fun toString(): String {
        return features.joinToString(prefix = "AppFeatures=[\n", postfix = "\n]", separator = ",") {
            "\t" + it.toString()
        }
    }
}

/**
 * Registry of all the features in-app
 */
sealed class Feature {

    data class Database(val option: FeatureOption.DatabaseType) : Feature()
}

/**
 * Registry of all the possible options for each feature
 */
sealed class FeatureOption {

    sealed class DatabaseType : FeatureOption() {

        data object Firebase : DatabaseType()
    }
}