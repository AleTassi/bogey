import com.riccardofinazzi.newclean.bogey.common.ApplicationProperties;
import com.riccardofinazzi.newclean.bogey.imgprocessing.ImageProcessor;

public class TestDrive {

    public static void main(String[] args) {

        ApplicationProperties pl = ApplicationProperties.getInstance();
        pl.init();

        new ImageProcessor().evaluateCheckboxes();
    }
}
