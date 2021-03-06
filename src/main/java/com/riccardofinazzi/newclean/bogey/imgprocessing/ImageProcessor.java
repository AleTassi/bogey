package com.riccardofinazzi.newclean.bogey.imgprocessing;

import com.riccardofinazzi.newclean.bogey.common.ApplicationProperties;
import nu.pattern.OpenCV;
import org.apache.commons.imaging.ImageReadException;
import org.opencv.core.*;
import static org.opencv.core.Core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import static org.opencv.imgproc.Imgproc.*;

public class ImageProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImageProcessor.class);

    {
        // Carica la libreria openCV (è scritta in C, java la richiama con metodi nativi
        OpenCV.loadShared();
    }

    ApplicationProperties props = ApplicationProperties.getInstance();

    /**
     * 8bit, 3-channel image.
     */
    public static final int LOAD_COLOR = Imgcodecs.CV_LOAD_IMAGE_COLOR;

    /**
     * 8bit, 1-channel image.
     */
    public static final int LOAD_GRAYSCALE = Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;

    /**
     * Loads an image from a file.
     * This is a wrapper around imread() which fails if the file
     * is inside a jar/zip. This function takes care of that case, loads the
     * image in Java and manually creates the Mat...
     *
     * @param name  name of the resource
     * @param flags specifying the color type of a loaded image;
     *              supported: LOAD_COLOR (8-bit, 3-channels),
     *              LOAD_GRAYSCALE (8-bit, 1-channel),
     * @return Mat of type CV_8UC3 or CV_8UC1 (empty Mat is returned in case of an error)
     */
    public Mat readImage(String name, int flags) throws IOException {
        URL url = getClass().getClassLoader().getResource(name);

        // make sure the file exists
        if (url == null) {
            throw new FileNotFoundException(name);
        }

        String path = url.getPath();

        // not sure why we (sometimes; while running unpacked from the IDE) end
        // up with the authority-part of the path (a single slash) as prefix,
        // ...anyways: imread can't handle it, so that's why.
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        Mat image = Imgcodecs.imread(path, flags);

        // ...and if imread() has failed, we simply assume that the file
        // is packed in a jar (i.e. Java should be able to read the image)
        if (image.empty()) {
            BufferedImage buf;

            buf = ImageIO.read(url);

            int height = buf.getHeight();
            int width = buf.getWidth();
            int rgb, type, channels;

            switch (flags) {
                case LOAD_GRAYSCALE:
                    type = CvType.CV_8UC1;
                    channels = 1;
                    break;
                case LOAD_COLOR:
                default:
                    type = CvType.CV_8UC3;
                    channels = 3;
                    break;
            }

            byte[] px = new byte[channels];
            image = new Mat(height, width, type);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    rgb = buf.getRGB(x, y);
                    px[0] = (byte) (rgb & 0xFF);
                    if (channels == 3) {
                        px[1] = (byte) ((rgb >> 8) & 0xFF);
                        px[2] = (byte) ((rgb >> 16) & 0xFF);
                    }
                    image.put(y, x, px);
                }
            }
        }

        return image;
    }

    /**
     * L'immagine che passerà per questo metodo verrà:
     * 1) Trasformata in scala di grigi
     * 2) Sfocata
     * 3) Ricontrastata
     * <p>
     * Ideale per effettuare OMR/ICR
     */
    public Mat preprocess(Mat image) {

        // crea una Matrix con quattro canali di colore in un range da 0 a 255. (unsigned byte, 2^8)
        Mat hsvLayer = new Mat(image.size(), CvType.CV_8UC4);
        // conversione immagine input da colore a bianco e nero
        // output: hsvLayer
        cvtColor(image, hsvLayer, COLOR_BGR2GRAY);

        // crea una Matrix con quattro canali di colore in un range da 0 a 255. (unsigned byte, 2^8)
        Mat blurLayer = new Mat(image.size(), CvType.CV_8UC4);
        // sfocatura gaussiana immagine
        // output: blurLayer
        GaussianBlur(hsvLayer, blurLayer, new Size(5, 5), 0);

        // crea una Matrix con quattro canali di colore in un range da 0 a 1.0. (signed 4 byte float)
        Mat adaptiveThrsLayer = new Mat(image.size(), CvType.CV_32F);
        // ricontrastatura
        // output: adaptiveThrsLayer
        adaptiveThreshold(blurLayer, adaptiveThrsLayer, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 7, 5);

        return adaptiveThrsLayer;
    }

    /**
     * TODO Work in progress
     */
    public void evaluateCheckboxes() {

        try {
            // Processamento

            // Nome del file
            String filename = props.getRequiredProperty("in.file");

            File file = new File(filename);

            Mat rawInputBGR = null;
            try {
                log.debug("Reading file {}", filename);
                rawInputBGR = this.readImage(file.getName(), LOAD_COLOR);

            } catch (FileNotFoundException e) {
                log.error("Error while loading file {}" + e.getMessage(), e);
                System.exit(1);
            }

            double skewAngle;
            try {
                skewAngle = RadonDeskewer.calculateAngle(file);
                rawInputBGR = this.adjustRotation(rawInputBGR, skewAngle);
                log.debug("Image succesfully deskewed for angle {}", skewAngle);
            } catch (ImageReadException | IOException e) {
                log.error("Error while calculating angle {}" + e.getMessage(), e);
                System.exit(1);
            }

            Mat preprocessed = this.preprocess(rawInputBGR);

            /*
             * Ignorare questo pezzo
             *
             * Scalar lower = new Scalar(0, 0, 0);
             * Scalar upper = new Scalar(15, 15, 15);
             *
             * Mat out = new Mat();
             * inRange(rawInputBGR, lower, upper, out);
             */

            Mat hierarchy = new Mat(); // la hierarchy stabilisce il grado di parentela tra i contorni
            List<MatOfPoint> contours = new ArrayList<>();

            findContours(preprocessed, contours, hierarchy, RETR_LIST, CHAIN_APPROX_SIMPLE);
            log.debug("I've found {} contours", contours.size());

            Collections.sort(contours, ImageSortUtils.contourYComparator());

            /*
             * La hierarchy al momento non la ho utilizzata, comunque interessante, permette ad esempio
             * di capire se un contour segue una geometria aperta o chiusa
             *
             * // per le geometrie senza riempimento solid
             * double parent = hierarchy.get(0, i)[2];
             *
             * if (parent < 0) {
             *   // chiusa
             * } else {
             *   // aperta
             * }
             *
             *
             * // per geometrie con riempimento solid
             * // (https://stackoverflow.com/a/42585938)
             * double parent = hierarchy.get(0, i)[2];
             * double children = hierarchy.get(0, i)[3];
             *
             * if (parent < 0 && children < 0) {
             *   // chiusa
             * } else {
             *   // aperta
             * }
             */

            ArrayList<Mat> checkboxes = new ArrayList<>();

            // per ogni contorno
            int j = 1;
            for (MatOfPoint e : contours) {

                // creo un rettangolo esterno al contorno individuato.
                Rect bRect = boundingRect(e);

                // Se l'area è compresa in questi parametri si tratta di una checkbox
                if ((bRect.height > 27 && bRect.height < 32) && (bRect.width > 27 && bRect.width < 32)) {

                    // Aggiungo un ritaglio virtuale del contenuto della checkbox...
                    Point a = new Point(bRect.x + 7, bRect.y + 7);
                    Point b = new Point(bRect.x + bRect.width - 8, bRect.y + bRect.height - 8);
                    Rect cropRect = new Rect(a, b);
                    Mat subImage = new Mat(preprocessed, cropRect);
                    // ...in una lista
                    checkboxes.add(subImage);

                    // disegno un rettangolo sul file di output per marcare quanto ho trovato
                    Scalar red = new Scalar(0, 0, 255);
                    rectangle(rawInputBGR, a, b, red);

                    putText(rawInputBGR, String.valueOf(j++), cropRect.tl(), Core.FONT_ITALIC, 0.4, new Scalar(0,0,255));
                }
            }

            int i = 0;
            for (Mat e : checkboxes) {

                int totalPixels = e.rows() * e.cols();
                /*
                 * // recupero i pixels neri
                 *
                 * int whitePixels = countNonZero(e);
                 * int blackPixels = totalPixels - whitePixels;
                 */

                // oppure inverto i colori dell'immagine
                Mat inverted = new Mat();
                bitwise_not(e, inverted);

                // calcolo quanti pixel sono marcati in percentuale
                int blackPixels = countNonZero(inverted);
                float percentage = ((float) blackPixels / (float) totalPixels) * 100.0F;

                // occhio e croce, se oltre il 30% del centro della checkbox è marcata per me è un sì
                if (percentage >= 30.0F)
                    log.debug("Checkbox n° {} has {}% pixels marked", i, percentage);

                i++;
            }

            String outFilename = props.getRequiredProperty("out.file");
            log.debug("Writing to output success: {}, {}", (Imgcodecs.imwrite(outFilename, rawInputBGR)), outFilename);
        } catch (IOException e) {
            log.error("Error while processing image {}", e.getMessage(), e);
        }

    }

    /**
     * TODO
     */
    public Mat adjustSize(Mat e) {
        return null;
    }

    /**
     * TODO
     */
    public Mat cropAtMarks(Mat e) {
        return null;
    }

    /**
     * Raddrizza immagini storte
     *
     * @param input Immagine
     * @param angle Angolo da ruotare espresso in gradi
     * @return Immagine ruotata su di un perno centrale (len/2)
     */
    public Mat adjustRotation(Mat input, double angle) {

        Mat result = new Mat();

        int len;
        if (input.cols() >= input.rows())
            len = input.cols();
        else
            len = input.rows();

        Point pt = new Point(len / 2, len / 2);
        Mat r = getRotationMatrix2D(pt, angle, 1.0);

        warpAffine(input, result, r, new Size(len, len));

        return result;
    }

}