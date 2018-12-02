package com.riccardofinazzi.newclean.bogey.imgprocessing;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;

import java.util.*;

import static org.opencv.imgproc.Imgproc.boundingRect;
import static org.opencv.imgproc.Imgproc.contourArea;

class ImageSortUtils {

    private ImageSortUtils() {
    }

//    /**
//     * Ordina una mappa in base alla implementazione dell'interfaccia Comparable del tipo dei suoi Values
//     *
//     * @param input Mappa
//     * @param <K>   Chiave mappa
//     * @param <V>   Valore mappa, deve implementare Comparable
//     * @return nuova LinkedHashMap ordinata
//     */
//    static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> input) {
//        List<Map.Entry<K, V>> list = new ArrayList<>(input.entrySet());
//        list.sort(Map.Entry.comparingByValue());
//
//        Map<K, V> output = new LinkedHashMap<>();
//        for (Map.Entry<K, V> entry : list) {
//            output.put(entry.getKey(), entry.getValue());
//        }
//
//        return output;
//    }

    /**
     * @return ordina in base all'area del contour
     */
    static Comparator<MatOfPoint> contourAreaComparator() {

        return new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                double o1Area = contourArea(o1);
                double o2Area = contourArea(o2);

                if (o1Area < o2Area) return 1;
                if (o1Area > o2Area) return -1;
                return 0;
            }

        };
    }

    /**
     * @return ordina i contour verticalmente
     */
    static Comparator<MatOfPoint> contourYComparator() {

        return new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                Rect rect1 = boundingRect(o1);
                Rect rect2 = boundingRect(o2);
                int result = Double.compare(rect1.tl().y, rect2.tl().y);
                return result;
            }
        };
    }

    /**
     * @return ordina i contour orizzontalmente
     */
    static Comparator<MatOfPoint> contourXComparator() {

        return new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                Rect rect1 = boundingRect(o1);
                Rect rect2 = boundingRect(o2);
                int result = 0;
                double total = rect1.tl().y / rect2.tl().y;
                if (total >= 0.9 && total <= 1.4) {
                    result = Double.compare(rect1.tl().x, rect2.tl().x);
                }
                return result;
            }
        };
    }
}
