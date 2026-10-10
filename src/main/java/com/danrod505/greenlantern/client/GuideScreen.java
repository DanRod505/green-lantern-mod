package com.danrod505.greenlantern.client;

import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.registry.ModItems;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/**
 * The Corps Manual: an in-game guide for first-time ring bearers. Chapters are listed on the left;
 * each chapter's text (from the lang files, {@code guide.greenlantern.<chapter>}) is split into pages
 * that fit the screen. The recipes chapter shows one crafting grid per page.
 */
public class GuideScreen extends Screen {
    private static final int SIDEBAR = 108;
    private static final int LINE = 10;
    private static final int SLOT = 18;

    private static final int PANEL = 0xF0071409;
    private static final int SIDEBAR_BG = 0xF00B1F10;
    private static final int BORDER = 0xFF3CE064;
    private static final int BORDER_DARK = 0xFF1E6A33;
    private static final int TEXT = 0xFFD8F5DE;
    private static final int TITLE = 0xFF7CFF96;
    private static final int MUTED = 0xFF8FB89A;

    private record Chapter(String id, Supplier<ItemStack> icon) {
        Component title() {
            return Component.translatable("guide.greenlantern." + id + ".title");
        }

        Component text() {
            return Component.translatable("guide.greenlantern." + id);
        }
    }

    /** A shaped 3x3 recipe as shown in the guide (mirrors the JSON recipes in data/greenlantern/recipe). */
    private record Recipe(String id, Supplier<ItemStack> result, Supplier<ItemStack[]> grid) {}

    private static final String RECIPES = "recipes";

    private static final List<Chapter> CHAPTERS = List.of(
            new Chapter("welcome", () -> new ItemStack(ModItems.GUIDE_BOOK.get())),
            new Chapter("ring", () -> PowerRingItem.charged(ModItems.POWER_RING.get().getDefaultInstance())),
            new Chapter("battery", () -> new ItemStack(ModItems.POWER_BATTERY.get())),
            new Chapter("uniform", () -> new ItemStack(ModItems.LANTERN_SUIT.get())),
            new Chapter("flight", () -> new ItemStack(Items.FEATHER)),
            new Chapter("constructs", () -> new ItemStack(Items.EMERALD)),
            new Chapter("drill", () -> new ItemStack(Items.DIAMOND_PICKAXE)),
            new Chapter("mecha", () -> new ItemStack(Items.NETHERITE_CHESTPLATE)),
            new Chapter("oa", () -> new ItemStack(Items.ENDER_EYE)),
            new Chapter("flash", () -> new ItemStack(ModItems.FLASH_RING.get())),
            new Chapter("flash_powers", () -> new ItemStack(Items.LIGHTNING_ROD)),
            new Chapter("aquaman", () -> new ItemStack(ModItems.AQUAMAN_EMBLEM.get())),
            new Chapter("aquaman_powers", () -> new ItemStack(Items.TRIDENT)),
            new Chapter("kraken", () -> new ItemStack(Items.INK_SAC)),
            new Chapter("atlantis", () -> new ItemStack(ModItems.ATLANTIS_GATE.get())),
            new Chapter("controls", () -> new ItemStack(Items.LEVER)),
            new Chapter(RECIPES, () -> new ItemStack(Items.CRAFTING_TABLE)),
            new Chapter("tips", () -> new ItemStack(Items.TORCH)));

