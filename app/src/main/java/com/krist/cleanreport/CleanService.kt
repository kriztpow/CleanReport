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

    override fun onServiceConnected() {
        iniciarServidor()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkgName = event.packageName?.toString() ?: return
        
        // Log de lo que está pasando
        val time = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        if (activityLogs.size > 100) activityLogs.removeAt(0)
        activityLogs.add("[$time] Scan: $pkgName")

        // SI EL INTRUSO INTENTA INTERRUMPIR O MOSTRAR SU OVERLAY
        if (activeDefense && (pkgName == targetBlock || pkgName.contains("scorpio"))) {
            
            // 1. Abrir Administradores de Dispositivo (Ruta genérica para mayor compatibilidad)
            val intent = Intent().apply {
                action = "android.settings.DEVICE_ADMIN_SETTINGS"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                // Si falla, intentamos la ruta de seguridad
                val intentFallback = Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
                intentFallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intentFallback)
            }

            // 2. Cerrar el panel de notificaciones (Por si el intruso se auto-habilita ahí)
            performGlobalAction(GLOBAL_ACTION_BACK)
            
            // 3. Matar el proceso para ganar unos segundos de fluidez
            val am = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            am.killBackgroundProcesses(targetBlock)
            
            activityLogs.add("<span style='color:red;'><b>[DEFENSA]</b> Bloqueando persistencia de $pkgName</span>")
        }
    }

    private fun iniciarServidor() {
        try {
            server = embeddedServer(Netty, port = 8080) {
                routing {
                    get("/") {
                        val html = "<html><body style='background:#000;color:#0f0;font-family:monospace;padding:20px;'><h1>[ ADMIN UNLOCK MODE ]</h1><div style='border:1px solid #0f0;padding:10px;height:400px;overflow-y:scroll;'>${activityLogs.asReversed().joinToString("<br>")}</div></body></html>"
                        context.respondText(html, ContentType.Text.Html)
                    }
                }
            }.start(wait = false)
        } catch (e: Exception) {}
    }

    override fun onInterrupt() {}
    override fun onDestroy() {
        server?.stop(500, 1000)
        super.onDestroy()
    }
}
