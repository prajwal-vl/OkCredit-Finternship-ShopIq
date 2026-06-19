package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "temperature") val temperature: Double? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content? = null
)

// UI-focused parsed AI structures
@JsonClass(generateAdapter = true)
data class AISuggestedItem(
    @Json(name = "name") val name: String,
    @Json(name = "category") val category: String,
    @Json(name = "price") val price: Double,
    @Json(name = "costPrice") val costPrice: Double,
    @Json(name = "barcode") val barcode: String
)

@JsonClass(generateAdapter = true)
data class AIParsedBillLine(
    @Json(name = "itemName") val itemName: String,
    @Json(name = "quantity") val quantity: Int,
    @Json(name = "price") val price: Double
)

@JsonClass(generateAdapter = true)
data class AIParsedBillResponse(
    @Json(name = "items") val items: List<AIParsedBillLine>
)
