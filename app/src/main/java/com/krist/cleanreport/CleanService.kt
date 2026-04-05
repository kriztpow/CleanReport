package com.krist.cleanreport

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.http.*
import java.util.*

class CleanService : AccessibilityService() {

    private val activityLogs = mutableListOf<String>()
    private var targetBlock: String? = null
    private var activeDefense = false

    override fun onServiceConnected() {
        // Servidor Ktor en puerto 8080
        embeddedServer(Netty, port = 8080) {
            routing {
                get("/") {
                    val status = if(activeDefense) "<b style='color:green'>ACTIVA</b>" else "<b style='color:red'>INACTIVA</b>"
                    val html = """
                        <html><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"></head>
                        <body style='font-family:sans-serif; padding:20px;'>
                            <h1>CleanReport Control Center</h1>
                            <p>Defensa actual: $status</p>
                            <p>Objetivo actual: <b>$targetBlock</b></p>
                            <hr>
                            <form action='/set'>
                                Paquete a expulsar: <input type='text' name='pkg' placeholder='com.ejemplo.app'>
                                <input type='submit' value='ACTIVAR ATAQUE'>
                            </form>
                            <a href='/stop'><button>DETENER TODO</button></a>
                            <h3>Historial de Procesos:</h3>
                            <div style='background:#eee; padding:10px; border-radius:5px;'>
                                ${activityLogs.asReversed().joinToString("<br>")}
                            </div>
                        </body></html>
                    """.trimIndent()
                    call.respondText(html, ContentType.Text.Html)
                }
                get("/set") {
                    targetBlock = call.parameters["pkg"]
                    activeDefense = true
                    call.respondRedirect("/")
                }
                get("/stop") {
                    activeDefense = false
                    targetBlock = null
                    call.respondRedirect("/")
                }
            }
        }.start(wait = false)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkgName = event.packageName?.toString() ?: return
        
        // Registrar para el log remoto
        val time = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        if (activityLogs.size > 100) activityLogs.removeAt(0)
        activityLogs.add("[$time] $pkgName")

        // DEFENSA AGRESIVA: Si el paquete coincide, lo mandamos al Home inmediatamente
        if (activeDefense && pkgName == targetBlock) {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            activityLogs.add("<span style='color:red;'><b>[SISTEMA]</b> Expulsión realizada con éxito.</span>")
        }
    }

    override fun onInterrupt() {}
}
