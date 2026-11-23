package com.example.zitraksmode.client.max;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MaxMessageInputBox {

    private final Font font;
    private int x;
    private int y;
    private int width;
    private int height;
    private boolean focused = false;
    private String value = "";
    private int cursor = 0;
    private int selection = 0;
    private int scrollLine = 0;
    private boolean draggingSelection = false;
    private long focusedAt = 0L;

    public MaxMessageInputBox(Font font, int x, int y, int width, int height) {
        this.font = font;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        ensureCursorVisible();
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
        if (focused) {
            this.focusedAt = System.currentTimeMillis();
        }
    }

    public boolean isFocused() {
        return focused;
    }

    public void setValue(String value) {
        this.value = value == null ? "" : value.replace("\r", "");
        this.cursor = Math.min(this.value.length(), this.cursor);
        this.selection = Math.min(this.value.length(), this.selection);
        ensureCursorVisible();
    }

    public String getValue() {
        return value;
    }

    public boolean isBlank() {
        return value.isBlank();
    }

    public void clear() {
        this.value = "";
        this.cursor = 0;
        this.selection = 0;
        this.scrollLine = 0;
    }

    public void insertText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        replaceSelection(text.replace("\r", ""));
    }

    public int getPreferredHeight(int minLines, int maxLines) {
        int lines = Math.max(minLines, Math.min(maxLines, buildVisualLines().size()));
        return 8 + lines * this.font.lineHeight;
    }

    public int getVisibleLineCapacity() {
        return Math.max(1, (height - 4) / Math.max(1, this.font.lineHeight));
    }

    public void render(PoseStack poseStack, int mouseX, int mouseY) {
        List<VisualLine> lines = buildVisualLines();
        int visibleLines = getVisibleLineCapacity();
        int from = Math.max(0, Math.min(scrollLine, Math.max(0, lines.size() - visibleLines)));
        int to = Math.min(lines.size(), from + visibleLines);

        int selectionStart = Math.min(cursor, selection);
        int selectionEnd = Math.max(cursor, selection);
        int drawY = y + 2;

        for (int lineIndex = from; lineIndex < to; lineIndex++) {
            VisualLine line = lines.get(lineIndex);
            if (selectionStart != selectionEnd) {
                int overlapStart = Math.max(selectionStart, line.start());
                int overlapEnd = Math.min(selectionEnd, line.end());
                if (overlapStart < overlapEnd || (overlapStart == overlapEnd && line.start() == line.end() && overlapStart == line.start())) {
                    int sx = x + 4 + widthOfRange(line.text(), 0, Math.max(0, overlapStart - line.start()));
                    int ex = x + 4 + widthOfRange(line.text(), 0, Math.max(0, overlapEnd - line.start()));
                    if (overlapStart == overlapEnd) {
                        ex = sx + 2;
                    }
                    GuiComponent.fill(poseStack, sx, drawY - 1, ex, drawY + font.lineHeight, 0x553C89FF);
                }
            }

            drawString(poseStack, line.text(), x + 4, drawY, 0xFFFFFFFF);
            drawY += font.lineHeight;
        }

        if (focused && isCaretVisible()) {
            int caretX = x + 4;
            int caretY = y + 2;
            int caretLineIndex = 0;
            for (int i = 0; i < lines.size(); i++) {
                VisualLine line = lines.get(i);
                if (cursor >= line.start() && cursor <= line.end()) {
                    caretLineIndex = i;
                    caretX = x + 4 + widthOfRange(line.text(), 0, Math.max(0, cursor - line.start()));
                    caretY = y + 2 + (i - from) * font.lineHeight;
                    break;
                }
            }
            if (caretLineIndex >= from && caretLineIndex < to) {
                GuiComponent.fill(poseStack, caretX, caretY - 1, caretX + 1, caretY + font.lineHeight - 1, 0xFFFFFFFF);
            }
        }
    }

    private void drawString(PoseStack poseStack, String text, int x, int y, int color) {
        Minecraft.getInstance().font.draw(poseStack, text, x, y, color);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        if (!contains(mouseX, mouseY)) {
            draggingSelection = false;
            return false;
        }
        setFocused(true);
        int index = getIndexAt(mouseX, mouseY);
        cursor = index;
        selection = index;
        draggingSelection = true;
        ensureCursorVisible();
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!draggingSelection || button != 0) {
            return false;
        }
        cursor = getIndexAt(mouseX, mouseY);
        ensureCursorVisible();
        return true;
    }

    public void mouseReleased() {
        draggingSelection = false;
    }

    public boolean mouseScrolled(double delta) {
        int max = Math.max(0, buildVisualLines().size() - getVisibleLineCapacity());
        if (delta < 0 && scrollLine < max) {
            scrollLine++;
            return true;
        }
        if (delta > 0 && scrollLine > 0) {
            scrollLine--;
            return true;
        }
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (!focused) {
            return false;
        }
        if (codePoint >= 32 || codePoint == '\n') {
            replaceSelection(String.valueOf(codePoint));
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int modifiers) {
        if (!focused) {
            return false;
        }

        boolean ctrl = (modifiers & 2) != 0 || ScreenCompat.hasControlDown();
        boolean shift = (modifiers & 1) != 0 || ScreenCompat.hasShiftDown();

        switch (keyCode) {
            case 65 -> {
                if (ctrl) {
                    selection = 0;
                    cursor = value.length();
                    ensureCursorVisible();
                    return true;
                }
            }
            case 67 -> {
                if (ctrl) {
                    copySelection();
                    return true;
                }
            }
            case 88 -> {
                if (ctrl) {
                    copySelection();
                    deleteSelection();
                    return true;
                }
            }
            case 86 -> {
                if (ctrl) {
                    insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
                    return true;
                }
            }
            case 259 -> {
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursor > 0) {
                    deleteRange(cursor - 1, cursor);
                }
                return true;
            }
            case 261 -> {
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursor < value.length()) {
                    deleteRange(cursor, cursor + 1);
                }
                return true;
            }
            case 263 -> {
                moveCursor(ctrl ? findWordBoundaryLeft(cursor) : Math.max(0, cursor - 1), shift);
                return true;
            }
            case 262 -> {
                moveCursor(ctrl ? findWordBoundaryRight(cursor) : Math.min(value.length(), cursor + 1), shift);
                return true;
            }
            case 268 -> {
                moveCursor(0, shift);
                return true;
            }
            case 269 -> {
                moveCursor(value.length(), shift);
                return true;
            }
            case 265 -> {
                moveVertical(-1, shift);
                return true;
            }
            case 264 -> {
                moveVertical(1, shift);
                return true;
            }
            default -> {
                return false;
            }
        }
        return false;
    }

    private void moveVertical(int direction, boolean shift) {
        List<VisualLine> lines = buildVisualLines();
        if (lines.isEmpty()) {
            return;
        }
        int currentLine = 0;
        int columnX = 0;
        for (int i = 0; i < lines.size(); i++) {
            VisualLine line = lines.get(i);
            if (cursor >= line.start() && cursor <= line.end()) {
                currentLine = i;
                columnX = widthOfRange(line.text(), 0, Math.max(0, cursor - line.start()));
                break;
            }
        }
        int targetLine = Math.max(0, Math.min(lines.size() - 1, currentLine + direction));
        VisualLine line = lines.get(targetLine);
        int targetIndex = line.start() + findCharIndexByWidth(line.text(), columnX);
        moveCursor(targetIndex, shift);
    }

    private void moveCursor(int newCursor, boolean shift) {
        cursor = Math.max(0, Math.min(value.length(), newCursor));
        if (!shift) {
            selection = cursor;
        }
        ensureCursorVisible();
    }

    private void ensureCursorVisible() {
        List<VisualLine> lines = buildVisualLines();
        int lineIndex = 0;
        for (int i = 0; i < lines.size(); i++) {
            VisualLine line = lines.get(i);
            if (cursor >= line.start() && cursor <= line.end()) {
                lineIndex = i;
                break;
            }
        }
        int visible = getVisibleLineCapacity();
        if (lineIndex < scrollLine) {
            scrollLine = lineIndex;
        } else if (lineIndex >= scrollLine + visible) {
            scrollLine = lineIndex - visible + 1;
        }
        scrollLine = Math.max(0, scrollLine);
    }

    private void replaceSelection(String text) {
        int start = Math.min(cursor, selection);
        int end = Math.max(cursor, selection);
        value = value.substring(0, start) + text + value.substring(end);
        cursor = start + text.length();
        selection = cursor;
        ensureCursorVisible();
    }

    private void deleteSelection() {
        if (!hasSelection()) {
            return;
        }
        replaceSelection("");
    }

    private void deleteRange(int start, int end) {
        int s = Math.max(0, Math.min(start, end));
        int e = Math.max(0, Math.max(start, end));
        value = value.substring(0, s) + value.substring(e);
        cursor = s;
        selection = s;
        ensureCursorVisible();
    }

    private void copySelection() {
        if (!hasSelection()) {
            return;
        }
        int start = Math.min(cursor, selection);
        int end = Math.max(cursor, selection);
        Minecraft.getInstance().keyboardHandler.setClipboard(value.substring(start, end));
    }

    private boolean hasSelection() {
        return cursor != selection;
    }

    private int getIndexAt(double mouseX, double mouseY) {
        List<VisualLine> lines = buildVisualLines();
        if (lines.isEmpty()) {
            return 0;
        }
        int lineIndex = scrollLine + (int) ((mouseY - (y + 2)) / Math.max(1, font.lineHeight));
        lineIndex = Math.max(0, Math.min(lines.size() - 1, lineIndex));
        VisualLine line = lines.get(lineIndex);
        int relativeX = (int) mouseX - (x + 4);
        int charIndex = findCharIndexByWidth(line.text(), relativeX);
        return Math.max(line.start(), Math.min(line.end(), line.start() + charIndex));
    }

    private int findCharIndexByWidth(String text, int targetWidth) {
        if (targetWidth <= 0) {
            return 0;
        }
        for (int i = 1; i <= text.length(); i++) {
            if (font.width(text.substring(0, i)) >= targetWidth) {
                int prev = i - 1;
                return Math.abs(font.width(text.substring(0, i)) - targetWidth) < Math.abs(font.width(text.substring(0, prev)) - targetWidth) ? i : prev;
            }
        }
        return text.length();
    }

    private int widthOfRange(String text, int start, int end) {
        int s = Math.max(0, Math.min(start, text.length()));
        int e = Math.max(s, Math.min(end, text.length()));
        return font.width(text.substring(s, e));
    }

    private int findWordBoundaryLeft(int index) {
        int i = Math.max(0, Math.min(index, value.length()));
        while (i > 0 && Character.isWhitespace(value.charAt(i - 1))) {
            i--;
        }
        while (i > 0 && !Character.isWhitespace(value.charAt(i - 1))) {
            i--;
        }
        return i;
    }

    private int findWordBoundaryRight(int index) {
        int i = Math.max(0, Math.min(index, value.length()));
        while (i < value.length() && Character.isWhitespace(value.charAt(i))) {
            i++;
        }
        while (i < value.length() && !Character.isWhitespace(value.charAt(i))) {
            i++;
        }
        return i;
    }

    private List<VisualLine> buildVisualLines() {
        List<VisualLine> lines = new ArrayList<>();
        int innerWidth = Math.max(20, width - 8);
        String text = value;
        if (text.isEmpty()) {
            lines.add(new VisualLine(0, 0, ""));
            return lines;
        }

        int index = 0;
        while (index <= text.length()) {
            int lineBreak = text.indexOf('\n', index);
            int end = lineBreak >= 0 ? lineBreak : text.length();
            String paragraph = text.substring(index, end);

            if (paragraph.isEmpty()) {
                lines.add(new VisualLine(index, index, ""));
            } else {
                int paragraphIndex = index;
                while (paragraphIndex < end) {
                    String remaining = text.substring(paragraphIndex, end);
                    String piece = font.plainSubstrByWidth(remaining, innerWidth);
                    if (piece.isEmpty()) {
                        piece = remaining.substring(0, 1);
                    }
                    int cut = piece.length();
                    if (cut < remaining.length()) {
                        int lastSpace = piece.lastIndexOf(' ');
                        if (lastSpace > 3) {
                            piece = piece.substring(0, lastSpace);
                            cut = lastSpace + 1;
                        }
                    }
                    int lineEnd = Math.min(end, paragraphIndex + piece.length());
                    lines.add(new VisualLine(paragraphIndex, lineEnd, piece));
                    paragraphIndex += cut;
                    while (paragraphIndex < end && text.charAt(paragraphIndex) == ' ') {
                        paragraphIndex++;
                    }
                }
            }

            if (lineBreak < 0) {
                break;
            }
            index = lineBreak + 1;
            if (index == text.length()) {
                lines.add(new VisualLine(index, index, ""));
                break;
            }
        }

        if (lines.isEmpty()) {
            lines.add(new VisualLine(0, 0, ""));
        }
        return lines;
    }

    private boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private boolean isCaretVisible() {
        return ((System.currentTimeMillis() - focusedAt) / 500L) % 2L == 0L;
    }

    private record VisualLine(int start, int end, String text) {
    }

    private static final class ScreenCompat {
        private static boolean hasControlDown() {
            return org.lwjgl.glfw.GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), 341) == 1
                    || org.lwjgl.glfw.GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), 345) == 1;
        }

        private static boolean hasShiftDown() {
            return org.lwjgl.glfw.GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), 340) == 1
                    || org.lwjgl.glfw.GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), 344) == 1;
        }
    }
}
