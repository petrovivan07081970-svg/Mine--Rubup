package com.example.rubupminessimulator

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.GridLayout
import android.widget.Toast
import kotlin.random.Random

class MainActivity : Activity() {
    private val total = 25
    private var mineCount = 3
    private var mines = mutableSetOf<Int>()
    private val opened = mutableSetOf<Int>()
    private var ended = false
    private lateinit var grid: GridLayout
    private lateinit var status: TextView
    private lateinit var chance: TextView
    private lateinit var mineLabel: TextView
    private val cells = mutableListOf<Button>()

    private val bg = Color.rgb(14, 18, 34)
    private val tile = Color.rgb(53, 59, 84)
    private val accent = Color.rgb(104, 91, 245)
    private val green = Color.rgb(45, 145, 112)
    private val red = Color.rgb(173, 66, 83)
    private val white = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildUi()
        newRound()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun text(value: String, size: Float, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(white)
            if (bold) setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }

    private fun buildUi() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(14))
            setBackgroundColor(bg)
        }

        page.addView(text("MINES SIMULATOR", 23f, true), LinearLayout.LayoutParams(-1, dp(52)))
        page.addView(text("Учебная симуляция поля 5 × 5", 14f), LinearLayout.LayoutParams(-1, dp(30)))

        mineLabel = text("Количество мин: $mineCount", 16f, true)
        page.addView(mineLabel, LinearLayout.LayoutParams(-1, dp(42)))

        val seek = SeekBar(this).apply {
            max = 24
            progress = mineCount
            progressTintList = android.content.res.ColorStateList.valueOf(accent)
            thumbTintList = android.content.res.ColorStateList.valueOf(white)
        }
        page.addView(seek, LinearLayout.LayoutParams(-1, dp(42)))
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                mineCount = progress.coerceIn(1, 24)
                mineLabel.text = "Количество мин: $mineCount"
                if (fromUser) newRound()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        chance = text("", 16f, true)
        page.addView(chance, LinearLayout.LayoutParams(-1, dp(48)))

        status = text("", 14f)
        page.addView(status, LinearLayout.LayoutParams(-1, dp(44)))

        grid = GridLayout(this).apply {
            columnCount = 5
            rowCount = 5
            useDefaultMargins = false
        }
        val gridParams = LinearLayout.LayoutParams(-1, 0, 1f).apply {
            setMargins(0, dp(8), 0, dp(8))
        }
        page.addView(grid, gridParams)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val reset = Button(this).apply {
            text = "Новая игра"
            setTextColor(white)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent))
            setOnClickListener { newRound() }
        }
        actions.addView(reset, LinearLayout.LayoutParams(0, dp(54), 1f))
        page.addView(actions, LinearLayout.LayoutParams(-1, dp(60)))

        val note = text(
            "Важно: клетки генерируются случайно только внутри этой симуляции. " +
                "Приложение не видит и не предсказывает результаты сторонних игр.",
            12f
        )
        note.setPadding(dp(4), dp(8), dp(4), dp(4))
        page.addView(note, LinearLayout.LayoutParams(-1, dp(76)))

        setContentView(page)
        createCells()
    }

    private fun createCells() {
        grid.removeAllViews()
        cells.clear()
        for (i in 0 until total) {
            val button = Button(this).apply {
                text = "?"
                textSize = 18f
                setTextColor(white)
                setTypeface(null, Typeface.BOLD)
                setBackgroundTintList(android.content.res.ColorStateList.valueOf(tile))
                setPadding(0, 0, 0, 0)
                setOnClickListener { tap(i) }
            }
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = GridLayout.spec(i % 5, 1f)
                rowSpec = GridLayout.spec(i / 5, 1f)
                setMargins(dp(4), dp(4), dp(4), dp(4))
            }
            grid.addView(button, params)
            cells.add(button)
        }
    }

    private fun newRound() {
        mines = (0 until total).shuffled().take(mineCount).toMutableSet()
        opened.clear()
        ended = false
        cells.forEach {
            it.text = "?"
            it.isEnabled = true
            it.setBackgroundTintList(android.content.res.ColorStateList.valueOf(tile))
        }
        status.text = "Выбери клетку, чтобы проверить симуляцию."
        updateChance()
    }

    private fun tap(index: Int) {
        if (ended || index in opened) return
        opened.add(index)
        val button = cells[index]
        if (index in mines) {
            button.text = "✹"
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(red))
            status.text = "В симуляции найдена мина. Начни новый раунд."
            ended = true
            revealMines()
        } else {
            button.text = "✓"
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(green))
            button.isEnabled = false
            val safeTotal = total - mineCount
            val safeOpened = opened.count { it !in mines }
            status.text = "Безопасных клеток открыто: $safeOpened из $safeTotal."
            if (safeOpened == safeTotal) {
                status.text = "Все безопасные клетки открыты!"
                ended = true
            }
        }
        updateChance()
    }

    private fun revealMines() {
        mines.forEach { i ->
            cells[i].text = "✹"
            cells[i].setBackgroundTintList(android.content.res.ColorStateList.valueOf(red))
        }
        cells.forEach { it.isEnabled = false }
    }

    private fun updateChance() {
        val remaining = total - opened.size
        val remainingMines = (mines.size - opened.count { it in mines }).coerceAtLeast(0)
        val safe = remaining - remainingMines
        val percent = if (remaining > 0) safe * 100.0 / remaining else 0.0
        chance.text = "Теоретический шанс безопасной клетки: ${"%.1f".format(java.util.Locale.US, percent)}%"
    }
}
