import net.coobird.thumbnailator.Thumbnails;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class Main {

    public static void main(String... args) throws Exception {
        System.out.println("Circle Thumbnail start");

        try (Stream<Path> paths = Files.walk(Paths.get("/home/maciej/Desktop/ct"))) {
            paths.filter(Files::isRegularFile).forEach(Main::createCircleThumbnail);
        } catch (Exception e) {
            throw e;
        }

    }

    private static void createCircleThumbnail(Path path) {
        try {
            BufferedImage bufferedImage = ImageIO.read(path.toFile());
            Point faceCenter = faceCenter(bufferedImage);
            bufferedImage = square(bufferedImage, faceCenter);
//            bufferedImage = cropToCircle(bufferedImage);
            bufferedImage = resize(bufferedImage);
            ImageIO.write(bufferedImage, "PNG", new File(path.toString().substring(0, path.toString().length() - 4) + "_THUMBNAIL.png"));
        } catch (IOException e) {
            System.out.println("Exception at processing image: " + path.toString() + "\n" + e.getMessage());
        }
        System.out.println("Successfully precessed: " + path.toString());
    }

    private static BufferedImage square(BufferedImage bufferedImage, Point faceCenter) {
        int dimension = Math.min(bufferedImage.getWidth(), bufferedImage.getHeight());
        int x = 0, y = 0;
        if (faceCenter != null) {
            if (bufferedImage.getWidth() < bufferedImage.getHeight()) {
                y = yWhenWidthIsLower(dimension, bufferedImage.getHeight(), faceCenter);
            } else {
                x = xWhenHeightIsLower(dimension, bufferedImage.getWidth(), faceCenter);
            }
        }

        BufferedImage squareBuffer = new BufferedImage(dimension, dimension, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = squareBuffer.createGraphics();
        g2.drawImage(bufferedImage, -x, -y, bufferedImage.getWidth(), bufferedImage.getHeight(), null);
        return squareBuffer;
    }

    private static int yWhenWidthIsLower(int dimension, int height, Point faceCenter) {
        int y = faceCenter.y - dimension / 2;
        if (faceCenter.y + dimension / 2 > height) {
            y -= faceCenter.y + dimension / 2 - height;
        }
        if (y < 0) {
            y = 0;
        }
        return y;
    }

    private static int xWhenHeightIsLower(int dimension, int width, Point faceCenter) {
        int x = faceCenter.x - dimension / 2;
        if (faceCenter.x + dimension / 2 > width) {
            x -= faceCenter.x + dimension / 2 - width;
        }
        if (x < 0) {
            x = 0;
        }
        return x;
    }

    private static BufferedImage resize(BufferedImage bufferedImage) throws IOException {
        int lowerDimension = Math.min(bufferedImage.getWidth(), bufferedImage.getHeight());
        int greaterDimension = Math.max(bufferedImage.getWidth(), bufferedImage.getHeight());
        double factor = 200D / lowerDimension;
        int newGreaterDim = (int) (factor * (double) greaterDimension);
        return Thumbnails.of(bufferedImage).size(newGreaterDim, newGreaterDim).asBufferedImage();
    }

    private static BufferedImage cropToCircle(BufferedImage bufferedImage) throws IOException {
        int diameter = bufferedImage.getWidth();
        BufferedImage circleBuffer = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = circleBuffer.createGraphics();
        g2.setClip(new Ellipse2D.Float(0, 0, diameter, diameter));
        g2.drawImage(bufferedImage, 0, 0, diameter, diameter, null);
        return circleBuffer;
    }

    private static Point faceCenter(BufferedImage bufferedImage) throws IOException {
        final HaarCascadeDetector detector = new HaarCascadeDetector();
        final List<DetectedFace> faces = detector.detectFaces(ImageUtilities.createFImage(bufferedImage));
        if (faces == null || faces.isEmpty()) {
            System.out.println("No faces found in the image");
            return null;
        }
        DetectedFace detectedFace = faces.stream().max(Comparator.comparingDouble(DetectedFace::getConfidence)).get();
//        ImageIO.write(ImageUtilities.createBufferedImage(detectedFace.getFacePatch()), "PNG", new File("/home/maciej/Desktop/ct/face.png"));
        return new Point(
                (int) (detectedFace.getBounds().x + detectedFace.getBounds().width / 2),
                (int) (detectedFace.getBounds().y + detectedFace.getBounds().height / 2));
    }


}
