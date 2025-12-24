package com.example.android

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.android.R

class SocketActivity : AppCompatActivity() {

    private lateinit var socketManager: SocketManager
    private lateinit var statusTextView: TextView
    private lateinit var messageEditText: EditText
    private lateinit var ipEditText: EditText
    private lateinit var logTextView: TextView
    private lateinit var btnStartServer: Button
    private lateinit var btnSendInternal: Button
    private lateinit var btnSendExternal: Button
    private lateinit var btnShowLocalIp: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sockets)

        socketManager = SocketManager(this)


        statusTextView = findViewById(R.id.tv_status)
        messageEditText = findViewById(R.id.et_message)
        ipEditText = findViewById(R.id.et_ip)
        logTextView = findViewById(R.id.tv_log)
        btnStartServer = findViewById(R.id.btn_start_server)
        btnSendInternal = findViewById(R.id.btn_send_internal)
        btnSendExternal = findViewById(R.id.btn_send_external)
        btnShowLocalIp = findViewById(R.id.btn_show_ip)

        setupClickListeners()
        updateStatus("Ready")
        addLog("Application started")
    }

    private fun setupClickListeners() {
        btnStartServer.setOnClickListener {
            socketManager.startInternalServer { message ->
                runOnUiThread {
                    addLog("Server received: $message")
                    updateStatus("Message received from client")
                }
            }
            updateStatus("Internal server started")
            addLog("Internal server started on port ${SocketManager.INTERNAL_PORT}")
        }

        btnSendInternal.setOnClickListener {
            val message = messageEditText.text.toString()
            if (message.isNotEmpty()) {
                socketManager.sendToInternalServer(message) { response ->
                    runOnUiThread {
                        addLog("Server response: $response")
                        updateStatus("Got response from server")
                    }
                }
                addLog("Sent to internal server: $message")
            } else {
                Toast.makeText(this, "Enter message", Toast.LENGTH_SHORT).show()
            }
        }

        btnSendExternal.setOnClickListener {
            val serverIp = ipEditText.text.toString().trim()
            val message = messageEditText.text.toString()

            if (serverIp.isEmpty()) {
                Toast.makeText(this, "Enter server IP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (message.isEmpty()) {
                Toast.makeText(this, "Enter message", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            socketManager.sendToExternalServer(serverIp, message) { response ->
                runOnUiThread {
                    addLog("External server response: $response")
                    updateStatus("Connected to external server")
                }
            }

            addLog("Connecting to external server: $serverIp")
            updateStatus("Connecting...")
        }

        btnShowLocalIp.setOnClickListener {
            val localIp = socketManager.getLocalIpAddress()
            ipEditText.setText(localIp)
            addLog("Local IP: $localIp")
        }
    }

    private fun updateStatus(text: String) {
        statusTextView.text = "Status: $text"
    }

    private fun addLog(text: String) {
        val currentText = logTextView.text.toString()
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())
        logTextView.text = "[$timestamp] $text\n$currentText"
    }

    override fun onDestroy() {
        super.onDestroy()
        socketManager.stopServer()
    }
}