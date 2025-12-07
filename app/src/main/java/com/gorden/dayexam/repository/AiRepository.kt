package com.gorden.dayexam.repository

import com.gorden.dayexam.ContextHolder
import com.gorden.dayexam.repository.model.QuestionDetail
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

import okhttp3.OkHttpClient

class AiNetworkException(message: String, cause: Throwable? = null) : IOException(message, cause)
class AiResponseParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
class AiNoApiKeyException(message: String, cause: Throwable? = null) : Exception(message, cause)

object AiRepository {
	// prompt 从 assets 加载以便运行时编辑；提供回退模板
	private const val PROMPT_ASSET_FILE = "question_template.json"

	private val gson = Gson()
	private val client = OkHttpClient.Builder()
		.connectTimeout(600, java.util.concurrent.TimeUnit.SECONDS)
		.readTimeout(600, java.util.concurrent.TimeUnit.SECONDS)
		.writeTimeout(600, java.util.concurrent.TimeUnit.SECONDS)
		.build()

	private fun loadTemplate(): String {
		return try {
			ContextHolder.application.assets.open(PROMPT_ASSET_FILE).use { it.readBytes().toString(Charsets.UTF_8) }
		} catch (e: Exception) {
			e.printStackTrace()
			return ""
		}
	}

	private fun buildPrompt(documentText: String): String {
		val template = loadTemplate()
		return StringBuilder().apply {
			append("请严格返回与下面示例结构一致的 JSON 数组，仅返回 JSON 数组（不要返回任何说明文字）。示例模板:\n")
			append(template)
			append("\n\n字段说明:\n")
			append("- type: 题型整数标识，例如 1/2/3/4/5；\n")
			append("- body: 题干，List<Element>，Element = {\\\"elementType\\\": Int, \\\"content\\\": String}，elementType=0 文本，=1 图片等；\n")
			append("- options: 可选项，List<OptionItems>，OptionItems = {\\\"element\\\": List<Element>}；\n")
			append("- answer: 正确答案，用 Element 列表表示（与 body 中 Element 结构相同）；\n")
			append("- realAnswer: 可选，{\\\"answer\\\": 字符串} 表示简洁答案，如 A/B/BD，或 null 。\n")
			append("\n\n请解析以下文档内容中的试题:\n")
			append(documentText)
		}.toString()
	}

