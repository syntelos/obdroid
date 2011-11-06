package ob.droid.term;

/**
 * An ASCII key listener. Supports control characters and escape. Keeps track of
 * the current state of the alt, shift, and control keys.
 */
class TermKeyListener {
    /**
     * The state engine for a modifier key. Can be pressed, released, locked,
     * and so on.
     *
     */
    private class ModifierKey {

        private int mState;

        private static final int UNPRESSED = 0;

        private static final int PRESSED = 1;

        private static final int RELEASED = 2;

        private static final int USED = 3;

        private static final int LOCKED = 4;

        /**
         * Construct a modifier key. UNPRESSED by default.
         *
         */
        public ModifierKey() {
            mState = UNPRESSED;
        }

        public void onPress() {
            switch (mState) {
            case PRESSED:
                // This is a repeat before use
                break;
            case RELEASED:
                mState = LOCKED;
                break;
            case USED:
                // This is a repeat after use
                break;
            case LOCKED:
                mState = UNPRESSED;
                break;
            default:
                mState = PRESSED;
                break;
            }
        }

        public void onRelease() {
            switch (mState) {
            case USED:
                mState = UNPRESSED;
                break;
            case PRESSED:
                mState = RELEASED;
                break;
            default:
                // Leave state alone
                break;
            }
        }

        public void adjustAfterKeypress() {
            switch (mState) {
            case PRESSED:
                mState = USED;
                break;
            case RELEASED:
                mState = UNPRESSED;
                break;
            default:
                // Leave state alone
                break;
            }
        }

        public boolean isActive() {
            return mState != UNPRESSED;
        }
    }

    private ModifierKey mAltKey = new ModifierKey();

    private ModifierKey mCapKey = new ModifierKey();

    private ModifierKey mControlKey = new ModifierKey();

    /**
     * Construct a term key listener.
     *
     */
    public TermKeyListener() {
    }

    public void handleControlKey(boolean down) {
        if (down) {
            mControlKey.onPress();
        } else {
            mControlKey.onRelease();
        }
    }

    public int mapControlChar(int ch) {
        int result = ch;
        if (mControlKey.isActive()) {
            // Search is the control key.
            if (result >= 'a' && result <= 'z') {
                result = (char) (result - 'a' + '\001');
            } else if (result == ' ') {
                result = 0;
            } else if ((result == '[') || (result == '1')) {
                result = 27;
            } else if ((result == '\\') || (result == '.')) {
                result = 28;
            } else if ((result == ']') || (result == '0')) {
                result = 29;
            } else if ((result == '^') || (result == '6')) {
                result = 30; // control-^
            } else if ((result == '_') || (result == '5')) {
                result = 31;
            }
        }

        if (result > -1) {
            mAltKey.adjustAfterKeypress();
            mCapKey.adjustAfterKeypress();
            mControlKey.adjustAfterKeypress();
        }
        return result;
    }

    /**
     * Handle a keyDown event.
     *
     * @param keyCode the keycode of the keyDown event
     * @return the ASCII byte to transmit to the pseudo-teletype, or -1 if this
     *         event does not produce an ASCII byte.
     */
    public int keyDown(int keyCode, KeyEvent event) {
        int result = -1;
        switch (keyCode) {
        case KeyEvent.KEYCODE_ALT_RIGHT:
        case KeyEvent.KEYCODE_ALT_LEFT:
            mAltKey.onPress();
            break;

        case KeyEvent.KEYCODE_SHIFT_LEFT:
        case KeyEvent.KEYCODE_SHIFT_RIGHT:
            mCapKey.onPress();
            break;

        case KeyEvent.KEYCODE_ENTER:
            // Convert newlines into returns. The vt100 sends a
            // '\r' when the 'Return' key is pressed, but our
            // KeyEvent translates this as a '\n'.
            result = '\r';
            break;

        case KeyEvent.KEYCODE_DEL:
            // Convert DEL into 127 (instead of 8)
            result = 127;
            break;

        default: {
            result = event.getUnicodeChar(
                   (mCapKey.isActive() ? KeyEvent.META_SHIFT_ON : 0) |
                   (mAltKey.isActive() ? KeyEvent.META_ALT_ON : 0));
            break;
            }
        }

        result = mapControlChar(result);

        return result;
    }

    /**
     * Handle a keyUp event.
     *
     * @param keyCode the keyCode of the keyUp event
     */
    public void keyUp(int keyCode) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_ALT_LEFT:
        case KeyEvent.KEYCODE_ALT_RIGHT:
            mAltKey.onRelease();
            break;
        case KeyEvent.KEYCODE_SHIFT_LEFT:
        case KeyEvent.KEYCODE_SHIFT_RIGHT:
            mCapKey.onRelease();
            break;
        default:
            // Ignore other keyUps
            break;
        }
    }
}
