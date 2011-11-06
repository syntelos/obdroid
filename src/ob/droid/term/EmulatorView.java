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

import android.content.Context;
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

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.util.AttributeSet;
import android.util.Log;

import android.view.View;
import android.view.GestureDetector;
import android.view.KeyEvent;

import android.view.MotionEvent;

import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import ob.droid.R;

/**
 * A view on a transcript and a terminal emulator. Displays the text of the
 * transcript and the current cursor position of the terminal emulator.
 */
class EmulatorView extends View implements GestureDetector.OnGestureListener {

    /**
     * We defer some initialization until we have been layed out in the view
     * hierarchy. The boolean tracks when we know what our size is.
     */
    private boolean mKnownSize;

    /**
     * Our transcript. Contains the screen and the transcript.
     */
    private TranscriptScreen mTranscriptScreen;

    /**
     * Number of rows in the transcript.
     */
    private static final int TRANSCRIPT_ROWS = 10000;

    /**
     * Total width of each character, in pixels
     */
    private int mCharacterWidth;

    /**
     * Total height of each character, in pixels
     */
    private int mCharacterHeight;

    /**
     * Used to render text
     */
    private TextRenderer mTextRenderer;

    /**
     * Text size. Zero means 4 x 8 font.
     */
    private int mTextSize;

    /**
     * Foreground color.
     */
    private int mForeground;

    /**
     * Background color.
     */
    private int mBackground;

    /**
     * Used to paint the cursor
     */
    private Paint mCursorPaint;

    private Paint mBackgroundPaint;

    /**
     * Our terminal emulator. We use this to get the current cursor position.
     */
    private TerminalEmulator mEmulator;

    /**
     * The number of rows of text to display.
     */
    private int mRows;

    /**
     * The number of columns of text to display.
     */
    private int mColumns;

    /**
     * The number of columns that are visible on the display.
     */

    private int mVisibleColumns;

    /**
     * The top row of text to display. Ranges from -activeTranscriptRows to 0
     */
    private int mTopRow;

    private int mLeftColumn;

    private FileDescriptor mTermFd;
    /**
     * Used to receive data from the remote process.
     */
    private FileInputStream mTermIn;

    private FileOutputStream mTermOut;

    private ByteQueue mByteQueue;

    /**
     * Used to temporarily hold data received from the remote process. Allocated
     * once and used permanently to minimize heap thrashing.
     */
    private byte[] mReceiveBuffer;

    /**
     * Our private message id, which we use to receive new input from the
     * remote process.
     */
    private static final int UPDATE = 1;

    /**
     * Thread that polls for input from the remote process
     */

    private Thread mPollingThread;

    private GestureDetector mGestureDetector;
    private float mScrollRemainder;
    private TermKeyListener mKeyListener;

