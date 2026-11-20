
package com.neonpulse.game;

public class ScoreManager {

    private static final int PERFECT_POINTS = 100;
    private static final int GOOD_POINTS = 50;
    private static final int MISS_POINTS = 0;

    private int score;
    private int perfectCount;
    private int goodCount;
    private int missCount;
    private int combo;
    private int maxCombo;

    public ScoreManager() {
        reset();
    }

    public void registerPerfect() {
        score += PERFECT_POINTS;
        perfectCount++;
        combo++;
        updateMaxCombo();
    }

    public void registerGood() {
        score += GOOD_POINTS;
        goodCount++;
        combo++;
        updateMaxCombo();
    }

    public void registerMiss() {
        score += MISS_POINTS;
        missCount++;
        combo = 0;
    }

    private void updateMaxCombo() {
        if (combo > maxCombo) {
            maxCombo = combo;
        }
    }

    public int getScore() {
        return score;
    }

    public int getPerfectCount() {
        return perfectCount;
    }

    public int getGoodCount() {
        return goodCount;
    }

    public int getMissCount() {
        return missCount;
    }

    public int getCombo() {
        return combo;
    }

    public int getMaxCombo() {
        return maxCombo;
    }

    public int getTotalJudgements() {
        return perfectCount + goodCount + missCount;
    }

    public double getAccuracy() {
        int total = getTotalJudgements();

        if (total == 0) {
            return 0.0;
        }

        return (double) score / (total * PERFECT_POINTS) * 100;
    }

    public void reset() {
        score = 0;
        perfectCount = 0;
        goodCount = 0;
        missCount = 0;
        combo = 0;
        maxCombo = 0;
    }
}
