package com.example.musicremover;

import android.media.MediaPlayer;

public class VideoPreparedListener implements MediaPlayer.OnPreparedListener {
    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.setLooping(true);
    }
}
