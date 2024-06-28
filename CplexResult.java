public class CplexResult {
    private double[][][] xMatrix;
    private double[][][] yMatrix;
    private double[] Z; 
    private double objectiveValue;
    private int maxOvertime;
    private int[] timeVector;
    private ScheduleInstance instance;
    double epsilon = 1e-9;

    public CplexResult(double[][][] xMatrix, double[][][] yMatrix, double[] Z, double objectiveValue, int[] timeVector){
        this.xMatrix = xMatrix;
        this.yMatrix = yMatrix;
        this.Z = Z;
        this.objectiveValue = objectiveValue;
        this.timeVector = timeVector;
    }

    public CplexResult(double[][][] xMatrix, double[][][] yMatrix, double[] Z, double objectiveValue, int[] timeVector, ScheduleInstance instance){
        this.xMatrix = xMatrix;
        this.yMatrix = yMatrix;
        this.Z = Z;
        this.objectiveValue = objectiveValue;
        this.timeVector = timeVector;
    }

    public CplexResult(){
        this.xMatrix = null;
        this.yMatrix = null;
        this.Z = null;
        this.objectiveValue = Double.NaN;
    }

    public void setInstance(ScheduleInstance instance){
        this.instance = instance;
    }

    public ScheduleInstance getInstance(){
        return this.instance;
    }

    public double[][][] getXMatrix(){
        return this.xMatrix;
    }

    public double[][][] getYMatrix(){
        return this.yMatrix;
    }

    public double[] getZ(){
        return this.Z;
    }

    public double getObjectiveValue(){
        return this.objectiveValue;
    }

    public int[] getTimeVector(){
        return this.timeVector;
    }

    public int getRegularTime(){
        return this.getTimeVector()[0];
    }

    public int getIdleTime(){
        return this.getTimeVector()[1];
    }

    public int getOverTime(){
        return this.getTimeVector()[2];
    }
    

    
    

    
}
