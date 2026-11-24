package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CutoutConfig
import com.example.model.CutoutShapeMode
import com.example.model.QuickNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.capsuleDataStore: DataStore<Preferences> by preferencesDataStore(name = "capsule_settings")

class SettingsRepository(private val context: Context) {

  private object Keys {
    val MODE = stringPreferencesKey("cutout_mode")
    val OFFSET_X = floatPreferencesKey("offset_x_dp")
    val OFFSET_Y = floatPreferencesKey("offset_y_dp")
    val WIDTH = floatPreferencesKey("width_dp")
    val HEIGHT = floatPreferencesKey("height_dp")
    val RADIUS = floatPreferencesKey("corner_radius_dp")
    val PRIVACY_DOTS = booleanPreferencesKey("show_privacy_dots")
    val AUTO_COLLAPSE = intPreferencesKey("auto_collapse_sec")
    val QUICK_NOTES = stringPreferencesKey("quick_notes_json")
  }

  val cutoutConfigFlow: Flow<CutoutConfig> =
    context.capsuleDataStore.data.map { prefs ->
      val modeName = prefs[Keys.MODE] ?: CutoutShapeMode.FLOATING_PILL.name
      val mode =
        runCatching { CutoutShapeMode.valueOf(modeName) }.getOrDefault(CutoutShapeMode.FLOATING_PILL)
      CutoutConfig(
        mode = mode,
        offsetXdp = prefs[Keys.OFFSET_X] ?: 0f,
        offsetYdp = prefs[Keys.OFFSET_Y] ?: 8f,
        widthDp = prefs[Keys.WIDTH] ?: 128f,
        heightDp = prefs[Keys.HEIGHT] ?: 34f,
        cornerRadiusDp = prefs[Keys.RADIUS] ?: 20f,
        showPrivacyDots = prefs[Keys.PRIVACY_DOTS] ?: true,
        autoCollapseSeconds = prefs[Keys.AUTO_COLLAPSE] ?: 6,
      )
    }

  val quickNotesFlow: Flow<List<QuickNote>> =
    context.capsuleDataStore.data.map { prefs ->
      val raw = prefs[Keys.QUICK_NOTES] ?: "[]"
      parseNotes(raw)
    }

  suspend fun saveCutoutConfig(config: CutoutConfig) {
    context.capsuleDataStore.edit { prefs ->
      prefs[Keys.MODE] = config.mode.name
      prefs[Keys.OFFSET_X] = config.offsetXdp
      prefs[Keys.OFFSET_Y] = config.offsetYdp
      prefs[Keys.WIDTH] = config.widthDp
      prefs[Keys.HEIGHT] = config.heightDp
      prefs[Keys.RADIUS] = config.cornerRadiusDp
      prefs[Keys.PRIVACY_DOTS] = config.showPrivacyDots
      prefs[Keys.AUTO_COLLAPSE] = config.autoCollapseSeconds
    }
  }

  suspend fun addQuickNote(content: String) {
    val trimmed = content.trim()
    if (trimmed.isEmpty()) return
    context.capsuleDataStore.edit { prefs ->
      val current = parseNotes(prefs[Keys.QUICK_NOTES] ?: "[]").toMutableList()
      current.add(
        0,
        QuickNote(
          id = System.currentTimeMillis().toString(),
          content = trimmed,
          createdAtMs = System.currentTimeMillis(),
        ),
      )
      prefs[Keys.QUICK_NOTES] = serializeNotes(current.take(30))
    }
  }

  suspend fun deleteQuickNote(id: String) {
    context.capsuleDataStore.edit { prefs ->
      val updated = parseNotes(prefs[Keys.QUICK_NOTES] ?: "[]").filterNot { it.id == id }
      prefs[Keys.QUICK_NOTES] = serializeNotes(updated)
    }
  }

  private fun parseNotes(raw: String): List<QuickNote> =
    runCatching {
      val array = JSONArray(raw)
      buildList {
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          add(
            QuickNote(
              id = obj.getString("id"),
              content = obj.getString("content"),
              createdAtMs = obj.getLong("createdAtMs"),
            )
          )
        }
      }
    }.getOrDefault(emptyList())

  private fun serializeNotes(notes: List<QuickNote>): String {
    val array = JSONArray()
    notes.forEach { note ->
      array.put(
        JSONObject().apply {
          put("id", note.id)
          put("content", note.content)
          put("createdAtMs", note.createdAtMs)
        }
      )
    }
    return array.toString()
  }
}
