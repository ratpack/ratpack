package ratpack.kotlin

import okhttp3.*
import java.net.URL

class HttpClient (val base: URL = URL ("http://localhost:5050")) {
    val JSON = MediaType.parse("application/json; charset=utf-8")

    internal val client = OkHttpClient()

    private fun buildUrl(url: String) = Request.Builder().url(base.toString() + url)

    fun get(url: String = ""): Response {
        val request = buildUrl(url).get().build()
        return http(request)
    }

    fun delete(url: String = ""): Response {
        val request = buildUrl(url).delete().build()
        return http(request)
    }

    fun post(url: String = "", json: String): Response {
        val body = RequestBody.create(JSON, json)
        val request = buildUrl(url).post(body).build()
        return http(request)
    }

    fun postForm(url: String = "", vararg pair: Pair<String, String>): Response {
        val body = FormBody.Builder()
        pair.forEach { body.add(it.first, it.second) }
        val request = buildUrl(url).post(body.build()).build()
        return http(request)
    }

    fun put(url: String = "", json: String): Response {
        val body = RequestBody.create(JSON, json)
        val request = buildUrl(url).put(body).build()
        return http(request)
    }

    fun options(url: String = ""): Response {
        val request = buildUrl(url).method("OPTIONS", null).build()
        return http(request)
    }

    fun patch(url: String = "", json: String): Response {
        val body = RequestBody.create(JSON, json)
        val request = buildUrl(url).patch(body).build()
        return http(request)
    }

    fun getBody(url: String = ""): String? = get(url).body().string()
    fun deleteBody(url: String = ""): String? = delete(url).body().string()
    fun postBody(url: String = "", json: String): String? = post(url, json).body().string()
    fun putBody(url: String = "", json: String): String? = put(url, json).body().string()
    fun optionsBody(url: String = ""): String? = options(url).body().string()
    fun patchBody(url: String = "", json: String): String? = patch(url, json).body().string()

    fun http(request: Request) = client.newCall(request).execute()
}
