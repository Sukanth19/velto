package com.decisionexecution.app.di

import com.decisionexecution.app.domain.repository.PreferencesRepository
import com.decisionexecution.app.domain.service.ScoringEngine
import com.decisionexecution.app.domain.service.FocusSessionManager
import com.decisionexecution.app.domain.service.EisenhowerClassifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    
    @Provides
    @Singleton
    fun provideScoringEngine(): ScoringEngine = ScoringEngine()
    
    @Provides
    @Singleton
    fun provideEisenhowerClassifier(): EisenhowerClassifier = EisenhowerClassifier()
    
    @Provides
    @Singleton
    fun provideFocusSessionManager(
        preferencesRepository: PreferencesRepository
    ): FocusSessionManager = FocusSessionManager(preferencesRepository)
}
