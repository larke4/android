package com.example.android

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import java.lang.Exception
import kotlin.random.Random

class Calc : AppCompatActivity() {
    private lateinit var tvResult: TextView
    private var lastNumeric: Boolean = false
    private var isOperatorAdded: Boolean = false

    private lateinit var toggleMode1: ToggleButton
    private lateinit var toggleMode2: ToggleButton
    private lateinit var toggleMode3: ToggleButton

    private val numberAndDotButtons = listOf<Int>(
        R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
        R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9, R.id.btn_dot
    )

    private val operatorButtons = listOf<Int>(
        R.id.btn_add, R.id.btn_subtract, R.id.btn_multiply, R.id.btn_divide
    )

    private val functionButtons = listOf<Int>(
        R.id.btn_clear, R.id.btn_equals
    )

    private val allButtons by lazy {
        numberAndDotButtons + operatorButtons + functionButtons
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.calc)

        tvResult = findViewById(R.id.tvResult)
        setupToggles()
        setupButtons()

        findViewById<Button>(R.id.btn_back_calc).setOnClickListener {
            finish()
        }
    }

    private fun setupToggles() {
        toggleMode1 = findViewById(R.id.toggle_mode1)
        toggleMode2 = findViewById(R.id.toggle_mode2)
        toggleMode3 = findViewById(R.id.toggle_mode3)


        toggleMode1.isChecked = false
        toggleMode2.isChecked = false
        toggleMode3.isChecked = false
    }

    private fun setupButtons() {
        numberAndDotButtons.forEach { id ->
            findViewById<Button>(id).setOnClickListener { onDigit(it) }
        }

        operatorButtons.forEach { id ->
            findViewById<Button>(id).setOnClickListener { onOperator(it) }
        }

        findViewById<Button>(R.id.btn_clear).setOnClickListener { onClear() }
        findViewById<Button>(R.id.btn_equals).setOnClickListener { onEqual() }
    }

    private fun getRandomColor(): Int {
        return -0x1000000 or (Random.nextInt(0xFFFFFF))
    }

    private fun changeAllButtonsColor() {
        allButtons.forEach { id ->
            findViewById<Button>(id).setBackgroundColor(getRandomColor())
        }
    }

    private fun changeFunctionButtonsColor() {
        val functionalButtons = operatorButtons + functionButtons + listOf(R.id.btn_divide, R.id.btn_multiply, R.id.btn_subtract, R.id.btn_add)
        functionalButtons.distinct().forEach { id ->
            findViewById<Button>(id).setBackgroundColor(getRandomColor())
        }
    }

    private fun changeNumberButtonsColor() {
        numberAndDotButtons.forEach { id ->
            findViewById<Button>(id).setBackgroundColor(getRandomColor())
        }
    }

    fun onDigit(view: View) {
        val buttonText = (view as Button).text

        if (tvResult.text.toString() == "0" && buttonText != ".") {
            tvResult.text = buttonText
        } else if (buttonText == ".") {
            if (!tvResult.text.contains('.')) {
                tvResult.append(buttonText)
            }
        } else {
            tvResult.append(buttonText)
        }
        lastNumeric = true


        if (toggleMode1.isChecked) {
            changeAllButtonsColor()
        }
    }

    fun onOperator(view: View) {
        val operator = (view as Button).text

        if (lastNumeric && !isOperatorAdded) {
            tvResult.append(operator)
            isOperatorAdded = true
            lastNumeric = false
        }


        if (toggleMode2.isChecked) {
            changeFunctionButtonsColor()
        }


        if (toggleMode1.isChecked) {
            changeAllButtonsColor()
        }
    }

    fun onClear() {
        tvResult.text = "0"
        lastNumeric = false
        isOperatorAdded = false


        if (toggleMode2.isChecked) {
            changeFunctionButtonsColor()
        }


        if (toggleMode1.isChecked) {
            changeAllButtonsColor()
        }
    }

    fun onEqual() {
        if (lastNumeric && isOperatorAdded) {
            val expression = tvResult.text.toString()

            try {
                var operatorIndex = -1
                var operator: Char? = null


                for (i in expression.indices) {
                    val char = expression[i]
                    if (char == '+' || char == '*' || char == '/') {
                        operatorIndex = i
                        operator = char
                        break
                    }
                    if (char == '-' && i > 0) {
                        operatorIndex = i
                        operator = char
                        break
                    }
                }

                if (operatorIndex == -1 || operator == null) return


                val num1Chars = CharArray(operatorIndex)
                for (i in 0 until operatorIndex) {
                    num1Chars[i] = expression[i]
                }
                val num1String = String(num1Chars)
                val num1 = num1String.toDouble()


                val num2Chars = CharArray(expression.length - operatorIndex - 1)
                for (i in operatorIndex + 1 until expression.length) {
                    num2Chars[i - operatorIndex - 1] = expression[i]
                }
                val num2String = String(num2Chars)
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

                val resultText = if (result % 1.0 == 0.0) {
                    result.toLong().toString()
                } else {
                    result.toString()
                }

                tvResult.text = resultText
                isOperatorAdded = false


                if (toggleMode3.isChecked) changeNumberButtonsColor()


                if (toggleMode2.isChecked) changeFunctionButtonsColor()


                if (toggleMode1.isChecked) changeAllButtonsColor()

            } catch (e: Exception) {
                tvResult.text = "input Error"
                e.printStackTrace()
            }
        }
    }
}