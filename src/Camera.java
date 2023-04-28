
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Camera extends JFrame {

    private final JLabel cameraScreen;
    private VideoCapture capture;
    private Mat image;
    private boolean clicked = false;

    public Camera() {
        this.setTitle("Capture Image from Camera");
        setLayout(null);

        cameraScreen = new JLabel();
        cameraScreen.setBounds(0, 0, 640, 480);
        add(cameraScreen);

        JButton captureBtn = new JButton("Capture");
        captureBtn.setBounds(0, 480, 640, 50);
        captureBtn.setBackground(Color.decode("#6495ed"));
        captureBtn.setForeground(Color.white);
        captureBtn.setFont(new Font("Arial", Font.PLAIN, 18));
        add(captureBtn);

        captureBtn.addActionListener(e -> clicked = true);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                capture.release();
                image.release();
                System.exit(0);
            }
        });

        setSize(new Dimension(640, 565));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
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

            if(clicked){
                String imgName = JOptionPane.showInputDialog(this, "Save as: ");
                if(imgName == null){
                    imgName = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss").format(new Date());
                }
                Imgcodecs.imwrite("images/" + imgName + ".jpg", image);
                clicked = false;
            }
        }
    }

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        EventQueue.invokeLater(() -> {
            Camera camera = new Camera();
            new Thread(camera::startCamera).start();
        });
    }
}
