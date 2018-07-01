package me.vxti.core.opengl.component.components.text_box;

import me.vxti.core.opengl.component.Interactable;
import me.vxti.core.opengl.font.CFontRenderer;
import me.vxti.core.opengl.misc.DisplayManager;
import me.vxti.core.opengl.misc.text_highlighting.ITextHighlighting;
import me.vxti.core.opengl.misc.text_highlighting.TextParseResult;
import me.vxti.core.util.math.Dimension;
import me.vxti.core.util.math.MathHelper;
import me.vxti.core.util.math.vector.vec2;
import me.vxti.core.util.misc.ScrollListner;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.*;
import java.util.List;

import static me.vxti.Core.ctrlDown;
import static me.vxti.Core.shiftDown;
import static org.lwjgl.input.Keyboard.*;

public class Textbox2D extends Interactable {


    private static Map<String, CFontRenderer> font_map = new HashMap<>();

    // The font of the text renderer.
    private CFontRenderer font;

    // The amount of lines the text contains
    private int line_count;

    // The position of the line you're on right now
    private int line_position;

    // The relative line position of the current line
    private int line_pointer;

    // Some animation shit
    private vec2 pointer_position;

    // The array storing the lines.
    private List<String> lines;

    // The time representing the last time the pointer blinked.
    private long last_fade;

    // Whether there should be numbers next to the lines.
    private boolean show_line_numbers = false;

    // The color of the background
    private int background_color = 0xFFF0F0F0;

    // The color of the text
    private int text_color = 0xFF000000;

    // Whether the Text-Box is selected (For handling input)
    private boolean selected;

    // Whether or not to render a rounded border.
    private int rounded = 0;

    // Whether or not to render the background.
    private boolean show_background = true;

    // The highlighting for the text, used in IDE's for example.
    private TextParseResult default_result = new TextParseResult(text_color, 1, false);
    private ITextHighlighting text_highlighting = (a, b, c) -> default_result;

    // The scrollbars on the side, for when the text overlaps the dimensions
    private ScrollListner horizontal, vertical;

    // The width of the longest string in the list of lines.
    private int max_text_width;

    private vec2    selection_start = new vec2(-1, -1),
                    selection_end   = new vec2(-1, -1);


    /**
     * The main constructor for the textbox.
     */
    public Textbox2D(String text, Dimension dimensions) {
        super(dimensions);

        setSelected(true);
        setFont("Courier New");
        showLineNumbers(false);

        String[] data = text.split("\n");
        this.line_count = data.length;
        this.lines = new ArrayList<>();
        lines.addAll(Arrays.asList(data));

        this.max_text_width = dimensions.w-4;

        this.horizontal = new ScrollListner(dimensions.w, dimensions.w);
        this.vertical   = new ScrollListner(dimensions.h, dimensions.h);

        this.line_position = 0;
        this.line_pointer = 0;
        this.pointer_position = new vec2(0, 0);
        this.last_fade = System.currentTimeMillis();
    }

