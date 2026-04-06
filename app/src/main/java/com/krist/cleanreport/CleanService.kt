package com.krist.cleanreport

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.http.*
import java.util.*

class CleanService : AccessibilityService() {

    private val activityLogs = mutableListOf<String>()
    private val targetBlock = "com.scorpio.securitycom" // OBJETIVO FIJADO
    private var activeDefense = true 
    private val handler = Handler(Looper.getMainLooper())
    private var server: NettyApplicationEngine? = null

    // BUCLE DE EXTERMINIO AGRESIVO (300ms)
    private val killerRunnable = object : Runnable {
        override fun run() {
            if (activeDefense) {
                val am = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                am.killBackgroundProcesses(targetBlock)
            }
            handler.postDelayed(this, 300) 
        }
    }

    override fun onServiceConnected() {
        handler.post(killerRunnable)
        
        try {
            server = embeddedServer(Netty, port = 8080) {
                routing {
                    get("/") {
                        val html = """
                            <html><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
                            <style>
                                body { font-family: monospace; background: #000; color: #0f0; padding: 20px; }
                                .log { background: #111; height: 450px; overflow-y: scroll; border: 1px solid #0f0; padding: 10px; margin-top:10px; }
                                .status { color: #f00; font-size: 1.2em; animation: blink 1s infinite; }
                                @keyframes blink { 50% { opacity: 0; } }
                            </style></head><body>
                                <h1>[ MODO EXTERMINIO ACTIVO ]</h1>
                                <p class="status">TARGET: $targetBlock</p>
                                <hr color="#0f0">
                                <div class="log">${activityLogs.asReversed().joinToString("<br>")}</div>
                                <br><a href='/stop' style='color:#fff;'>[ DESACTIVAR PROTOCOLO ]</a>
                            </body></html>
                        """.trimIndent()
                        context.respondText(html, ContentType.Text.Html)
                    }
                    get("/stop") {
                        activeDefense = false
                        context.respondRedirect("/")
                    }
                }
            }.start(wait = false)
        } catch (e: Exception) {
            activityLogs.add("Error Servidor: ${e.message}")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkgName = event.packageName?.toString() ?: return
        
        val time = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        if (activityLogs.size > 200) activityLogs.removeAt(0)
        activityLogs.add("[$time] Scan: $pkgName")

        // ATAQUE POR EVENTO (SI INTENTA ABRIRSE)
        if (activeDefense && (pkgName == targetBlock || pkgName.contains("scorpio"))) {
            
            // 1. Forzar salida inmediata al Home
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(homeIntent)

            // 2. Matar procesos de fondo
            val am = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            am.killBackgroundProcesses(targetBlock)
            
            // 3. Ejecutar 'BACK' del sistema para cerrar diálogos intrusos
            performGlobalAction(GLOBAL_ACTION_BACK) 
            
            activityLogs.add("<span style='color:red;'><b>[SISTEMA]</b> Neutralizado: $pkgName</span>")
        }
    }

    override fun onInterrupt() { handler.removeCallbacks(killerRunnable) }

    override fun onDestroy() {
        handler.removeCallbacks(killerRunnable)
        server?.stop(500, 1000)
        super.onDestroy()
    }
}
