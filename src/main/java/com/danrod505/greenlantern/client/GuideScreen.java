package com.danrod505.greenlantern.client;

import com.danrod505.greenlantern.item.PowerRingItem;
import com.danrod505.greenlantern.registry.ModItems;
import java.util.ArrayList;
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
 * The Heroes' Guide: the in-game manual of the mod, organized in sections (one per hero, plus
 * Atlantis, the Trench and a "getting started" section) and subsections. The sidebar lists the sections; the
 * open section unfolds its subsections underneath. Each subsection's text (from the lang files,
 * {@code guide.greenlantern.<subsection>}) is split into pages that fit the screen; the recipe
 * subsections show one crafting grid per page.
 */
public class GuideScreen extends Screen {
    private static final int SIDEBAR = 128;
    private static final int LINE = 10;
    private static final int SLOT = 18;
    private static final int SECTION_ROW = 17;
    private static final int ENTRY_ROW = 12;

    private static final int PANEL = 0xF00A1018;
    private static final int SIDEBAR_BG = 0xF00F1823;
    private static final int BORDER = 0xFFE0B848;
    private static final int BORDER_DARK = 0xFF6B5A2A;
    private static final int TEXT = 0xFFE4E8EE;
    private static final int TITLE = 0xFFFFD86A;
    private static final int MUTED = 0xFF98A4B4;

    /** A shaped 3x3 recipe as shown in the guide (mirrors the JSON recipes in data/greenlantern/recipe). */
    private record Recipe(String id, Supplier<ItemStack> result, Supplier<ItemStack[]> grid) {}

    /** A subsection: a text (split in pages) or, when it has recipes, one recipe per page. */
    private record Entry(String id, List<Recipe> recipes) {
        Component title() {
            return Component.translatable(recipes.isEmpty() ? "guide.greenlantern." + id + ".title" : "guide.greenlantern.recipes.title");
        }

        Component text() {
            return Component.translatable("guide.greenlantern." + id);
        }

        boolean isRecipes() {
            return !recipes.isEmpty();
        }
    }

    /** A section of the guide, with its icon, its accent color and its subsections. */
    private record Section(String id, Supplier<ItemStack> icon, int accent, List<Entry> entries) {
        Component title() {
            return Component.translatable("guide.greenlantern.section." + id);
        }
    }

    private static Entry text(String id) {
        return new Entry(id, List.of());
    }

    private static Entry recipes(String section, Recipe... recipes) {
        return new Entry("recipes_" + section, List.of(recipes));
    }

    private static final Recipe POWER_RING = new Recipe("power_ring", () -> new ItemStack(ModItems.POWER_RING.get()), () -> grid(
            null, Items.DIAMOND, null,
            Items.EMERALD, null, Items.EMERALD,
            null, Items.EMERALD, null));
    private static final Recipe POWER_BATTERY = new Recipe("power_battery", () -> new ItemStack(ModItems.POWER_BATTERY.get()), () -> grid(
            Items.IRON_INGOT, Items.LIME_STAINED_GLASS, Items.IRON_INGOT,
            Items.LIME_STAINED_GLASS, Items.LANTERN, Items.LIME_STAINED_GLASS,
            Items.IRON_INGOT, Items.EMERALD_BLOCK, Items.IRON_INGOT));
    private static final Recipe FLASH_RING = new Recipe("flash_ring", () -> new ItemStack(ModItems.FLASH_RING.get()), () -> grid(
            Items.REDSTONE, Items.GOLD_INGOT, Items.REDSTONE,
            Items.GOLD_INGOT, Items.LIGHTNING_ROD, Items.GOLD_INGOT,
            Items.REDSTONE, Items.GOLD_INGOT, Items.REDSTONE));
    private static final Recipe AQUAMAN_EMBLEM = new Recipe("aquaman_emblem", () -> new ItemStack(ModItems.AQUAMAN_EMBLEM.get()), () -> grid(
            Items.PRISMARINE_SHARD, Items.GOLD_INGOT, Items.PRISMARINE_SHARD,
            Items.GOLD_INGOT, Items.NAUTILUS_SHELL, Items.GOLD_INGOT,
            Items.PRISMARINE_SHARD, Items.GOLD_INGOT, Items.PRISMARINE_SHARD));
    private static final Recipe ATLANTIS_GATE = new Recipe("atlantis_gate", () -> new ItemStack(ModItems.ATLANTIS_GATE.get()), () -> grid(
            Items.GOLD_INGOT, Items.PRISMARINE_CRYSTALS, Items.GOLD_INGOT,
            Items.PRISMARINE_CRYSTALS, Items.HEART_OF_THE_SEA, Items.PRISMARINE_CRYSTALS,
            Items.GOLD_INGOT, Items.ENDER_PEARL, Items.GOLD_INGOT));
    private static final Recipe ATLANTEAN_RESPIRATOR = new Recipe("atlantean_respirator", () -> new ItemStack(ModItems.ATLANTEAN_RESPIRATOR.get()), () -> grid(
            Items.PRISMARINE_SHARD, Items.GOLD_INGOT, Items.PRISMARINE_SHARD,
            Items.KELP, Items.GLASS_BOTTLE, Items.KELP,
            null, Items.PRISMARINE_SHARD, null));
    private static final Recipe UTILITY_BELT = new Recipe("utility_belt", () -> new ItemStack(ModItems.UTILITY_BELT.get()), () -> grid(
            Items.LEATHER, Items.GOLD_INGOT, Items.LEATHER,
            Items.IRON_INGOT, Items.PHANTOM_MEMBRANE, Items.IRON_INGOT,
            Items.LEATHER, Items.GOLD_INGOT, Items.LEATHER));
    private static final Recipe KRYPTONIAN_CRYSTAL = new Recipe("kryptonian_crystal", () -> new ItemStack(ModItems.KRYPTONIAN_CRYSTAL.get()), () -> grid(
            Items.AMETHYST_SHARD, Items.DIAMOND, Items.AMETHYST_SHARD,
            Items.GOLD_INGOT, Items.SUNFLOWER, Items.GOLD_INGOT,
            Items.AMETHYST_SHARD, Items.DIAMOND, Items.AMETHYST_SHARD));
    private static final Recipe GUIDE_BOOK = new Recipe("guide_book", () -> new ItemStack(ModItems.GUIDE_BOOK.get()), () -> grid(
            Items.BOOK, Items.EMERALD, null,
            null, null, null,
            null, null, null));

