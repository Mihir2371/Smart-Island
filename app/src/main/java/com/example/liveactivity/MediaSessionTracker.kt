package com.example.liveactivity

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import com.example.model.LiveActivity
import com.example.state.CapsuleStateHolder

class MediaSessionTracker(private val context: Context) {

  private val mediaSessionManager =
    context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
  private val handler = Handler(Looper.getMainLooper())
  private var activeControllers: List<MediaController> = emptyList()
  private var listenerComponent: ComponentName? = null

  private val controllerCallback =
    object : MediaController.Callback() {
      override fun onPlaybackStateChanged(state: PlaybackState?) = publishActiveMedia()
      override fun onMetadataChanged(metadata: MediaMetadata?) = publishActiveMedia()
    }

  private val sessionsChangedListener =
    MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
      bindControllers(controllers.orEmpty())
    }

  fun start(componentName: ComponentName) {
    listenerComponent = componentName
    runCatching {
      mediaSessionManager.addOnActiveSessionsChangedListener(
        sessionsChangedListener,
        componentName,
        handler,
      )
      bindControllers(mediaSessionManager.getActiveSessions(componentName))
    }
  }

  fun stop() {
    runCatching { mediaSessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener) }
    activeControllers.forEach { runCatching { it.unregisterCallback(controllerCallback) } }
    activeControllers = emptyList()
  }

  private fun bindControllers(controllers: List<MediaController>) {
    activeControllers.forEach { runCatching { it.unregisterCallback(controllerCallback) } }
    activeControllers = controllers
    activeControllers.forEach { runCatching { it.registerCallback(controllerCallback, handler) } }
    publishActiveMedia()
  }

  private fun publishActiveMedia() {
    val mapped =
      activeControllers.mapNotNull { controller ->
        val meta = controller.metadata ?: return@mapNotNull null
        val state = controller.playbackState
        val pbState = state?.state ?: PlaybackState.STATE_NONE
        val isPlaying = pbState == PlaybackState.STATE_PLAYING
        if (pbState == PlaybackState.STATE_NONE || pbState == PlaybackState.STATE_STOPPED) {
          return@mapNotNull null
        }
        val title =
          meta.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: return@mapNotNull null
        val artist =
          meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: controller.packageName
        val art =
          meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val duration = meta.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(1L)
        val pos = state?.position?.coerceIn(0L, duration) ?: 0L
        LiveActivity.MediaPlayback(
          id = "media_${controller.packageName}",
          packageName = controller.packageName,
          title = title,
          artist = artist,
          isPlaying = isPlaying,
          positionMs = pos,
          durationMs = duration,
          albumArt = art,
        )
      }
    CapsuleStateHolder.replaceMediaActivities(mapped)
  }

  fun togglePlayPause(packageName: String) {
    val controller = activeControllers.firstOrNull { it.packageName == packageName }
    if (controller != null) {
      val playing = controller.playbackState?.state == PlaybackState.STATE_PLAYING
      if (playing) controller.transportControls.pause() else controller.transportControls.play()
      return
    }
    // Simulated media fallback toggle
    val current =
      CapsuleStateHolder.uiState.value.liveActivities.firstOrNull {
        it is LiveActivity.MediaPlayback && it.packageName == packageName
      } as? LiveActivity.MediaPlayback ?: return
    CapsuleStateHolder.upsertLiveActivity(current.copy(isPlaying = !current.isPlaying))
  }

  fun skipNext(packageName: String) {
    val controller = activeControllers.firstOrNull { it.packageName == packageName }
    if (controller != null) {
      controller.transportControls.skipToNext()
      return
    }
    val current =
      CapsuleStateHolder.uiState.value.liveActivities.firstOrNull {
        it is LiveActivity.MediaPlayback && it.packageName == packageName
      } as? LiveActivity.MediaPlayback ?: return
    CapsuleStateHolder.upsertLiveActivity(
      current.copy(
        title = if (current.title == "Midnight City") "Resonance" else "Midnight City",
        artist = if (current.title == "Midnight City") "HOME" else "M83",
        positionMs = 0L,
      )
    )
  }

  fun skipPrevious(packageName: String) {
    val controller = activeControllers.firstOrNull { it.packageName == packageName }
    if (controller != null) {
      controller.transportControls.skipToPrevious()
      return
    }
    val current =
      CapsuleStateHolder.uiState.value.liveActivities.firstOrNull {
        it is LiveActivity.MediaPlayback && it.packageName == packageName
      } as? LiveActivity.MediaPlayback ?: return
    CapsuleStateHolder.upsertLiveActivity(current.copy(positionMs = 0L))
  }
}
