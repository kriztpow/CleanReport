package com.krist.cleanreport

import android.accessibilityservice.AccessibilityService
import android.content.Intent
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
    private var targetBlock: String? = null
    private var activeDefense = false
    private var server: NettyApplicationEngine? = null

    override fun onServiceConnected() {
        try {
            server = embeddedServer(Netty, port = 8080) {
                routing {
                    get("/") {
                        val status = if(activeDefense) "<b style='color:#2ecc71'>ACTIVA</b>" else "<b style='color:#e74c3c'>INACTIVA</b>"
                        val html = """
                            <html><head>
                                <meta charset="UTF-8">
                                <meta name="viewport" content="width=device-width, initial-scale=1">
                                <style>
                                    body { font-family: sans-serif; padding: 20px; background: #f4f4f9; color: #333; }
                                    .card { background: white; padding: 15px; border-radius: 10px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); margin-bottom: 20px; }
                                    .log-container { 
                                        background: #2c3e50; color: #ecf0f1; padding: 15px; 
                                        border-radius: 5px; height: 300px; overflow-y: scroll; 
                                        font-family: monospace; font-size: 12px; line-height: 1.5;
                                    }
                                    input[type=text] { padding: 8px; width: 250px; border: 1px solid #ddd; border-radius: 4px; }
                                    input[type=submit] { padding: 8px 15px; background: #3498db; color: white; border: none; border-radius: 4px; cursor: pointer; }
                                    button { padding: 8px 15px; background: #95a5a6; color: white; border: none; border-radius: 4px; }
                                </style>
                            </head>
                            <body>
                                <h1>🛡️ CleanReport Shield</h1>
                                <div class="card">
                                    <p>Estado de Defensa: $status</p>
                                    <p>Objetivo: <b>${targetBlock ?: "Ninguno"}</b></p>
                                    <form action='/set'>
                                        <input type='text' name='pkg' placeholder='com.paquete.intruso' required>
                                        <input type='submit' value='INICIAR BLOQUEO'>
                                    </form>
                                    <br>
                                    <a href='/stop'><button>DETENER DEFENSA</button></a>
                                </div>
                                
                                <h3>📋 Historial de Procesos (Scroll)</h3>
                                <div class="log-container">
                                    ${activityLogs.asReversed().joinToString("<br>")}
                                </div>
                                <script>
                                    // Auto-scroll al final al cargar
                                    var log = document.querySelector('.log-container');
                                    log.scrollTop = 0; 
                                </script>
                            </body></html>
                        """.trimIndent()
                        context.respondText(html, ContentType.Text.Html)
                    }
                    get("/set") {
                        targetBlock = context.parameters["pkg"]
                        activeDefense = true
                        context.respondRedirect("/")
                    }
                    get("/stop") {
                        activeDefense = false
                        targetBlock = null
                        context.respondRedirect("/")
                    }
                }
            }.start(wait = false)
        } catch (e: Exception) {
            // Si el puerto está ocupado, no crashea la app
            activityLogs.add("[ERROR] No se pudo iniciar el servidor: ${e.message}")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkgName = event.packageName?.toString() ?: return
        val time = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        
        // Mantener los últimos 200 procesos
        if (activityLogs.size > 200) activityLogs.removeAt(0)
        
        val logEntry = "[$time] $pkgName"
        if (activityLogs.isEmpty() || activityLogs.last() != logEntry) {
            activityLogs.add(logEntry)
        }

        if (activeDefense && pkgName == targetBlock) {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            activityLogs.add("<span style='color:#e74c3c;'><b>[SISTEMA]</b> Intruso expulsado: $pkgName</span>")
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        server?.stop(1000, 2000)
        super.onDestroy()
    }
}
