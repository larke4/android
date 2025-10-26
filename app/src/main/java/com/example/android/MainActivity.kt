package com.example.android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var display: TextView
    private var text = ""
    private var lastOp = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        display = findViewById(R.id.tvDisplay)
        setupButtons()
    }

    private fun setupButtons() {

        for (i in 0..9) {
            val btnId = resources.getIdentifier("btn$i", "id", packageName)
            findViewById<Button>(btnId).setOnClickListener {
                text = if (text == "0") i.toString() else text + i
                updateDisplay()
                lastOp = false
            }
        }


        findViewById<Button>(R.id.btnAdd).setOnClickListener { addOp("+") }
        findViewById<Button>(R.id.btnSubtract).setOnClickListener { addOp("-") }
        findViewById<Button>(R.id.btnMultiply).setOnClickListener { addOp("*") }
        findViewById<Button>(R.id.btnDivide).setOnClickListener { addOp("/") }

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            text = ""
            updateDisplay()
            lastOp = false
        }

        findViewById<Button>(R.id.btnEquals).setOnClickListener { calculate() }
    }

    private fun addOp(operator: String) {
        if (text.isEmpty()) return

        text = if (lastOp) text.dropLast(1) + operator else text + operator
        updateDisplay()
        lastOp = true
    }

    private fun updateDisplay() {
        display.text = if (text.isEmpty()) "0" else text
    }

    private fun calculate() {
        if (text.isEmpty()) return

        try {
            var expr = text
            if (lastOp) expr = expr.dropLast(1)

            val result = calc(expr)

            text = if (result % 1 == 0.0) result.toInt().toString() else result.toString()
            updateDisplay()
            lastOp = false

        } catch (e: Exception) {
            display.text = "Ошибка"
            text = ""
            lastOp = false
        }
    }

    private fun calc(expr: String): Double {
        val op = findOp(expr)
        if (op == ' ') return expr.toDouble()

        val parts = expr.split(op)
        if (parts.size != 2) throw Exception("Ошибка")

        val a = parts[0].toDouble()
        val b = parts[1].toDouble()

        return when (op) {
            '+' -> a + b
            '-' -> a - b
            '*' -> a * b
            '/' -> {
                if (b == 0.0) throw Exception("На ноль делить нельзя")
                a / b
            }
            else -> throw Exception("Ошибка")
        }
    }

    private fun findOp(s: String): Char {
        for (c in s) {
            if (c in "+-*/") return c
        }
        return ' '
    }
}