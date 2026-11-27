package com.neonpulse.client.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

import java.util.HashMap;
import java.util.Map;

public class AudioManager {
    private static final float MUSIC_VOLUME = 0.5f;
    private static final float SOUND_VOLUME = 0.15f;

    private static AudioManager instance;

    private final Map<String, Sound> soundMap;
    private final Map<String, Music> musicMap;
    private Music currentMusic;

    private AudioManager() {
        soundMap = new HashMap<>();
        musicMap = new HashMap<>();
    }

    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    public void registerSound(String key, String filename) {
        if (soundMap.containsKey(key)) {
            return;
        }
        if (Gdx.audio != null && Gdx.files != null && Gdx.files.internal(filename).exists()) {
            soundMap.put(key, Gdx.audio.newSound(Gdx.files.internal(filename)));
        } else {
            System.out.println("No SFX: " + filename);
        }
    }

    public void registerMusic(String key, String filename) {
        if (musicMap.containsKey(key)) {
            return;
        }
        if (Gdx.audio != null && Gdx.files != null && Gdx.files.internal(filename).exists()) {
            musicMap.put(key, Gdx.audio.newMusic(Gdx.files.internal(filename)));
        } else {
            System.out.println("No BGM: " + filename);
        }
    }

    public Sound getSound(String key) {
        return soundMap.get(key);
    }

    public Music getMusic(String key) {
        return musicMap.get(key);
    }

    public void playSound(String key) {
        Sound sound = getSound(key);
        if (sound != null) {
            sound.play(SOUND_VOLUME);
        }
    }

    public void playMusic(String key) {
        Music next = getMusic(key);
        stopMusic();
        if (next == null) {
            System.out.println("Music not registered: " + key);
            return;
        }
        currentMusic = next;
        next.setLooping(false);
        next.setVolume(MUSIC_VOLUME);
        next.play();
    }

    public void pauseMusic() {
        if (currentMusic != null) {
            currentMusic.pause();
        }
    }

    public void resumeMusic() {
        if (currentMusic != null) {
            currentMusic.play();
        }
    }

    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic = null;
        }
    }

    public void init() {
        registerMusic("bgm_stage1", "Music/stage1.ogg");
        registerMusic("bgm_stage2", "Music/stage2rev.ogg");

        // DIRECTORY
        registerSound("sfx_die", "");
    }

    public void dispose() {
        for (Sound sound : soundMap.values()) {
            sound.dispose();
        }
        for (Music music : musicMap.values()) {
            music.dispose();
        }
        soundMap.clear();
        musicMap.clear();
        currentMusic = null;
    }
}
