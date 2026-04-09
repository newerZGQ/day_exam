package com.gorden.dayexam.repository

import android.util.Log
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
	private const val TAG = "AiRepository"
	private const val COMPACT_PROMPT = """
你是试题解析助手。请把文档内容解析为 JSON 数组，且只能返回 JSON。

每个题目对象结构：
{
  "type": 1|2|3|4|5,
  "body": [{"elementType":0,"content":"题干文本"}],
  "options": [{"element":[{"elementType":0,"content":"选项文本"}]}],
  "answer": {
    "commonAnswer": [{"elementType":0,"content":"答案文本"}],
    "optionAnswer": [0,1],
    "tfAnswer": true
  },
  "realAnswer": null
}

规则：
1. type: 1填空 2判断 3单选 4多选 5问答。
2. body 必须保留完整题干，但不要包含题号、答案标记、选项文本。
3. 选择题的 options 只放选项；答案放 answer.optionAnswer，索引从 0 开始。
4. 判断题答案放 answer.tfAnswer；填空和问答答案放 answer.commonAnswer。
5. 无法确定的题不要编造；解析不到任何题时返回 []。
"""

	private val gson = Gson()
	private val client = OkHttpClient.Builder()
		.callTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
		.connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
		.readTimeout(240, java.util.concurrent.TimeUnit.SECONDS)
		.writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
		.build()

	private fun buildPrompt(documentText: String): String {
		val compactPrompt = COMPACT_PROMPT.trimIndent()
		return StringBuilder().apply {
			append(compactPrompt)
			append("\n\n文档内容：\n")
			append(documentText)
		}.toString()
	}

	/**
	 * 调用 Gemini API，返回解析后的 List<QuestionDetail>
	 * 使用 Gemini 1.5 Flash 模型
	 */
	suspend fun callGeminiApi(apiKey: String, documentText: String): Result<List<QuestionDetail>> = withContext(Dispatchers.IO) {
		try {
			val model = "gemini-2.5-flash-lite"
			val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

			val promptText = buildPrompt(documentText)
			Log.d(TAG, "gemini start promptChars=${promptText.length}")
			
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
				Log.d(TAG, "gemini response code=${resp.code}")
				if (!resp.isSuccessful) {
					val errorBody = resp.body?.string() ?: "Unknown error"
					Log.e(TAG, "gemini failed code=${resp.code} bodyLength=${errorBody.length}")
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
					Log.d(TAG, "gemini parsed questions=${list.size}")
					Result.success(list)
				} else {
					Result.failure(AiResponseParseException("Failed to parse JSON into QuestionDetail list"))
				}
			}
		} catch (e: Exception) {
			Log.e(TAG, "gemini exception", e)
			Result.failure(e)
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
			Log.d(TAG, "deepseek start promptChars=${promptText.length}")
			val requestMap = mapOf(
				"model" to "deepseek-chat",
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
				"max_tokens" to 4000
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
				Log.d(TAG, "deepseek response code=${resp.code}")
				if (!resp.isSuccessful) {
					val errorBody = resp.body?.string() ?: "Unknown error"
					Log.e(TAG, "deepseek failed code=${resp.code} bodyLength=${errorBody.length}")
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
					Log.d(TAG, "deepseek parsed questions=${list.size}")
					Result.success(list)
				} else {
					Result.failure(AiResponseParseException("Failed to parse JSON into QuestionDetail list"))
				}
			}
		} catch (e: Exception) {
			Log.e(TAG, "deepseek exception", e)
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
