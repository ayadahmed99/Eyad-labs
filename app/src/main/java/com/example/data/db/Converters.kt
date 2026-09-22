package com.example.data.db

import com.example.data.model.AttachmentItem
import org.json.JSONArray
import org.json.JSONObject

object AttachmentJsonConverter {
    fun toJson(attachments: List<AttachmentItem>): String {
        val array = JSONArray()
        for (item in attachments) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("sizeBytes", item.sizeBytes)
            obj.put("formattedSize", item.formattedSize)
            obj.put("mimeType", item.mimeType)
            obj.put("uriString", item.uriString)
            obj.put("isImage", item.isImage)
            obj.put("textContentSnippet", item.textContentSnippet ?: "")
            array.put(obj)
        }
        return array.toString()
    }

    fun fromJson(json: String?): List<AttachmentItem> {
        if (json.isNullOrBlank() || json == "[]") return emptyList()
        val list = mutableListOf<AttachmentItem>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val snippet = obj.optString("textContentSnippet")
                list.add(
                    AttachmentItem(
                        id = obj.optString("id", ""),
                        name = obj.optString("name", ""),
                        sizeBytes = obj.optLong("sizeBytes", 0L),
                        formattedSize = obj.optString("formattedSize", ""),
                        mimeType = obj.optString("mimeType", ""),
                        uriString = obj.optString("uriString", ""),
                        isImage = obj.optBoolean("isImage", false),
                        textContentSnippet = if (snippet.isBlank()) null else snippet
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
