
package com.neonpulse.client.level;

public class LevelData {

    public static final float GROUND_Y = 90f;

    public static final float LEVEL_LENGTH = 5000f;

    public static final float PLAYER_X = 150f;
    public static final float PLAYER_SIZE = 40f;

    public static final float[][] PLATFORMS = {
            {350, 180, 180, 15},
            {650, 260, 180, 15},
            {1050, 180, 200, 15},
            {1450, 230, 180, 15},
            {1850, 180, 220, 15},
            {2300, 270, 200, 15},
            {2750, 180, 220, 15},
            {3200, 230, 200, 15},
            {3650, 180, 220, 15},
            {4200, 250, 250, 15}
    };

    public static final float[][] SPIKES = {
            {550, GROUND_Y, 35, 55},
            {950, GROUND_Y, 35, 55},
            {1350, GROUND_Y, 35, 55},
            {1750, GROUND_Y, 35, 55},
            {2150, GROUND_Y, 35, 55},
            {2650, GROUND_Y, 35, 55},
            {3100, GROUND_Y, 35, 55},
            {3550, GROUND_Y, 35, 55},
            {4050, GROUND_Y, 35, 55},
            {4650, GROUND_Y, 35, 55}
    };

    private LevelData() {
        // Utility class
    }
}
