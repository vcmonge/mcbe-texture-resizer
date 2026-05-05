package service;

import java.awt.image.BufferedImage;

/**
 * Implementación del algoritmo Area Resampling (Pixel Area Relation) para
 * reducción de imágenes. Cada píxel se trata como un área 1×1 y se calcula
 * la intersección exacta con los píxeles fuente.
 * 
 * Usa alfa premultiplicado para mezclar correctamente píxeles con diferente
 * nivel de transparencia, evitando halos oscuros en bordes semitransparentes.
 * 
 * Aplica un umbral binario al alfa (≥ 0.5 → opaco, &lt; 0.5 → transparente)
 * para preservar bordes duros en texturas con transparencia binaria.
 * 
 * @author vmonge
 */
public final class AreaResampler {

    private AreaResampler() {
        throw new UnsupportedOperationException("Clase utilitaria - no instanciar");
    }

    /**
     * Redimensiona una imagen usando area resampling con alfa premultiplicado.
     *
     * @param source   Imagen fuente
     * @param dstWidth Ancho destino (debe ser &gt; 0)
     * @param dstHeight Alto destino (debe ser &gt; 0)
     * @return Nueva BufferedImage en TYPE_INT_ARGB
     */
    public static BufferedImage resize(BufferedImage source, int dstWidth, int dstHeight) {
        if (source == null) {
            throw new IllegalArgumentException("La imagen fuente no puede ser null");
        }
        if (dstWidth < 1 || dstHeight < 1) {
            throw new IllegalArgumentException("Las dimensiones destino deben ser >= 1");
        }

        int srcWidth = source.getWidth();
        int srcHeight = source.getHeight();

        double scaleX = (double) srcWidth / dstWidth;
        double scaleY = (double) srcHeight / dstHeight;

        int[] srcPixels = source.getRGB(0, 0, srcWidth, srcHeight, null, 0, srcWidth);

        BufferedImage dest = new BufferedImage(dstWidth, dstHeight, BufferedImage.TYPE_INT_ARGB);
        int[] dstPixels = new int[dstWidth * dstHeight];

        for (int dy = 0; dy < dstHeight; dy++) {
            double y0 = dy * scaleY;
            double y1 = (dy + 1) * scaleY;

            int syStart = (int) y0;
            int syEnd = Math.min((int) y1, srcHeight - 1);
            if (y1 == (int) y1 && syEnd > syStart) {
                syEnd = (int) y1 - 1;
            }

            for (int dx = 0; dx < dstWidth; dx++) {
                double x0 = dx * scaleX;
                double x1 = (dx + 1) * scaleX;

                int sxStart = (int) x0;
                int sxEnd = Math.min((int) x1, srcWidth - 1);
                if (x1 == (int) x1 && sxEnd > sxStart) {
                    sxEnd = (int) x1 - 1;
                }

                double sumR = 0.0, sumG = 0.0, sumB = 0.0, sumA = 0.0;
                double sumArea = 0.0;

                for (int sy = syStart; sy <= syEnd; sy++) {
                    double iy0 = Math.max(y0, sy);
                    double iy1 = Math.min(y1, sy + 1);
                    double hOverlap = iy1 - iy0;
                    if (hOverlap <= 0.0) continue;

                    int rowOffset = sy * srcWidth;

                    for (int sx = sxStart; sx <= sxEnd; sx++) {
                        double ix0 = Math.max(x0, sx);
                        double ix1 = Math.min(x1, sx + 1);
                        double wOverlap = ix1 - ix0;
                        if (wOverlap <= 0.0) continue;

                        double area = wOverlap * hOverlap;

                        int argb = srcPixels[rowOffset + sx];
                        double a = ((argb >> 24) & 0xFF) / 255.0;
                        double r = ((argb >> 16) & 0xFF) / 255.0;
                        double g = ((argb >> 8) & 0xFF) / 255.0;
                        double b = (argb & 0xFF) / 255.0;

                        double rp = r * a;
                        double gp = g * a;
                        double bp = b * a;

                        sumR += rp * area;
                        sumG += gp * area;
                        sumB += bp * area;
                        sumA += a * area;
                        sumArea += area;
                    }
                }

                int finalA, finalR, finalG, finalB;

                if (sumArea == 0.0 || sumA == 0.0) {
                    finalA = 0;
                    finalR = 0;
                    finalG = 0;
                    finalB = 0;
                } else {
                    double aOut = sumA / sumArea;

                    if (aOut >= 0.5) {
                        finalA = 255;
                        finalR = clamp255(sumR / sumA);
                        finalG = clamp255(sumG / sumA);
                        finalB = clamp255(sumB / sumA);
                    } else {
                        finalA = 0;
                        finalR = 0;
                        finalG = 0;
                        finalB = 0;
                    }
                }

                dstPixels[dy * dstWidth + dx] =
                        (finalA << 24) | (finalR << 16) | (finalG << 8) | finalB;
            }
        }

        dest.setRGB(0, 0, dstWidth, dstHeight, dstPixels, 0, dstWidth);
        return dest;
    }

