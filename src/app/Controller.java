package app;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Controller {
    public AnchorPane root;
    public ImageView originalImageView;
    public ImageView editedImageView;
    public Spinner<Integer> factorSpinner;
    public Text paletteText;

    FileChooser fileChooser = new FileChooser();
    List<Color> palette = new ArrayList<>();
    List<Rectangle> rectangles = new ArrayList<>();

    public void initialize() {

        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("Images", "*.png", "*.bmp", "*.jpg");
        fileChooser.getExtensionFilters().add(filter);

        factorSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 256));
        factorSpinner.setOnMouseClicked(mouseEvent -> {
            int factor = factorSpinner.getValue();
            palette.clear();

            if (factor == 0) {
                addColorToPalette(Color.BLACK);
                addColorToPalette(Color.WHITE);
            } else
                for (int x = 0; x <= factor; x++)
                    for (int y = 0; y <= factor; y++)
                        for (int z = 0; z <= factor; z++)
                            addColorToPalette(new Color((double) x / factor, (double) y / factor, (double) z / factor, 1.0));

        });
        factorSpinner.getOnMouseClicked().handle(null);

    }

    private void addColorToPalette(Color newColor) {
        palette.add(newColor);
        rectangles.forEach(node -> root.getChildren().remove(node));
        rectangles.clear();

        double x = 229.0;
        double y = 419.0;

        double width = 658.0 / palette.size();
        double height = 78.0;

        for (Color color : palette) {
            Rectangle rectangle = new Rectangle(x, y, width, height);
            rectangle.setFill(color);
            root.getChildren().add(rectangle);
            rectangles.add(rectangle);
            x += width;
        }

        paletteText.setText(palette.size() + "-color palette");
    }

    public void loadImage() {
        File file = fileChooser.showOpenDialog(root.getScene().getWindow());
        if (file != null) {
            Image image = new Image(file.toURI().toString());
            originalImageView.setImage(image);
            editedImageView.setImage(image);
        }
    }


    public void saveImage() throws IOException {
        File file = fileChooser.showSaveDialog(root.getScene().getWindow());
        if (file != null) {
            String name = file.getName();
            String extension = name.substring(1 + name.lastIndexOf(".")).toLowerCase();
            ImageIO.write(SwingFXUtils.fromFXImage(editedImageView.getImage(), null), extension, file);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void applyDither() {
        Image image = originalImageView.getImage();
        PixelReader pixelReader = image.getPixelReader();

        int width = (int) image.getWidth();
        int height = (int) image.getHeight();

        Color[][] pixelMatrix = new Color[width][height];

        WritableImage writableImage = new WritableImage(width, height);
        PixelWriter pixelWriter = writableImage.getPixelWriter();

        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                pixelMatrix[x][y] = pixelReader.getColor(x, y);

        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++) {
                Color oldColor = pixelMatrix[x][y];

                Color newColor = Processing.quantize(oldColor, factorSpinner.getValue());

                pixelMatrix[x][y] = newColor;

                double redError = oldColor.getRed() - newColor.getRed();
                double greenError = oldColor.getGreen() - newColor.getGreen();
                double blueError = oldColor.getBlue() - newColor.getBlue();

                try {
                    pixelMatrix[x + 1][y] = Processing.addErrorToColor(pixelMatrix[x + 1][y], redError, greenError, blueError, 7.0 / 16);
                } catch (IndexOutOfBoundsException ignored) {
                }
                try {
                    pixelMatrix[x - 1][y + 1] = Processing.addErrorToColor(pixelMatrix[x - 1][y + 1], redError, greenError, blueError, 3.0 / 16);
                } catch (IndexOutOfBoundsException ignored) {
                }
                try {
                    pixelMatrix[x][y + 1] = Processing.addErrorToColor(pixelMatrix[x][y + 1], redError, greenError, blueError, 5.0 / 16);
                } catch (IndexOutOfBoundsException ignored) {
                }
                try {
                    pixelMatrix[x + 1][y + 1] = Processing.addErrorToColor(pixelMatrix[x + 1][y + 1], redError, greenError, blueError, 1.0 / 16);
                } catch (IndexOutOfBoundsException ignored) {
                }

            }

        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                pixelWriter.setColor(x, y, pixelMatrix[x][y]);

        editedImageView.setImage(writableImage);
    }

}
