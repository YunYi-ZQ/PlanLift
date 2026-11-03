package app.planlift.fit.data.sync

import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 最小 WebDAV 实现：依赖只有 PROPFIND / MKCOL / GET / PUT / MOVE / DELETE。
 * 认证用 Basic（坚果云、Nextcloud、大部分 NAS 都支持）。
 */
class WebDavClient(
    baseUrl: String,
    private val user: String,
    private val password: String
) {
    private val base: String = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private fun auth(): String = Credentials.basic(user, password)

    private fun url(path: String): String =
        base + path.trimStart('/')

    /** 返回 null 表示成功，否则返回失败原因（便于在设置页直接展示） */
    fun ping(): String? = try {
        val req = Request.Builder()
            .url(url(""))
            .method("PROPFIND", null)
            .header("Depth", "0")
            .header("Authorization", auth())
            .build()
        http.newCall(req).execute().use { resp ->
            if (resp.code in 200..299 || resp.code == 207) null
            else "HTTP ${resp.code} ${resp.message}"
        }
    } catch (e: Exception) {
        e.javaClass.simpleName + ": " + (e.message ?: "")
    }

    /** 逐级创建目录；已存在（405/301）也算成功 */
    fun ensureDir(path: String): Boolean {
        val parts = path.trim('/').split('/')
        var current = ""
        for (p in parts) {
            current = if (current.isEmpty()) p else "$current/$p"
            val req = Request.Builder()
                .url(url("$current/"))
                .method("MKCOL", null)
                .header("Authorization", auth())
                .build()
            try {
                http.newCall(req).execute().use { resp ->
                    if (resp.code !in 200..299 && resp.code != 405 && resp.code != 301) {
                        // 405 = 已存在
                        if (resp.code != 409) return false
                    }
                }
            } catch (e: Exception) {
                return false
            }
        }
        return true
    }

    fun get(path: String): ByteArray? = try {
        val req = Request.Builder()
            .url(url(path))
            .get()
            .header("Authorization", auth())
            .build()
        http.newCall(req).execute().use { resp ->
            if (resp.code == 404) null
            else if (resp.code in 200..299) resp.body?.bytes()
            else null
        }
    } catch (e: Exception) {
        null
    }

    fun put(path: String, bytes: ByteArray, contentType: String = "application/json"): Boolean = try {
        val req = Request.Builder()
            .url(url(path))
            .put(bytes.toRequestBody(contentType.toMediaType()))
            .header("Authorization", auth())
            .build()
        http.newCall(req).execute().use { it.code in 200..299 }
    } catch (e: Exception) {
        false
    }

    fun delete(path: String): Boolean = try {
        val req = Request.Builder()
            .url(url(path))
            .delete()
            .header("Authorization", auth())
            .build()
        http.newCall(req).execute().use { it.code in 200..299 || it.code == 404 }
    } catch (e: Exception) {
        false
    }

    fun move(from: String, to: String): Boolean = try {
        val req = Request.Builder()
            .url(url(from))
            .method("MOVE", null)
            .header("Authorization", auth())
            .header("Destination", url(to))
            .header("Overwrite", "T")
            .build()
        http.newCall(req).execute().use { it.code in 200..299 }
    } catch (e: Exception) {
        false
    }
}
