package com.gorden.dayexam.repository

import com.gorden.dayexam.ContextHolder
import com.gorden.dayexam.repository.model.QuestionDetail
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
	// prompt 从 assets 加载以便运行时编辑；提供回退模板
	private const val PROMPT_ASSET_FILE = "question_template.json"

	private val fallbackTemplate = """
		[
		  {"type":1,"body":[{"elementType":0,"content":"示例题干"}],"options":[],"answer":[{"elementType":0,"content":"示例答案"}],"realAnswer":{"answer":"A"}}
		]
	""".trimIndent().replace("\n", " ")

	private fun loadTemplate(): String {
		return try {
			ContextHolder.application.assets.open(PROMPT_ASSET_FILE).use { it.readBytes().toString(Charsets.UTF_8) }
		} catch (e: Exception) {
			e.printStackTrace()
			fallbackTemplate
		}
	}

	private fun buildPrompt(): String {
		val template = loadTemplate()
		return StringBuilder().apply {
			append("请严格返回与下面示例结构一致的 JSON 数组，仅返回 JSON 数组（不要返回任何说明文字）。示例模板：\n")
			append(template)
			append("\n\n字段说明：\n")
			append("- type: 题型整数标识，例如 1/2/3/4/5；\n")
			append("- body: 题干，List<Element>，Element = {\\\"elementType\\\": Int, \\\"content\\\": String}，elementType=0 文本，=1 图片等；\n")
			append("- options: 可选项，List<OptionItems>，OptionItems = {\\\"element\\\": List<Element>}；\n")
			append("- answer: 正确答案，用 Element 列表表示（与 body 中 Element 结构相同）；\n")
			append("- realAnswer: 可选，{\\\"answer\\\": 字符串} 表示简洁答案，如 A/B/BD，或 null 。\n")
		}.toString()
	}
							{"element":[{"elementType":0,"content":"A. 4"}]},
							{"element":[{"elementType":0,"content":"B. 7"}]},
							{"element":[{"elementType":0,"content":"C. 9"}]}
						],
						"answer": [{"elementType":0,"content":"B. 7"}],
						"realAnswer": {"answer":"B"}
					}
				]
		""".trimIndent().replace("\n", " ")

	/**
	 * 调用 Gemini / Generative Language REST API，返回解析后的 List<QuestionDetail>
	 */
	suspend fun callGeminiApi(apiKey: String): List<QuestionDetail> = withContext(Dispatchers.IO) {
		val url = "https://generativelanguage.googleapis.com/v1beta2/models/text-bison-001:generate?key=$apiKey"

		val promptText = buildPrompt()
		val requestMap = mapOf(
			"prompt" to mapOf("text" to promptText),
			"temperature" to 0.0,
			"max_output_tokens" to 60000
		)
		val requestJson = gson.toJson(requestMap)

		val mediaType = "application/json; charset=utf-8".toMediaType()
		val body = requestJson.toRequestBody(mediaType)

		val request = Request.Builder()
			.url(url)
			.post(body)
			.addHeader("Content-Type", "application/json")
			.build()

		try {
			client.newCall(request).execute().use { resp ->
				if (!resp.isSuccessful) return@withContext emptyList()
				val respBody = resp.body?.string() ?: return@withContext emptyList()

				// 尝试从响应中提取 JSON 数组（如果响应包含额外文本）
				val jsonArray = extractJsonArray(respBody) ?: respBody

				return@withContext tryParseQuestionList(jsonArray)
					?: emptyList()
			}
		} catch (e: IOException) {
			e.printStackTrace()
			return@withContext emptyList()
		}
	}

	/**
	 * 调用 Deepseek API 的示例实现，返回解析后的 List<QuestionDetail>
	 * 注意：请根据实际 Deepseek API 的文档调整 `url` 与请求体格式/认证方式。
	 */
	suspend fun callDeepseekApi(apiKey: String): List<QuestionDetail> = withContext(Dispatchers.IO) {
		// 示例 URL：请替换为真实的 Deepseek API endpoint
		val url = "https://api.deepseek.ai/v1/generate"

		// Deepseek 请求体示例（很多 deepseek 风格的 API 可能使用 api_key 或 Authorization header）
		val promptText = buildPrompt()
		val requestMap = mapOf(
			"api_key" to apiKey,
			"prompt" to promptText,
			"max_tokens" to 60000
		)
		val requestJson = gson.toJson(requestMap)

		val mediaType = "application/json; charset=utf-8".toMediaType()
		val body = requestJson.toRequestBody(mediaType)

		val request = Request.Builder()
			.url(url)
			.post(body)
			.addHeader("Content-Type", "application/json")
			.build()

		try {
			client.newCall(request).execute().use { resp ->
				if (!resp.isSuccessful) return@withContext emptyList()
				val respBody = resp.body?.string() ?: return@withContext emptyList()

				val jsonArray = extractJsonArray(respBody) ?: respBody
				return@withContext tryParseQuestionList(jsonArray) ?: emptyList()
			}
		} catch (e: IOException) {
			e.printStackTrace()
			return@withContext emptyList()
		}
	}

	private fun extractJsonArray(text: String): String? {
		val start = text.indexOf('[')
		val end = text.lastIndexOf(']')
		return if (start >= 0 && end > start) text.substring(start, end + 1) else null
	}

	private fun tryParseQuestionList(jsonArrayString: String): List<QuestionDetail>? {
		return try {
			val type = object : TypeToken<List<QuestionDetail>>() {}.type
			gson.fromJson<List<QuestionDetail>>(jsonArrayString, type)
		} catch (e: Exception) {
			e.printStackTrace()
			null
		}
	}
}

