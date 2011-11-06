/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.term;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;

/**
 * A terminal emulator activity.
 */
public class Term extends Activity {
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
     * The tag we use when logging, so that our messages can be distinguished
     * from other messages in the log. Public because it's used by several
     * classes.
     */
    public static final String LOG_TAG = "Term";

    /**
     * Our main view. Displays the emulated terminal screen.
     */
    private EmulatorView mEmulatorView;

    /**
     * The pseudo-teletype (pty) file descriptor that we use to communicate with
     * another process, typically a shell.
     */
    private FileDescriptor mTermFd;

    /**
     * Used to send data to the remote process.
     */
    private FileOutputStream mTermOut;

    /**
     * A key listener that tracks the modifier keys and allows the full ASCII
     * character set to be entered.
     */
    private TermKeyListener mKeyListener;

    /**
     * The name of our emulator view in the view resource.
     */
    private static final int EMULATOR_VIEW = R.id.emulatorView;

    private int mFontSize = 9;
    private int mColorId = 2;
    private int mControlKeyId = 0;

    private static final String FONTSIZE_KEY = "fontsize";
    private static final String COLOR_KEY = "color";
    private static final String CONTROLKEY_KEY = "controlkey";
    private static final String SHELL_KEY = "shell";
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

    private int mControlKeyCode;

    private final static String DEFAULT_SHELL = "/system/bin/sh -";
    private String mShell;

    private final static String DEFAULT_INITIAL_COMMAND =
        "export PATH=/data/local/bin:$PATH";
    private String mInitialCommand;

    private SharedPreferences mPrefs;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.e(Term.LOG_TAG, "onCreate");
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        readPrefs();

        setContentView(R.layout.term_activity);

        mEmulatorView = (EmulatorView) findViewById(EMULATOR_VIEW);

        startListening();

        mKeyListener = new TermKeyListener();

        mEmulatorView.setFocusable(true);
        mEmulatorView.setFocusableInTouchMode(true);
        mEmulatorView.requestFocus();
        mEmulatorView.register(mKeyListener);

        updatePrefs();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTermFd != null) {
            Exec.close(mTermFd);
            mTermFd = null;
        }
    }

    private void startListening() {
        int[] processId = new int[1];

        createSubprocess(processId);
        final int procId = processId[0];

        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
            }
        };

        Runnable watchForDeath = new Runnable() {

            public void run() {
                Log.i(Term.LOG_TAG, "waiting for: " + procId);
               int result = Exec.waitFor(procId);
                Log.i(Term.LOG_TAG, "Subprocess exited: " + result);
                handler.sendEmptyMessage(result);
             }

        };
        Thread watcher = new Thread(watchForDeath);
        watcher.start();

        mTermOut = new FileOutputStream(mTermFd);

        mEmulatorView.initialize(mTermFd, mTermOut);

        sendInitialCommand();
    }

    private void sendInitialCommand() {
        String initialCommand = mInitialCommand;
        if (initialCommand == null || initialCommand.equals("")) {
            initialCommand = DEFAULT_INITIAL_COMMAND;
        }
        if (initialCommand.length() > 0) {
            write(initialCommand + '\r');
        }
    }

    private void restart() {
        startActivity(getIntent());
        finish();
    }

    private void write(String data) {
        try {
            mTermOut.write(data.getBytes());
            mTermOut.flush();
        } catch (IOException e) {
            // Ignore exception
            // We don't really care if the receiver isn't listening.
            // We just make a best effort to answer the query.
        }
    }

    private void createSubprocess(int[] processId) {
        String shell = mShell;
        if (shell == null || shell.equals("")) {
            shell = DEFAULT_SHELL;
        }
        ArrayList<String> args = parse(shell);
        String arg0 = args.get(0);
        String arg1 = null;
        String arg2 = null;
        if (args.size() >= 2) {
            arg1 = args.get(1);
        }
        if (args.size() >= 3) {
            arg2 = args.get(2);
        }
        mTermFd = Exec.createSubprocess(arg0, arg1, arg2, processId);
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
            String newShell = readStringPref(SHELL_KEY, mShell);
            if ((newShell == null) || ! newShell.equals(mShell)) {
                if (mShell != null) {
                    Log.i(Term.LOG_TAG, "New shell set. Restarting.");
                    restart();
                }
                mShell = newShell;
            }
        }
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
        } else if (id == R.id.menu_reset) {
            doResetTerminal();
        } else if (id == R.id.menu_send_email) {
            doEmailTranscript();
        } else if (id == R.id.menu_special_keys) {
            doDocumentKeys();
        }
        return super.onOptionsItemSelected(item);
    }

    private void doPreferences() {
        startActivity(new Intent(this, TermPreferences.class));
    }

    private void setColors() {
        int[] scheme = COLOR_SCHEMES[mColorId];
        mEmulatorView.setColors(scheme[0], scheme[1]);
    }

    private void doResetTerminal() {
        restart();
    }

    private void doEmailTranscript() {
        // Don't really want to supply an address, but
        // currently it's required, otherwise we get an
        // exception.
        String addr = "user@example.com";
        Intent intent =
                new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"
                        + addr));

        intent.putExtra("body", mEmulatorView.getTranscriptText());
        startActivity(intent);
    }

    private void doDocumentKeys() {
        String controlKey = CONTROL_KEY_NAME[mControlKeyId];
        new AlertDialog.Builder(this).
            setTitle("Press " + controlKey + " and Key").
            setMessage(controlKey + " Space ==> Control-@ (NUL)\n"
                    + controlKey + " A..Z ==> Control-A..Z\n"
                    + controlKey + " 1 ==> Control-[ (ESC)\n"
                    + controlKey + " 5 ==> Control-_\n"
                    + controlKey + " . ==> Control-\\\n"
                    + controlKey + " 0 ==> Control-]\n"
                    + controlKey + " 6 ==> Control-^").
            show();
     }
}
