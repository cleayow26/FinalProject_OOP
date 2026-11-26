package com.neonpulse.game;

import com.badlogic.gdx.math.Rectangle;

public class CollisionDetector {

    public static boolean isColliding(
            Rectangle player,
            Rectangle obstacle) {

        return player.overlaps(obstacle);
    }

    public static boolean isLanding(
            Rectangle player,
            Rectangle platform,
            float previousY) {

        float platformTop = platform.y + platform.height;

        boolean horizontalOverlap =
                player.x < platform.x + platform.width &&
                        player.x + player.width > platform.x;

        boolean movingDown = player.y < previousY;

        boolean crossingPlatform =
                previousY >= platformTop &&
                        player.y <= platformTop;

        return horizontalOverlap &&
                movingDown &&
                crossingPlatform;
    }

    public static boolean isFallingOut(
            Rectangle player,
            float bottomLimit) {

        return player.y + player.height < bottomLimit;
    }
}
