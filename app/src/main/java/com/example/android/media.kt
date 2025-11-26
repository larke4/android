package com.example.android

import android.os.Environment
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.TimeUnit

class Media : AppCompatActivity() {

    private var titleText: TextView? = null
    private var playButton: Button? = null
    private var stopButton: Button? = null
    private var prevButton: Button? = null
    private var nextButton: Button? = null
    private var seekBar: SeekBar? = null
    private var volumeBar: SeekBar? = null
    private var trackList: ListView? = null
    private var currentTimeText: TextView? = null
    private var totalTimeText: TextView? = null
    private var player: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var musicFiles: Array<File> = arrayOf()
    private var musicTitles: Array<String> = arrayOf()
    private var currentSong = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.media)

        findViewById<Button>(R.id.btn_back_media).setOnClickListener {
            finish()
        }

        titleText = findViewById(R.id.trackTitleTextView)
        playButton = findViewById(R.id.btn_play_pause)
        stopButton = findViewById(R.id.btn_stop)
        prevButton = findViewById(R.id.btn_prev)
        nextButton = findViewById(R.id.btn_next)
        seekBar = findViewById(R.id.trackSeekBar)
        volumeBar = findViewById(R.id.volumeSeekBar)
        trackList = findViewById(R.id.tracksListView)
        currentTimeText = findViewById(R.id.currentTimeText)
        totalTimeText = findViewById(R.id.totalTimeText)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        setupVolumeControl()

        playButton!!.setOnClickListener { onPlayPauseClicked() }
        stopButton!!.setOnClickListener { stopMusic() }
        prevButton!!.setOnClickListener { playPreviousSong() }
        nextButton!!.setOnClickListener { playNextSong() }

        seekBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && player != null) {
                    player!!.seekTo(progress)
                }
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })

        checkStoragePermission()

        Thread {
            while (true) {
                try {
                    Thread.sleep(1000)
                } catch (e: Exception) {}

                runOnUiThread {
                    if (player != null && player!!.isPlaying) {
                        seekBar!!.progress = player!!.currentPosition
                        currentTimeText!!.text = formatTime(player!!.currentPosition)
                    }
                }
            }
        }.start()
    }

    private fun onPlayPauseClicked() {
        if (currentSong == -1) {
            Toast.makeText(this, "Выберите песню", Toast.LENGTH_SHORT).show()
            return
        }

        if (player == null) {
            playSongAtIndex(currentSong)
        } else if (player!!.isPlaying) {
            player!!.pause()
            playButton!!.text = "▶"
        } else {
            player!!.start()
            playButton!!.text = "||"
        }
    }

    private fun stopMusic() {
        if (player != null) {
            player!!.stop()
            player!!.release()
            player = null
        }
        playButton!!.text = "▶"
        seekBar!!.progress = 0
        titleText!!.text = "Название трека"
        currentTimeText!!.text = "0:00"
        totalTimeText!!.text = "0:00"
        currentSong = -1
    }

    private fun playPreviousSong() {
        if (currentSong <= 0) {
            Toast.makeText(this, "Это первая песня", Toast.LENGTH_SHORT).show()
            return
        }
        playSongAtIndex(currentSong - 1)
    }

    private fun playNextSong() {
        if (currentSong >= musicFiles.size - 1) {
            Toast.makeText(this, "Это последняя песня", Toast.LENGTH_SHORT).show()
            return
        }
        playSongAtIndex(currentSong + 1)
    }

    private fun playSongAtIndex(index: Int) {
        stopMusic()

        currentSong = index
        val file = musicFiles[index]
        val title = musicTitles[index]

        player = MediaPlayer()
        try {
            player!!.setDataSource(file.absolutePath)
            player!!.prepare()
            player!!.start()

            titleText!!.text = title
            playButton!!.text = "||"

            val duration = player!!.duration
            seekBar!!.max = duration
            seekBar!!.progress = 0

            totalTimeText!!.text = formatTime(duration)
            currentTimeText!!.text = formatTime(0)


            player!!.setOnCompletionListener {
                stopMusic()
                titleText!!.text = "Название трека"
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            stopMusic()
        }
    }

    private fun formatTime(ms: Int): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds - (minutes * 60)
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun setupVolumeControl() {
        val maxVol = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val curVol = audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC)
        volumeBar!!.max = maxVol
        volumeBar!!.progress = curVol
        volumeBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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
        if (player != null && player!!.isPlaying) {
            player!!.pause()
            playButton!!.text = "▶"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMusic()
    }
    private fun checkStoragePermission() {
        val permission: String
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_AUDIO
        }
        else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadAllMusic()
        }
        else {
            permissionLauncher.launch(permission)
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            loadAllMusic()
            Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Please grant permission", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadAllMusic() {
        val musicPath = Environment.getExternalStorageDirectory().path + "/Music/"
        val musicDirectory = File(musicPath)
        val fileList = mutableListOf<File>()
        val titleList = mutableListOf<String>()
        fun scanDirectory(directory: File) {
            if (!directory.exists() || !directory.isDirectory) {
                return
            }

            val allFiles = directory.listFiles()
            allFiles?.forEach { file ->
                if (file.isDirectory) {
                    scanDirectory(file)
                } else if (file.isFile && file.extension == "mp3" || file.extension == "flac" || file.extension == "ogg") {
                    fileList.add(file)
                    titleList.add(file.nameWithoutExtension)
                }
            }
        }
        scanDirectory(musicDirectory)

        if (fileList.isEmpty()) {
            Toast.makeText(this, "Музыка не найдена по пути $musicPath и в его подкаталогах", Toast.LENGTH_LONG).show()
            return
        }

        musicFiles = fileList.toTypedArray()
        musicTitles = titleList.toTypedArray()
        showMusicList()
    }

    private fun showMusicList() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, musicTitles.toList())

        if (trackList != null) {
            trackList!!.adapter = adapter
        }

        if (trackList != null) {
            trackList!!.setOnItemClickListener { _, _, position, _ ->
                playSongAtIndex(position)
            }
        }
    }
}