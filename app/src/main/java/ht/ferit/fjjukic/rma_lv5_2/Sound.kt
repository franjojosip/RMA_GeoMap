package ht.ferit.fjjukic.rma_lv5_2

import android.content.Context
import android.media.SoundPool

class Sound {
    private var mSoundPool: SoundPool = SoundPool.Builder().setMaxStreams(10).build()
    private var soundMap: HashMap<Int, Int> = HashMap()
    private var isLoaded: Boolean = false

    init{
        this.mSoundPool.setOnLoadCompleteListener { _, _, _ -> isLoaded = true }
    }

    fun load(context: Context, rawId: Int, priority: Int) {
        this.soundMap[rawId] = this.mSoundPool.load(context, rawId, priority)
    }

    fun play(sound: Int) {
        val soundID: Int = this.soundMap[sound] ?: 0
        this.mSoundPool.play(soundID, 1f, 1f, 1, 0, 1f)
    }
}