public class ScheduleInstance {

    private double[] timeWeights;
    private int numberOfOR;
    private int numberOfTimePeriods;
    private int maxOvertime;
    private int[] surgeries;
    private int maxOverlap;

    public ScheduleInstance(double[] timeWeights, int numberOfOR, int numberOfTimePeriods, int maxOvertime, int[] surgeries, int maxOverlap){
        this.timeWeights = timeWeights;
        this.numberOfOR = numberOfOR;
        this.numberOfTimePeriods = numberOfTimePeriods;
        this.maxOvertime = maxOvertime;
        this.surgeries = surgeries;
        this.maxOverlap = maxOverlap;
    }

    public double[] getTimeWeights(){
        return this.timeWeights;
    }

    public int getNumberOfSurgeries(){
        return this.surgeries.length;
    }

    public int getNumberOfOR(){
        return this.numberOfOR;
    }

    public int getNumberOfTimePeriods(){
        return this.numberOfTimePeriods;
    }

    public int getMaxOvertime(){
        return this.maxOvertime;
    }

    public int[] getSurgeries(){
        return this.surgeries;
    }

    public int getMaxOverlap(){
        return this.maxOverlap;
    }

    
}