    private static final List<Recipe> RECIPE_LIST = List.of(
            new Recipe("power_ring", () -> new ItemStack(ModItems.POWER_RING.get()), () -> grid(
                    null, Items.DIAMOND, null,
                    Items.EMERALD, null, Items.EMERALD,
                    null, Items.EMERALD, null)),
            new Recipe("power_battery", () -> new ItemStack(ModItems.POWER_BATTERY.get()), () -> grid(
                    Items.IRON_INGOT, Items.LIME_STAINED_GLASS, Items.IRON_INGOT,
                    Items.LIME_STAINED_GLASS, Items.LANTERN, Items.LIME_STAINED_GLASS,
                    Items.IRON_INGOT, Items.EMERALD_BLOCK, Items.IRON_INGOT)),
            new Recipe("flash_ring", () -> new ItemStack(ModItems.FLASH_RING.get()), () -> grid(
                    Items.REDSTONE, Items.GOLD_INGOT, Items.REDSTONE,
                    Items.GOLD_INGOT, Items.LIGHTNING_ROD, Items.GOLD_INGOT,
                    Items.REDSTONE, Items.GOLD_INGOT, Items.REDSTONE)),
            new Recipe("aquaman_emblem", () -> new ItemStack(ModItems.AQUAMAN_EMBLEM.get()), () -> grid(
                    Items.PRISMARINE_SHARD, Items.GOLD_INGOT, Items.PRISMARINE_SHARD,
                    Items.GOLD_INGOT, Items.NAUTILUS_SHELL, Items.GOLD_INGOT,
                    Items.PRISMARINE_SHARD, Items.GOLD_INGOT, Items.PRISMARINE_SHARD)),
            new Recipe("atlantis_gate", () -> new ItemStack(ModItems.ATLANTIS_GATE.get()), () -> grid(
                    Items.GOLD_INGOT, Items.PRISMARINE_CRYSTALS, Items.GOLD_INGOT,
                    Items.PRISMARINE_CRYSTALS, Items.HEART_OF_THE_SEA, Items.PRISMARINE_CRYSTALS,
                    Items.GOLD_INGOT, Items.ENDER_PEARL, Items.GOLD_INGOT)),
            new Recipe("atlantean_respirator", () -> new ItemStack(ModItems.ATLANTEAN_RESPIRATOR.get()), () -> grid(
                    Items.PRISMARINE_SHARD, Items.GOLD_INGOT, Items.PRISMARINE_SHARD,
                    Items.KELP, Items.GLASS_BOTTLE, Items.KELP,
                    null, Items.PRISMARINE_SHARD, null)),
            new Recipe("guide_book", () -> new ItemStack(ModItems.GUIDE_BOOK.get()), () -> grid(
                    Items.BOOK, Items.EMERALD, null,
                    null, null, null,
                    null, null, null)));

    // Remembered while the game runs, so the manual reopens where the player stopped reading.
    private static int lastChapter;
    private static int lastPage;

    private int chapter;
    private int page;
    private List<List<FormattedCharSequence>> pages = List.of();
    private int hoveredChapter = -1;

    private int x0;
    private int y0;
    private int w;
    private int h;

    public GuideScreen() {
        super(Component.translatable("guide.greenlantern.title"));
        this.chapter = Mth.clamp(lastChapter, 0, CHAPTERS.size() - 1);
        this.page = lastPage;
    }

    /** Opens the manual at the chapter with the given id (e.g. "atlantis"). */
    public static GuideScreen atChapter(String id) {
        for (int i = 0; i < CHAPTERS.size(); i++) {
            if (CHAPTERS.get(i).id().equals(id)) {
                lastChapter = i;
                lastPage = 0;
            }
        }
        return new GuideScreen();
    }

