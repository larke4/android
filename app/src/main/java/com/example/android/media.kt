package com.example.android

import android.graphics.Color

import android.widget.TextView
import android.os.Environment
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class Media : AppCompatActivity() {

    private var trackTitleTextView: TextView? = null
    private var trackArtistTextView: TextView? = null
    private var albumTitleTextView: TextView? = null
    private var playPauseButton: ImageButton? = null
    private var prevButton: ImageButton? = null
    private var nextButton: ImageButton? = null
    private var shuffleButton: ImageButton? = null
    private var repeatButton: ImageButton? = null
    private var seekBar: SeekBar? = null
    private var volumeBar: SeekBar? = null
    private var tracksRecyclerView: RecyclerView? = null
    private var currentTimeText: TextView? = null
    private var totalTimeText: TextView? = null
    private var albumArtImageView: ImageView? = null
    private var player: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var musicFiles: Array<File> = arrayOf()
    private var musicTitles: Array<String> = arrayOf()
    private var currentSong = -1
    private var isShuffle = false
    private var isRepeat = false

    companion object {
        private const val KEY_CURRENT_SONG = "current_song"
        private const val KEY_CURRENT_POSITION = "current_position"
        private const val KEY_IS_PLAYING = "is_playing"
        private const val KEY_IS_SHUFFLE = "is_shuffle"
        private const val KEY_IS_REPEAT = "is_repeat"
    }

    private val updateExecutor = Executors.newSingleThreadScheduledExecutor()
    private val isUpdating = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.media)

        findViewById<Button>(R.id.btn_back_media).setOnClickListener {
            finish()
        }

        initializeViews()
        setupVolumeControl()
        setupButtons()
        setupSeekBar()
        checkStoragePermission()

        // Восстановление состояния
        if (savedInstanceState != null) {
            restoreState(savedInstanceState)
        }
    }

    private fun initializeViews() {
        trackTitleTextView = findViewById(R.id.trackTitleTextView)
        trackArtistTextView = findViewById(R.id.trackArtistTextView)
        albumTitleTextView = findViewById(R.id.albumTitleTextView)
        playPauseButton = findViewById(R.id.btn_play_pause)
        prevButton = findViewById(R.id.btn_prev)
        nextButton = findViewById(R.id.btn_next)
        shuffleButton = findViewById(R.id.btn_shuffle)
        repeatButton = findViewById(R.id.btn_repeat)
        seekBar = findViewById(R.id.trackSeekBar)
        volumeBar = findViewById(R.id.volumeSeekBar)
        tracksRecyclerView = findViewById(R.id.tracksRecyclerView)
        currentTimeText = findViewById(R.id.currentTimeText)
        totalTimeText = findViewById(R.id.totalTimeText)
        albumArtImageView = findViewById(R.id.albumArtImageView)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Устанавливаем RecyclerView
        tracksRecyclerView?.layoutManager = LinearLayoutManager(this)
    }

    private fun setupButtons() {
        playPauseButton?.setOnClickListener { onPlayPauseClicked() }
        prevButton?.setOnClickListener { playPreviousSong() }
        nextButton?.setOnClickListener { playNextSong() }
        shuffleButton?.setOnClickListener { toggleShuffle() }
        repeatButton?.setOnClickListener { toggleRepeat() }

        // Устанавливаем начальные цвета для кнопок
        shuffleButton?.setColorFilter(ContextCompat.getColor(this, android.R.color.white))
        repeatButton?.setColorFilter(ContextCompat.getColor(this, android.R.color.white))
    }

    private fun setupSeekBar() {
        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && player != null) {
                    player!!.seekTo(progress)
                    currentTimeText!!.text = formatTime(progress)
                }
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {
                isUpdating.set(false)
            }
            override fun onStopTrackingTouch(bar: SeekBar?) {
                isUpdating.set(true)
            }
        })
    }

    private fun toggleShuffle() {
        isShuffle = !isShuffle
        shuffleButton?.setColorFilter(
            if (isShuffle) ContextCompat.getColor(this, R.color.primary_color)
            else ContextCompat.getColor(this, android.R.color.white)
        )
        Toast.makeText(this, if (isShuffle) "Перемешивание включено" else "Перемешивание выключено",
            Toast.LENGTH_SHORT).show()
    }

    private fun toggleRepeat() {
        isRepeat = !isRepeat
        repeatButton?.setColorFilter(
            if (isRepeat) ContextCompat.getColor(this, R.color.primary_color)
            else ContextCompat.getColor(this, android.R.color.white)
        )
        Toast.makeText(this, if (isRepeat) "Повтор включен" else "Повтор выключен",
            Toast.LENGTH_SHORT).show()
    }

    private fun restoreState(savedInstanceState: Bundle) {
        val savedSong = savedInstanceState.getInt(KEY_CURRENT_SONG, -1)
        val savedPosition = savedInstanceState.getInt(KEY_CURRENT_POSITION, 0)
        val wasPlaying = savedInstanceState.getBoolean(KEY_IS_PLAYING, false)
        isShuffle = savedInstanceState.getBoolean(KEY_IS_SHUFFLE, false)
        isRepeat = savedInstanceState.getBoolean(KEY_IS_REPEAT, false)

        // Обновляем цвета кнопок на основе сохраненного состояния
        shuffleButton?.setColorFilter(
            if (isShuffle) ContextCompat.getColor(this, R.color.primary_color)
            else ContextCompat.getColor(this, android.R.color.white)
        )
        repeatButton?.setColorFilter(
            if (isRepeat) ContextCompat.getColor(this, R.color.primary_color)
            else ContextCompat.getColor(this, android.R.color.white)
        )

        if (savedSong != -1) {
            val handler = android.os.Handler()
            handler.postDelayed({
                if (musicFiles.isNotEmpty() && savedSong < musicFiles.size) {
                    restorePlayback(savedSong, savedPosition, wasPlaying)
                }
            }, 500)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (currentSong != -1) {
            outState.putInt(KEY_CURRENT_SONG, currentSong)
            outState.putInt(KEY_CURRENT_POSITION, player?.currentPosition ?: 0)
            outState.putBoolean(KEY_IS_PLAYING, player?.isPlaying ?: false)
            outState.putBoolean(KEY_IS_SHUFFLE, isShuffle)
            outState.putBoolean(KEY_IS_REPEAT, isRepeat)
        }
    }

    private fun restorePlayback(index: Int, position: Int, wasPlaying: Boolean) {
        currentSong = index
        val file = musicFiles[index]
        val title = musicTitles[index]

        try {
            player = MediaPlayer()
            player!!.setDataSource(file.absolutePath)
            player!!.prepare()

            updateTrackInfo(title, file)
            updatePlaybackControls(wasPlaying)
            player!!.seekTo(position)

            player!!.setOnCompletionListener {
                handleTrackCompletion()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateTrackInfo(title: String, file: File) {
        trackTitleTextView?.text = title
        trackArtistTextView?.text = extractArtistFromFile(file)
        albumTitleTextView?.text = extractAlbumFromFile(file)
    }

    private fun extractArtistFromFile(file: File): String {
        val fileName = file.nameWithoutExtension
        val parts = fileName.split("-", "_", ".")
        return if (parts.size > 1) {
            parts[0].trim().takeIf { it.isNotBlank() } ?: "Неизвестный исполнитель"
        } else {
            "Неизвестный исполнитель"
        }
    }

    private fun extractAlbumFromFile(file: File): String {
        return file.parentFile?.name?.takeIf { it.isNotBlank() } ?: "Неизвестный альбом"
    }

    private fun updatePlaybackControls(isPlaying: Boolean) {
        if (isPlaying) {
            playPauseButton?.setImageResource(R.drawable.ic_pause)
            startSeekBarUpdates()
        } else {
            playPauseButton?.setImageResource(R.drawable.ic_play)
            stopSeekBarUpdates()
        }
    }

    private fun handleTrackCompletion() {
        if (isRepeat) {
            player?.seekTo(0)
            player?.start()
        } else {
            playNextSong()
        }
    }

    private fun startSeekBarUpdates() {
        if (isUpdating.get() || player == null) return

        isUpdating.set(true)
        updateExecutor.scheduleAtFixedRate({
            runOnUiThread {
                if (player != null && player!!.isPlaying) {
                    val currentPos = player!!.currentPosition
                    val duration = player!!.duration

                    seekBar?.progress = currentPos
                    currentTimeText?.text = formatTime(currentPos)

                    if (seekBar?.max != duration) {
                        seekBar?.max = duration
                        totalTimeText?.text = formatTime(duration)
                    }
                }
            }
        }, 0, 1000, TimeUnit.MILLISECONDS)
    }

    private fun stopSeekBarUpdates() {
        isUpdating.set(false)
    }

    private fun onPlayPauseClicked() {
        if (currentSong == -1 && musicFiles.isNotEmpty()) {
            playSongAtIndex(0)
            return
        }

        if (player == null && currentSong != -1) {
            playSongAtIndex(currentSong)
            return
        }

        player?.let {
            if (it.isPlaying) {
                it.pause()
                playPauseButton?.setImageResource(R.drawable.ic_play)
                stopSeekBarUpdates()
            } else {
                it.start()
                playPauseButton?.setImageResource(R.drawable.ic_pause)
                startSeekBarUpdates()
            }
        }
    }

    private fun playPreviousSong() {
        if (currentSong <= 0) {
            Toast.makeText(this, "Это первая песня", Toast.LENGTH_SHORT).show()
            return
        }
        playSongAtIndex(currentSong - 1)
    }

    private fun playNextSong() {
        if (musicFiles.isEmpty()) return

        val nextIndex = if (isShuffle) {
            (0 until musicFiles.size).random()
        } else if (currentSong >= musicFiles.size - 1) {
            0
        } else {
            currentSong + 1
        }

        playSongAtIndex(nextIndex)
    }

    private fun playSongAtIndex(index: Int) {
        stopSeekBarUpdates()

        player?.let {
            it.stop()
            it.release()
        }

        currentSong = index
        val file = musicFiles[index]
        val title = musicTitles[index]

        player = MediaPlayer()
        try {
            player!!.setDataSource(file.absolutePath)
            player!!.prepare()
            player!!.start()

            updateTrackInfo(title, file)
            updatePlaybackControls(true)

            seekBar?.max = player!!.duration
            seekBar?.progress = 0
            totalTimeText?.text = formatTime(player!!.duration)
            currentTimeText?.text = "0:00"

            player!!.setOnCompletionListener {
                handleTrackCompletion()
            }

            startSeekBarUpdates()
            highlightCurrentTrack(index)

        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка воспроизведения: ${e.message}", Toast.LENGTH_LONG).show()
            resetPlayer()
        }
    }

    private fun highlightCurrentTrack(index: Int) {
        (tracksRecyclerView?.adapter as? TrackAdapter)?.setSelectedPosition(index)
    }

    private fun resetPlayer() {
        stopSeekBarUpdates()
        player?.release()
        player = null
        currentSong = -1
        playPauseButton?.setImageResource(R.drawable.ic_play)
        seekBar?.progress = 0
        currentTimeText?.text = "0:00"
        totalTimeText?.text = "0:00"
    }

    private fun formatTime(ms: Int): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun setupVolumeControl() {
        val maxVol = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val curVol = audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC)
        volumeBar?.max = maxVol
        volumeBar?.progress = curVol
        volumeBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, vol: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0)
                }
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })
    }

    override fun onPause() {
        super.onPause()
        // Сохраняем состояние, но не останавливаем музыку
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSeekBarUpdates()
        updateExecutor.shutdownNow()
        player?.release()
    }

    private fun checkStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadAllMusic()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            loadAllMusic()
        } else {
            Toast.makeText(this, "Разрешение необходимо для загрузки музыки", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadAllMusic() {
        Thread {
            val musicPaths = mutableSetOf(
                Environment.getExternalStorageDirectory().path + "/Music/",
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).path,
                Environment.getExternalStorageDirectory().path + "/Download/",
                Environment.getExternalStorageDirectory().path + "/"
            )

            val fileList = mutableListOf<File>()
            val titleList = mutableListOf<String>()

            fun scanDirectory(directory: File) {
                if (!directory.exists() || !directory.isDirectory) {
                    return
                }

                try {
                    directory.listFiles()?.forEach { file ->
                        if (file.isDirectory) {
                            scanDirectory(file)
                        } else if (file.isFile) {
                            val extension = file.extension.lowercase()
                            if (extension == "mp3" || extension == "flac" ||
                                extension == "ogg" || extension == "wav" ||
                                extension == "m4a" || extension == "aac") {
                                fileList.add(file)
                                titleList.add(file.nameWithoutExtension)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            musicPaths.forEach { path ->
                scanDirectory(File(path))
            }

            runOnUiThread {
                if (fileList.isEmpty()) {
                    Toast.makeText(this, "Музыка не найдена", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }

                musicFiles = fileList.toTypedArray()
                musicTitles = titleList.toTypedArray()
                showMusicList()

                Toast.makeText(this, "Найдено ${musicFiles.size} треков", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun showMusicList() {
        val adapter = TrackAdapter(musicTitles.toList()) { position ->
            playSongAtIndex(position)
        }
        tracksRecyclerView?.adapter = adapter
    }

    inner class TrackAdapter(
        private val tracks: List<String>,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<TrackAdapter.ViewHolder>() {

        private var selectedPosition = -1

        fun setSelectedPosition(position: Int) {
            val oldPosition = selectedPosition
            selectedPosition = position
            if (oldPosition >= 0) notifyItemChanged(oldPosition)
            if (position >= 0) notifyItemChanged(position)
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val trackName: TextView = itemView.findViewById(android.R.id.text1)
            val trackNumber: TextView = itemView.findViewById(R.id.trackNumberTextView)
            val trackArtist: TextView = itemView.findViewById(R.id.trackArtistTextView)
            val trackDuration: TextView = itemView.findViewById(R.id.trackDurationTextView)

            init {
                itemView.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClick(position)
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_track, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.trackName.text = tracks[position]
            holder.trackNumber.text = "${position + 1}"

            // Извлекаем исполнителя из имени файла
            val fileName = tracks[position]
            holder.trackArtist.text = extractArtistFromFileName(fileName)

            // Здесь можно добавить длительность трека
            holder.trackDuration.text = getTrackDuration(position)

            // Подсвечиваем текущий трек
            holder.itemView.setBackgroundColor(
                if (position == selectedPosition) {
                    ContextCompat.getColor(this@Media, R.color.selected_track_background)
                } else {
                    Color.TRANSPARENT
                }
            )

            // Меняем цвет текста для выделенного трека
            val textColor = if (position == selectedPosition) {
                ContextCompat.getColor(this@Media, R.color.primary_color)
            } else {
                ContextCompat.getColor(this@Media, android.R.color.white)
            }

            holder.trackName.setTextColor(textColor)
            holder.trackNumber.setTextColor(textColor)
        }

        override fun getItemCount() = tracks.size

        private fun extractArtistFromFileName(fileName: String): String {
            val parts = fileName.split("-", "_", ".")
            return if (parts.size > 1) {
                parts[0].trim().takeIf { it.isNotBlank() } ?: "Неизвестный исполнитель"
            } else {
                "Неизвестный исполнитель"
            }
        }

        private fun getTrackDuration(position: Int): String {
            return if (position == currentSong && player != null) {
                formatTime(player!!.duration)
            } else {
                "" // Можно получить длительность из метаданных файла
            }
        }
    }}