package com.decisionexecution.app.di

import com.decisionexecution.app.domain.service.ScoringEngine
import com.decisionexecution.app.domain.service.EisenhowerClassifier
import com.decisionexecution.app.domain.usecase.DayPhaseSource
import com.decisionexecution.app.domain.usecase.GetDayPhaseUseCase
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
    fun provideDayPhaseSource(useCase: GetDayPhaseUseCase): DayPhaseSource = useCase

    @Provides
    @Singleton
    fun provideEisenhowerClassifier(): EisenhowerClassifier = EisenhowerClassifier()
}
