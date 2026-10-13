package com.animevost.app.core.data.repository

import com.animevost.app.core.domain.model.AnimeStatus
import com.google.gson.JsonObject
import com.google.gson.JsonParser

internal fun decodePendingUserListChanges(value: String): Map<Int, String?> {
    val result = linkedMapOf<Int, String?>()
    JsonParser.parseString(value).asJsonObject.entrySet().forEach { (key, entry) ->
        val id = key.toIntOrNull()?.takeIf { it > 0 }
            ?: throw UserListPayloadException("Invalid pending anime id")
        val status = if (entry.isJsonNull) null else entry.asString
        if (status != null && AnimeStatus.fromCode(status) == null) {
            throw UserListPayloadException("Invalid pending anime status")
        }
        result[id] = status
    }
    return result
}

internal fun encodePendingUserListChanges(changes: Map<Int, String?>): String =
    JsonObject().apply {
        changes.forEach { (id, status) -> addProperty(id.toString(), status) }
    }.toString()

internal fun applyUserListChanges(
    remote: Map<Int, String>,
    changes: Map<Int, String?>,
): Map<Int, String> = remote.toMutableMap().apply {
    changes.forEach { (id, status) ->
        if (status == null) remove(id) else put(id, status)
    }
}