	/**
	 * 调用 Gemini API，返回解析后的 List<QuestionDetail>
	 * 使用 Gemini 1.5 Flash 模型
	 */
	suspend fun callGeminiApi(apiKey: String, documentText: String): Result<List<QuestionDetail>> = withContext(Dispatchers.IO) {
		try {
			val model = "gemini-1.5-flash"
			val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

			val promptText = buildPrompt(documentText)
			
			// Gemini API 使用 contents 数组格式
			val requestMap = mapOf(
				"contents" to listOf(
					mapOf(
						"parts" to listOf(
							mapOf("text" to promptText)
						)
					)
				),
				"generationConfig" to mapOf(
					"temperature" to 0.1,
					"maxOutputTokens" to 8192
				)
			)
			val requestJson = gson.toJson(requestMap)

			val mediaType = "application/json; charset=utf-8".toMediaType()
			val body = requestJson.toRequestBody(mediaType)

			val request = Request.Builder()
				.url(url)
				.post(body)
				.addHeader("Content-Type", "application/json")
				.build()

			client.newCall(request).execute().use { resp ->
				if (!resp.isSuccessful) {
					val errorBody = resp.body?.string() ?: "Unknown error"
					return@use Result.failure(AiNetworkException("Gemini API error ${resp.code}: $errorBody"))
				}
				val respBody = resp.body?.string() ?: return@use Result.failure(AiResponseParseException("Empty response body"))

				// 从 Gemini 响应中提取文本内容
				val textContent = extractGeminiResponse(respBody)
				if (textContent.isEmpty()) return@use Result.failure(AiResponseParseException("Gemini response content is empty"))

				// 尝试从响应中提取 JSON 数组
				val jsonArray = extractJsonArray(textContent) ?: textContent

				val list = tryParseQuestionList(jsonArray) 
				if (list != null) {
					Result.success(list)
				} else {
					Result.failure(AiResponseParseException("Failed to parse JSON into QuestionDetail list"))
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
			if (e is AiNetworkException || e is AiResponseParseException) {
				Result.failure(e)
			} else {
				Result.failure(AiNetworkException("Gemini network error: ${e.message}", e))
			}
		}
	}

	/**
	 * 调用 Deepseek API，返回解析后的 List<QuestionDetail>
	 * 使用 deepseek-chat 模型
	 */
	suspend fun callDeepseekApi(apiKey: String, documentText: String): Result<List<QuestionDetail>> = withContext(Dispatchers.IO) {
		try {
			val url = "https://api.deepseek.com/chat/completions"
			val promptText = buildPrompt(documentText)
			val requestMap = mapOf(
				"model" to "deepseek-reasoner",
				"messages" to listOf(
					mapOf(
						"role" to "system",
						"content" to "你是一个专业的试题解析助手，请严格按照要求返回 JSON 格式的数据。"
					),
					mapOf(
						"role" to "user",
						"content" to promptText
					)
				),
				"stream" to false,
				"temperature" to 0.1,
				"max_tokens" to 64000
			)
			val requestJson = gson.toJson(requestMap)

			val mediaType = "application/json; charset=utf-8".toMediaType()
			val body = requestJson.toRequestBody(mediaType)

			val request = Request.Builder()
				.url(url)
				.post(body)
				.addHeader("Content-Type", "application/json")
				.addHeader("Authorization", "Bearer $apiKey")
				.build()

			client.newCall(request).execute().use { resp ->
				if (!resp.isSuccessful) {
					val errorBody = resp.body?.string() ?: "Unknown error"
					return@use Result.failure(AiNetworkException("Deepseek API error ${resp.code}: $errorBody"))
				}
				val respBody = resp.body?.string() ?: return@use Result.failure(AiResponseParseException("Empty response body"))

				// 从 Deepseek 响应中提取消息内容
				val textContent = extractDeepseekResponse(respBody)
				if (textContent.isEmpty()) return@use Result.failure(AiResponseParseException("Deepseek response content is empty"))

				// 尝试从响应中提取 JSON 数组
				val jsonArray = extractJsonArray(textContent) ?: textContent

				val list = tryParseQuestionList(jsonArray)
				if (list != null) {
					Result.success(list)
				} else {
					Result.failure(AiResponseParseException("Failed to parse JSON into QuestionDetail list"))
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
			Result.failure(e)
		}
	}

	/**
	 * 从 Gemini API 响应中提取文本内容
	 */
	@Throws
	private fun extractGeminiResponse(responseBody: String): String {
		val jsonObject = gson.fromJson(responseBody, Map::class.java)
		val candidates = jsonObject["candidates"] as? List<*>
		val firstCandidate = candidates?.firstOrNull() as? Map<*, *>
		val content = firstCandidate?.get("content") as? Map<*, *>
		val parts = content?.get("parts") as? List<*>
		val firstPart = parts?.firstOrNull() as? Map<*, *>
		return firstPart?.get("text") as? String ?: ""
	}

	/**
	 * 从 Deepseek API 响应中提取文本内容
	 */
	@Throws
	private fun extractDeepseekResponse(responseBody: String): String {
		val jsonObject = gson.fromJson(responseBody, Map::class.java)
		val choices = jsonObject["choices"] as? List<*>
		val firstChoice = choices?.firstOrNull() as? Map<*, *>
		val message = firstChoice?.get("message") as? Map<*, *>
		return message?.get("content") as? String ?: ""
	}

	@Throws
	private fun extractJsonArray(text: String): String? {
		val start = text.indexOf('[')
		val end = text.lastIndexOf(']')
		return if (start in 0 until end) text.substring(start, end + 1) else null
	}

	@Throws
	private fun tryParseQuestionList(jsonArrayString: String): List<QuestionDetail>? {
		val type = object : TypeToken<List<QuestionDetail>>() {}.type
		return gson.fromJson<List<QuestionDetail>>(jsonArrayString, type)
	}
}

