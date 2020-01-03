package app;

import javafx.scene.paint.Color;

public class Processing {


    public static Double getGray(Color color) {
        return color.getRed() * 0.3 + color.getGreen() * 0.59 + color.getBlue() * 0.11;
    }

    public static Color addErrorToColor(Color color, double redError, double greenError, double blueError, double x) {
        return new Color(
                Math.min(1.0, Math.max(0.0, color.getRed() + redError * x)),
                Math.min(1.0, Math.max(0.0, color.getGreen() + greenError * x)),
                Math.min(1.0, Math.max(0.0, color.getBlue() + blueError * x)),
                color.getOpacity());
    }

    static Color quantize(Color color, int factor) {
        if (factor == 0) {
            double gray = getGray(color);
            double bw = Math.round(gray);
            return new Color(bw, bw, bw, color.getOpacity());
        }

        double red = (double) (Math.round(factor * color.getRed())) / factor;
        double green = (double) (Math.round(factor * color.getGreen())) / factor;
        double blue = (double) (Math.round(factor * color.getBlue())) / factor;

        return new Color(red, green, blue, color.getOpacity());
    }
}
