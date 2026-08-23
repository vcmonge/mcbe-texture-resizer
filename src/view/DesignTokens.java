package view;

import javafx.scene.Parent;
import javafx.scene.paint.Color;

/**
 * Shared visual tokens for the JavaFX controls and the canvas-rendered grid.
 *
 * JavaFX CSS cannot expose looked-up colors directly to a GraphicsContext, so
 * the palette lives here. The root node receives the CSS-facing aliases while
 * CanvasTextureGrid consumes the same Color instances.
 */
public final class DesignTokens {

    static final String DISPLAY_FONT = "Times New Roman";
    static final String BODY_FONT = "Segoe UI";

    static final Color PRIMARY = Color.web("#292524");
    static final Color PRIMARY_ACTIVE = Color.web("#0c0a09");

    static final Color CANVAS = Color.web("#f5f5f5");
    static final Color CANVAS_SOFT = Color.web("#fafafa");
    static final Color SURFACE_CARD = Color.WHITE;
    static final Color SURFACE_STRONG = Color.web("#f0efed");

    static final Color HAIRLINE = Color.web("#e7e5e4");
    static final Color HAIRLINE_SOFT = Color.web("#f0efed");
    static final Color HAIRLINE_STRONG = Color.web("#d6d3d1");

    static final Color INK = Color.web("#0c0a09");
    static final Color BODY = Color.web("#4e4e4e");
    static final Color BODY_STRONG = Color.web("#292524");
    static final Color MUTED = Color.web("#777169");
    static final Color MUTED_SOFT = Color.web("#a8a29e");
    static final Color ON_PRIMARY = Color.WHITE;

    static final Color GRADIENT_MINT = Color.web("#a7e5d3");
    static final Color GRADIENT_PEACH = Color.web("#f4c5a8");
    static final Color GRADIENT_LAVENDER = Color.web("#c8b8e0");
    static final Color GRADIENT_SKY = Color.web("#a8c8e8");
    static final Color GRADIENT_ROSE = Color.web("#e8b8c4");

    static final Color SUCCESS = Color.web("#16a34a");
    static final Color ERROR = Color.web("#dc2626");
    static final Color SHADOW_SOFT = Color.rgb(12, 10, 9, 0.08);

    private static final String ROOT_LOOKED_UP_COLORS = String.join(" ",
            "-iron-primary: #292524;",
            "-iron-primary-active: #0c0a09;",
            "-iron-canvas: #f5f5f5;",
            "-iron-canvas-soft: #fafafa;",
            "-iron-surface-card: #ffffff;",
            "-iron-surface-strong: #f0efed;",
            "-iron-hairline: #e7e5e4;",
            "-iron-hairline-soft: #f0efed;",
            "-iron-hairline-strong: #d6d3d1;",
            "-iron-ink: #0c0a09;",
            "-iron-body: #4e4e4e;",
            "-iron-body-strong: #292524;",
            "-iron-muted: #777169;",
            "-iron-muted-soft: #a8a29e;",
            "-iron-on-primary: #ffffff;",
            "-iron-gradient-mint: #a7e5d3;",
            "-iron-gradient-peach: #f4c5a8;",
            "-iron-gradient-lavender: #c8b8e0;",
            "-iron-gradient-sky: #a8c8e8;",
            "-iron-gradient-rose: #e8b8c4;",
            "-iron-gradient-mint-soft: rgba(167, 229, 211, 0.42);",
            "-iron-gradient-lavender-soft: rgba(200, 184, 224, 0.38);",
            "-iron-success: #16a34a;",
            "-iron-error: #dc2626;",
            "-iron-shadow-soft: rgba(12, 10, 9, 0.08);",
            "-iron-shadow-dialog: rgba(12, 10, 9, 0.14);"
    );

    private DesignTokens() {
    }

    public static void install(Parent root) {
        root.setStyle(ROOT_LOOKED_UP_COLORS);
    }
}
