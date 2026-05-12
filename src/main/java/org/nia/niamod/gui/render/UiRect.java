package org.nia.niamod.gui.render;

public record UiRect(int x, int y, int width, int height) {
    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= right() && mouseY >= y && mouseY <= bottom();
    }

    public UiRect inset(int amount) {
        return new UiRect(x + amount, y + amount, width - amount * 2, height - amount * 2);
    }

    public UiRect withHeight(int h) {
        return new UiRect(x, y, width, h);
    }

    public UiRect withY(int newY) {
        return new UiRect(x, newY, width, height);
    }

    public UiRect withWidth(int w) {
        return new UiRect(x, y, w, height);
    }

    public int centerX() {
        return x + width / 2;
    }

    public int centerY() {
        return y + height / 2;
    }
}
