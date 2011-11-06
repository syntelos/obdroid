package ob.droid.term;

/**
 * Text renderer interface
 */
interface TextRenderer {

    public int getCharacterWidth();
    public int getCharacterHeight();

    public void drawTextRun(Canvas canvas, 
                            float x, float y,
                            int lineOffset, char[] text,
                            int index, int count, 
                            boolean cursor, 
                            int foreColor, int backColor);
}