    private static final List<Section> SECTIONS = List.of(
            new Section("start", () -> new ItemStack(ModItems.GUIDE_BOOK.get()), 0xFFFFD86A, List.of(
                    text("welcome"), text("heroes"), text("tips"), recipes("start", GUIDE_BOOK))),
            new Section("lantern", () -> PowerRingItem.charged(ModItems.POWER_RING.get().getDefaultInstance()), 0xFF4CE070, List.of(
                    text("ring"), text("battery"), text("uniform"), text("flight"), text("constructs"), text("drill"), text("mecha"),
                    text("oa"), text("controls_lantern"), recipes("lantern", POWER_RING, POWER_BATTERY))),
            new Section("flash", () -> new ItemStack(ModItems.FLASH_RING.get()), 0xFFFF5A4A, List.of(
                    text("flash"), text("flash_powers"), text("controls_flash"), recipes("flash", FLASH_RING))),
            new Section("aquaman", () -> new ItemStack(ModItems.AQUAMAN_EMBLEM.get()), 0xFFFFA040, List.of(
                    text("aquaman"), text("aquaman_swim"), text("aquaman_trident"), text("aquaman_shark"), text("aquaman_sea_call"),
                    text("kraken"), text("controls_aquaman"), recipes("aquaman", AQUAMAN_EMBLEM))),
            new Section("atlantis", () -> new ItemStack(ModItems.ATLANTIS_GATE.get()), 0xFF4FE0E8, List.of(
                    text("atlantis"), text("atlantis_people"), text("atlantis_travel"), text("atlantis_respirator"),
                    text("atlantis_creatures"), text("atlantis_manta"), text("atlantis_seahorse"), text("atlantis_dolphin"),
                    recipes("atlantis", ATLANTIS_GATE, ATLANTEAN_RESPIRATOR))),
            new Section("trench", () -> new ItemStack(ModItems.TRENCH_CREATURE_EGG.get()), 0xFFD0453A, List.of(
                    text("trench"), text("trench_territory"), text("trench_nest"), text("trench_creatures"), text("trench_captives"),
                    text("trench_raids"))),
            new Section("batman", () -> new ItemStack(ModItems.UTILITY_BELT.get()), 0xFFB8C0CC, List.of(
                    text("batman"), text("batman_powers"), text("controls_batman"), recipes("batman", UTILITY_BELT))),
            new Section("superman", () -> com.danrod505.greenlantern.item.KryptonianCrystalItem.charged(ModItems.KRYPTONIAN_CRYSTAL.get().getDefaultInstance()), 0xFF4A86FF, List.of(
                    text("superman"), text("superman_powers"), text("controls_superman"), recipes("superman", KRYPTONIAN_CRYSTAL))));