    /**
     * The function used for rendering the textbox.
     */
    public @Override void draw(int mouseX, int mouseY) {

        int line_nr_offset = show_line_numbers ? 40 : 0;

        this.horizontal.updateSizes(dimension.w, max_text_width + line_nr_offset + 6);
        this.vertical.updateSizes(dimension.h, (line_count * line_height()));

        this.horizontal.update();
        this.vertical.update();

        if (show_background)
            rounded_rect(dimension.x, dimension.y, dimension.x + dimension.w, dimension.y + dimension.h, rounded, background_color);

        if (show_line_numbers)
            rect(dimension.x + line_nr_offset-1, dimension.y, dimension.x + line_nr_offset, dimension.y + dimension.h, 0xff484848);

        start_scissors(dimension.x, dimension.y, dimension.w, dimension.h);
        if (show_line_numbers)
            for (int i = 0; i < line_count; i++) font.drawString(String.valueOf(i + 1), dimension.x + line_nr_offset - 10 - font.getStringWidth(String.valueOf(i+1)), dimension.y + 4 + line_height()*i + vertical.getScroll(), text_color);

        start_scissors(dimension.x + line_nr_offset, dimension.y, dimension.w - line_nr_offset, dimension.h);

        for (int i = 0; i < lines.size(); i++)
        {

            if (4 + line_height()*i + vertical.getScroll() > dimension.h)
                break;

            if (4 + line_height() * (i+1) + vertical.getScroll() < 0)
                continue;

            font.drawString(lines.get(i), dimension.x + 5 + line_nr_offset + horizontal.getScroll(), dimension.y + 4 + line_height()*i + vertical.getScroll(), text_highlighting);
        }

        if (is_selecting())
        {

            vec2 min = selection_start.ix > selection_end.ix ? selection_end : selection_start;
            vec2 max = selection_start.ix > selection_end.ix ? selection_start : selection_end;

            if (max.ix - min.ix> 1) {

                // Rendering the line from L2R
                rect(dimension.x + line_nr_offset + 5 + horizontal.getScroll() + font.getStringWidth(lines.get(min.ix).substring(0, min.iy)),
                        dimension.y + min.ix * line_height() + vertical.getScroll(),
                        dimension.x + dimension.w,
                        dimension.y + (min.ix+1) * line_height() + vertical.getScroll(),
                        0x40366DBF);

                rect(dimension.x + line_nr_offset + 5,
                        dimension.y + max.ix * line_height() + vertical.getScroll(),
                        dimension.x + line_nr_offset + 5 + font.getStringWidth(lines.get(max.ix).substring(0, max.iy)) + horizontal.getScroll(),
                        dimension.y + (max.ix+1) * line_height() + vertical.getScroll(),
                        0x40366DBF);



                // Rendering the lines inbetween
                rect(dimension.x + line_nr_offset + 5 + horizontal.getScroll(),
                        dimension.y + (min.ix+1)*line_height() + vertical.getScroll(),
                        dimension.x + dimension.w,
                        dimension.y + (max.ix)*line_height() + vertical.getScroll(),
                        0x40366DBF);
            }
            else
            {

                if (max.ix - min.ix > 0)
                {
                    // Rendering the line from L2R
                    rect(dimension.x + line_nr_offset + 5 + horizontal.getScroll() + font.getStringWidth(lines.get(min.ix).substring(0, min.iy)),
                            dimension.y + min.ix * line_height() + vertical.getScroll(),
                            dimension.x + dimension.w,
                            dimension.y + (min.ix+1) * line_height() + vertical.getScroll(),
                            0x40366DBF);

                    rect(dimension.x + line_nr_offset + 5,
                            dimension.y + max.ix * line_height() + vertical.getScroll(),
                            dimension.x + line_nr_offset + 5 + font.getStringWidth(lines.get(max.ix).substring(0, max.iy)) + horizontal.getScroll(),
                            dimension.y + (max.ix+1) * line_height() + vertical.getScroll(),
                            0x40366DBF);
                }
                else
                {
                    rect(
                            dimension.x + line_nr_offset + 5 + font.getStringWidth(lines.get(min.ix).substring(0, min.iy)) + horizontal.getScroll(),

                            dimension.y + min.ix * line_height() + vertical.getScroll(),

                            dimension.x + line_nr_offset + 5 + font.getStringWidth(lines.get(max.ix).substring(0, max.iy)) + horizontal.getScroll(),

                            dimension.y + (max.ix + 1) * line_height() + vertical.getScroll(),
                            0x40366DBF);
                }

            }
        }


        pointer_position.translate_x(
                pointer_position.x + (font.getStringWidth(lines.get(line_position).substring(0, line_pointer)) - pointer_position.x) * DisplayManager.SMOOTH_OFFSET
        );

        pointer_position.translate_y(
                pointer_position.y + (line_height()*line_position - pointer_position.y) * DisplayManager.SMOOTH_OFFSET
        );


        if ((System.currentTimeMillis() - last_fade < 400 || (isKeyDown(KEY_UP) || isKeyDown(KEY_DOWN) || isKeyDown(KEY_LEFT) || isKeyDown(KEY_RIGHT))) && selected)
        {
            rect(dimension.x + pointer_position.fx + 4 + line_nr_offset + horizontal.getScroll(),
                    dimension.y + pointer_position.fy + vertical.getScroll() + 4,
                    dimension.x + pointer_position.fx + 5 + line_nr_offset + horizontal.getScroll(),
                    dimension.y + pointer_position.fy + line_height() +  vertical.getScroll(),
                    text_color);
        }
        else if (System.currentTimeMillis() - last_fade > 800)
        {
            last_fade = System.currentTimeMillis();
        }

        stop_scissors();


    }


    private enum KeyAction { DOWN, UP, LEFT, RIGHT, RETURN, HOME, END, DEL_LEFT, DEL_RIGHT }


