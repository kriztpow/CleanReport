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

    private val targetBlock = "com.scorpio.securitycom"
    private val handler = Handler(Looper.getMainLooper())
    private var server: NettyApplicationEngine? = null

    // BUCLE DE FUEGO RÁPIDO (150ms) 
    // Su objetivo es "aturdir" al intruso para que no pueda bloquear tu clic
    private val killerRunnable = object : Runnable {
        override fun run() {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            am.killBackgroundProcesses(targetBlock)
            handler.postDelayed(this, 150) 
        }
    }

    override fun onServiceConnected() {
        handler.post(killerRunnable)
        iniciarServidor()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkgName = event.packageName?.toString() ?: return
        
        // Si detecta que el intruso intenta "tapar" la pantalla de desactivación
        if (pkgName == targetBlock || pkgName.contains("scorpio")) {
            // Mandamos un 'BACK' para quitar cualquier cartel invisible que él ponga
            performGlobalAction(GLOBAL_ACTION_BACK)
            
            val am = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            am.killBackgroundProcesses(targetBlock)
        }
    }

    private fun iniciarServidor() {
        try {
            server = embeddedServer(Netty, port = 8080) {
                routing { get("/") { context.respondText("MODO ASALTO INFINIX ACTIVO", ContentType.Text.Plain) } }
            }.start(wait = false)
        } catch (e: Exception) {}
    }

    override fun onInterrupt() {}
    override fun onDestroy() {
        handler.removeCallbacks(killerRunnable)
        server?.stop(500, 1000)
        super.onDestroy()
    }
}