    /**
     * Our message handler class. Implements a periodic callback.
     */
    private final Handler mHandler = new Handler() {
        /**
         * Handle the callback message. Call our enclosing class's update
         * method.
         *
         * @param msg The callback message.
         */
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == UPDATE) {
                update();
            }
        }
    };

    public EmulatorView(Context context) {
        super(context);
        commonConstructor(context);
    }

    public void register(TermKeyListener listener) {
        mKeyListener = listener;
    }

    public void setColors(int foreground, int background) {
        mForeground = foreground;
        mBackground = background;
        updateText();
    }

    public String getTranscriptText() {
        return mEmulator.getTranscriptText();
    }

    public void resetTerminal() {
        mEmulator.reset();
        invalidate();
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return new BaseInputConnection(this, false) {

            @Override
            public boolean beginBatchEdit() {
                return true;
            }

            @Override
            public boolean clearMetaKeyStates(int states) {
                return true;
            }

            @Override
            public boolean commitCompletion(CompletionInfo text) {
                return true;
            }

            @Override
            public boolean commitText(CharSequence text, int newCursorPosition) {
                sendText(text);
                return true;
            }

            @Override
            public boolean deleteSurroundingText(int leftLength, int rightLength) {
                return true;
            }

            @Override
            public boolean endBatchEdit() {
                return true;
            }

            @Override
            public boolean finishComposingText() {
                return true;
            }

            @Override
            public int getCursorCapsMode(int reqModes) {
                return 0;
            }

            @Override
            public ExtractedText getExtractedText(ExtractedTextRequest request,
                    int flags) {
                return null;
            }

            @Override
            public CharSequence getTextAfterCursor(int n, int flags) {
                return null;
            }

            @Override
            public CharSequence getTextBeforeCursor(int n, int flags) {
                return null;
            }

            @Override
            public boolean performEditorAction(int actionCode) {
                if(actionCode == EditorInfo.IME_ACTION_UNSPECIFIED) {
                    // The "return" key has been pressed on the IME.
                    sendText("\n");
                    return true;
                }
                return false;
            }

            @Override
            public boolean performContextMenuAction(int id) {
                return true;
            }

            @Override
            public boolean performPrivateCommand(String action, Bundle data) {
                return true;
            }

            @Override
            public boolean sendKeyEvent(KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch(event.getKeyCode()) {
                    case KeyEvent.KEYCODE_DEL:
                        sendChar(127);
                        break;
                    }
                }
                return true;
            }

            @Override
            public boolean setComposingText(CharSequence text, int newCursorPosition) {
                return true;
            }

            @Override
            public boolean setSelection(int start, int end) {
                return true;
            }

            private void sendChar(int c) {
                try {
                    mapAndSend(c);
                } catch (IOException ex) {

                }
            }
            private void sendText(CharSequence text) {
                int n = text.length();
                try {
                    for(int i = 0; i < n; i++) {
                        char c = text.charAt(i);
                        mapAndSend(c);
                    }
                } catch (IOException e) {
                }
            }

            private void mapAndSend(int c) throws IOException {
                mTermOut.write(
                        mKeyListener.mapControlChar(c));
            }
        };
    }

    public boolean getKeypadApplicationMode() {
        return mEmulator.getKeypadApplicationMode();
    }

    public EmulatorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EmulatorView(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a =
                context.obtainStyledAttributes(android.R.styleable.View);
        initializeScrollbars(a);
        a.recycle();
        commonConstructor(context);
    }

    private void commonConstructor(Context context) {
        mTextRenderer = null;
        mCursorPaint = new Paint();
        mCursorPaint.setARGB(255,128,128,128);
        mBackgroundPaint = new Paint();
        mTopRow = 0;
        mLeftColumn = 0;
        mGestureDetector = new GestureDetector(context, this, null);
        mGestureDetector.setIsLongpressEnabled(false);
        setVerticalScrollBarEnabled(true);
    }

    @Override
    protected int computeVerticalScrollRange() {
        return mTranscriptScreen.getActiveRows();
    }

    @Override
    protected int computeVerticalScrollExtent() {
        return mRows;
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return mTranscriptScreen.getActiveRows() + mTopRow - mRows;
    }

    /**
     * Call this to initialize the view.
     *
     * @param termFd the file descriptor
     * @param termOut the output stream for the pseudo-teletype
     */
    public void initialize(FileDescriptor termFd, FileOutputStream termOut) {
        mTermOut = termOut;
        mTermFd = termFd;
        mTextSize = 10;
        mForeground = Term.WHITE;
        mBackground = Term.BLACK;
        updateText();
        mTermIn = new FileInputStream(mTermFd);
        mReceiveBuffer = new byte[4 * 1024];
        mByteQueue = new ByteQueue(4 * 1024);
    }

    /**
     * Accept a sequence of bytes (typically from the pseudo-tty) and process
     * them.
     *
     * @param buffer a byte array containing bytes to be processed
     * @param base the index of the first byte in the buffer to process
     * @param length the number of bytes to process
     */
    public void append(byte[] buffer, int base, int length) {
        mEmulator.append(buffer, base, length);
        ensureCursorVisible();
        invalidate();
    }

    /**
     * Page the terminal view (scroll it up or down by delta screenfulls.)
     *
     * @param delta the number of screens to scroll. Positive means scroll down,
     *        negative means scroll up.
     */
    public void page(int delta) {
        mTopRow =
                Math.min(0, Math.max(-(mTranscriptScreen
                        .getActiveTranscriptRows()), mTopRow + mRows * delta));
        invalidate();
    }

    /**
     * Page the terminal view horizontally.
     *
     * @param deltaColumns the number of columns to scroll. Positive scrolls to
     *        the right.
     */
    public void pageHorizontal(int deltaColumns) {
        mLeftColumn =
                Math.max(0, Math.min(mLeftColumn + deltaColumns, mColumns
                        - mVisibleColumns));
        invalidate();
    }

    /**
     * Sets the text size, which in turn sets the number of rows and columns
     *
     * @param fontSize the new font size, in pixels.
     */
    public void setTextSize(int fontSize) {
        mTextSize = fontSize;
        updateText();
    }

    // Begin GestureDetector.OnGestureListener methods

    public boolean onSingleTapUp(MotionEvent e) {
        return true;
    }

    public void onLongPress(MotionEvent e) {
    }

    public boolean onScroll(MotionEvent e1, MotionEvent e2,
            float distanceX, float distanceY) {
        distanceY += mScrollRemainder;
        int deltaRows = (int) (distanceY / mCharacterHeight);
        mScrollRemainder = distanceY - deltaRows * mCharacterHeight;
        mTopRow =
            Math.min(0, Math.max(-(mTranscriptScreen
                    .getActiveTranscriptRows()), mTopRow + deltaRows));
        invalidate();

        return true;
   }

    public void onSingleTapConfirmed(MotionEvent e) {
    }

    public boolean onJumpTapDown(MotionEvent e1, MotionEvent e2) {
       // Scroll to bottom
       mTopRow = 0;
       invalidate();
       return true;
    }

    public boolean onJumpTapUp(MotionEvent e1, MotionEvent e2) {
        // Scroll to top
        mTopRow = -mTranscriptScreen.getActiveTranscriptRows();
        invalidate();
        return true;
    }

    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
            float velocityY) {
        // TODO: add animation man's (non animated) fling
        mScrollRemainder = 0.0f;
        onScroll(e1, e2, 2 * velocityX, -2 * velocityY);
        return true;
    }

    public void onShowPress(MotionEvent e) {
    }

    public boolean onDown(MotionEvent e) {
        mScrollRemainder = 0.0f;
        return true;
    }

    // End GestureDetector.OnGestureListener methods

    @Override public boolean onTouchEvent(MotionEvent ev) {
        return mGestureDetector.onTouchEvent(ev);
    }

    private void updateText() {
        if (mTextSize > 0) {
            mTextRenderer = new PaintRenderer(mTextSize, mForeground,
                    mBackground);
        }
        else {
            mTextRenderer = new Bitmap4x8FontRenderer(getResources(),
                    mForeground, mBackground);
        }
        mBackgroundPaint.setColor(mBackground);
        mCharacterWidth = mTextRenderer.getCharacterWidth();
        mCharacterHeight = mTextRenderer.getCharacterHeight();

        if (mKnownSize) {
            updateSize(getWidth(), getHeight());
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        updateSize(w, h);
        if (!mKnownSize) {
            mKnownSize = true;

            // Set up a thread to read input from the
            // pseudo-teletype:

            mPollingThread = new Thread(new Runnable() {

                public void run() {
                    try {
                        while(true) {
                            int read = mTermIn.read(mBuffer);
                            mByteQueue.write(mBuffer, 0, read);
                            mHandler.sendMessage(
                                    mHandler.obtainMessage(UPDATE));
                        }
                    } catch (IOException e) {
                    } catch (InterruptedException e) {
                    }
                }
                private byte[] mBuffer = new byte[4096];
            });
            mPollingThread.setName("Input reader");
            mPollingThread.start();
        }
    }

    private void updateSize(int w, int h) {
        mColumns = w / mCharacterWidth;
        mRows = h / mCharacterHeight;
        /**************************************************************************
         *        // Inform the attached pty of our new size:                     *
         *        Exec.setPtyWindowSize(mTermFd, mRows, mColumns, w, h);          *
         **************************************************************************/

        if (mTranscriptScreen != null) {
            mEmulator.updateSize(mColumns, mRows);
        } else {
            mTranscriptScreen =
                    new TranscriptScreen(mColumns, TRANSCRIPT_ROWS, mRows, 0, 7);
            mEmulator =
                    new TerminalEmulator(mTranscriptScreen, mColumns, mRows,
                            mTermOut);
        }

        // Reset our paging:
        mTopRow = 0;
        mLeftColumn = 0;

        invalidate();
    }

    void updateSize() {
        if (mKnownSize) {
            updateSize(getWidth(), getHeight());
        }
    }

    /**
     * Look for new input from the ptty, send it to the terminal emulator.
     */
    private void update() {
        int bytesAvailable = mByteQueue.getBytesAvailable();
        int bytesToRead = Math.min(bytesAvailable, mReceiveBuffer.length);
        try {
            int bytesRead = mByteQueue.read(mReceiveBuffer, 0, bytesToRead);
            append(mReceiveBuffer, 0, bytesRead);
        } catch (InterruptedException e) {
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        canvas.drawRect(0, 0, w, h, mBackgroundPaint);
        mVisibleColumns = w / mCharacterWidth;
        float x = -mLeftColumn * mCharacterWidth;
        float y = mCharacterHeight;
        int endLine = mTopRow + mRows;
        int cx = mEmulator.getCursorCol();
        int cy = mEmulator.getCursorRow();
        for (int i = mTopRow; i < endLine; i++) {
            int cursorX = -1;
            if (i == cy) {
                cursorX = cx;
            }
            mTranscriptScreen.drawText(i, canvas, x, y, mTextRenderer, cursorX);
            y += mCharacterHeight;
        }
    }

    private void ensureCursorVisible() {
        mTopRow = 0;
        if (mVisibleColumns > 0) {
            int cx = mEmulator.getCursorCol();
            int visibleCursorX = mEmulator.getCursorCol() - mLeftColumn;
            if (visibleCursorX < 0) {
                mLeftColumn = cx;
            } else if (visibleCursorX >= mVisibleColumns) {
                mLeftColumn = (cx - mVisibleColumns) + 1;
            }
        }
    }
}