    /**
     * Methods for basic key functions
     */
    private void KeyAction(KeyAction action) {

        int _cur_pointer = this.line_pointer;
        int _cur_line    = this.line_position;

        switch (action)
        {
            case UP:
                int prev1 = line_position;

                line_position = (line_position - 1 < 0 ? 0 : line_position - 1);
                int len1 = lines.get(line_position).length();

                if (line_pointer == lines.get(prev1).length())
                {
                    line_pointer = len1;
                }
                else if (line_pointer > len1)
                {
                    line_pointer = len1;
                }
                select(_cur_line, _cur_pointer);
                break;

            case DOWN:
                int prev2 = line_position;

                line_position = (line_position + 1 > line_count - 1 ? line_count - 1 : line_position + 1);

                int len2 = lines.get(line_position).length();

                if (line_pointer == lines.get(prev2).length())
                {
                    line_pointer = len2;
                }
                else if (line_pointer > len2)
                {
                    line_pointer = len2;
                }
                select(_cur_line, _cur_pointer);
                break;

            case LEFT:
                // Checking if you can go left
                if (line_pointer - 1 < 0)
                {
                    // Check if the line position is higher than 0, if so, move one line up
                    if (line_position > 0)
                    {
                        line_pointer = lines.get(--line_position).length();
                    }
                    else
                    {
                        line_pointer = 0;
                    }
                }
                else
                {
                    line_pointer--;
                }

                select(_cur_line, _cur_pointer);

                break;

            case RIGHT:
                if (line_pointer + 1 > lines.get(line_position).length())
                {
                    if (line_position + 1 < line_count)
                    {
                        line_position++;
                        line_pointer = 0;
                    }
                }
                else
                {
                    if (line_pointer + 1 <= lines.get(line_position).length()) line_pointer++;
                }

                select(_cur_line, _cur_pointer);

                break;

            case RETURN:
                int cur = line_position;
                String line1 = lines.get(cur);

                lines.set(cur, line1.substring(0, line_pointer));
                lines.add(cur+1, line1.substring(line_pointer, line1.length()));

                line_position++;
                line_pointer = 0;
                break;

            case HOME:
                this.line_pointer = 0;
                select(_cur_line, _cur_pointer);
                break;

            case END:
                this.line_pointer = lines.get(line_position).length();
                select(_cur_line, _cur_pointer);
                break;

            case DEL_LEFT:

                if (is_selecting())
                {
                    delete_selection();
                    return;
                }

                String line = lines.get(line_position);
                if (line_pointer == 0)
                {
                    if (lines.size() == 1)
                    {
                        return;
                    }

                    if (line_position > 0)
                    {
                        String prev = lines.get(line_position - 1) + line;
                        lines.set(line_position - 1, prev);
                        lines.remove(line_position);

                        line_position--;
                        line_pointer = prev.length();

                        return;
                    }

                    KeyAction(KeyAction.LEFT);

                    return;
                }
                int len = line.length();


                if (ctrlDown())
                {
                    line = line.substring(0, Math.max(line.substring(0, line_pointer).lastIndexOf(' '), 0)) + line.substring(line_pointer);
                }
                else
                {
                    line = line.substring(0, Math.max(line_pointer-1, 0)) + line.substring(line_pointer);
                }
                lines.set(line_position, line);
                line_pointer -= len - line.length();
                break;

            case DEL_RIGHT:

                if (is_selecting())
                {
                    delete_selection();
                    return;
                }

                String line2 = lines.get(line_position);

                if (line_pointer == line2.length())
                {
                    if (line_position < lines.size() - 1)
                    {
                        String line_add = line2 + lines.get(line_position + 1);
                        lines.remove(line_position + 1);
                        lines.set(line_position, line_add);
                    }
                    return;
                }

                if (ctrlDown())
                {
                    String s_text = line2.substring(line_pointer + 1);
                    line2 = line2.substring(0, line_pointer) + ((s_text.indexOf(' ') == -1)  ? "" : s_text.substring(s_text.indexOf(' ')+1));
                }
                else
                {
                    line2 = line2.substring(0, Math.max(line_pointer, 0)) + line2.substring(line_pointer+1);
                }
                lines.set(line_position, line2);
                break;
        }
    }


