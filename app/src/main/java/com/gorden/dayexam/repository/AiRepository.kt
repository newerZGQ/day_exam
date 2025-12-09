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
		val template = loadTemplate() // 使用加载的模板
		return StringBuilder().apply {
			append("你是一个专业的试题解析助手。请解析以下文档内容中的试题，并严格按照 JSON 数组格式返回结果。\n\n")
            append("### 数据结构定义\n")
            append("1. **Element**: 试题内容的基本单元\n")
            append("   - `elementType`: 整数 (0: 文本, 1: 图片)\n")
            append("   - `content`: 字符串 (文本内容 或 图片URL)\n")
            append("2. **OptionItems**: 选项容器\n")
            append("   - `element`: List<Element> (选项的具体内容)\n")
            append("3. **Answer**: 答案容器\n")
            append("   - `commonAnswer`: List<Element> (通用的答案描述，适用于填空、问答等)\n")
            append("   - `optionAnswer`: List<Int> (选项索引列表，适用于选择题，从0开始)\n")
            append("   - `tfAnswer`: Boolean (判断题答案，true为正确/对，false为错误/错)\n")
            append("4. **QuestionDetail**: 试题对象\n")
            append("   - `type`: 整数 (1: 填空, 2: 判断, 3: 单选, 4: 多选, 5: 问答)\n")
            append("   - `body`: List<Element> (题干)\n")
            append("   - `options`: List<OptionItems> (选项列表，填空/问答/判断题为空列表)\n")
            append("   - `answer`: Answer (标准答案)\n")
            append("   - `realAnswer`: Answer? (用户作答，解析时请设为 null)\n\n")

            append("### 题型解析规则\n")
            append("- **填空题 (type=1)**: `options` 为空；`answer.commonAnswer` 存放填空答案。\n")
            append("- **判断题 (type=2)**: `options` 为空；`answer.tfAnswer` 存放 true/false。\n")
            append("- **单选题 (type=3)**: `options` 存放选项；`answer.optionAnswer` 存放一个正确选项的索引 (如 [0])。\n")
            append("- **多选题 (type=4)**: `options` 存放选项；`answer.optionAnswer` 存放所有正确选项的索引 (如 [0, 2])。\n")
            append("- **问答题 (type=5)**: `options` 为空；`answer.commonAnswer` 存放参考答案。\n\n")

            append("### 题目格式识别规则\n")
            append("**中文题目格式：**\n")
            append("- 题目编号可能是：1. / 一、/ （1）/ 第1题 等格式\n")
            append("- 选项标记：A. / A、/ (A) / 【A】等\n")
            append("- 答案标记：答案：/ 答：/ 正确答案：等\n\n")
            append("**英文题目格式：**\n")
            append("- 题目编号：Question 1: / Q1: / 1. / 1) 等格式\n")
            append("- 填空标记：__________ / _____ / (blank) / [blank] 等\n")
            append("- 选项标记：A. / A) / (A) 等\n")
            append("- 答案标记：Answer: / Ans: / Key: / Correct Answer: 等\n\n")
            append("**重要：题干提取规则**\n")
            append("- 题干（body）必须包含题目的**完整描述**，不要在遇到填空标记或特殊符号时就截断\n")
            append("- 对于填空题，题干应该包含填空线前后的所有文本内容\n")
            append("- 示例：\"Question 11: The Tokugawa Shogunate in Japan implemented a policy of __________, severely limiting foreign contact and trade for over 200 years.\"\n")
            append("  - 正确的题干应该是完整句子：\"The Tokugawa Shogunate in Japan implemented a policy of __________, severely limiting foreign contact and trade for over 200 years.\"\n")
            append("  - 错误做法：只提取到 \"The Tokugawa Shogunate in Japan implemented a policy of\"\n")
            append("- 题目编号（如 \"Question 11:\"）不应包含在题干中，应该去掉\n")
            append("- 答案标记（如 \"Answer:\"）后面的内容是答案，不是题干的一部分\n\n")

            append("### 示例 JSON\n")
            append("参考以下 JSON 结构：\n")
            append("```json\n")
            append(template)
            append("\n```\n\n")

			append("### 待解析文档内容\n")
			append(documentText)
            append("\n\n### 解析要求\n")
            append("1. **题干完整性**：确保题干包含题目的完整描述，不要在填空标记处截断\n")
            append("2. **选项分离**：严格不要把选项内容放到题干（`body`）中。所有选项文本必须只出现在 `options` 列表中\n")
            append("3. **答案提取**：正确识别 \"Answer:\" 或 \"答案：\" 后面的内容作为答案\n")
            append("4. **题目编号**：去除题目编号（如 \"Question 11:\" 或 \"1.\"），只保留题目内容\n")
            append("5. **填空标记**：保留填空标记（如 __________）在题干中，答案放在 `answer.commonAnswer` 中\n")
            append("6. **格式兼容**：同时支持中文和英文题目格式\n\n")
            append("请直接返回 JSON 数组，无需Markdown标记。")
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
					"maxOutputTokens" to 65536
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
				"max_tokens" to 8000
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

