package io.github.projetoalgoritmos.minigames;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;

public class MinigameAnimation {
    public static final float INTRO_DURATION = 0.9f;
    public static final float OUTRO_DURATION = 0.8f;
    private static final float OFFSCREEN_Y = -760f;

    private final Matrix4 transform = new Matrix4();
    private final Matrix4 identity = new Matrix4();

    private float time;
    private float introTimer;
    private float outroTimer;
    private boolean outroStarted;
    private float shakeTimer;
    private float shakeDuration = 1f;
    private float shakeIntensity;

    public void reiniciar() {
        time = 0f;
        introTimer = 0f;
        outroTimer = 0f;
        outroStarted = false;
        shakeTimer = 0f;
        shakeIntensity = 0f;
    }

    public void update(float delta) {
        time += delta;

        if (introTimer < INTRO_DURATION) {
            introTimer = Math.min(INTRO_DURATION, introTimer + delta);
        }
        if (outroStarted && outroTimer < OUTRO_DURATION) {
            outroTimer = Math.min(OUTRO_DURATION, outroTimer + delta);
        }
        if (shakeTimer > 0f) {
            shakeTimer = Math.max(0f, shakeTimer - delta);
        }
    }

    public void startOutro() {
        if (outroStarted) return;
        introTimer = INTRO_DURATION;
        outroTimer = 0f;
        outroStarted = true;
    }

    public void shake(float duration, float intensity) {
        shakeDuration = Math.max(0.01f, duration);
        shakeTimer = shakeDuration;
        shakeIntensity = intensity;
    }

    public boolean isIntroActive() {
        return introTimer < INTRO_DURATION;
    }

    public boolean isOutroActive() {
        return outroStarted && outroTimer < OUTRO_DURATION;
    }

    public boolean isOutroFinished() {
        return outroStarted && outroTimer >= OUTRO_DURATION;
    }

    public boolean isAnimating() {
        return isIntroActive() || isOutroActive();
    }

    /** Aplica o deslocamento de entrada/saída e o tremor antes de iniciar o batch. */
    public void apply(SpriteBatch batch) {
        transform.idt().translate(shakeX(), offsetY() + shakeY(), 0f);
        batch.setTransformMatrix(transform);
    }

    /** Devolve o batch à matriz padrão para não afetar o restante do jogo. */
    public void reset(SpriteBatch batch) {
        batch.setTransformMatrix(identity);
    }

    private float offsetY() {
        float offset = 0f;

        if (introTimer < INTRO_DURATION) {
            float p = MathUtils.clamp(introTimer / INTRO_DURATION, 0f, 1f);
            float eased = 1f - (1f - p) * (1f - p) * (1f - p);
            offset += OFFSCREEN_Y * (1f - eased);
        }

        if (outroStarted) {
            float p = MathUtils.clamp(outroTimer / OUTRO_DURATION, 0f, 1f);
            float eased = p * p;
            offset += OFFSCREEN_Y * eased;
        }

        return offset;
    }

    private float shakeX() {
        if (shakeTimer <= 0f) return 0f;
        float decay = shakeTimer / shakeDuration;
        return MathUtils.sin(time * 95f) * shakeIntensity * decay;
    }

    private float shakeY() {
        if (shakeTimer <= 0f) return 0f;
        float decay = shakeTimer / shakeDuration;
        return MathUtils.cos(time * 73f) * shakeIntensity * decay * 0.7f;
    }
}