    // Remembered while the game runs, so the guide reopens where the player stopped reading.
    private static int lastSection;
    private static int lastEntry;
    private static int lastPage;

    private int section;
    private int entry;
    private int page;
    private List<List<FormattedCharSequence>> pages = List.of();

    private int x0;
    private int y0;
    private int w;
    private int h;

    public GuideScreen() {
        super(Component.translatable("guide.greenlantern.title"));
        this.section = Mth.clamp(lastSection, 0, SECTIONS.size() - 1);
        this.entry = Mth.clamp(lastEntry, 0, SECTIONS.get(section).entries().size() - 1);
        this.page = lastPage;
    }

    /** Opens the guide at the subsection with the given id (e.g. "atlantis"), or at a section's first page. */
    public static GuideScreen atChapter(String id) {
        for (int s = 0; s < SECTIONS.size(); s++) {
            List<Entry> entries = SECTIONS.get(s).entries();
            for (int e = 0; e < entries.size(); e++) {
                if (entries.get(e).id().equals(id)) {
                    lastSection = s;
                    lastEntry = e;
                    lastPage = 0;
                    return new GuideScreen();
                }
            }
            if (SECTIONS.get(s).id().equals(id)) {
                lastSection = s;
                lastEntry = 0;
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
        w = Math.min(380, width - 16);
        h = Math.min(250, height - 16);
        x0 = (width - w) / 2;
        y0 = (height - h) / 2;
        paginate();
    }

    // ---- layout ----------------------------------------------------------------------------------

    private Section current() {
        return SECTIONS.get(section);
    }

    private Entry currentEntry() {
        return current().entries().get(entry);
    }

    private int contentX() {
        return x0 + SIDEBAR + 10;
    }

    private int contentWidth() {
        return w - SIDEBAR - 20;
    }

    private int textTop() {
        return y0 + 40;
    }

    private int textBottom() {
        return y0 + h - 20;
    }

    /** Height of the subsection rows: shrinks if the open section doesn't fit. */
    private int entryRow() {
        int free = h - 30 - SECTIONS.size() * sectionRow();
        return Mth.clamp(free / Math.max(1, current().entries().size()), 9, ENTRY_ROW);
    }

    private int sectionRow() {
        return Math.min(SECTION_ROW, (h - 30) / (SECTIONS.size() + 3));
    }

    /** One row of the sidebar: a section header or one of the open section's subsections. */
    private record Row(int section, int entry, int y, int height) {}

    private List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        int y = y0 + 26;
        for (int s = 0; s < SECTIONS.size(); s++) {
            rows.add(new Row(s, -1, y, sectionRow()));
            y += sectionRow();
            if (s == section) {
                for (int e = 0; e < SECTIONS.get(s).entries().size(); e++) {
                    rows.add(new Row(s, e, y, entryRow()));
                    y += entryRow();
                }
                y += 2;
            }
        }
        return rows;
    }

    private int pageCount() {
        Entry e = currentEntry();
        return e.isRecipes() ? e.recipes().size() : Math.max(1, pages.size());
    }

    private void paginate() {
        Entry e = currentEntry();
        if (e.isRecipes()) {
            pages = List.of();
        } else {
            List<FormattedCharSequence> lines = font.split(e.text(), contentWidth());
            int perPage = Math.max(1, (textBottom() - textTop()) / LINE);
            List<List<FormattedCharSequence>> split = new ArrayList<>();
            for (int i = 0; i < lines.size(); i += perPage) {
                split.add(lines.subList(i, Math.min(lines.size(), i + perPage)));
            }
            pages = split;
        }
        page = Mth.clamp(page, 0, pageCount() - 1);
        remember();
    }

    private void remember() {
        lastSection = section;
        lastEntry = entry;
        lastPage = page;
    }

    private void open(int newSection, int newEntry, boolean lastPageOfIt) {
        if (newSection < 0 || newSection >= SECTIONS.size()) return;
        newEntry = Mth.clamp(newEntry, 0, SECTIONS.get(newSection).entries().size() - 1);
        if (newSection == section && newEntry == entry && !lastPageOfIt) return;
        section = newSection;
        entry = newEntry;
        page = lastPageOfIt ? Integer.MAX_VALUE : 0;
        paginate();
        playPageSound();
    }

    /** The next / previous subsection, moving into the next / previous section at the ends. */
    private void stepEntry(int delta, boolean lastPageOfIt) {
        int e = entry + delta;
        if (e < 0) {
            if (section > 0) open(section - 1, SECTIONS.get(section - 1).entries().size() - 1, lastPageOfIt);
        } else if (e >= current().entries().size()) {
            if (section < SECTIONS.size() - 1) open(section + 1, 0, false);
        } else {
            open(section, e, lastPageOfIt);
        }
    }

    private void turnPage(int delta) {
        int target = page + delta;
        if (target < 0) {
            stepEntry(-1, true);
        } else if (target >= pageCount()) {
            stepEntry(1, false);
        } else {
            page = target;
            remember();
            playPageSound();
        }
    }

    private boolean atStart() {
        return section == 0 && entry == 0 && page == 0;
    }

    private boolean atEnd() {
        return section == SECTIONS.size() - 1 && entry == current().entries().size() - 1 && page >= pageCount() - 1;
    }

    private void playPageSound() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }

