import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.stream.*;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;
import java.awt.*;

/**
 * Decode Morse code from a WAV file.
 * <p>
 * We begin by classifying regions of the Morse code file as tone or silence. This requires binning
 * the file and computing power measurements for all of the samples that fall into each bin. The
 * BIN_SIZE parameter controls how long each bin is.
 * <p>
 * The next step is to classify each bin as tone or silence. The POWER_THRESHOLD parameter controls
 * this cutoff. We combine this with looking for runs of tone or silence and classifying them as dot
 * (short run of tone), dash (long run of tone), or space (long run of silence, equivalent in length
 * to dash). (In real Morse code there are also longer runs of silence that separate words, but
 * we'll ignore them for now.) DASH_BIN_COUNT controls the cutoff between dots and dashes when
 * considering tone, and is also used for spaces when considering silence.
 * <p>
 * At this point we have valid Morse code as space-separated letters! So the last step is to look up
 * each symbol in a table and return the corresponding letter value.
 */
public class MorseDecoder {

    /**
     * Bin size for power binning. We compute power over bins of this size. You will probably not
     * need to modify this value.
     */
    private static final int BIN_SIZE = 100;

    /**
     * Compute power measurements for fixed-size bins of WAV samples.
     * <p>
     * We have started this function for you. You should finish it.
     *
     * @param inputFile the input file to process
     * @return the double[] array of power measurements, one per bin
     * @throws WavFileException thrown if there is a WavFile-specific IO error
     * @throws IOException thrown on other IO errors
     */
    private static double[] binWavFilePower(final WavFile inputFile)
            throws IOException, WavFileException {

        /*
         * We should check the results of getNumFrames to ensure that they are safe to cast to int.
         */
        int totalBinCount = (int) Math.ceil(inputFile.getNumFrames() / BIN_SIZE);
        double[] returnBuffer = new double[totalBinCount];

        double[] sampleBuffer = new double[BIN_SIZE * inputFile.getNumChannels()];
        for (int binIndex = 0; binIndex < totalBinCount; binIndex++) {
            inputFile.readFrames(sampleBuffer, BIN_SIZE);
                                        //sums the magnitudes of data in sampleBuffer
            returnBuffer[binIndex] = Arrays.stream(sampleBuffer).reduce( 0, (a, b) -> Math.abs(a) + Math.abs(b));
            // Get the right number of samples from the inputFile
            // Sum all the samples together and store them in the returnBuffer
        }
        return returnBuffer;

    }

    /** Power threshold for power or no power. You may need to modify this value. */
    private static final double POWER_THRESHOLD = 10;

    /** Bin threshold for dots or dashes. Related to BIN_SIZE. You may need to modify this value. */
    private static final int DASH_BIN_COUNT = 8;