    /**
     * Redimensiona tratando RGB y Alpha como canales independientes.
     * Diseñado para TGA donde el alfa es una máscara de recorte y el RGB
     * contiene información válida en toda la imagen (foreground + background).
     *
     * <ul>
     *   <li>RGB: promedio ponderado por área sin premultiplicar por alfa</li>
     *   <li>Alpha: promedio ponderado por área con umbral binario (≥ 0.5 → 255)</li>
     * </ul>
     *
     * @param source    Imagen fuente
     * @param dstWidth  Ancho destino (debe ser &gt; 0)
     * @param dstHeight Alto destino (debe ser &gt; 0)
     * @return Nueva BufferedImage en TYPE_INT_ARGB
     */
    public static BufferedImage resizeIndependentAlpha(BufferedImage source,
            int dstWidth, int dstHeight) {
        if (source == null) {
            throw new IllegalArgumentException("La imagen fuente no puede ser null");
        }
        if (dstWidth < 1 || dstHeight < 1) {
            throw new IllegalArgumentException("Las dimensiones destino deben ser >= 1");
        }

        int srcWidth = source.getWidth();
        int srcHeight = source.getHeight();

        double scaleX = (double) srcWidth / dstWidth;
        double scaleY = (double) srcHeight / dstHeight;

        int[] srcPixels = source.getRGB(0, 0, srcWidth, srcHeight, null, 0, srcWidth);

        BufferedImage dest = new BufferedImage(dstWidth, dstHeight, BufferedImage.TYPE_INT_ARGB);
        int[] dstPixels = new int[dstWidth * dstHeight];

        for (int dy = 0; dy < dstHeight; dy++) {
            double y0 = dy * scaleY;
            double y1 = (dy + 1) * scaleY;

            int syStart = (int) y0;
            int syEnd = Math.min((int) y1, srcHeight - 1);
            if (y1 == (int) y1 && syEnd > syStart) {
                syEnd = (int) y1 - 1;
            }

            for (int dx = 0; dx < dstWidth; dx++) {
                double x0 = dx * scaleX;
                double x1 = (dx + 1) * scaleX;

                int sxStart = (int) x0;
                int sxEnd = Math.min((int) x1, srcWidth - 1);
                if (x1 == (int) x1 && sxEnd > sxStart) {
                    sxEnd = (int) x1 - 1;
                }

                double sumR = 0.0, sumG = 0.0, sumB = 0.0, sumA = 0.0;
                double sumArea = 0.0;

                for (int sy = syStart; sy <= syEnd; sy++) {
                    double iy0 = Math.max(y0, sy);
                    double iy1 = Math.min(y1, sy + 1);
                    double hOverlap = iy1 - iy0;
                    if (hOverlap <= 0.0) continue;

                    int rowOffset = sy * srcWidth;

                    for (int sx = sxStart; sx <= sxEnd; sx++) {
                        double ix0 = Math.max(x0, sx);
                        double ix1 = Math.min(x1, sx + 1);
                        double wOverlap = ix1 - ix0;
                        if (wOverlap <= 0.0) continue;

                        double area = wOverlap * hOverlap;

                        int argb = srcPixels[rowOffset + sx];
                        double a = ((argb >> 24) & 0xFF) / 255.0;
                        double r = ((argb >> 16) & 0xFF) / 255.0;
                        double g = ((argb >> 8) & 0xFF) / 255.0;
                        double b = (argb & 0xFF) / 255.0;

                        sumR += r * area;
                        sumG += g * area;
                        sumB += b * area;
                        sumA += a * area;
                        sumArea += area;
                    }
                }

                int finalA, finalR, finalG, finalB;

                if (sumArea == 0.0) {
                    finalA = 0;
                    finalR = 0;
                    finalG = 0;
                    finalB = 0;
                } else {
                    finalR = clamp255(sumR / sumArea);
                    finalG = clamp255(sumG / sumArea);
                    finalB = clamp255(sumB / sumArea);

                    double aOut = sumA / sumArea;
                    finalA = (aOut >= 0.5) ? 255 : 0;
                }

                dstPixels[dy * dstWidth + dx] =
                        (finalA << 24) | (finalR << 16) | (finalG << 8) | finalB;
            }
        }

        dest.setRGB(0, 0, dstWidth, dstHeight, dstPixels, 0, dstWidth);
        return dest;
    }

    private static int clamp255(double v) {
        int i = (int) Math.round(v * 255.0);
        return (i < 0) ? 0 : (i > 255) ? 255 : i;
    }
}
