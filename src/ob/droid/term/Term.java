/*
 * Copyright (C) 2007 The Android Open Source Project
 * Copyright (C) 2011 John Pritchard, Syntelos 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ob.droid.term;

import android.app.Activity;
import android.app.AlertDialog;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;

import android.net.Uri;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.preference.PreferenceManager;

import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;

import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;

import ob.droid.Connection;
import ob.droid.R;

/**
 * A terminal emulator activity.
 */
public abstract class Term 
    extends Activity
{
    /**
     * Set to true to add debugging code and logging.
     */
    public static final boolean DEBUG = false;

    /**
     * Set to true to log each character received from the remote process to the
     * android log, which makes it easier to debug some kinds of problems with
     * emulating escape sequences and control codes.
     */
    public static final boolean LOG_CHARACTERS_FLAG = DEBUG && false;

    /**
     * Set to true to log unknown escape sequences.
     */
    public static final boolean LOG_UNKNOWN_ESCAPE_SEQUENCES = DEBUG && false;
    /**
     * The name of our emulator view in the view resource.
     */
    private static final int EMULATOR_VIEW = R.id.emulatorView;

    private static final String FONTSIZE_KEY = "fontsize";
    private static final String COLOR_KEY = "color";

    private static final String CONTROLKEY_KEY = "controlkey";

    private static final String INITIALCOMMAND_KEY = "initialcommand";

    public static final int WHITE = 0xffffffff;
    public static final int BLACK = 0xff000000;
    public static final int BLUE = 0xff344ebd;

    private static final int[][] COLOR_SCHEMES = {
        {BLACK, WHITE}, {WHITE, BLACK}, {WHITE, BLUE}};

    private static final int[] CONTROL_KEY_SCHEMES = {
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_AT,
        KeyEvent.KEYCODE_ALT_LEFT,
        KeyEvent.KEYCODE_ALT_RIGHT
    };
    private static final String[] CONTROL_KEY_NAME = {
        "Ball", "@", "Left-Alt", "Right-Alt"
    };

    /**
     * The tag we use when logging, so that our messages can be distinguished
     * from other messages in the log. Public because it's used by several
     * classes.
     */
    public static final String LOG_TAG = "Term";

    private final static String DEFAULT_INITIAL_COMMAND = null;

    /**
     * Our main view. Displays the emulated terminal screen.
     */
    private EmulatorView emulatorView;

    /**
     * A key listener that tracks the modifier keys and allows the full ASCII
     * character set to be entered.
     */
    private TermKeyListener keyListener;

    private int fontSize = 9;
    private int colorId = 2;
    private int controlKeyId = 0;

    private int controlKeyCode;

    private String initialCommand;

    private SharedPreferences prefs;


    protected abstract Connection createConnection();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.e(Term.LOG_TAG, "onCreate");
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
        this.readPrefs();

        this.setContentView(R.layout.main);

        this.emulatorView = (EmulatorView) findViewById(EMULATOR_VIEW);

        this.connection = this.createConnection();

        this.emulatorView.init(this.connection);

        this.sendInitialCommand();

        this.keyListener = new TermKeyListener();

        this.emulatorView.setFocusable(true);
        this.emulatorView.setFocusableInTouchMode(true);
        this.emulatorView.requestFocus();
        this.emulatorView.register(this.keyListener);

        this.updatePrefs();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTermFd != null) {
            /****************************************************
             *            Exec.close(mTermFd);                  *
             ****************************************************/
            mTermFd = null;
        }
    }


    private void sendInitialCommand() {
        String initialCommand = mInitialCommand;
        if (initialCommand == null || 0 == initialCommand.length()) {
            initialCommand = DEFAULT_INITIAL_COMMAND;
        }
        if (null != initialCommand && 0 < initialCommand.length()) {
            this.send(initialCommand + '\n');
        }
    }

    private void restart() {
        this.startActivity(getIntent());
        this.finish();
    }

    private void send(String data) {

    }

    private ArrayList<String> parse(String cmd) {
        final int PLAIN = 0;
        final int WHITESPACE = 1;
        final int INQUOTE = 2;
        int state = WHITESPACE;
        ArrayList<String> result =  new ArrayList<String>();
        int cmdLen = cmd.length();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < cmdLen; i++) {
            char c = cmd.charAt(i);
            if (state == PLAIN) {
                if (Character.isWhitespace(c)) {
                    result.add(builder.toString());
                    builder.delete(0,builder.length());
                    state = WHITESPACE;
                } else if (c == '"') {
                    state = INQUOTE;
                } else {
                    builder.append(c);
                }
            } else if (state == WHITESPACE) {
                if (Character.isWhitespace(c)) {
                    // do nothing
                } else if (c == '"') {
                    state = INQUOTE;
                } else {
                    state = PLAIN;
                    builder.append(c);
                }
            } else if (state == INQUOTE) {
                if (c == '\\') {
                    if (i + 1 < cmdLen) {
                        i += 1;
                        builder.append(cmd.charAt(i));
                    }
                } else if (c == '"') {
                    state = PLAIN;
                } else {
                    builder.append(c);
                }
            }
        }
        if (builder.length() > 0) {
            result.add(builder.toString());
        }
        return result;
    }

    private void readPrefs() {
        mFontSize = readIntPref(FONTSIZE_KEY, mFontSize, 20);
        mColorId = readIntPref(COLOR_KEY, mColorId, COLOR_SCHEMES.length - 1);
        mControlKeyId = readIntPref(CONTROLKEY_KEY, mControlKeyId,
                CONTROL_KEY_SCHEMES.length - 1);
        {
            String newInitialCommand = readStringPref(INITIALCOMMAND_KEY,
                    mInitialCommand);
            if ((newInitialCommand == null)
                    || ! newInitialCommand.equals(mInitialCommand)) {
                if (mInitialCommand != null) {
                    Log.i(Term.LOG_TAG, "New initial command set. Restarting.");
                    restart();
                }
                mInitialCommand = newInitialCommand;
            }
        }
    }

    private void updatePrefs() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mEmulatorView.setTextSize((int) (mFontSize * metrics.density));
        setColors();
        mControlKeyCode = CONTROL_KEY_SCHEMES[mControlKeyId];
    }

    private int readIntPref(String key, int defaultValue, int maxValue) {
        int val;
        try {
            val = Integer.parseInt(
                mPrefs.getString(key, Integer.toString(defaultValue)));
        } catch (NumberFormatException e) {
            val = defaultValue;
        }
        val = Math.max(0, Math.min(val, maxValue));
        return val;
    }

    private String readStringPref(String key, String defaultValue) {
        return mPrefs.getString(key, defaultValue);
    }

    @Override
    public void onResume() {
        super.onResume();
        readPrefs();
        updatePrefs();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        mEmulatorView.updateSize();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (handleControlKey(keyCode, true)) {
            return true;
        } else if (isSystemKey(keyCode, event)) {
            // Don't intercept the system keys
            return super.onKeyDown(keyCode, event);
        } else if (handleDPad(keyCode, true)) {
            return true;
        }

        // Translate the keyCode into an ASCII character.
        int letter = mKeyListener.keyDown(keyCode, event);

        if (letter >= 0) {
            try {
                mTermOut.write(letter);
            } catch (IOException e) {
                // Ignore I/O exceptions
            }
        }
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (handleControlKey(keyCode, false)) {
            return true;
        } else if (isSystemKey(keyCode, event)) {
            // Don't intercept the system keys
            return super.onKeyUp(keyCode, event);
        } else if (handleDPad(keyCode, false)) {
            return true;
        }

        mKeyListener.keyUp(keyCode);
        return true;
    }

    private boolean handleControlKey(int keyCode, boolean down) {
        if (keyCode == mControlKeyCode) {
            mKeyListener.handleControlKey(down);
            return true;
        }
        return false;
    }

    /**
     * Handle dpad left-right-up-down events. Don't handle
     * dpad-center, that's our control key.
     * @param keyCode
     * @param down
     */
    private boolean handleDPad(int keyCode, boolean down) {
        if (keyCode < KeyEvent.KEYCODE_DPAD_UP ||
                keyCode > KeyEvent.KEYCODE_DPAD_CENTER) {
            return false;
        }

        if (down) {
            try {
                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                    mTermOut.write('\r');
                } else {
                    char code;
                    switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_UP:
                        code = 'A';
                        break;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        code = 'B';
                        break;
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        code = 'D';
                        break;
                    default:
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        code = 'C';
                        break;
                    }
                    mTermOut.write(27); // ESC
                    if (mEmulatorView.getKeypadApplicationMode()) {
                        mTermOut.write('O');
                    } else {
                        mTermOut.write('[');
                    }
                    mTermOut.write(code);
                }
            } catch (IOException e) {
                // Ignore
            }
        }
        return true;
    }

    private boolean isSystemKey(int keyCode, KeyEvent event) {
        return event.isSystem();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_preferences) {
            doPreferences();
        }
        else if (id == R.id.menu_reset) {
            doResetTerminal();
        }
        return super.onOptionsItemSelected(item);
    }

    protected abstract void doPreferences();

    protected void doResetTerminal() {
        restart();
    }

    private void setColors() {
        int[] scheme = COLOR_SCHEMES[mColorId];
        mEmulatorView.setColors(scheme[0], scheme[1]);
    }
}