    /**
     *  The method for typing several characters.
     */
    private void CharTyped(char... character) {
        StringBuilder chars = new StringBuilder();
        for (char c  : character)
        {
            if ((c >= 0x20 && c <= 0x3C9 || c == '\n'))
            {
                chars.append(c);
            }
        }
        String add_s  = chars.toString();

        if (add_s.contains("\n"))
        {
            String[] lines = add_s.split("\n");
            for (String line_ : lines)
            {
                CharTyped(line_.toCharArray());
                KeyAction(KeyAction.RETURN);
            }
        }
        else
        {

            // This is where the magic happens.

            if (is_selecting())
                delete_selection();

            String line = lines.get(line_position);
            String add = line.substring(0, line_pointer) + chars.toString() + line.substring(line_pointer);
            lines.set(line_position, add);
            line_pointer += chars.length();
        }

    }

    /**
     * Returns the height of the lines.
     */
    private int line_height() { return (int) (font.getHeight() * 1.1F); }

    /**
     * Returns whether or not there's text selected.
     */
    private boolean is_selecting() { return selection_start.x > -1 && selection_start.y > -1 && selection_end.x > -1 && selection_end.y > -1 && selection_start.compare(selection_end) != 0; }

    /**
     * Stops selecting text.
     */
    private void stop_selecting() { this.selection_start.translate(-1, -1); this.selection_end.translate(-1, -1); }

    /**
     * Moves the selection from line l_nr to the current nr, and line_pos from l_pos to the current position.
     */
    private void select(int l_nr, int l_pos) {
        if (shiftDown())
        {
            if (is_selecting())
            {
                this.selection_end.translate(line_position, line_pointer);
            }
            else
            {
                this.selection_start.translate(l_nr, l_pos);
                this.selection_end.translate(line_position, line_pointer);
            }
        }
        else if (is_selecting())
        {
            stop_selecting();
        }
    }

    private String get_selection() {
        vec2 min = selection_start.ix > selection_end.ix ? selection_end : selection_start;
        vec2 max = selection_start.ix > selection_end.ix ? selection_start : selection_end;

        StringBuilder _lines = new StringBuilder();

        if (max.ix - min.ix > 1)
        {
            _lines.append(lines.get(min.ix).substring(0, min.iy) + "\n");

            for (int line = min.ix + 1; line < max.ix; line++) {
                _lines.append(lines.get(line) + "\n");
            }

            _lines.append(lines.get(max.ix).substring(max.iy));
        }
        else if (max.ix - min.ix > 0)
        {
            _lines.append(lines.get(min.ix).substring(0, min.iy) + "\n");
            _lines.append(lines.get(max.ix).substring(max.iy));
        }
        else
        {
            _lines.append(lines.get(min.ix).substring(0, min.iy) + lines.get(min.ix).substring(max.iy));
        }
        return _lines.toString();
    }

    /**
     * Deletes the current selection.
     */
    private void delete_selection() {
        vec2 min = selection_start.ix > selection_end.ix ? selection_end : selection_start;
        vec2 max = selection_start.ix > selection_end.ix ? selection_start : selection_end;

        if (max.ix - min.ix > 1)
        {
            lines.set(min.ix, lines.get(min.ix).substring(0, min.iy));
            lines.set(max.ix, lines.get(max.ix).substring(max.iy));

            for (int line = min.ix + 1; line < max.ix; line++) {
                lines.remove(min.ix+1);
            }

        }
        else if (max.ix - min.ix > 0)
        {
            lines.set(min.ix, lines.get(min.ix).substring(0, min.iy));
            lines.set(max.ix, lines.get(max.ix).substring(max.iy));
        }
        else
        {
           lines.set(min.ix, lines.get(min.ix).substring(0, min.iy) + lines.get(min.ix).substring(max.iy));
        }

        line_count = lines.size();

        line_position = min.iy-1;
        line_pointer = min.ix;


        stop_selecting();
    }


