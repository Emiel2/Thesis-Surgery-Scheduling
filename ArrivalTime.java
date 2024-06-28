public class ArrivalTime {
    private double arrivalTimeDouble;
    private String arrivalTimeString;
    private double t;
    private int duration; //in time units
    
    public ArrivalTime(double arrivalTimeDouble){
        this.arrivalTimeDouble = arrivalTimeDouble;
        double timeValue = 7.0 + (arrivalTimeDouble * 10.0);
        int timeHour = (int) Math.floor(timeValue);
        int minutes = (int) ((timeValue - timeHour)*60);
        if(minutes < 10){
            this.arrivalTimeString = timeHour + ":0" + minutes;;
        } else {
            this.arrivalTimeString = timeHour + ":" + minutes;
        }
        this.t = arrivalTimeDouble*20.0;
    }

    public ArrivalTime(double arrivalTimeDouble, int duration){
        this.arrivalTimeDouble = arrivalTimeDouble;
        double timeValue = 7.0 + (arrivalTimeDouble * 10.0);
        int timeHour = (int) Math.floor(timeValue);
        int minutes = (int) ((timeValue - timeHour)*60);
        if(minutes < 10){
            this.arrivalTimeString = timeHour + ":0" + minutes;;
        } else {
            this.arrivalTimeString = timeHour + ":" + minutes;
        }
        this.t = arrivalTimeDouble*20.0;
    }

    public double getTime(){
        return this.t;
    }

    public double getTimeDouble(){
        return this.arrivalTimeDouble;
    }

    public String getArrivalTimeString(){
        return this.arrivalTimeString;
    }

    public int getDuration(){
        return this.duration;
    }

    public void setDuration(int duration){
        this.duration = duration;
    }
}
