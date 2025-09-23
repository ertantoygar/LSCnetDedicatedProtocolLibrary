package tr.com.logidex.cnetdedicated.fxcontrols;

import java.text.DecimalFormat;

public record ImperialMeasurement(int yard, int inch, int inchFraction) {

    public static final int YARD_MAX = 35;
    public static final int INCH_MAX = 99;
    public static final int INCH_FRACTION_MAX = 31;


    public static ImperialMeasurement of(int yard, int inch, int inchFraction) {

        if (yard > YARD_MAX || inch > INCH_MAX || inchFraction > INCH_FRACTION_MAX ||
                yard < 0 || inch < 0 || inchFraction < 0) {

            throw new IllegalArgumentException("""
                     - LIMIT ERROR -
                    YARD_MAX :  0 - %s
                    INCH_MAX :  0 - %s
                    INCH_FRACTION_MAX : 0 - %s
                    """.formatted(YARD_MAX, INCH_MAX, INCH_FRACTION_MAX));

        }

        if (inch > 35 ) {

            var saveYard = yard;
            var saveInch = inch;

            yard += inch / 36;
            inch = inch % 36;


            if (yard > YARD_MAX) {
                yard =saveYard;
                inch = saveInch;

            }


        }



        return new ImperialMeasurement(yard, inch, inchFraction);


    }

    public double getTotalInInches() {

        return (this.yard * 36) + this.inch + ((double) this.inchFraction / 32);


    }

//    public double getTotalInMM() {
//        DecimalFormat df = new DecimalFormat("#.#");
//        return Double.parseDouble(df.format(getTotalInInches() * 25.4));
//    }

    public double getTotalInMM() {
        double result = getTotalInInches() * 25.4;
        return Math.round(result * 10.0) / 10.0;
    }


    public static ImperialMeasurement fromMillimeters(double mm) {
        // Convert mm to total inches (1 inch = 25.4 mm)
        double totalInches = mm / 25.4;

        // Extract yards (1 yard = 36 inches)
        int yards = (int) (totalInches / 36);
        double remainingInches = totalInches - (yards * 36);

        // Extract whole inches
        int inches = (int) remainingInches;
        double fractionalInches = remainingInches - inches;

        // Convert fractional inches to 32nds
        // Round to nearest 32nd
        int thirtySeconds = (int) Math.round(fractionalInches * 32);

        return ImperialMeasurement.of(yards, inches, thirtySeconds);
    }


}
