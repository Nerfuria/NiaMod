package org.nia.niamod.gui.animation;

import lombok.Getter;
import lombok.Setter;

import java.util.function.Function;

@Getter
@Setter
public class Animation {
    private Function<Double, Double> easing;
    private long duration;
    private long millis;
    private double value;
    private double destination;
    private boolean finished = true;

    public Animation(Function<Double, Double> easing, long duration) {
        this.easing = easing;
        this.duration = duration;
    }

    public void run(double destination) {
        if (this.destination != destination) {
            this.millis = System.currentTimeMillis();
            this.value = getValue();
            this.destination = destination;
            this.finished = false;
        }
    }

    public double getValue() {
        if (finished) return destination;
        double progress = getProgress();
        return value + (destination - value) * progress;
    }

    public void setValue(double value) {
        this.value = value;
        this.destination = value;
        this.finished = true;
    }

    public double getProgress() {
        if (duration <= 0) {
            finished = true;
            return 1;
        }
        double progress = (double) (System.currentTimeMillis() - millis) / duration;
        progress = Math.min(1, Math.max(0, progress));
        if (progress >= 1) finished = true;
        return easing.apply(progress);
    }

    public void reset() {
        millis = System.currentTimeMillis();
        finished = false;
    }
}
