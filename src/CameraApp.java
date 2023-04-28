import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class CameraApp extends JFrame {
    private JLabel cameraScreen;
    private JPanel mainPanel;
    private JButton captureBtn;
    private JButton detectShapesBtn;
    private VideoCapture capture;
    private Mat image;
    private boolean captureBtnClicked = false;
    private boolean detectShapesBtnClicked = false;


    public CameraApp() {
        setTitle("Camera");
        setContentPane(mainPanel);
        this.pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        captureBtn.addActionListener(e -> captureBtnClicked = true);
        detectShapesBtn.addActionListener(e -> {
            detectShapesBtnClicked = !detectShapesBtnClicked;
            String text = detectShapesBtn.getText().equals("Detect Shapes") ? "Stop Detecting Shapes" : "Detect Shapes";
            detectShapesBtn.setText(text);

        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                capture.release();
                image.release();
                System.exit(0);
            }
        });
    }

    public void startCamera() {
        capture = new VideoCapture(0);
        image = new Mat();
        byte[] imageData;
        ImageIcon icon;

        while(true){
            capture.read(image);
            final MatOfByte buf = new MatOfByte();
            Imgcodecs.imencode(".jpg", image, buf);
            imageData = buf.toArray();
            icon = new ImageIcon(imageData);
            cameraScreen.setIcon(icon);

            if(captureBtnClicked){
                String imgName = JOptionPane.showInputDialog(this, "Save as: ");
                if(imgName == null){
                    imgName = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss").format(new Date());
                }
                Imgcodecs.imwrite("images/" + imgName + ".jpg", image);
                captureBtnClicked = false;
            }

            if(detectShapesBtnClicked){
                final Mat processed = processImage(image);
                markOuterContour(processed, image);
                drawImage(image, this.mainPanel);
            }

        }
    }

    public static Mat processImage(final Mat mat){
        final Mat processed = new Mat(mat.height(), mat.width(), mat.type());
        //blur image using Gaussian filter
        Imgproc.GaussianBlur(mat, processed, new Size(7,7), 1);
        //switch from RGB to gray
        Imgproc.cvtColor(processed, processed, Imgproc.COLOR_RGB2GRAY);
        //find edges in image using Canny algorithm
        Imgproc.Canny(processed, processed, 200, 25);
        //dilate image using a specific structuring element
        Imgproc.dilate(processed, processed, new Mat(), new Point(-1, -1), 1);

        return processed;
    }

    public static void markOuterContour(final Mat processedImage, final Mat originalImage){
        final List<MatOfPoint> allContours = new ArrayList<>();
        Imgproc.findContours(
                processedImage,
                allContours,
                new Mat(processedImage.size(), processedImage.type()),
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_NONE
        );

        //filter out noise and display contour area value
        final List<MatOfPoint> filteredContours = allContours.stream()
                .filter(contour -> {
                   final double value = Imgproc.contourArea(contour);
                   final Rect rect = Imgproc.boundingRect(contour);

                   final boolean isNotNoise = value > 1000;

                   if(isNotNoise) {

                       /*
                       Imgproc.putText(
                               originalImage,
                               "Area: " + (int) value,
                               new Point(rect.x + rect.width, rect.y + rect.height),
                               2,
                               0.5,
                               new Scalar(124, 252, 0),  //green color
                               1
                       );
                       */

                       MatOfPoint2f dst = new MatOfPoint2f();
                       contour.convertTo(dst, CvType.CV_32F);
                       Imgproc.approxPolyDP(dst, dst, 0.02 * Imgproc.arcLength(dst, true), true );
                       int numPoints = dst.toArray().length;
                       String shape;
                       switch (numPoints) {
                           case 3 -> shape = "triangle";
                           case 4 -> {
                                        RotatedRect quad = Imgproc.minAreaRect(dst);
                                        double aspectRatio = quad.size.width / quad.size.height;
                                        double diff = quad.boundingRect().area() - value;
                                        // temp 3000 threshold just based on observation
                                        if (diff <= 3000){
                                            if (Math.round(aspectRatio) == 1){
                                                shape = "square";
                                            }else {
                                                shape = "rectangle";
                                            }
                                        }else{
                                            shape = "quadrilateral";
                                        }
//                                        System.out.println(quad.boundingRect().area());
//                                        System.out.println(value);
                                  }
                           case 5 -> shape = "pentagon";
                           case 6 -> shape = "hexagon";
                           case 7 -> shape = "heptagon";
                           case 8 -> {
                               float[] radius = new float[1];
                               Imgproc.minEnclosingCircle(dst, null, radius);
                               double area = Math.pow(radius[0], 2) * Math.PI;
                               shape = area - value < 3000 ? "circle" : "octagon";
//                               shape = numPoints + "";
                               System.out.println(area);
                               System.out.println(value);
                           }


                           default -> shape = numPoints + "-sided polygon";
                       }

                      drawText(
                               originalImage,
                               "Shape: " + shape,
                               new Point(rect.x + rect.width, rect.y + rect.height + 15),
                               2,
                               0.5,
                               new Scalar(124, 252, 0),
                               new Scalar(0, 0, 0),
                               1
                       );
                   }

                   return  isNotNoise;

                }).collect(Collectors.toList());

        Imgproc.drawContours(
                originalImage,
                filteredContours,
                -1,             //negative value means to draw all of the contours in list
                new Scalar(124, 252, 0),
                1
        );

    }

    public static void drawImage(final Mat mat, final JPanel panel){
        final BufferedImage image = convertMatToBufferedImage(mat);
        final Graphics graphics = panel.getGraphics();
        graphics.drawImage(image, 0, 0, panel);
    }

    private static BufferedImage convertMatToBufferedImage(final Mat mat){
        final BufferedImage bufferedImage = new BufferedImage(
                mat.width(),
                mat.height(),
                mat.channels() == 1 ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_3BYTE_BGR
        );

        final WritableRaster raster = bufferedImage.getRaster();
        final DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
        mat.get(0,0,dataBuffer.getData());

        return bufferedImage;

    }

    public static void drawText(final Mat image, String text, Point point, int fontFace, double fontScale, Scalar textColor, Scalar backgroundColor, int thickness ){
        Size textSize = Imgproc.getTextSize(text, fontFace, fontScale, thickness, null);
        double width = textSize.width;
        double height = textSize.height;
        Imgproc.rectangle(image, new Point(point.x, point.y), new Point(point.x + width, point.y + height), backgroundColor, -1);
        Imgproc.putText(image, text, new Point(point.x, point.y + height + fontScale - 1), fontFace, fontScale, textColor, thickness );
    }

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        EventQueue.invokeLater(() -> {
            CameraApp camera = new CameraApp();
            new Thread(camera::startCamera).start();
        });
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
