import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import javafx.application.Platform;

public class Main extends Application {
    private ServerSocket serverSocket;

    @Override
    public void start(Stage primaryStage) {
        try {
            serverSocket = new ServerSocket(7777);
            new Thread(() -> {
                while (true) {
                    try {
                        System.out.println("[SERVER]: Listening incoming requests on port 7777");
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("[SERVER]: Main "+clientSocket.getInetAddress().toString()+" connected");
                        Platform.runLater(() -> openNewWindow(clientSocket));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openNewWindow(Socket clientSocket) {
        Stage stage = new Stage();
        ImageView imageView = new ImageView();
        Scene scene = new Scene(new StackPane(imageView), 640, 480);
        stage.setScene(scene);
        stage.setTitle("Camera Feed - " + clientSocket.getInetAddress().toString());
        stage.show();

        new Thread(() -> {
            try {
                InputStream inputStream = clientSocket.getInputStream();
                WritableImage writableImage = new WritableImage(640, 480);
                PixelWriter pixelWriter = writableImage.getPixelWriter();

                byte[] buffer = new byte[640 * 480 * 3]; // Assuming RGB format
                while (true) {
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead == -1) {
                        break;
                    }

                    // Convert the byte buffer to an image
                    for (int y = 0; y < 480; y++) {
                        for (int x = 0; x < 640; x++) {
                            int r = buffer[(y * 640 + x)] & 0xFF;
                            int g = buffer[(y * 640 + x) + 1] & 0xFF;
                            int b = buffer[(y * 640 + x) + 2] & 0xFF;
                            int argb = (0xFF << 24) | (r << 16) | (g << 8) | b;
                            pixelWriter.setArgb(x, y, argb);
                        }
                    }

                    Platform.runLater(() -> imageView.setImage(writableImage));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
