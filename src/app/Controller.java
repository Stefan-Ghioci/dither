package app;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Callback;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.pow;

public class Controller {
    public AnchorPane root;
    public ImageView originalImageView;
    public ImageView editedImageView;
    public ListView<Color> paletteListView;
    public Spinner factorSpinner;
    FileChooser fileChooser = new FileChooser();

    //TODO: refactor color palette list view into pure rectangles
    public void initialize() {
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("Images", "*.png", "*.bmp", "*.jpg");
        fileChooser.getExtensionFilters().add(filter);

        paletteListView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<Color> call(ListView<Color> colorListView) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(Color color, boolean empty) {
                        super.updateItem(color, empty);

                        if (color != null) {
                            setBackground(new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)));
                            int paletteSize = 2;
                            if ((int) factorSpinner.getValue() != 0)
                                paletteSize = (int) (pow((int) factorSpinner.getValue() + 1, 3));
                            setPrefWidth(colorListView.getWidth() / paletteSize);
                        } else {
                            setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
                        }
                    }

                };
            }
        });

        factorSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 256));
        factorSpinner.setOnMouseClicked(mouseEvent -> {
            int factor = (int) factorSpinner.getValue();
            paletteListView.getItems().clear();
            if (factor == 0) {
                paletteListView.getItems().add(Color.BLACK);
                paletteListView.getItems().add(Color.WHITE);
            } else
                for (int x = 0; x <= factor; x++)
                    for (int y = 0; y <= factor; y++)
                        for (int z = 0; z <= factor; z++)
                            paletteListView.getItems().add(new Color((double) x / factor, (double) y / factor, (double) z / factor, 1.0));

        });
        factorSpinner.getOnMouseClicked().handle(null);

    }

    public void loadImage() {
        File file = fileChooser.showOpenDialog(root.getScene().getWindow());
        if (file != null) {
            Image image = new Image(file.toURI().toString());
            originalImageView.setImage(image);
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

        List<Color> palette = paletteListView.getItems();
        palette.sort(Comparator.comparing(Color::toString));

        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++) {
                Color oldColor = pixelMatrix[x][y];

                Color newColor = Processing.quantize(oldColor, (Integer) factorSpinner.getValue());

                pixelMatrix[x][y] = newColor;

                double redError = oldColor.getRed() - newColor.getRed();
                double greenError = oldColor.getGreen() - newColor.getGreen();
                double blueError = oldColor.getBlue() - newColor.getBlue();

                try {
                    pixelMatrix[x + 1][y] = Processing.addErrorToColor(pixelMatrix[x + 1][y], redError, greenError, blueError, 7.0 / 16);
                } catch (IndexOutOfBoundsException ignored) {
                }
                try {
                    pixelMatrix[x - 1][y + 1] = Processing.addErrorToColor(pixelMatrix[x - 1][y + 1], redError, greenError, blueError, 7.0 / 16);
                } catch (IndexOutOfBoundsException ignored) {
                }
                try {
                    pixelMatrix[x][y + 1] = Processing.addErrorToColor(pixelMatrix[x][y + 1], redError, greenError, blueError, 3.0 / 16);
                } catch (IndexOutOfBoundsException ignored) {
                }
                try {
                    pixelMatrix[x + 1][y + 1] = Processing.addErrorToColor(pixelMatrix[x + 1][y + 1], redError, greenError, blueError, 5.0 / 16);
                } catch (IndexOutOfBoundsException ignored) {
                }

            }

        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                pixelWriter.setColor(x, y, pixelMatrix[x][y]);

        editedImageView.setImage(writableImage);
    }

}