    private static ItemStack[] grid(Object... items) {
        ItemStack[] stacks = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            stacks[i] = items[i] == null ? ItemStack.EMPTY : new ItemStack((net.minecraft.world.level.ItemLike) items[i]);
        }
        return stacks;
    }

    @Override
    protected void init() {
        w = Math.min(340, width - 16);
        h = Math.min(224, height - 16);
        x0 = (width - w) / 2;
        y0 = (height - h) / 2;
        paginate();
    }

    // ---- layout ----------------------------------------------------------------------------------

    private int contentX() {
        return x0 + SIDEBAR + 10;
    }

    private int contentWidth() {
        return w - SIDEBAR - 20;
    }

    private int textTop() {
        return y0 + 30;
    }

    private int textBottom() {
        return y0 + h - 20;
    }

    private int rowHeight() {
        return Math.min(18, (h - 30) / CHAPTERS.size());
    }

    private int rowY(int index) {
        return y0 + 26 + index * rowHeight();
    }

    private boolean isRecipes() {
        return CHAPTERS.get(chapter).id().equals(RECIPES);
    }

    private int pageCount() {
        return isRecipes() ? RECIPE_LIST.size() : Math.max(1, pages.size());
    }

    private void paginate() {
        if (isRecipes()) {
            pages = List.of();
        } else {
            List<FormattedCharSequence> lines = font.split(CHAPTERS.get(chapter).text(), contentWidth());
            int perPage = Math.max(1, (textBottom() - textTop()) / LINE);
            java.util.ArrayList<List<FormattedCharSequence>> split = new java.util.ArrayList<>();
            for (int i = 0; i < lines.size(); i += perPage) {
                split.add(lines.subList(i, Math.min(lines.size(), i + perPage)));
            }
            pages = split;
        }
        page = Mth.clamp(page, 0, pageCount() - 1);
        lastChapter = chapter;
        lastPage = page;
    }

    private void openChapter(int index) {
        if (index == chapter || index < 0 || index >= CHAPTERS.size()) return;
        chapter = index;
        page = 0;
        paginate();
        playPageSound();
    }

    private void turnPage(int delta) {
        int target = page + delta;
        if (target < 0) {
            if (chapter > 0) {
                chapter--;
                page = Integer.MAX_VALUE;
                paginate();
                playPageSound();
            }
            return;
        }
        if (target >= pageCount()) {
            if (chapter < CHAPTERS.size() - 1) {
                chapter++;
                page = 0;
                paginate();
                playPageSound();
            }
            return;
        }
        page = target;
        lastPage = page;
        playPageSound();
    }

    private void playPageSound() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }

    // ---- input -----------------------------------------------------------------------------------

    private int chapterAt(double mouseX, double mouseY) {
        if (mouseX < x0 + 4 || mouseX >= x0 + SIDEBAR - 4) return -1;
        for (int i = 0; i < CHAPTERS.size(); i++) {
            int y = rowY(i);
            if (mouseY >= y && mouseY < y + rowHeight()) return i;
        }
        return -1;
    }

    private boolean overPrev(double mouseX, double mouseY) {
        return mouseY >= textBottom() && mouseY < y0 + h && mouseX >= contentX() && mouseX < contentX() + 40;
    }

    private boolean overNext(double mouseX, double mouseY) {
        int right = contentX() + contentWidth();
        return mouseY >= textBottom() && mouseY < y0 + h && mouseX >= right - 40 && mouseX < right;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int index = chapterAt(event.x(), event.y());
            if (index >= 0) {
                openChapter(index);
                return true;
            }
            if (overPrev(event.x(), event.y())) {
                turnPage(-1);
                return true;
            }
            if (overNext(event.x(), event.y())) {
                turnPage(1);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0) return false;
        if (mouseX < x0 + SIDEBAR) {
            openChapter(Mth.clamp(chapter + (scrollY > 0 ? -1 : 1), 0, CHAPTERS.size() - 1));
        } else {
            turnPage(scrollY > 0 ? -1 : 1);
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        switch (event.key()) {
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_PAGE_UP, GLFW.GLFW_KEY_A -> {
                turnPage(-1);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_PAGE_DOWN, GLFW.GLFW_KEY_D -> {
                turnPage(1);
                return true;
            }
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_W -> {
                openChapter(chapter - 1);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_S -> {
                openChapter(chapter + 1);
                return true;
            }
            default -> {
                return super.keyPressed(event);
            }
        }
    }

    // ---- rendering -------------------------------------------------------------------------------

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBlurredBackground(graphics);
        graphics.fill(0, 0, width, height, 0x66000A04);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        hoveredChapter = chapterAt(mouseX, mouseY);

        // Panel, sidebar and frame.
        graphics.fill(x0, y0, x0 + w, y0 + h, PANEL);
        graphics.fill(x0, y0, x0 + SIDEBAR, y0 + h, SIDEBAR_BG);
        graphics.renderOutline(x0, y0, w, h, BORDER);
        graphics.renderOutline(x0 + 1, y0 + 1, w - 2, h - 2, BORDER_DARK);
        graphics.fill(x0 + SIDEBAR, y0 + 2, x0 + SIDEBAR + 1, y0 + h - 2, BORDER_DARK);

        // Manual title above the chapter list.
        Component bookTitle = getTitle().copy().withStyle(ChatFormatting.BOLD);
        graphics.drawCenteredString(font, bookTitle, x0 + SIDEBAR / 2, y0 + 9, TITLE);
        graphics.fill(x0 + 8, y0 + 21, x0 + SIDEBAR - 8, y0 + 22, BORDER_DARK);

        int rowH = rowHeight();
        for (int i = 0; i < CHAPTERS.size(); i++) {
            Chapter c = CHAPTERS.get(i);
            int y = rowY(i);
            if (i == chapter) {
                graphics.fill(x0 + 4, y, x0 + SIDEBAR - 4, y + rowH, 0xFF1F5A2E);
                graphics.fill(x0 + 4, y, x0 + 6, y + rowH, BORDER);
            } else if (i == hoveredChapter) {
                graphics.fill(x0 + 4, y, x0 + SIDEBAR - 4, y + rowH, 0x6630A050);
            }
            int iconY = y + (rowH - 16) / 2;
            graphics.renderItem(c.icon().get(), x0 + 8, iconY);
            List<FormattedCharSequence> name = font.split(c.title(), SIDEBAR - 34);
            graphics.drawString(font, name.getFirst(), x0 + 27, y + (rowH - 8) / 2, i == chapter ? 0xFFFFFFFF : TEXT, false);
        }

        // Chapter title.
        int cx = contentX();
        int cw = contentWidth();
        Chapter current = CHAPTERS.get(chapter);
        graphics.drawString(font, current.title().copy().withStyle(ChatFormatting.BOLD), cx, y0 + 10, TITLE, false);
        graphics.fill(cx, y0 + 22, cx + cw, y0 + 23, BORDER_DARK);

        if (isRecipes()) {
            renderRecipe(graphics, RECIPE_LIST.get(page), mouseX, mouseY);
        } else if (!pages.isEmpty()) {
            int y = textTop();
            for (FormattedCharSequence line : pages.get(page)) {
                graphics.drawString(font, line, cx, y, TEXT, false);
                y += LINE;
            }
        }

        // Footer: page arrows and number.
        int footerY = y0 + h - 14;
        boolean hasPrev = page > 0 || chapter > 0;
        boolean hasNext = page < pageCount() - 1 || chapter < CHAPTERS.size() - 1;
        if (hasPrev) {
            graphics.drawString(font, "◀", cx, footerY, overPrev(mouseX, mouseY) ? 0xFFFFFFFF : TITLE, false);
        }
        if (hasNext) {
            graphics.drawString(font, "▶", cx + cw - font.width("▶"), footerY, overNext(mouseX, mouseY) ? 0xFFFFFFFF : TITLE, false);
        }
        graphics.drawCenteredString(font, Component.translatable("guide.greenlantern.page", page + 1, pageCount()), cx + cw / 2, footerY, MUTED);
    }

    private void renderRecipe(GuiGraphics graphics, Recipe recipe, int mouseX, int mouseY) {
        int cx = contentX();
        int cw = contentWidth();
        ItemStack result = recipe.result().get();
        graphics.drawString(font, result.getHoverName(), cx, textTop(), TEXT, false);

        int gridW = SLOT * 3;
        int totalW = gridW + 30 + SLOT;
        int gx = cx + (cw - totalW) / 2;
        int gy = textTop() + 14;
        ItemStack[] grid = recipe.grid().get();
        ItemStack hovered = ItemStack.EMPTY;
        for (int i = 0; i < 9; i++) {
            int sx = gx + (i % 3) * SLOT;
            int sy = gy + (i / 3) * SLOT;
            if (slot(graphics, grid[i], sx, sy, mouseX, mouseY)) hovered = grid[i];
        }
        int arrowX = gx + gridW + 8;
        graphics.drawString(font, "→", arrowX, gy + SLOT + 5, TITLE, false);
        int rx = gx + gridW + 30;
        int ry = gy + SLOT;
        if (slot(graphics, result, rx, ry, mouseX, mouseY)) hovered = result;

        int y = gy + SLOT * 3 + 10;
        for (FormattedCharSequence line : font.split(Component.translatable("guide.greenlantern.recipe." + recipe.id()), cw)) {
            if (y > textBottom() - LINE) break;
            graphics.drawString(font, line, cx, y, TEXT, false);
            y += LINE;
        }
        if (!hovered.isEmpty()) {
            graphics.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
        }
    }

    /** Draws a crafting slot and returns whether the mouse is over a non-empty one. */
    private boolean slot(GuiGraphics graphics, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        graphics.fill(x, y, x + SLOT, y + SLOT, BORDER_DARK);
        graphics.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0xFF0F2415);
        boolean over = mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT;
        if (over) graphics.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0xFF245C34);
        if (!stack.isEmpty()) graphics.renderItem(stack, x + 1, y + 1);
        return over && !stack.isEmpty();
    }

    @Override
    public void removed() {
        lastChapter = chapter;
        lastPage = page;
        super.removed();
    }
}
