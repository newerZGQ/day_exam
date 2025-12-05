package com.gorden.dayexam.repository

import com.gorden.dayexam.repository.model.QuestionDetail
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object AiRepository {
	private val client = OkHttpClient()
	private val gson = Gson()

	// 可单独编辑的 prompt 变量（用于 Gemini）
	// 指示模型输出与项目中 `QuestionDetail` 数据类完全匹配的 JSON。
	// QuestionDetail 字段说明：
	// - type: 整数 (例如 1 表示单选)
	// - body: 数组，元素为 Element ({"elementType":0,"content":"..."})
	// - options: 数组，每项为 OptionItems {"element": [ Element, ... ]}
	// - answer: 数组，包含正确答案表示为 Element 列表（与 body 相同结构）
	// - realAnswer: 可选对象 {"answer": "A"} 或 null
	// 请严格使用这些字段名并只返回 JSON 数组（不要返回多余的说明文字）。示例只需 3 个 items 即可。
	private val geminiPrompt = """
		Output a JSON array of objects that exactly match the following Kotlin data structure:
		QuestionDetail {
		  type: Int,
		  body: List<Element> where Element = {"elementType": Int, "content": String},
		  options: List<OptionItems> where OptionItems = {"element": List<Element>},
		  answer: List<Element>,
		  realAnswer: {"answer": String} | null
		}
		Use the exact field names: "type", "body", "options", "answer", "realAnswer".
		Return only the JSON array (no surrounding text). Provide 3 example items.
	""".trimIndent().replace("\n", " ")

	// Deepseek 使用的 prompt（可按需修改），与 Gemini prompt 保持一致的结构要求
	private val deepseekPrompt = """
		Output a JSON array of objects that exactly match the Kotlin data structure QuestionDetail:
		- type: Int
		- body: List of Element objects like {"elementType": Int, "content": String}
		- options: List of OptionItems like {"element": [ Element, ... ]}
		- answer: List of Element objects (the correct answer as Elements)
		- realAnswer: either {"answer": "..."} or null
		Use only the field names: "type","body","options","answer","realAnswer" and return only the JSON array.
		Provide 3 example items.
	""".trimIndent().replace("\n", " ")
    // 单一可编辑的 prompt 变量（用于 Gemini 与 Deepseek）
    // 指示模型输出与项目中 `QuestionDetail` 数据类完全匹配的 JSON。
    // QuestionDetail 字段说明：
    // - type: 整数
    // - body: 数组，元素为 Element ({"elementType":Int,"content":String})
    // - options: 数组，每项为 OptionItems {"element": [ Element, ... ]}
    // - answer: 数组，包含正确答案表示为 Element 列表
    // - realAnswer: 可选对象 {"answer": String} 或 null
    // 请严格使用这些字段名并只返回 JSON 数组（不要返回多余的说明文字）。示例只需 3 个 items 即可。
    private val questionDetailPrompt = """
        Output a JSON array of objects that exactly match the Kotlin data structure QuestionDetail:
        - type: Int
        - body: List of Element objects like {"elementType": Int, "content": String}
        - options: List of OptionItems like {"element": [ Element, ... ]}
        - answer: List of Element objects
        - realAnswer: either {"answer": "..."} or null
        Use only the field names: "type","body","options","answer","realAnswer" and return only the JSON array.
        Provide 3 example items.
    """.trimIndent().replace("\n", " ")

	/**
	 * 调用 Gemini / Generative Language REST API，返回解析后的 List<QuestionDetail>
	 */
	suspend fun callGeminiApi(apiKey: String): List<QuestionDetail> = withContext(Dispatchers.IO) {
		val url = "https://generativelanguage.googleapis.com/v1beta2/models/text-bison-001:generate?key=$apiKey"

		val requestJson = """{"prompt":{"text":"$questionDetailPrompt"},"temperature":0.0,"max_output_tokens":60000}"""

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
		val requestJson = """{"api_key":"$apiKey","prompt":"$questionDetailPrompt","max_tokens":60000}"""

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

