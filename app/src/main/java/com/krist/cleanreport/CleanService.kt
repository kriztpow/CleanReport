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
    private val targetBlock = "com.scorpio.securitycom"
    private var activeDefense = true 
    private val handler = Handler(Looper.getMainLooper())
    private var server: NettyApplicationEngine? = null

    // BUCLE DE EXTERMINIO ULTRA (Bajamos a 250ms para mayor velocidad)
    private val killerRunnable = object : Runnable {
        override fun run() {
            if (activeDefense) {
                val am = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                am.killBackgroundProcesses(targetBlock)
            }
            handler.postDelayed(this, 250) 
        }
    }

    override fun onServiceConnected() {
        handler.post(killerRunnable)
        iniciarServidor()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkgName = event.packageName?.toString() ?: return
        
        val time = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        if (activityLogs.size > 150) activityLogs.removeAt(0)
        activityLogs.add("[$time] Scan: $pkgName")

        // SI DETECTA AL INTRUSO O CUALQUIER VENTANA DE ÉL
        if (activeDefense && (pkgName == targetBlock || pkgName.contains("scorpio"))) {
            
            // 1. Mandar al Home inmediatamente
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(homeIntent)

            // 2. ATAQUE TRIPLE PARA LIMPIAR SUPERPOSICIÓN
            performGlobalAction(GLOBAL_ACTION_BACK)      // Cerrar diálogos
            performGlobalAction(GLOBAL_ACTION_RECENTS)   // Romper el foco de la ventana flotante
            
            // 3. Forzar refresco de la pantalla (abre y cierra rápido el panel)
            handler.postDelayed({
                performGlobalAction(GLOBAL_ACTION_BACK) 
            }, 100)

            val am = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            am.killBackgroundProcesses(targetBlock)
            
            activityLogs.add("<span style='color:red;'><b>[ANIQUILADO]</b> Overlay removido de: $pkgName</span>")
        }
    }

    private fun iniciarServidor() {
        try {
            server = embeddedServer(Netty, port = 8080) {
                routing {
                    get("/") {
                        val html = "<html><head><meta charset='UTF-8'><meta http-equiv='refresh' content='2'></head><body style='background:#000;color:#0f0;font-family:monospace;padding:20px;'><h1>[ SHIELD ACTIVE ]</h1><div style='border:1px solid #0f0;padding:10px;height:400px;overflow-y:scroll;'>${activityLogs.asReversed().joinToString("<br>")}</div></body></html>"
                        context.respondText(html, ContentType.Text.Html)
                    }
                }
            }.start(wait = false)
        } catch (e: Exception) {}
    }

    override fun onInterrupt() { handler.removeCallbacks(killerRunnable) }
    override fun onDestroy() {
        handler.removeCallbacks(killerRunnable)
        server?.stop(500, 1000)
        super.onDestroy()
    }
}