    /**
     * Convert power measurements to dots, dashes, and spaces.
     * <p>
     * This function receives the result from binWavPower. It's job is to convert intervals of tone
     * or silence into dots (short tone), dashes (long tone), or space (long silence).
     * <p>
     * Write this function.
     *
     * @param powerMeasurements the array of power measurements from binWavPower
     * @return the Morse code string of dots, dashes, and spaces
     */
    private static String powerToDotDash(final double[] powerMeasurements) {
        String a = "";
        //changes numbers to "." (sound) and " " (no sound)
        for (double x : powerMeasurements)
            a += x > POWER_THRESHOLD ? "." : " ";
        System.out.println(a);
            //finds the duration of . in morse code
        int c = Arrays.stream(a.split(" ")).reduce(new String(new char[50]).replaceAll("\0","."),(e,d) -> e.length() < d.length() && e.length() > 0 ? e : d).length();
        //converts from silence-not-silence form to morese code form
        //handles -
        a = a.replaceAll("\\.{"+(3*c - 3)+","+(3*c+3)+"}","-");
        //handles .
        a = a.replaceAll("\\.{"+(c-1)+","+(c+1)+"}",".");
        //handles spaces between chars
        a = a.replaceAll(" {"+(3*c)+",}"," ");
        //handles single chars
        a = a.replaceAll(" {"+c+","+(2*c)+"}","");

        return a;
    }
    private final static double UNIT = .06;
    private final static double DOT_DUR = UNIT;
    private final static double DASH_DUR = 3*UNIT;
    private final static double INTERELEM = 1*UNIT;
    private final static double INTRAELEM = 4*UNIT;
    private final static int SAMPLE_RATE = 44100;
    private static Map<String, Double> EASY_LOOKUP = new HashMap<String, Double>() {
        {
            this.put(".",DOT_DUR);
            this.put("-",DASH_DUR);
        }
    };
    private static Map<String, Double[]> SOUND_LOOKUP = new HashMap<String, Double[]>(){
        {
            ArrayList<Double> buf = new ArrayList<>();
            BigDecimal d;
            for (Entry<String, Double> e : EASY_LOOKUP.entrySet()) {
                buf.clear();
                for (int i = 0; i < SAMPLE_RATE*e.getValue(); ++i){
                    d = new BigDecimal(Math.sin(2*Math.PI*FREQ*i/SAMPLE_RATE)).setScale(0, RoundingMode.UP);
                    buf.add(d.doubleValue());
                }
                this.put(e.getKey(),(Double[]) buf.toArray(new Double[0]));
            }
            Double[] kek = new Double[(int)(SAMPLE_RATE*INTRAELEM)];
            Arrays.fill(kek,new Double(0));
            this.put(" ", kek);
            kek = new Double[(int)(SAMPLE_RATE*INTERELEM)];
            Arrays.fill(kek,new Double(0));
            this.put("", kek);

        }
    };
    private final static int FREQ = 500;
    private static void dotDashToWave (final String s) throws IOException, WavFileException{
        double duration = s.replaceAll("[^-]","").length()*DASH_DUR +
                s.replaceAll("[^\\.]","").length()*DOT_DUR +
                (s.replaceAll(" ","").length()+2)*INTERELEM +
                s.replaceAll("[^ ]","").length()*INTRAELEM;
        long frames = (long) (SAMPLE_RATE * duration);
        WavFile file = WavFile.newWavFile(new File("kek.wav"), 1, frames, 16, SAMPLE_RATE);
        String[] parts = s.replaceAll(" $","").split("");
        file.writeFrames(Arrays.stream(SOUND_LOOKUP.get("")).mapToDouble(Double::doubleValue).toArray(),SOUND_LOOKUP.get("").length);
        for (String p : parts) {
            file.writeFrames(Arrays.stream(SOUND_LOOKUP.get(p)).mapToDouble(Double::doubleValue).toArray(),SOUND_LOOKUP.get(p).length);
            if (!p.equals(" ")) {
                file.writeFrames(Arrays.stream(SOUND_LOOKUP.get("")).mapToDouble(Double::doubleValue).toArray(),SOUND_LOOKUP.get("").length);
            }
        }
        file.writeFrames(Arrays.stream(SOUND_LOOKUP.get("")).mapToDouble(Double::doubleValue).toArray(),SOUND_LOOKUP.get("").length);
    }

    /**
     * Morse code to alpha mapping.
     *
     * @see <a href="https://morsecode.scphillips.com/morse2.html">Morse code reference</a>
     **/
    private static final Map<String, String> MORSE_TO_ALPHA = //
            new HashMap<String, String>() {
                {
                    put(".-", "a");
                    put("-...", "b");
                    put("-.-.", "c");
                    put("-..", "d");
                    put(".", "e");
                    put("..-.", "f");
                    put("--.", "g");
                    put("....", "h");
                    put("..", "i");
                    put(".---", "j");
                    put("-.-", "k");
                    put(".-..", "l");
                    put("--", "m");
                    put("-.", "n");
                    put("---", "o");
                    put(".--.", "p");
                    put("--.-", "q");
                    put(".-.", "r");
                    put("...", "s");
                    put("-", "t");
                    put("..-", "u");
                    put("...-", "v");
                    put(".--", "w");
                    put("-..-", "x");
                    put("-.--", "y");
                    put("--..", "z");
                    put(".----", "1");
                    put("..---", "2");
                    put("...--", "3");
                    put("....-", "4");
                    put(".....", "5");
                    put("-....", "6");
                    put("--...", "7");
                    put("---..", "8");
                    put("----.", "9");
                    put("-----", "0");
                    put(".-.-.-", ".");
                }
            };
    private static final Map<String,String> ALPHA_TO_MORSE = new HashMap<String,String>()
    {{
            for (Map.Entry<String, String> x : MORSE_TO_ALPHA.entrySet()) {
                this.put(x.getValue(), x.getKey());
            }
    }};
    /**
     * Convert a Morse code string to alphanumeric characters using the mapping above.
     * <p>
     * Note that this will output "_" if it cannot look up a mapping. Usually this indicates that
     * there is a problem with your parameters. However, there are missing mappings in the table
     * above. Feel free to add them.
     * <p>
     * We have provided this function and the mapping above for you. You're welcome!
     *
     * @param dotDashes the Morse code string to decode
     * @return the decoded alphanumeric string
     * @see <a href="https://morsecode.scphillips.com/morse2.html">Morse code reference</a>
     */
    private static String dotDashToAlpha(final String dotDashes) {
        String returnString = "";
        for (String dotDash : dotDashes.split(" ")) {
            if (MORSE_TO_ALPHA.containsKey(dotDash)) {
                returnString += MORSE_TO_ALPHA.get(dotDash);
            } else {
                returnString += "_";
            }
        }
        return returnString;
    }
    private static String alphaToDotDash(final String alpha) {
        String res = "";
        for (String x : alpha.split("")) {
            res += ALPHA_TO_MORSE.getOrDefault(x,"") + " ";
        }
        return res.replaceAll(" $","");
    }