    private void CtrlAction(int key) {
        switch (key)
        {
            case KEY_V:

                String _clipboard = Sys.getClipboard();

                if (_clipboard == null || _clipboard.isEmpty())
                    break;

                CharTyped(_clipboard.toCharArray());

                break;

            case KEY_A:

                selection_start.translate(0, 0);
                selection_end.translate(line_count-1, lines.get(line_count-1).length());

                break;

            case KEY_C:
                if (is_selecting())
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(get_selection()), null);

                break;
        }
    }

    /*
     * The method for handling key events.
     */
    public @Override void onKeyPress(int key, char character) {
        if (!selected)
            return;

        switch (key)
        {
            case KEY_UP:        KeyAction(KeyAction.UP);        break;
            case KEY_DOWN:      KeyAction(KeyAction.DOWN);      break;
            case KEY_LEFT:      KeyAction(KeyAction.LEFT);      break;
            case KEY_RIGHT:     KeyAction(KeyAction.RIGHT);     break;
            case KEY_RETURN:    KeyAction(KeyAction.RETURN);    break;

            case KEY_HOME:      KeyAction(KeyAction.HOME);      break;
            case KEY_END:       KeyAction(KeyAction.END);       break;
            case KEY_DELETE:    KeyAction(KeyAction.DEL_RIGHT); break;
            case KEY_BACK:      KeyAction(KeyAction.DEL_LEFT);  break;

            case KEY_TAB:
                CharTyped(' ', ' ', ' ', ' ');
                break;

            default:

                if (ctrlDown())
                {
                    CtrlAction(key);
                }
                else
                {
                    if (character >= 0x20 && character <= 0x3C9)
                    {
                        CharTyped(character);
                    }
                }

                break;

        }

        int biggest = dimension.w;
        for (String s : lines)
        {
            if (font.getStringWidth(s) > biggest)
                biggest = font.getStringWidth(s);
        }
        this.max_text_width = biggest;

        line_count = lines.size();


        if (!MathHelper.inBetween(font.getStringWidth(lines.get(line_position).substring(0, line_pointer)), -horizontal.getScroll(), dimension.w - horizontal.getScroll()))
        {
            horizontal.scroll((int) (-horizontal.getScroll() - font.getStringWidth(lines.get(line_position).substring(0, line_pointer))));
        }


    }


    /*
     * The method for handling mouse events
     */
    public @Override void onMousePress(int mx, int my, int btn) {
        if ((setSelected(dimension.intersects(mx, my))) && btn == 0)
        {

            vec2 cur = new vec2(line_position, line_pointer);

            int line_nr_offset = show_line_numbers ? 40 : 0;

            vec2 difference = new vec2(
                    (mx-line_nr_offset) - dimension.x - horizontal.getScroll() - 1,
                    my - dimension.y -  vertical.getScroll() + 4
            );

            int y_index = (int) ((1.0F / (line_height())) * difference.y);
            y_index = MathHelper.clampi(y_index, 0, line_count - 1);

            int x_index = (int) (((float) lines.get(y_index).length() / (float) font.getStringWidth(lines.get(y_index))) * difference.x);
            x_index = MathHelper.clampi(x_index, 0, lines.get(y_index).length());

            this.line_position = y_index;
            this.line_pointer = x_index;

            if (shiftDown())
            {
                selection_start.translate(cur.x, cur.y);
                selection_end.translate(line_position, line_pointer);
            }
            else
            {
                stop_selecting();
            }


        }


    }

    /*
     * The method for handling scroll events.
     */
    public @Override void onScroll(int scroll) {
        if (shiftDown())
        {
            horizontal.scroll(scroll);
        }
        else
        {
            vertical.scroll(scroll);
        }
    }



    /**
     * Getter and setter methods.
     */
    public int getTextColor() { return text_color; }

    public void setTextColor(int color) {
        this.text_color = color;
        this.default_result.color = color;
    }

    public String getText() {

        StringBuilder builder = new StringBuilder();
        lines.forEach((o) -> builder.append(o).append("\n"));
        return builder.toString();
    }

    public void setText(String... lines) {
        this.lines.clear();
        this.lines.addAll(Arrays.asList(lines));
    }

    public void showLineNumbers(boolean flag) {
        this.show_line_numbers = flag;

    }

    public boolean showBackground(boolean flag) {
        return this.show_background = flag;
    }

    public void setFont(String font) {
        if (font_map.containsKey(font))
        {
            this.font = font_map.get(font);

        }
        else
        {
            font_map.put(font, this.font = new CFontRenderer(new Font(font, Font.PLAIN, 13), true, false));

        }
    }

    public CFontRenderer getFont() { return this.font; }

    public boolean setSelected(boolean flag) {
        Keyboard.enableRepeatEvents(flag);
        return this.selected = flag;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setTextHighlighting(ITextHighlighting highlighting) { this.text_highlighting = highlighting; }

    public @Override int unique_id() {
        return 0x01F;
    }
}
