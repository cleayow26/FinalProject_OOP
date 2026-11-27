package com.neonpulse.client.objects.player;

public class JumpingState implements PlayerState {

    @Override
    public void onEnter(Player player) {
        player.setVelocityY(Player.JUMP_VELOCITY);
    }

    @Override
    public void update(Player player, float delta) {
        // Platforms are one-way, so rising passes through them.
        player.applyGravity(delta);

        if (player.getVelocityY() <= 0f) {
            player.changeState(new FallingState());
        }
    }
}
