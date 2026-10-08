package com.limelight.binding.input.virtual_controller;

import java.util.List;

// The on-screen controller or keyboard that owns a set of elements
public interface ElementHost {
    ControllerMode getControllerMode();

    List<? extends VirtualControllerElement<?>> getElements();

    // Colour of an element while it is pressed
    default int getPressedColor() {
        return 0xF07272ED;
    }

    // Whether moved elements snap to the others and get resized to fit
    default boolean snapsElements() {
        return false;
    }

    default void vibrate(int action) {
    }
}