    /**
     * Convert a WAV file containing Morse code to a string.
     * <p>
     * This function brings everything together: the binning, the power thresholding, and the final
     * Morse to alphanumeric conversion.
     *
     * @param inputFile the input file to process
     * @return the decoded Morse code from the WAV file as a string
     * @throws WavFileException thrown if there is a WavFile-specific IO error
     * @throws IOException thrown on other IO errors
     */
    public static String morseWavToString(final WavFile inputFile)
            throws IOException, WavFileException {
        double[] binnedSamples = binWavFilePower(inputFile);
        String dotDash = powerToDotDash(binnedSamples);
        String outputString = dotDashToAlpha(dotDash);
        return dotDash + "\n" + outputString;
    }


    public static void displayWaveform(WavFile file) throws Exception{
        int fac = 2;
        int len = (file.getNumFrames() > (long) Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) file.getNumFrames());
        double[] amps = new double[len];
        file.readFrames(amps,len);

        int[] intTs = new int[len];
        for (int i = 0; i < len; ++i) {
            intTs[i] = (int) i/fac;//frame.getWidth()*i/len;
        }

        JFrame frame = new JFrame();
        frame.setSize(3000,1500);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        int[] intAmps = Arrays.stream(amps).mapToInt(a -> (int)(frame.getHeight()/2 - 25 + a*frame.getHeight()/2)).toArray();

        JComponent comp = new JComponent() {
            protected void paintComponent(Graphics g){
                g.drawPolyline(intTs,intAmps,len);
            }
        };
        comp.setPreferredSize(new Dimension(len/fac,1000));
        JScrollPane p = new JScrollPane(comp);
        p.setPreferredSize(frame.getSize());
        frame.add(p);
        frame.setVisible(true);
    }
    /**
     * Main method for testing.
     * <p>
     * Takes an input file from the user and tries to print out the Morse code by processing the
     * file using your code above. You should feel free to modify this, as well as to insert
     * additional print statements above to help determine what is going on... or what is going
     * wrong.
     *
     * @param unused unused input arguments
     */
    public static void main(final String[] unused) {

        String inputPrompt = String.format("Enter the WAV filename (in src/main/resources):");
        Scanner lineScanner = new Scanner(System.in);

        while (true) {
            String inputFilename = null;
            System.out.println(inputPrompt);

            /*
             * We could just use lineScanner.hasNextInt() and not initialize a separate scanner. But
             * the default Scanner class ignores blank lines and continues to search for input until
             * a non-empty line is entered. This approach allows us to detect empty lines and remind
             * the user to provide a valid input.
             */
            String nextLine = lineScanner.nextLine();
            Scanner inputScanner = new Scanner(nextLine);
            if (!(inputScanner.hasNext())) {
                /*
                 * These should be printed as errors using System.err.println. Unfortunately,
                 * Eclipse can't keep System.out and System.err ordered properly.
                 */
                System.out.println("Invalid input: please enter a filename with no spaces.");
                continue;
            }
            inputFilename = inputScanner.next();
            /*
             * If the line started with a string but contains other tokens, reinitialize userInput
             * and prompt the user again.
             */
            if (inputScanner.hasNext()) {
                System.out.println("Invalid input: please enter only a single filename.");
                continue;
            }
            inputScanner.close();

            WavFile inputWavFile;
            try {
                String inputFilePath = MorseDecoder.class.getClassLoader()
                        .getResource(inputFilename).getFile();
                inputFilePath = new URI(inputFilePath).getPath();
                File inputFile = new File(inputFilePath);
                inputWavFile = WavFile.openWavFile(inputFile);
                displayWaveform(inputWavFile);
                if (inputWavFile.getNumChannels() != 1) {
                    throw new InvalidParameterException("We only process files with one channel.");
                }

                System.out.println(morseWavToString(inputWavFile));
                break;
            } catch (Exception e) {
                throw new InvalidParameterException("Bad file path" + e);
            }
        }
        try {

            dotDashToWave(alphaToDotDash("fuck off"));
        } catch (Exception e){
            System.exit(1);
        }
        lineScanner.close();
    }
}
