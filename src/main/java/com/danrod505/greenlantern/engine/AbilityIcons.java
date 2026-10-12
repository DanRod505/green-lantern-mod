package com.danrod505.greenlantern.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * The icons of the wheel and the HUD, drawn from the preset and the colour of each ability (presets
 * batch 5): each preset has a shape (a fist, a beam, a wing, a shield...), the ability may pick
 * another with {@code "icone"} in its settings, and it is painted in the ability's colour
 * ({@code "icone_cor"}, else {@code "cor"}, else the character's main colour) with a dark outline and
 * a light shine. The supreme ability gets a gold border; the sheet also has the icons of the extra
 * functions and of the traits, and its last cell is the emblem. Pure Java: the client turns the
 * pixels into a texture ({@code client.engine.EngineIcons}).
 */
public final class AbilityIcons {
    public static final int SIZE = 16;

    private static final int EMPTY = 0;
    private static final int MAIN = 1;
    private static final int DARK = 2;
    private static final int SHINE = 3;
    private static final int SECOND = 4;
    private static final int OUTLINE = 5;

    /** A 16x16 drawing: the codes above, row by row. */
    private static final class Canvas {
        final int[] px = new int[SIZE * SIZE];

        Canvas set(int x, int y, int code) {
            if (x >= 0 && y >= 0 && x < SIZE && y < SIZE) px[y * SIZE + x] = code;
            return this;
        }

        int get(int x, int y) {
            return x >= 0 && y >= 0 && x < SIZE && y < SIZE ? px[y * SIZE + x] : EMPTY;
        }

        Canvas disc(double cx, double cy, double r, int code) {
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    double dx = x + 0.5 - cx, dy = y + 0.5 - cy;
                    if (dx * dx + dy * dy <= r * r) set(x, y, code);
                }
            }
            return this;
        }

        Canvas ring(double cx, double cy, double r, double width, int code) {
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    double d = Math.hypot(x + 0.5 - cx, y + 0.5 - cy);
                    if (Math.abs(d - r) <= width / 2) set(x, y, code);
                }
            }
            return this;
        }

        Canvas rect(int x0, int y0, int x1, int y1, int code) {
            for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) set(x, y, code);
            return this;
        }

        Canvas line(double x0, double y0, double x1, double y1, double width, int code) {
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    double px0 = x + 0.5, py0 = y + 0.5;
                    double vx = x1 - x0, vy = y1 - y0;
                    double len2 = vx * vx + vy * vy;
                    double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((px0 - x0) * vx + (py0 - y0) * vy) / len2));
                    double dx = px0 - (x0 + t * vx), dy = py0 - (y0 + t * vy);
                    if (dx * dx + dy * dy <= width * width / 4) set(x, y, code);
                }
            }
            return this;
        }

        /** A filled polygon through the points {@code x0, y0, x1, y1...}. */
        Canvas poly(int code, double... points) {
            int n = points.length / 2;
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    double px0 = x + 0.5, py0 = y + 0.5;
                    boolean inside = false;
                    for (int i = 0, j = n - 1; i < n; j = i++) {
                        double xi = points[2 * i], yi = points[2 * i + 1], xj = points[2 * j], yj = points[2 * j + 1];
                        if ((yi > py0) != (yj > py0) && px0 < (xj - xi) * (py0 - yi) / (yj - yi) + xi) inside = !inside;
                    }
                    if (inside) set(x, y, code);
                }
            }
            return this;
        }

        /** A dark line around everything drawn. */
        Canvas outline() {
            int[] copy = px.clone();
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    if (copy[y * SIZE + x] != EMPTY) continue;
                    for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                        int nx = x + d[0], ny = y + d[1];
                        if (nx >= 0 && ny >= 0 && nx < SIZE && ny < SIZE && copy[ny * SIZE + nx] != EMPTY && copy[ny * SIZE + nx] != OUTLINE) {
                            px[y * SIZE + x] = OUTLINE;
                            break;
                        }
                    }
                }
            }
            return this;
        }
    }

    private static final Map<String, Consumer<Canvas>> SHAPES = new HashMap<>();

    private static void shape(String name, Consumer<Canvas> draw) {
        SHAPES.put(name, draw);
    }

    static {
        shape("punho", c -> c.rect(4, 4, 11, 11, MAIN).rect(4, 4, 11, 5, SHINE).line(6.5, 4, 6.5, 9, 1, DARK).line(9.5, 4, 9.5, 9, 1, DARK)
                .rect(2, 7, 4, 11, MAIN).rect(5, 12, 10, 14, DARK));
        shape("punhos", c -> c.rect(1, 3, 7, 9, MAIN).rect(1, 3, 7, 4, SHINE).line(4, 3, 4, 7, 1, DARK)
                .rect(8, 7, 14, 13, MAIN).rect(8, 7, 14, 8, SHINE).line(11, 7, 11, 11, 1, DARK));
        shape("espada", c -> c.line(3, 13, 12, 4, 2.2, SHINE).line(4, 12, 12, 4, 1, MAIN).line(2, 10, 6, 14, 1.6, SECOND).line(1.5, 14.5, 3.5, 12.5, 1.6, DARK));
        shape("mao", c -> c.rect(4, 7, 11, 13, MAIN).line(5, 7, 5, 2, 1.6, MAIN).line(7.5, 7, 7.5, 1.5, 1.6, MAIN).line(10, 7, 10, 2, 1.6, MAIN)
                .line(12, 9, 14, 5, 1.6, MAIN).rect(4, 12, 11, 13, DARK));
        shape("investida", c -> c.poly(MAIN, 6, 3, 14, 8, 6, 13, 8, 8).line(1, 5, 6, 5, 1.2, SHINE).line(0, 8, 7, 8, 1.2, SHINE).line(1, 11, 6, 11, 1.2, SHINE));
        shape("pisao", c -> c.rect(5, 1, 9, 7, MAIN).rect(5, 7, 12, 10, MAIN).rect(5, 1, 6, 9, SHINE).rect(4, 10, 12, 11, DARK).rect(0, 13, 15, 14, SECOND)
                .line(2, 13, 0, 11, 1, SHINE).line(14, 13, 15.5, 10.5, 1, SHINE).line(8, 14, 7, 15.5, 1, DARK));
        shape("bola", c -> c.line(1, 8, 8, 8, 1.6, SHINE).line(2, 5, 7, 6, 1, DARK).line(2, 11, 7, 10, 1, DARK).disc(10.5, 8, 4, MAIN).disc(9.5, 7, 1.8, SHINE));
        shape("leque", c -> c.line(2, 13, 14, 2, 1.6, MAIN).line(2, 13, 15, 8, 1.6, MAIN).line(2, 13, 9, 15, 1.6, MAIN).disc(2.5, 13, 2, SECOND)
                .disc(13.5, 2.5, 1.4, SHINE).disc(14.5, 8, 1.4, SHINE).disc(9.5, 14.5, 1.2, SHINE));
        shape("feixe", c -> c.line(1, 14, 15, 2, 4.2, MAIN).line(1, 14, 15, 2, 1.6, SHINE).disc(2.5, 13.5, 2.4, SECOND));
        shape("raio", c -> c.poly(MAIN, 9, 0, 3, 9, 7, 9, 5, 16, 13, 6, 9, 6, 11, 0).line(8.5, 1, 5, 8, 1, SHINE));
        shape("bumerangue", c -> c.line(2, 5, 8, 12, 3, MAIN).line(8, 12, 14, 5, 3, MAIN).line(2.5, 5, 7.5, 11, 1, SHINE).disc(8, 12, 1.2, DARK));
        shape("explosao", c -> {
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                c.line(8, 8, 8 + Math.cos(a) * 7, 8 + Math.sin(a) * 7, 2.2, i % 2 == 0 ? MAIN : DARK);
            }
            c.disc(8, 8, 3.8, SECOND).disc(8, 8, 2, SHINE);
        });
        shape("ondas", c -> c.ring(8, 8, 6.5, 1.4, MAIN).ring(8, 8, 3.8, 1.4, DARK).disc(8, 8, 1.6, SHINE));
        shape("cone", c -> c.poly(MAIN, 1, 8, 15, 1, 15, 15).poly(SHINE, 1, 8, 15, 5, 15, 8).disc(2, 8, 1.8, SECOND));
        shape("chuva", c -> {
            c.disc(8, 4, 3.5, DARK).disc(4.5, 5, 2.5, DARK).disc(11.5, 5, 2.5, DARK);
            for (int i = 0; i < 4; i++) c.line(3 + i * 3.3, 9, 2 + i * 3.3, 14, 1.4, i % 2 == 0 ? MAIN : SHINE);
        });
        shape("asa", c -> c.poly(MAIN, 1, 13, 3, 7, 8, 3, 15, 1, 14, 4, 15, 6, 12, 8, 13, 10, 9, 11, 9, 13).line(2, 12, 13, 2.5, 1, SHINE)
                .line(9, 7, 13.5, 4.5, 1, DARK).line(7, 10, 12, 8.5, 1, DARK));
        shape("velocidade", c -> c.line(1, 4, 8, 4, 1.4, SHINE).line(0, 8, 6, 8, 1.4, SHINE).line(1, 12, 8, 12, 1.4, SHINE)
                .poly(MAIN, 11, 1, 7, 8, 10, 8, 8, 15, 14, 6, 11, 6, 13, 1));
        shape("portal", c -> c.ring(8, 8, 6.2, 1.6, MAIN).ring(8, 8, 3.6, 1.4, SECOND).disc(8, 8, 1.4, SHINE));
        shape("seta_cima", c -> c.poly(MAIN, 8, 1, 14, 8, 10, 8, 10, 15, 6, 15, 6, 8, 2, 8).line(8, 2, 8, 14, 1, SHINE));
        shape("gancho", c -> c.line(2, 14, 11, 5, 1.2, DARK).ring(11, 5, 3.4, 1.6, MAIN).rect(11, 1, 15, 3, EMPTY).disc(13.5, 3.2, 1.3, SHINE));
        shape("onda", c -> {
            for (int x = 0; x < SIZE; x++) {
                double y = 6 + Math.sin(x * 0.7) * 2;
                c.line(x, y, x + 1, 6 + Math.sin((x + 1) * 0.7) * 2, 1.8, MAIN);
                c.line(x, y + 5, x + 1, 11 + Math.sin((x + 1) * 0.7) * 2, 1.8, SHINE);
            }
        });
        shape("fantasma", c -> c.disc(8, 6, 5, MAIN).rect(3, 6, 12, 12, MAIN).poly(EMPTY, 3, 13, 5, 11, 7, 13, 9, 11, 11, 13, 13, 11, 13, 14, 3, 14)
                .rect(5, 5, 6, 7, DARK).rect(9, 5, 10, 7, DARK));
        shape("escudo", c -> c.poly(MAIN, 2, 2, 14, 2, 14, 8, 8, 15, 2, 8).poly(SECOND, 6, 4, 10, 4, 10, 8, 8, 11, 6, 8).line(3, 3, 3, 8, 1, SHINE));
        shape("bolha", c -> c.ring(8, 8, 6.5, 1.6, MAIN).disc(8, 8, 5.2, DARK).disc(5.5, 5.5, 1.6, SHINE));
        shape("parede", c -> c.rect(1, 2, 14, 14, MAIN).line(1, 6, 14, 6, 1, DARK).line(1, 10, 14, 10, 1, DARK).line(5, 2, 5, 6, 1, DARK)
                .line(10, 6, 10, 10, 1, DARK).line(5, 10, 5, 14, 1, DARK).rect(1, 2, 14, 2, SHINE));
        shape("espelho", c -> c.poly(MAIN, 4, 1, 12, 1, 14, 15, 2, 15).line(5, 3, 3, 13, 1.2, SHINE).line(9, 4, 12, 7, 1, SHINE));
        shape("corrente", c -> c.ring(4.5, 4.5, 2.6, 1.4, MAIN).ring(8, 8, 2.6, 1.4, SHINE).ring(11.5, 11.5, 2.6, 1.4, MAIN));
        shape("ima", c -> c.ring(8, 8, 5.5, 3, MAIN).rect(2, 8, 13, 15, EMPTY).rect(2, 8, 5, 13, MAIN).rect(10, 8, 13, 13, MAIN)
                .rect(2, 11, 5, 13, SHINE).rect(10, 11, 13, 13, SHINE));
        shape("espiral", c -> {
            for (int i = 0; i < 70; i++) {
                double t = i / 70.0 * 4.4;
                double r = 1.0 + t * 1.45;
                c.disc(8 + Math.cos(t * 2.4) * r, 8 + Math.sin(t * 2.4) * r, 0.9, i < 25 ? SHINE : MAIN);
            }
        });
        shape("olho", c -> c.poly(MAIN, 1, 8, 5, 4, 11, 4, 15, 8, 11, 12, 5, 12).disc(8, 8, 3, SECOND).disc(8, 8, 1.4, DARK).disc(7, 7, 0.8, SHINE));
        shape("relogio", c -> c.disc(8, 8, 6.5, MAIN).disc(8, 8, 5, SHINE).line(8, 8, 8, 4, 1.4, DARK).line(8, 8, 11, 10, 1.4, DARK));
        shape("figura", c -> c.disc(8, 4, 2.8, MAIN).poly(MAIN, 3, 15, 4, 8, 12, 8, 13, 15).line(5, 9, 5, 14, 1, SHINE).disc(7, 3, 0.9, SHINE));
        shape("figuras", c -> c.disc(5, 5, 2.3, DARK).poly(DARK, 1, 14, 2, 8, 8, 8, 9, 14).disc(10.5, 6, 2.5, MAIN).poly(MAIN, 6, 15, 7, 9, 14, 9, 15, 15));
        shape("enxame", c -> {
            double[][] dots = {{3, 3}, {8, 2}, {13, 4}, {5, 8}, {11, 8}, {2, 12}, {8, 12}, {13, 13}};
            for (int i = 0; i < dots.length; i++) c.disc(dots[i][0], dots[i][1], 1.6, i % 3 == 0 ? SHINE : MAIN);
        });
        shape("martelo", c -> c.rect(2, 2, 13, 7, MAIN).rect(2, 2, 13, 3, SHINE).rect(7, 7, 9, 15, DARK));
        shape("torre", c -> c.rect(4, 5, 11, 15, MAIN).rect(3, 2, 5, 5, MAIN).rect(7, 2, 9, 5, MAIN).rect(11, 2, 13, 5, MAIN).rect(7, 8, 8, 10, DARK)
                .line(5, 6, 5, 14, 1, SHINE));
        shape("veiculo", c -> c.poly(MAIN, 1, 11, 3, 6, 11, 6, 15, 9, 15, 12, 1, 12).rect(5, 7, 9, 9, SHINE).disc(4.5, 12.5, 2, DARK).disc(11.5, 12.5, 2, DARK));
        shape("vulto", c -> c.disc(10, 4, 2.5, MAIN).poly(MAIN, 7, 15, 8, 8, 13, 8, 12, 15).line(1, 7, 6, 7, 1.2, SHINE).line(0, 11, 6, 11, 1.2, SHINE)
                .line(2, 14, 6, 14, 1.2, DARK));
        shape("invisivel", c -> c.poly(DARK, 1, 8, 5, 4, 11, 4, 15, 8, 11, 12, 5, 12).disc(8, 8, 2.6, MAIN).line(2, 14, 14, 2, 1.6, SHINE));
        shape("nuvem", c -> c.disc(5, 9, 3.6, MAIN).disc(10, 7, 4.2, MAIN).disc(12.5, 11, 3, DARK).rect(3, 11, 13, 13, MAIN).disc(9, 5.5, 1.6, SHINE));
        shape("encolher", c -> c.line(2, 2, 6, 6, 1.6, SHINE).line(14, 2, 10, 6, 1.6, SHINE).line(2, 14, 6, 10, 1.6, SHINE).line(14, 14, 10, 10, 1.6, SHINE)
                .rect(6, 6, 10, 10, MAIN));
        shape("coracao", c -> c.disc(5, 6, 3.4, MAIN).disc(11, 6, 3.4, MAIN).poly(MAIN, 1.8, 7, 14.2, 7, 8, 14.5).disc(4.5, 5, 1.2, SHINE));
        shape("chama", c -> c.poly(MAIN, 8, 1, 13, 8, 12, 13, 8, 15, 4, 13, 3, 8, 6, 9).poly(SECOND, 8, 7, 10, 11, 8, 14, 6, 11).disc(8, 12, 1, SHINE));
        shape("crescer", c -> c.rect(5, 9, 10, 15, DARK).rect(3, 3, 12, 15, EMPTY).poly(MAIN, 8, 0, 13, 5, 10, 5, 10, 11, 6, 11, 6, 5, 3, 5).rect(4, 12, 11, 15, DARK));
        shape("estrela", c -> c.poly(MAIN, 8, 1, 10, 6, 15, 6, 11, 9.5, 12.5, 15, 8, 11.5, 3.5, 15, 5, 9.5, 1, 6, 6, 6).disc(8, 8, 1.5, SHINE));
        shape("coroa", c -> c.poly(SECOND, 1, 4, 5, 8, 8, 2, 11, 8, 15, 4, 14, 12, 2, 12).rect(2, 12, 14, 14, MAIN).disc(8, 5, 1.2, SHINE)
                .rect(4, 12, 5, 13, SHINE).rect(10, 12, 11, 13, SHINE));
        shape("picareta", c -> c.line(3, 13, 11, 5, 1.6, DARK).poly(MAIN, 4, 3, 9, 2, 14, 7, 13, 12, 11, 5));
        shape("lupa", c -> c.ring(6.5, 6.5, 4.2, 1.8, MAIN).disc(6.5, 6.5, 3.1, SHINE).line(9.5, 9.5, 14, 14, 2.2, DARK));
        shape("broto", c -> c.line(8, 15, 8, 7, 1.4, DARK).poly(MAIN, 8, 9, 3, 6, 4, 2, 8, 6).poly(MAIN, 8, 8, 13, 4, 12, 9).line(4.5, 3.5, 7, 6, 1, SHINE));
        shape("floco", c -> {
            for (int i = 0; i < 3; i++) {
                double a = i * Math.PI / 3;
                c.line(8 - Math.cos(a) * 7, 8 - Math.sin(a) * 7, 8 + Math.cos(a) * 7, 8 + Math.sin(a) * 7, 1.6, i == 0 ? SHINE : MAIN);
            }
            c.disc(8, 8, 1.8, SECOND);
        });
        shape("bloco", c -> c.poly(MAIN, 8, 1, 15, 4.5, 8, 8, 1, 4.5).poly(DARK, 1, 4.5, 8, 8, 8, 15, 1, 11.5).poly(SHINE, 15, 4.5, 8, 8, 8, 15, 15, 11.5));
        shape("lampada", c -> c.disc(8, 6, 4.6, MAIN).disc(6.5, 4.5, 1.5, SHINE).rect(6, 10, 10, 13, DARK).rect(7, 13, 9, 14, DARK));
        // The new model of abilities (2.6.0): the classes and presets of the world.
        shape("troca", c -> c.ring(8, 8, 5, 2, MAIN).rect(0, 7, 6, 9, EMPTY).rect(10, 7, 16, 9, EMPTY)
                .poly(SHINE, 1, 6, 6, 6, 3.5, 10).poly(SHINE, 10, 10, 15, 10, 12.5, 6));
        shape("bau", c -> c.rect(2, 4, 13, 13, MAIN).rect(2, 4, 13, 6, SHINE).rect(2, 8, 13, 8, DARK).rect(7, 7, 8, 10, SECOND).rect(2, 13, 13, 13, DARK));
        shape("machado", c -> c.line(4, 14, 11, 3, 1.6, DARK).poly(MAIN, 9, 2, 15, 5, 13, 10, 10, 7).line(10, 3, 14, 5.5, 1, SHINE));
        shape("pa", c -> c.line(3, 13, 10, 6, 1.6, DARK).poly(MAIN, 9, 3, 14, 2, 13, 7, 10, 8).line(10, 3.5, 13, 3, 1, SHINE)
                .line(2, 14.5, 4, 12, 2.4, DARK));
        shape("anzol", c -> c.line(9, 1, 9, 10, 1.6, MAIN).ring(6.5, 10, 2.6, 1.6, MAIN).rect(4, 6, 6, 9, EMPTY).poly(SHINE, 3, 7, 5, 9, 3.5, 10.5)
                .disc(9, 1.5, 1.2, SECOND));
        shape("engrenagem", c -> {
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                c.line(8, 8, 8 + Math.cos(a) * 6.5, 8 + Math.sin(a) * 6.5, 2.4, MAIN);
            }
            c.disc(8, 8, 4.6, MAIN).disc(8, 8, 1.8, DARK).disc(6.5, 6.5, 1, SHINE);
        });
    }

    /** The shape of each preset, fight and world, when the ability doesn't pick one. */
    private static final Map<String, String> BY_PRESET = Map.ofEntries(
            Map.entry("soco_forte", "punho"), Map.entry("sequencia", "punhos"), Map.entry("investida", "investida"), Map.entry("pisao", "pisao"),
            Map.entry("agarrar_e_arremessar", "mao"), Map.entry("golpe_de_arma", "espada"),
            Map.entry("projetil", "bola"), Map.entry("leque", "leque"), Map.entry("feixe_continuo", "feixe"), Map.entry("raio_do_ceu", "raio"),
            Map.entry("balistico", "bola"), Map.entry("arremesso_que_volta", "bumerangue"),
            Map.entry("explosao_no_ponto", "explosao"), Map.entry("onda_em_volta", "ondas"), Map.entry("cone", "cone"), Map.entry("chuva", "chuva"),
            Map.entry("pulso", "ondas"),
            Map.entry("voo", "asa"), Map.entry("supervelocidade", "velocidade"), Map.entry("teleporte", "portal"), Map.entry("salto_gigante", "seta_cima"),
            Map.entry("planar", "asa"), Map.entry("gancho", "gancho"), Map.entry("nado_rapido", "onda"), Map.entry("atravessar", "fantasma"),
            Map.entry("bolha", "bolha"), Map.entry("parede", "parede"), Map.entry("bloqueio", "escudo"), Map.entry("pele_dura", "escudo"),
            Map.entry("refletir", "espelho"),
            Map.entry("prender", "corrente"), Map.entry("puxar", "ima"), Map.entry("levitar", "espiral"), Map.entry("lentidao", "relogio"),
            Map.entry("medo_e_confusao", "olho"), Map.entry("dominar", "espiral"),
            Map.entry("ajudante", "figura"), Map.entry("construto", "martelo"), Map.entry("construto_de_hoje", "martelo"), Map.entry("poder_de_hoje", "estrela"), Map.entry("copia", "figuras"), Map.entry("enxame", "enxame"),
            Map.entry("torre", "torre"), Map.entry("veiculo", "veiculo"),
            Map.entry("esquiva", "vulto"), Map.entry("invisivel", "invisivel"), Map.entry("fumaca", "nuvem"), Map.entry("encolher", "encolher"),
            Map.entry("fuga_de_forma", "vulto"),
            Map.entry("cura", "coracao"), Map.entry("furia", "chama"), Map.entry("crescer", "crescer"), Map.entry("aura", "estrela"),
            Map.entry("sentidos", "olho"),
            Map.entry("grande_obra", "coroa"), Map.entry("suprema_de_combate", "coroa"),
            Map.entry("minerar_area", "picareta"), Map.entry("construir", "bloco"), Map.entry("plantar_e_colher", "broto"),
            Map.entry("gelo_e_calor", "floco"), Map.entry("luz", "lampada"), Map.entry("mover_blocos", "bloco"), Map.entry("abrir_caminho", "picareta"),
            Map.entry("procurar", "lupa"),
            Map.entry("cortar_arvore", "machado"), Map.entry("cavar_solto", "pa"), Map.entry("aplanar", "pa"), Map.entry("pescar", "anzol"),
            Map.entry("projeto_em_fantasma", "bloco"), Map.entry("trocar_bloco", "troca"), Map.entry("ligar_maquina", "engrenagem"),
            Map.entry("domar_e_guiar", "corrente"), Map.entry("clima", "nuvem"), Map.entry("rastro", "olho"), Map.entry("ima_de_itens", "ima"),
            Map.entry("ajudante_de_trabalho", "figura"), Map.entry("organizar", "bau"), Map.entry("levar_junto", "figuras"),
            Map.entry("montaria", "veiculo"));

    /** The shape of each class, for a preset that has none (or an ability with no preset yet). */
    private static final Map<AbilityKind, String> BY_KIND = Map.ofEntries(Map.entry(AbilityKind.STRIKE, "punho"), Map.entry(AbilityKind.SHOT, "bola"),
            Map.entry(AbilityKind.BLAST, "explosao"), Map.entry(AbilityKind.CONTROL, "espiral"), Map.entry(AbilityKind.DEFENSE, "escudo"),
            Map.entry(AbilityKind.SUMMON, "figura"), Map.entry(AbilityKind.EVASION, "vulto"), Map.entry(AbilityKind.EMPOWER, "seta_cima"),
            Map.entry(AbilityKind.MOBILITY, "portal"), Map.entry(AbilityKind.GATHERING, "picareta"), Map.entry(AbilityKind.BUILDING, "bloco"),
            Map.entry(AbilityKind.TRANSMUTATION, "troca"), Map.entry(AbilityKind.NATURE, "broto"), Map.entry(AbilityKind.DETECTION, "lupa"),
            Map.entry(AbilityKind.LOGISTICS, "bau"));

    /** The shape of each trait (passiva), in the HUD while it is on. */
    private static final Map<String, String> BY_TRAIT = Map.ofEntries(Map.entry("voo", "asa"), Map.entry("levitar", "espiral"),
            Map.entry("supervelocidade", "velocidade"), Map.entry("aquatico", "onda"), Map.entry("planar", "asa"), Map.entry("superforca", "punho"),
            Map.entry("invulneravel", "escudo"), Map.entry("visao_no_escuro", "olho"), Map.entry("sentidos_agucados", "olho"),
            Map.entry("pulo_alto", "seta_cima"), Map.entry("escalar", "mao"), Map.entry("regeneracao", "coracao"), Map.entry("elastico", "mao"),
            Map.entry("imune_a_veneno", "bolha"));

    /** The gold of the border of the supreme ability. */
    public static final int GOLD = 0xF2B705;

    private AbilityIcons() {}

    public static Set<String> shapes() {
        return new TreeSet<>(SHAPES.keySet());
    }

    public static boolean hasShape(String name) {
        return SHAPES.containsKey(name);
    }

    /** The shape a preset gets when the ability doesn't pick one, or null. */
    public static String presetShape(String preset) {
        return BY_PRESET.get(preset);
    }

    /** The shape of a class of ability. */
    public static String kindShape(AbilityKind kind) {
        return BY_KIND.getOrDefault(kind, "estrela");
    }

    /** The shape of a trait. */
    public static String traitShape(String trait) {
        return BY_TRAIT.getOrDefault(trait, "estrela");
    }

    /** The shape of an ability: its {@code icone}, its preset's, or its class's. */
    public static String shape(CharacterData.Ability ability, AbilityKind kind) {
        return shape(ability.settings(), ability.preset(), kind);
    }

    /** The shape of one more function of an ability: its own {@code icone}, its preset's, or the ability's class's. */
    public static String shape(CharacterData.Function function, AbilityKind kind) {
        return shape(function.settings(), function.preset(), kind);
    }

    private static String shape(CharacterData.Settings settings, String preset, AbilityKind kind) {
        String chosen = settings.text("icone", "");
        if (SHAPES.containsKey(chosen)) return chosen;
        String byPreset = BY_PRESET.get(preset);
        return byPreset != null ? byPreset : kindShape(kind);
    }

    /** The colour of an ability's icon: {@code icone_cor}, else {@code cor}, else the character's main colour. */
    public static int color(CharacterData.Ability ability, CharacterData data) {
        CharacterData.Settings s = ability.settings();
        return s.color("icone_cor", s.color("cor", data.colors().main()));
    }

    /**
     * How many cells the sheet of a character has: one per ability in wheel order, one per extra
     * function of each ability (the function the HUD shows before it goes off), one per trait, and
     * the emblem last.
     */
    public static int cells(CharacterData data) {
        int functions = data.abilities().stream().mapToInt(a -> a.functions().size()).sum();
        return data.abilities().size() + functions + data.traits().size() + 1;
    }

    /** The cell of function {@code function} (0 is the default one, the ability's own icon) of ability {@code ability}. */
    public static int functionCell(CharacterData data, int ability, int function) {
        if (function == 0) return ability;
        int cell = data.abilities().size();
        for (int i = 0; i < ability; i++) cell += data.abilities().get(i).functions().size();
        return cell + function - 1;
    }

    /** The cell of trait {@code index}. */
    public static int traitCell(CharacterData data, int index) {
        return cells(data) - 1 - data.traits().size() + index;
    }

    /**
     * The icon sheet of a character, ARGB pixels {@code 16 * cells} wide and 16 high (see
     * {@link #cells}). The icon of the supreme ability has a gold border.
     */
    public static int[] sheet(CharacterData data, List<AbilityKind> kinds) {
        List<CharacterData.Ability> abilities = data.abilities();
        int cells = cells(data);
        int width = SIZE * cells;
        int[] out = new int[width * SIZE];
        int cell = abilities.size();
        for (int i = 0; i < abilities.size(); i++) {
            CharacterData.Ability ability = abilities.get(i);
            paint(out, width, i, SHAPES.get(shape(ability, kinds.get(i))), color(ability, data), data.colors());
            if (ability.supreme()) border(out, width, i);
            for (CharacterData.Function function : ability.functions()) {
                CharacterData.Settings s = function.settings();
                int color = s.color("icone_cor", s.color("cor", color(ability, data)));
                paint(out, width, cell++, SHAPES.get(shape(function, kinds.get(i))), color, data.colors());
            }
        }
        for (CharacterData.Trait trait : data.traits()) paint(out, width, cell++, SHAPES.get(traitShape(trait.type())), data.colors().light(), data.colors());
        paint(out, width, cell, SHAPES.get("estrela"), data.colors().second(), data.colors());
        return out;
    }

    /** A gold frame around a cell (the supreme ability). */
    private static void border(int[] out, int width, int cell) {
        for (int k = 0; k < SIZE; k++) {
            for (int[] p : new int[][] {{k, 0}, {k, SIZE - 1}, {0, k}, {SIZE - 1, k}}) out[p[1] * width + cell * SIZE + p[0]] = 0xFF000000 | GOLD;
        }
    }

    /** One icon of {@code shape} painted in {@code color}, ARGB pixels 16x16 (for the tests and the tools). */
    public static int[] icon(String shape, int color, CharacterData.Colors colors) {
        int[] out = new int[SIZE * SIZE];
        paint(out, SIZE, 0, SHAPES.get(shape), color, colors);
        return out;
    }

    private static void paint(int[] out, int width, int cell, Consumer<Canvas> shape, int color, CharacterData.Colors colors) {
        Canvas canvas = new Canvas();
        shape.accept(canvas);
        canvas.outline();
        int[] palette = {0, color, scale(color, 0.62), mix(color, colors.light(), 0.6), colors.second() == color ? mix(color, 0xFFFFFF, 0.4) : colors.second(),
                scale(color, 0.2)};
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int code = canvas.px[y * SIZE + x];
                out[y * width + cell * SIZE + x] = code == EMPTY ? 0 : 0xFF000000 | palette[code] & 0xFFFFFF;
            }
        }
    }

    private static int scale(int color, double f) {
        int r = (int) ((color >> 16 & 0xFF) * f), g = (int) ((color >> 8 & 0xFF) * f), b = (int) ((color & 0xFF) * f);
        return r << 16 | g << 8 | b;
    }

    private static int mix(int a, int b, double t) {
        int r = (int) Math.round((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
        int g = (int) Math.round((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
        int bl = (int) Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return r << 16 | g << 8 | bl;
    }
}
