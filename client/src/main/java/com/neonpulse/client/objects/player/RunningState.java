package com.neonpulse.client.objects.player;

public class RunningState implements PlayerState {

    @Override
    public void jump(Player player) {
        player.changeState(new JumpingState());
    }

    @Override
    public void update(Player player, float delta) {
        // Ran off the edge of a platform.
        if (player.surfaceBelow(player.getY()) < player.getY()) {
            player.changeState(new FallingState());
        }
    }
}
