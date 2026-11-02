package com.decisionexecution.app.presentation.sound

import android.content.Context
import com.decisionexecution.app.domain.model.FocusSessionState
import com.decisionexecution.app.domain.model.FocusSound
import com.decisionexecution.app.domain.repository.PreferencesRepository
import com.decisionexecution.app.domain.service.FocusSessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Plays the chosen focus sound while a focus phase runs, and stops it on pause, break or end */
@Singleton
class FocusSoundController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val focusSessionManager: FocusSessionManager,
    private val preferencesRepository: PreferencesRepository
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(
                focusSessionManager.sessionState.map { it is FocusSessionState.Focusing }.distinctUntilChanged(),
                preferencesRepository.getPreferences().map { it.focusSound }.distinctUntilChanged()
            ) { focusing, sound -> if (focusing) sound else FocusSound.OFF }
                .distinctUntilChanged()
                .collect { sound ->
                    if (sound == FocusSound.OFF) FocusSoundService.stop(context) else FocusSoundService.play(context, sound)
                }
        }
    }
}
