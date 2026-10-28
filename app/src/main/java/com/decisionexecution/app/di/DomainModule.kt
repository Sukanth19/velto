package com.decisionexecution.app.di

import com.decisionexecution.app.domain.service.ScoringEngine
import com.decisionexecution.app.domain.service.EisenhowerClassifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
// FocusSessionManager is constructor-injected; see FocusTimerModule for its collaborators
object DomainModule {
    
    @Provides
    @Singleton
    fun provideScoringEngine(): ScoringEngine = ScoringEngine()
    
    @Provides
    @Singleton
    fun provideEisenhowerClassifier(): EisenhowerClassifier = EisenhowerClassifier()
}
