package org.nia.niamod.gui.screen;

record OverlayPreviewBounds(int left, int top, int right, int bottom) {
    public boolean contains(double x, double y) {
        return x >= left - 6 && x <= right + 6 && y >= top - 6 && y <= bottom + 6;
    }

    public boolean containsCorner(double x, double y) {
        int handleSize = 6;
        return x >= left - handleSize && x <= left + handleSize && y >= top - handleSize && y <= top + handleSize;
    }
}
