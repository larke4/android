package com.example.android

import android.content.Context
import android.util.Log
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.NetworkInterface

class SocketManager(private val context: Context) {

    companion object {
        private const val TAG = "SocketManager"
        const val INTERNAL_PORT = 5555
        const val EXTERNAL_PORT = 5556
    }

    private var serverJob: Job? = null
    private var clientJob: Job? = null


    fun getLocalIpAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addresses = intf.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr.hostAddress.indexOf(':') == -1) {
                        return addr.hostAddress
                    }
                }
            }
            "127.0.0.1"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting IP: ${e.message}")
            "127.0.0.1"
        }
    }


    fun startInternalServer(onMessageReceived: (String) -> Unit = {}) {
        serverJob = CoroutineScope(Dispatchers.IO).launch {
            val context = ZContext()
            val socket = context.createSocket(SocketType.REP)

            try {
                socket.bind("tcp://*:$INTERNAL_PORT")
                Log.d(TAG, "Internal server started on port $INTERNAL_PORT")

                while (isActive) {
                    val message = socket.recvStr()
                    Log.d(TAG, "Server received: $message")

                    withContext(Dispatchers.Main) {
                        onMessageReceived(message)
                    }


                    val response = "Server response to: $message"
                    socket.send(response)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server error: ${e.message}")
            } finally {
                socket.close()
                context.close()
                Log.d(TAG, "Internal server stopped")
            }
        }
    }


    fun sendToInternalServer(message: String, onResponse: (String) -> Unit = {}) {
        clientJob = CoroutineScope(Dispatchers.IO).launch {
            val context = ZContext()
            val socket = context.createSocket(SocketType.REQ)

            try {
                socket.connect("tcp://localhost:$INTERNAL_PORT")
                socket.send(message)

                val response = socket.recvStr()
                Log.d(TAG, "Client received response: $response")

                withContext(Dispatchers.Main) {
                    onResponse(response)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Client error: ${e.message}")
                withContext(Dispatchers.Main) {
                    onResponse("Error: ${e.message}")
                }
            } finally {
                socket.close()
                context.close()
            }
        }
    }


    fun sendToExternalServer(
        serverIp: String,
        message: String,
        onResponse: (String) -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val context = ZContext()
            val socket = context.createSocket(SocketType.REQ)

            try {

                socket.connect("tcp://$serverIp:$EXTERNAL_PORT")

                socket.send(message)
                Log.d(TAG, "Sent to external server ($serverIp): $message")

                val response = socket.recvStr()
                Log.d(TAG, "External server response: $response")

                withContext(Dispatchers.Main) {
                    onResponse(response)
                }
            } catch (e: Exception) {
                Log.e(TAG, "External connection error: ${e.message}")
                withContext(Dispatchers.Main) {
                    onResponse("Connection failed: ${e.message}")
                }
            } finally {
                socket.close()
                context.close()
            }
        }
    }

    fun stopServer() {
        serverJob?.cancel()
        Log.d(TAG, "Server stopped")
    }
}