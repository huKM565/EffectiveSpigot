package ru.hukm.effectiveSpigot.http

import ru.hukm.effectiveSpigot.Config
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.interfaces.IModule
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import com.sun.net.httpserver.HttpServer as JdkHttpServer

object EffectiveHttpServer {
    private var server: JdkHttpServer? = null

    fun start(port: Int) {
        stop()
        try {
            server = JdkHttpServer.create(InetSocketAddress(port), 0).also {
                it.executor = Executors.newFixedThreadPool(10)
                it.start()
            }
        } catch (e: Exception) {
            EffectiveSpigot.instance.logger.severe(Locale.getMessage("errors.http_server.bind_failed", port))
            throw e
        }
    }

    fun serve(path: String, bytes: ByteArray) {
        val s = server ?: error(Locale.getMessage("errors.http_server.not_started"))
        s.createContext(path) { exchange ->
            exchange.responseHeaders.add("Content-Type", "application/octet-stream")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
    }

    fun stop() {
        server?.stop(0)
        server = null
    }

    internal fun getModule(): IModule {
        return object : IModule {
            override fun init() {
                if (!Config.isResourcepackHttpServerEnabled()) return
                start(Config.getResourcepackHttpServerPort())
            }
        }
    }
}
