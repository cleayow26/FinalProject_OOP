package com.neonpulse.game;

import com.badlogic.gdx.math.Rectangle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CollisionDetectorTest {

    @Test
    void testPlayerHitsObstacle() {
        Rectangle player = new Rectangle(0, 0, 20, 20);
        Rectangle obstacle = new Rectangle(10, 10, 20, 20);

        assertTrue(CollisionDetector.isColliding(player, obstacle));
    }

    @Test
    void testPlayerAvoidsObstacle() {
        Rectangle player = new Rectangle(0, 0, 20, 20);
        Rectangle obstacle = new Rectangle(100, 100, 20, 20);

        assertFalse(CollisionDetector.isColliding(player, obstacle));
    }

    @Test
    void testPlayerLandsOnPlatform() {
        Rectangle player = new Rectangle(10, 9, 20, 20);
        Rectangle platform = new Rectangle(0, 0, 100, 10);

        float previousY = 15;

        assertTrue(CollisionDetector.isLanding(
                player, platform, previousY
        ));
    }

    @Test
    void testPlayerMissesPlatform() {
        Rectangle player = new Rectangle(150, 9, 20, 20);
        Rectangle platform = new Rectangle(0, 0, 100, 10);

        float previousY = 15;

        assertFalse(CollisionDetector.isLanding(
                player, platform, previousY
        ));
    }

    @Test
    void testPlayerFallsOutOfLevel() {
        Rectangle player = new Rectangle(0, -50, 20, 20);

        assertTrue(CollisionDetector.isFallingOut(player, 0));
    }

    @Test
    void testPlayerStillInsideLevel() {
        Rectangle player = new Rectangle(0, 50, 20, 20);

        assertFalse(CollisionDetector.isFallingOut(player, 0));
    }
}