    // ---- input -----------------------------------------------------------------------------------

    private Row rowAt(double mouseX, double mouseY) {
        if (mouseX < x0 + 4 || mouseX >= x0 + SIDEBAR - 4) return null;
        for (Row row : rows()) {
            if (mouseY >= row.y() && mouseY < row.y() + row.height()) return row;
        }
        return null;
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
            Row row = rowAt(event.x(), event.y());
            if (row != null) {
                // A section header opens the section at its first subsection.
                open(row.section(), Math.max(0, row.entry()), false);
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
            stepEntry(scrollY > 0 ? -1 : 1, false);
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
                stepEntry(-1, false);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_S -> {
                stepEntry(1, false);
                return true;
            }
            case GLFW.GLFW_KEY_TAB -> {
                open((section + ((event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 ? SECTIONS.size() - 1 : 1)) % SECTIONS.size(), 0, false);
                return true;
            }
            default -> {
                return super.keyPressed(event);
            }
        }
    }

    // ---- rendering -------------------------------------------------------------------------------

    /** Draws one line of the sidebar: shrinks a long name a little to fit, and only then cuts it with "…". */
    private void drawFitted(GuiGraphics graphics, Component text, boolean bold, int x, int y, int maxWidth, int color) {
        String plain = text.getString();
        Component line = bold ? Component.literal(plain).withStyle(ChatFormatting.BOLD) : Component.literal(plain);
        int width = font.width(line);
        float k = width <= maxWidth ? 1.0F : Math.max(0.75F, maxWidth / (float) width);
        if (width * k > maxWidth) {
            int room = (int) (maxWidth / k) - font.width("…");
            String cut = font.plainSubstrByWidth(plain, bold ? room * 6 / 7 : room).stripTrailing() + "…";
            line = bold ? Component.literal(cut).withStyle(ChatFormatting.BOLD) : Component.literal(cut);
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y + (8 - 8 * k) / 2);
        graphics.pose().scale(k, k);
        graphics.drawString(font, line, 0, 0, color, false);
        graphics.pose().popMatrix();
    }


    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBlurredBackground(graphics);
        graphics.fill(0, 0, width, height, 0x66000408);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Row hovered = rowAt(mouseX, mouseY);
        int accent = current().accent();

        // Panel, sidebar and frame.
        graphics.fill(x0, y0, x0 + w, y0 + h, PANEL);
        graphics.fill(x0, y0, x0 + SIDEBAR, y0 + h, SIDEBAR_BG);
        graphics.renderOutline(x0, y0, w, h, BORDER);
        graphics.renderOutline(x0 + 1, y0 + 1, w - 2, h - 2, BORDER_DARK);
        graphics.fill(x0 + SIDEBAR, y0 + 2, x0 + SIDEBAR + 1, y0 + h - 2, BORDER_DARK);

        // Guide title above the list.
        Component bookTitle = getTitle().copy().withStyle(ChatFormatting.BOLD);
        graphics.drawCenteredString(font, bookTitle, x0 + SIDEBAR / 2, y0 + 9, TITLE);
        graphics.fill(x0 + 8, y0 + 21, x0 + SIDEBAR - 8, y0 + 22, BORDER_DARK);

        for (Row row : rows()) {
            Section s = SECTIONS.get(row.section());
            boolean isHovered = hovered != null && hovered.section() == row.section() && hovered.entry() == row.entry();
            if (row.entry() < 0) {
                // Section header: icon and name.
                boolean open = row.section() == section;
                if (open) {
                    graphics.fill(x0 + 4, row.y(), x0 + SIDEBAR - 4, row.y() + row.height(), 0x40FFFFFF & s.accent() | 0x30000000);
                } else if (isHovered) {
                    graphics.fill(x0 + 4, row.y(), x0 + SIDEBAR - 4, row.y() + row.height(), 0x30FFFFFF);
                }
                graphics.fill(x0 + 4, row.y(), x0 + 6, row.y() + row.height(), s.accent());
                int iconY = row.y() + (row.height() - 16) / 2;
                graphics.pose().pushMatrix();
                float k = Math.min(1.0F, row.height() / 16.0F);
                graphics.pose().translate(x0 + 9, iconY + (16 - 16 * k) / 2);
                graphics.pose().scale(k, k);
                graphics.renderItem(s.icon().get(), 0, 0);
                graphics.pose().popMatrix();
                drawFitted(graphics, s.title(), true, x0 + 28, row.y() + (row.height() - 8) / 2, SIDEBAR - 32, open ? s.accent() : TEXT);
            } else {
                // Subsection of the open section, indented.
                Entry e = s.entries().get(row.entry());
                boolean selected = row.entry() == entry;
                if (selected) {
                    graphics.fill(x0 + 14, row.y(), x0 + SIDEBAR - 4, row.y() + row.height(), 0x55FFFFFF & accent | 0x50000000);
                    graphics.fill(x0 + 14, row.y(), x0 + 16, row.y() + row.height(), accent);
                } else if (isHovered) {
                    graphics.fill(x0 + 14, row.y(), x0 + SIDEBAR - 4, row.y() + row.height(), 0x25FFFFFF);
                }
                drawFitted(graphics, e.title(), false, x0 + 19, row.y() + (row.height() - 8) / 2 + 1, SIDEBAR - 24,
                        selected ? 0xFFFFFFFF : MUTED);
            }
        }

        // Breadcrumb (section) and the subsection title.
        int cx = contentX();
        int cw = contentWidth();
        Entry e = currentEntry();
        graphics.drawString(font, current().title(), cx, y0 + 8, accent, false);
        graphics.drawString(font, e.title().copy().withStyle(ChatFormatting.BOLD), cx, y0 + 20, TITLE, false);
        graphics.fill(cx, y0 + 32, cx + cw, y0 + 33, BORDER_DARK);
        graphics.fill(cx, y0 + 32, cx + Math.min(cw, 40), y0 + 33, accent);

        if (e.isRecipes()) {
            renderRecipe(graphics, e.recipes().get(page), mouseX, mouseY);
        } else if (!pages.isEmpty()) {
            int y = textTop();
            for (FormattedCharSequence line : pages.get(page)) {
                graphics.drawString(font, line, cx, y, TEXT, false);
                y += LINE;
            }
        }

        // Footer: page arrows and number.
        int footerY = y0 + h - 14;
        if (!atStart()) {
            graphics.drawString(font, "◀", cx, footerY, overPrev(mouseX, mouseY) ? 0xFFFFFFFF : TITLE, false);
        }
        if (!atEnd()) {
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
        ItemStack hoveredStack = ItemStack.EMPTY;
        for (int i = 0; i < 9; i++) {
            int sx = gx + (i % 3) * SLOT;
            int sy = gy + (i / 3) * SLOT;
            if (slot(graphics, grid[i], sx, sy, mouseX, mouseY)) hoveredStack = grid[i];
        }
        int arrowX = gx + gridW + 8;
        graphics.drawString(font, "→", arrowX, gy + SLOT + 5, TITLE, false);
        int rx = gx + gridW + 30;
        int ry = gy + SLOT;
        if (slot(graphics, result, rx, ry, mouseX, mouseY)) hoveredStack = result;

        int y = gy + SLOT * 3 + 10;
        for (FormattedCharSequence line : font.split(Component.translatable("guide.greenlantern.recipe." + recipe.id()), cw)) {
            if (y > textBottom() - LINE) break;
            graphics.drawString(font, line, cx, y, TEXT, false);
            y += LINE;
        }
        if (!hoveredStack.isEmpty()) {
            graphics.setTooltipForNextFrame(font, hoveredStack, mouseX, mouseY);
        }
    }

    /** Draws a crafting slot and returns whether the mouse is over a non-empty one. */
    private boolean slot(GuiGraphics graphics, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        graphics.fill(x, y, x + SLOT, y + SLOT, BORDER_DARK);
        graphics.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0xFF141C26);
        boolean over = mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT;
        if (over) graphics.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0xFF2A3A4C);
        if (!stack.isEmpty()) graphics.renderItem(stack, x + 1, y + 1);
        return over && !stack.isEmpty();
    }

    @Override
    public void removed() {
        remember();
        super.removed();
    }
}
