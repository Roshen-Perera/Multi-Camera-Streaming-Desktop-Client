import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main extends Application {
    private ServerSocket serverSocket;

    @Override
    public void start(Stage primaryStage) {
        try {
            serverSocket = new ServerSocket(7777);
            new Thread(() -> {
                while (true) {
                    try {
                        System.out.println("[SERVER]: Listening for incoming requests on port 7777");
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("[SERVER]: " + clientSocket.getInetAddress().toString() + " connected");
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
            try (InputStream inputStream = clientSocket.getInputStream()) {
                BufferedImage bufferedImage = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
                WritableImage writableImage = new WritableImage(640, 480);
                byte[] buffer = new byte[640 * 480 * 3]; // Assuming RGB format
                ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);

                ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

                executorService.scheduleAtFixedRate(() -> {
                    Platform.runLater(() -> imageView.setImage(writableImage));
                }, 0, 33, TimeUnit.MILLISECONDS); // Approximately 30 FPS

                while (true) {
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead == -1) {
                        break;
                    }

                    // Convert the byte buffer to an image using parallel processing
                    for (int y = 0; y < 480; y++) {
                        for (int x = 0; x < 640; x++) {
                            int index = (y * 640 + x) * 3;
                            int r = byteBuffer.get(index) & 0xFF;
                            int g = byteBuffer.get(index + 1) & 0xFF;
                            int b = byteBuffer.get(index + 2) & 0xFF;
                            int rgb = (r << 16) | (g << 8) | b;
                            bufferedImage.setRGB(x, y, rgb);
                        }
                    }

                    // Convert BufferedImage to WritableImage only when necessary
                    SwingFXUtils.toFXImage(bufferedImage, writableImage);
                }

                executorService.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
