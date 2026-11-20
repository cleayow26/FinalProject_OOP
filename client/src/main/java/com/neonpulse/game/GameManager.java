
package com.neonpulse.game;

public class GameManager {

    public enum GameState {
        READY,
        PLAYING,
        PAUSED,
        GAME_OVER,
        LEVEL_COMPLETED
    }

    private GameState currentState;
    private final ScoreManager scoreManager;

    public GameManager() {
        scoreManager = new ScoreManager();
        currentState = GameState.READY;
    }

    public void startGame() {
        if (currentState == GameState.READY) {
            currentState = GameState.PLAYING;
        }
    }

    public void pauseGame() {
        if (currentState == GameState.PLAYING) {
            currentState = GameState.PAUSED;
        }
    }

    public void resumeGame() {
        if (currentState == GameState.PAUSED) {
            currentState = GameState.PLAYING;
        }
    }

    public void endGame() {
        if (currentState == GameState.PLAYING) {
            currentState = GameState.GAME_OVER;
        }
    }

    public void completeLevel() {
        if (currentState == GameState.PLAYING) {
            currentState = GameState.LEVEL_COMPLETED;
        }
    }

    public void resetGame() {
        scoreManager.reset();
        currentState = GameState.READY;
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public ScoreManager getScoreManager() {
        return scoreManager;
    }

    public boolean isPlaying() {
        return currentState == GameState.PLAYING;
    }
}
