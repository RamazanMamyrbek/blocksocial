package com.blocksocial.di

import com.blocksocial.detection.WriteScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WriteScopeModule {

    @Provides
    @Singleton
    @WriteScope
    fun writeScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
