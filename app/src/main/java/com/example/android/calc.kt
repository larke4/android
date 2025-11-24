package com.example.android

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.lang.Exception

class Calc : AppCompatActivity() {
    private lateinit var tvResult: TextView
    private var lastNumeric: Boolean = false
    private var isOperatorAdded: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.calc)

        tvResult = findViewById(R.id.tvResult)
        setupButtons()
    }

    private fun setupButtons() {
        val numberAndDotButtons = listOf<Int>(
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9, R.id.btn_dot
        )

        val operatorButtons = listOf<Int>(
            R.id.btn_add, R.id.btn_subtract, R.id.btn_multiply, R.id.btn_divide
        )

        numberAndDotButtons.forEach { id ->
            findViewById<Button>(id).setOnClickListener { onDigit(it) }
        }

        operatorButtons.forEach { id ->
            findViewById<Button>(id).setOnClickListener { onOperator(it) }
        }

        findViewById<Button>(R.id.btn_clear).setOnClickListener { onClear() }
        findViewById<Button>(R.id.btn_equals).setOnClickListener { onEqual() }
    }


    fun onDigit(view: View) {
        val buttonText = (view as Button).text

        if (tvResult.text.toString() == "0" && buttonText != ".") {
            tvResult.text = buttonText
        } else if (buttonText == ".") {
            if (!tvResult.text.contains('.')) {
                tvResult.append(buttonText)
            }
        }
        else {
            tvResult.append(buttonText)
        }
        lastNumeric = true
    }

    fun onOperator(view: View) {
        val operator = (view as Button).text

        if (lastNumeric && !isOperatorAdded) {
            tvResult.append(operator)
            isOperatorAdded = true
            lastNumeric = false
        }
    }

    fun onClear() {
        tvResult.text = "0"
        lastNumeric = false
        isOperatorAdded = false
    }

    fun onEqual() {
        if (lastNumeric && isOperatorAdded) {
            val expression = tvResult.text.toString()

            try {
                val operatorIndex: Int
                val operator: Char

                when {
                    expression.contains('+') -> { operatorIndex = expression.indexOf('+'); operator = '+' }
                    expression.contains('*') -> { operatorIndex = expression.indexOf('*'); operator = '*' }
                    expression.contains('/') -> { operatorIndex = expression.indexOf('/'); operator = '/' }
                    expression.contains('-') -> { operatorIndex = expression.indexOf('-', 1); operator = '-' }
                    else -> return
                }

                val num1String = expression.substring(0, operatorIndex)
                val num2String = expression.substring(operatorIndex + 1)

                val num1 = num1String.toDouble()
                val num2 = num2String.toDouble()
                var result = 0.0

                when (operator) {
                    '+' -> result = num1 + num2
                    '-' -> result = num1 - num2
                    '*' -> result = num1 * num2
                    '/' -> {
                        if (num2 == 0.0) {
                            tvResult.text = "Error: Div by zero"
                            onClear()
                            return
                        }
                        result = num1 / num2
                    }
                }

                tvResult.text = if (result % 1.0 == 0.0) {
                    result.toLong().toString()
                } else {
                    result.toString()
                }

                isOperatorAdded = false
            } catch (e: Exception) {
                tvResult.text = "input Error"
                e.printStackTrace()
            }
        }
    }
}