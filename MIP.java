/* This program is used for comparing a so called emergency robust (ER
 * surgery schedule (i.e. it is constrained by the Break-In-Moments or BIM constraint)
 * to a schedule that is not robust to emergency (that is, we only schedule electives)
 * and we want to compare the costs
 * 
 * It uses the MIP model as is described by Jung et al., 2019 
 * 
 * Problem P:
 * Describes an optimization problem involving n elective surgeries that have to
 * be scheduled in m ORs on a given day subject to the following constraint (for the ER schedule)
 * BIM constraint: surgeries may not overlap more than 2 hours. That is, when an emergency patient
 * arrives, they never have to wait for more than 2 hours until treatment. 
 * The goal is to minimize the weighted sum of the ORs' operating time, idle time and overtime. 
 * The weights are relative cost parameters. 
 * 
 * 
 * Question to be answered: Is time after surgeries are finished in an OR but unntil the amount of time units considered idle time
 * --> probably yes since we need them to be empty for the BIM. 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 */
import ilog.concert.*;
import ilog.cplex.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;
import java.util.Scanner;
import java.util.random.*;
import java.io.*;

public class MIP {
    
    private static final Boolean TRUE = null;
    private static final String FALSE = null;






    public static void main(String[]args){
        double[] costVector = {10.0, 15.0, 20.0};
        int numberOfOR = 3;
        int numberOfTimePeriods = 20;
        int maxOvertime = 16;
        int maxOverlap = 4; //u in the BIM constraint
        int[] surgeries = {13, 9, 7, 4, 4, 3}; //in time periods (so divide by 2 for amount of hours)
        int[] surgeries2 = {15, 12, 9, 9, 9, 8, 6, 5, 5, 4, 4, 3};
        double[] probabilities = readProbabilities("durationProbabilities.txt");





        

    }

    // emergencyCategoryProbabilities is a double array with the probabilities of any random emergency being in category low, medium, high
    // time intervals for each emergency surgery time (a workday has regular time from 7:00 to 17:00):
    // low: 10:00 to 17:00 (category 0) -----------  [0.3, 1.0]
    // medium: 7:00, 16:00 (category 1) ----------- [0.0, 0.9]
    // high: 7:00 to 9:00 and 15:00 to 17:00 (category 2) ----------- [0.0, 0.2] V [0.8, 1.0]
    // sample amount of surgeries in a day according to Poisson(0.282)
    // get the duration category for each surgery
    // get the time of the said duration category
    // also sets duration
    public static ArrayList<ArrivalTime> simulateEmergency(){
        // first we get the amount of emergency arrivals
        int numberOfEmergencies = 0;
        double[] poissonProb = {0.75427, 0.21271, 0.02999};
        double[] cumulativePoisson = {0.75427, 0.96698, 0.99697};
        double poissonRandom = Double.NaN;
        Boolean flag = true;
        while(flag){
            poissonRandom = Math.random();
            if (poissonRandom<=cumulativePoisson[2]) {
                flag=false;
            }
        }
        System.out.println("poisson random " + poissonRandom);        
        if (poissonRandom<cumulativePoisson[0]) {
            numberOfEmergencies = 0;// 0 emergencies
        } else if (poissonRandom>= cumulativePoisson[0] && poissonRandom<cumulativePoisson[1]) {
            numberOfEmergencies = 1;// 1 emergency
        } else if (poissonRandom>= cumulativePoisson[1]) {
            numberOfEmergencies = 2;// 2 emergencies
        }
        System.out.println("number of emergencies: " + numberOfEmergencies);

        // after we get the amount of emergencies, we get the category for each of the surgeries
        ArrayList<Integer> emergencyCategories = new ArrayList<>();
        double[] cumulativeCategoryProbabilities = {1.0*2/8, 1.0*7/8, 1.0*1};
        if (numberOfEmergencies>0){
            for(int i=0; i<numberOfEmergencies; i++){         
                double categoryRandom = Math.random();
                int emergencyCategory = 0;
                System.out.println("Sampled random:" + categoryRandom);
                if(categoryRandom<cumulativeCategoryProbabilities[0]){
                    emergencyCategory = 0;
                    System.out.println("Category low"); //category low
                } else if (categoryRandom>= cumulativeCategoryProbabilities[0] && categoryRandom< cumulativeCategoryProbabilities[1]) {
                    emergencyCategory = 1;
                    System.out.println("Category medium"); //category medium
                } else if (categoryRandom>=cumulativeCategoryProbabilities[1]) {
                    emergencyCategory = 2;            
                    System.out.println("Category high"); //category high
                }
                emergencyCategories.add(emergencyCategory);
            }
        }
        for(int j=0; j<emergencyCategories.size(); j++){
            System.out.println("category " + emergencyCategories.get(j));
        }

        //after we have the list of categories in the emergencyCategories arraylist, we sample the arrival time for each of them
        ArrayList<Double> emergencyArrivalTimes = new ArrayList<>();
        for(int emergency=0; emergency<emergencyCategories.size(); emergency++){
        
            //first for low: [0.3, 1.0]
            if (emergencyCategories.get(emergency) == 0) {
                double randomTime = Math.random();
                double arrivalTime = 0.3 + (randomTime*0.7);
                emergencyArrivalTimes.add(arrivalTime);
            }
            //for medium [0.0, 0.9]
            if (emergencyCategories.get(emergency) == 1) {
                double randomTime = Math.random();
                double arrivalTime = randomTime*0.9;
                emergencyArrivalTimes.add(arrivalTime);
            }
            //for high [0.0, 0.2] V [0.8, 1.0]
            if (emergencyCategories.get(emergency) == 2) {
                double randomTime = Math.random();
                double arrivalTime = 0;
                if (randomTime < 0.5) {
                    arrivalTime = (2*randomTime*0.2);
                } else{
                    arrivalTime = 0.8 + (2*(randomTime-0.5)*0.2); 
                }
                emergencyArrivalTimes.add(arrivalTime);
            }
        }
        ArrayList<ArrivalTime> emergencyList = new ArrayList<>();
        for(int i=0; i<emergencyArrivalTimes.size(); i++){
            ArrivalTime newArrival = new ArrivalTime((double) emergencyArrivalTimes.get(i));
            System.out.println("Emergency arrival at time: " + newArrival.getArrivalTimeString());
            emergencyList.add(newArrival);
        }

        //SETTING DURATION: TAKEN FROM TABLE S5 APPENDIX 7
        for(int i=0; i<emergencyCategories.size();i++){
            int duration = 0;
            double random = Math.random();
            if(emergencyCategories.get(i) == 0){ //category low
                if(random<(9.0/14)){
                    duration = 3;
                } else if (random>=(9.0/14) && random<1.0) {
                    duration = 4;
                }
            } else if (emergencyCategories.get(i) == 1) { //category medium
                if (random<(21.0/34)) {
                    duration = 5;
                } else if (random>=(21.0/34) && random<(26.0/34)) {
                    duration = 6;
                } else if (random>= (26.0/34) && random<1.0) {
                    duration = 7;
                }
            } else if (emergencyCategories.get(i) == 2) {
                if (random<(3.0/7)) {
                    duration = 9; 
                } else if (random>=(3.0/7) && random<(5.0/7)) {
                    duration = 11;
                } else if (random>=(5.0/7) && random<(6.0/7)) {
                    duration = 13;
                } else if (random>=(6.0/7) && random<1.0) {
                    duration = 15;
                }
            }
            emergencyList.get(i).setDuration(duration);
        }

        return emergencyList;
    }

    // for rescheduling with a list of emergencies, rather than just with one emergency
    public static CplexResult reschedulingWithEmergency(CplexResult BaseModel, ArrayList<ArrivalTime> emergencyList, ScheduleInstance instance){
        int numberOfEmergencies = emergencyList.size();
        ArrayList<CplexResult> rescheduledResultsList = new ArrayList<>();
        if(emergencyList.size()>0){ //if the sampled emergencylist is not empty
            CplexResult rescheduledResult1 = reschedulingOneEmergency(BaseModel, emergencyList.get(0), instance);
            rescheduledResultsList.add(rescheduledResult1);
            for(int i=1; i<numberOfEmergencies; i++){
                CplexResult rescheduledResult = reschedulingOneEmergency(rescheduledResultsList.get(i-1), emergencyList.get(i), instance);
                rescheduledResultsList.add(rescheduledResult);
            }
            return rescheduledResultsList.get(numberOfEmergencies-1); //final rescheduled
        } else{
            return BaseModel;
        }

    }

    // for rescheduling with only one emergency
    //ATTENTION 20/06
    public static CplexResult reschedulingOneEmergency(CplexResult BaseModel, ArrivalTime emergency, ScheduleInstance scheduleInstance){
        //variables for solving the MIP after rescheduling
        if(BaseModel == null){
            return null;
        }
        ScheduleInstance instance = BaseModel.getInstance();
        double[] timeWeights = instance.getTimeWeights();
        int[] surgeries = instance.getSurgeries(); 
        
        double idleTimeWeight = timeWeights[1];
        double regularTimeWeight = timeWeights[0];
        double overTimeWeight = timeWeights[2];
        int numberOfSurgeries = BaseModel.getYMatrix().length;
        System.out.println(numberOfSurgeries);
        int numberOfOR = instance.getNumberOfOR();
        int numberOfTimePeriods = instance.getNumberOfTimePeriods();
        int maxOvertime = instance.getMaxOvertime();
        int maxOverlap = instance.getMaxOverlap();

        double epsilon = 1e-9;
        double objectiveValue = Double.NaN;
        CplexResult resultRescheduled = new CplexResult();
        
        //setting the arrival time of an emergency surgery (for testing, later will implement the simulated arrivals)
        double arrivalTime1 = emergency.getTimeDouble(); //0.3 is equal to 10:00 or t=6 (can be scheduled at t=7 at the earliest due to integer constraint)
        int emergencyDuration = emergency.getDuration(); //also manual input for now

        //for findign the earliest available scheduling time
        //first we should check if there is idle time at the arrival time
        //if there is no idle time at the arrival time, we look at all the ongoing surgeries and find the one that finishes the soonest
        //a surgery is ongoing at the arrival time 
        double[][][] startingTimes = BaseModel.getYMatrix();
        double[][][] ongoingTimeMatrix = BaseModel.getXMatrix();
        int earliestAvailableTime = numberOfTimePeriods+maxOvertime;
        int earliestFinishingSurgery= 0;
        int earliestAvailableOR = 0;
        double arrivalTimeUnitDouble = arrivalTime1*20+epsilon;
        int arrivalTimeUnit = (int) Math.ceil(arrivalTimeUnitDouble); //rounding up since we can only start at the first following whole time period (arrival at t=6.02 is the same as arrival at t=7)

    

        //if an emergency arrives at a non integer time for example t=6.34, then for a surgery to be ongoing at that time, it must be ongoing at time t=6
        //if theres no idle time, we find the surgery that ends the soonest        
        System.out.println();
        System.out.println("Emergency arrival time: t=" + arrivalTimeUnitDouble);
        System.out.println("Earliest timeslot to schedule: t=" + arrivalTimeUnit);
        
        for(int j=0; j<numberOfOR; j++){ // no idle time implies that all ORS are operating\
            boolean isIdle = true; //boolean that keeps track if the operating room is idle at the time of emergency arrival            
            for(int i=0; i<numberOfSurgeries; i++){ //check all the surgeries and find the one that is ongoing on OR j+1
                if(Math.abs(ongoingTimeMatrix[i][j][arrivalTimeUnit-1 /*t=6 is at index 5 */] - 1.0) < epsilon){ //if there is a surgery planned: add +1 to regularTimeUsed and its not idle
                    isIdle = false;
                    boolean ongoing = true; //
                    for(int t=arrivalTimeUnit; t<numberOfTimePeriods+maxOvertime; t++){
                        if (!(Math.abs(ongoingTimeMatrix[i][j][t] - 1.0) < epsilon)) {                           
                            ongoing=false;
                            int endingTimePeriodIndex = t;
                            if((endingTimePeriodIndex+1) < earliestAvailableTime){
                                earliestAvailableOR = j+1;
                                earliestFinishingSurgery = i+1;
                                earliestAvailableTime = endingTimePeriodIndex+1;
                                System.out.println("Earliest finishing surgery is now: " + earliestFinishingSurgery + " at OR " + earliestAvailableOR + " finishing at time " + earliestAvailableTime);
                            }
                            break; 
                        }
                        if (!ongoing) {
                            break;
                        }
                    }
                }                 
            }
            if (isIdle) { //if OR j+1 is not being used (no surgeries found) then we set it to be the earliest available unit
                //setting the availability (in units, not index)
                earliestAvailableOR = j+1;
                earliestAvailableTime = arrivalTimeUnit; 
                System.out.println("operating room " + earliestAvailableOR + " is available immediately (at time t=" + arrivalTimeUnit + ")");
                break;
            }
        }

        int emergencyCompletionTimeIndex = (earliestAvailableTime-1)+emergencyDuration;
        int earliestCompletionTimeIndex = numberOfTimePeriods+maxOvertime-1;
        //earliest minimum completion time among the existing patients currently being treated at time earliestAvailableTime
        for(int j=0; j<numberOfOR; j++){            
            for(int i=0; i<numberOfSurgeries;i++){
                boolean ongoing = true;
                if(Math.abs(ongoingTimeMatrix[i][j][earliestAvailableTime-1 /*t=6 is at index 5 */] - 1.0) < epsilon){ //if the surgery i is ongoing at the earliest available starting time of the emergency surgery, then we check when it ends
                    for(int t=earliestAvailableTime; t<numberOfTimePeriods+maxOvertime; t++){
                        if (!(Math.abs(ongoingTimeMatrix[i][j][t] - 1.0) < epsilon)) {                           
                            ongoing=false;
                            int endingTimePeriodIndex = t;
                            if(endingTimePeriodIndex<earliestCompletionTimeIndex){
                                earliestCompletionTimeIndex = endingTimePeriodIndex;
                            }
                            break; 
                        }
                        if (!ongoing) {
                            break;
                        }
                    }
                }
            }
        }
        System.out.println("Earliest completion time from earliestAvailableTime: t=" + (earliestCompletionTimeIndex+1));

        System.out.println("Earliest available OR is " + earliestAvailableOR + " at t=" + earliestAvailableTime);

        
        int[] tempSurgeries = new int[surgeries.length+1];
        for(int i=0; i<surgeries.length;i++){
            tempSurgeries[i] = surgeries[i];
        } 
        tempSurgeries[surgeries.length] = emergencyDuration;
        surgeries = tempSurgeries;
        numberOfSurgeries = surgeries.length;
        System.out.println("surgeries:" + numberOfSurgeries);





        try{
            IloCplex cplex = new IloCplex();
            cplex.setParam(IloCplex.Param.MIP.Display, 2);
            cplex.setParam(IloCplex.Param.Output.WriteLevel, 2);

            //VARIABLES
            // Xijt: 3 dimensional array of Xijt binary (1 and 0) with 1 if surgery i uses time slot t in OR j (n*m*T) (also constraint (7a))
            IloNumVar[][][] X = new IloNumVar[numberOfSurgeries][numberOfOR][numberOfTimePeriods+maxOvertime];
            for(int i=1; i<=numberOfSurgeries; i++){
                for(int j=1; j<=numberOfOR; j++){
                    for(int t=1; t<=numberOfTimePeriods+maxOvertime; t++){
                        X[i-1][j-1][t-1] = cplex.boolVar(); //sets them to be binary variables
                    }
                }
            }

            // Yijt: 3 dimensional array of Yijt binary (1 and 0) with 1 if surgery i starts at time slot t in OR j (n*m*T) (also constraint (7b))
            IloNumVar[][][] Y = new IloNumVar[numberOfSurgeries][numberOfOR][numberOfTimePeriods+maxOvertime];
            for(int i=1; i<=numberOfSurgeries; i++){
                for(int j=1; j<=numberOfOR; j++){
                    for(int t=1; t<=numberOfTimePeriods+maxOvertime; t++){
                        Y[i-1][j-1][t-1] = cplex.boolVar(); //sets them to be binary variables
                    }
                }
            }

            // Zt: Array of Zt integer decision variable representing the number of surgeries occupied at time slot t in m ORs. (also constraint (8))
            IloNumVar[] Z = new IloNumVar[numberOfTimePeriods+maxOvertime];
            for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                Z[t] = cplex.intVar(0, Integer.MAX_VALUE);
            }
            //-------------------------------------------------------



            //OBJECTIVE FUNCTION
            IloLinearNumExpr objective = cplex.linearNumExpr();
            for(int i=1; i<=numberOfSurgeries; i++){
                for(int j=1; j<=numberOfOR; j++){
                    for(int t=1; t<=numberOfTimePeriods+maxOvertime; t++){
                        if(t<=numberOfTimePeriods){
                            objective.addTerm((regularTimeWeight-idleTimeWeight), X[i-1][j-1][t-1]);
                        } else if(numberOfTimePeriods < t && t <= numberOfTimePeriods + maxOvertime){
                            objective.addTerm(overTimeWeight, X[i-1][j-1][t-1]);
                        }
                    }
                }
            }
            //-------------------------------------------------------

            //adding minimization objective
            cplex.addMinimize(objective);  
            //-------------------------------------------------------


            //CONSTRAINTS
            //Constraint (1)*** 15/05/2024
            for (int i = 0; i < numberOfSurgeries; i++) {
                IloLinearNumExpr expression1 = cplex.linearNumExpr();
                for (int t = 0; t < numberOfTimePeriods+maxOvertime; t++) {
                    for (int j = 0; j < numberOfOR; j++) {
                        expression1.addTerm(1.0, Y[i][j][t]); // adds 1* Yijt to the expression 1
                    }
                }
                cplex.addEq(1, expression1); //adds the constraint that expression 1 should equal 1
            }

            //Constraint (2) 
            for(int j=0; j<numberOfOR;j++){
                for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                    IloLinearNumExpr expression2 = cplex.linearNumExpr();
                    for(int i=0; i<numberOfSurgeries; i++){
                        expression2.addTerm(1.0, X[i][j][t]); // adds 1*Xijt to the expression 2
                    }
                    cplex.addLe(expression2, 1); //adds the constraint that expression 2 should <= 1
                }
            }

            //Constraint (3)
            for(int i=1; i<=numberOfSurgeries; i++){
                for(int j=1; j<=numberOfOR; j++){
                    for(int t=1; t<=numberOfTimePeriods+maxOvertime; t++){   
                        IloLinearNumExpr expression3 = cplex.linearNumExpr();
                        int min = Math.min(t+surgeries[i-1]-1,numberOfTimePeriods+maxOvertime);
                        for(int r=t; r<=min; r++){ //note that t+surgeries[i] doesnt have -1 since t is index adjusted (starting at 0 until T+L-1)
                            expression3.addTerm(1.0, X[i-1][j-1][r-1]);
                        }
                        IloLinearNumExpr RHS = cplex.linearNumExpr();
                        RHS.addTerm(surgeries[i-1], Y[i-1][j-1][t-1]);
                        cplex.addGe(expression3, RHS);
                    }
                }
            }



            //Constraint (4)
            for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                IloLinearNumExpr expression4 = cplex.linearNumExpr();
                for(int i=0; i<numberOfSurgeries; i++){
                    for(int j=0; j<numberOfOR; j++){
                        expression4.addTerm(1.0, X[i][j][t]);
                    }
                }
                cplex.addEq(expression4, Z[t]); //should i switch these around?
            }

            //Constraint (5) (BIM)
            for(int k=earliestCompletionTimeIndex; k<numberOfTimePeriods+maxOvertime; k++){
                int RHS = ((maxOverlap +1)*numberOfOR) - 1;
                IloLinearNumExpr expression5 = cplex.linearNumExpr();
                
                // Sum Z_t from t=k to t=k+maxOverlap
                for(int t=k; t<=Math.min(k+maxOverlap, numberOfTimePeriods+maxOvertime-1); t++){
                    expression5.addTerm(1.0, Z[t]);
                }
                
                // Subtract Y_ijt for overlapping surgeries
                for(int i=0; i<numberOfSurgeries; i++){
                    for(int j=0; j<numberOfOR; j++){
                        for(int t=k+1; t<=Math.min(k+maxOverlap, numberOfTimePeriods+maxOvertime-1); t++){
                            expression5.addTerm(-1.0, Y[i][j][t]);
                        }
                    }
                }
                
                cplex.addLe(expression5, RHS);
            }

            //Constraint (6)
            IloLinearNumExpr expression6 = cplex.linearNumExpr();
            for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                expression6.addTerm(1.0, Z[t]);
            }
            double totalSurgeries = 0;
            for(int i=0; i<numberOfSurgeries; i++){
                totalSurgeries+=surgeries[i];
            }
            cplex.addEq(expression6, totalSurgeries);





            int earliestAvailableTimeIndex = earliestAvailableTime-1;
            //RESCHEDULING CONSTRAINTs:
            //emergency arrival, ongoing and completed surgeries should be fixed (where fixed equals: the same as before the rescheduling (so the same as in the BIM base model))
            //scheduling the emergency
            cplex.addLe(Y[numberOfSurgeries-1][earliestAvailableOR-1][earliestAvailableTimeIndex], 1.01); //the emergency surgery must start at the earliest available time
            cplex.addGe(Y[numberOfSurgeries-1][earliestAvailableOR-1][earliestAvailableTimeIndex], 0.99);
            
            
            for(int i=0; i<numberOfSurgeries-1; i++){
                for(int j=0; j<numberOfOR; j++){
                    for(int t=0; t<earliestAvailableTimeIndex; t++){
                        if(Math.abs(startingTimes[i][j][t]-1) < epsilon){ //all starting times before the emergency should remain the same
                            cplex.addLe(Y[i][j][t], 1.01);
                            cplex.addGe(Y[i][j][t], 0.99);
                        }
                    }
                }
            }

            for(int i=0; i<numberOfSurgeries-1; i++){
                for(int j=0; j<numberOfOR; j++){
                    for(int t=0; t<earliestAvailableTimeIndex; t++){
                        if((Math.abs(ongoingTimeMatrix[i][j][t]-0) < epsilon)){ //all idle times before the surgery should remain the same (to avoid later surgeries being performed "in the past")
                            cplex.addLe(X[i][j][t], 0.01);
                            cplex.addGe(X[i][j][t], -0.01);
                        }
                    }
                }
            }


            
            //-------------------------------------------------------


            //SOLVE and OUTPUT
            if (cplex.solve()) {
                System.out.println("obj = "+cplex.getObjValue());

                //output solutions for X by surgery
                for(int i=1; i<=numberOfSurgeries; i++){
                    System.out.println();
                    System.out.println("Surgery " + i + " takes " + surgeries[i-1] + " time units");
                    for(int j=1; j<=numberOfOR; j++){
                        for(int t=1; t<=numberOfTimePeriods+maxOvertime; t++){
                            if(Math.abs(cplex.getValue(X[i-1][j-1][t-1]) - 1.0) < epsilon){
                                System.out.println("Surgery " + i + " at OR " + j + " at time t= " + t);
                            }
                        }
                    }
                }
                //-------------------------------------------------------

                
                //output solution for X by operating room
                for(int j=0; j<numberOfOR; j++){
                    System.out.println();
                    System.out.println("Operating room " + (j+1));
                    for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                        for(int i=0; i<numberOfSurgeries; i++){
                            if(Math.abs(cplex.getValue(X[i][j][t]) - 1.0) < epsilon){
                                System.out.println("Surgery " + (i+1) + " at t= " + (t+1));
                            }
                        }
                    }                   
                }
                //-------------------------------------------------------



                //output solution by time usage: regular time, idle time, overtime
                int regularTimeUsed = 0;
                int idleTimeUsed = 0;
                int overTimeUsed = 0;
                boolean idleBoolean = true;
                for(int j=0; j<numberOfOR; j++){ //iterate over operating rooms
                    System.out.println();
                    for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){ //iterate over time periods

                        //boolean that becomes false if OR j at time t has a surgery  that indicates idle time
                        idleBoolean = true;
                       
                        if(t<numberOfTimePeriods){ //during regular time
                            for(int i=0; i<numberOfSurgeries; i++){ //we check if there is a surgery planned in OR j at time t
                                if(Math.abs(cplex.getValue(X[i][j][t]) - 1.0) < epsilon){ //if there is a surgery planned: add +1 to regularTimeUsed and its not idle
                                    regularTimeUsed++;
                                    idleBoolean = false;
                                    break;
                                }
                            }
                            if(idleBoolean){idleTimeUsed++; System.out.println("OR " + (j+1) + " is idle at time " + (t+1));} //else if there is no surgery planned in OR j at time t, we add +1 to idleTimeUsed
                        }

                        else if(!(t<numberOfTimePeriods)){ //else if we are in overtime
                            for(int i=0; i<numberOfSurgeries; i++){ //we check if there is a surgery planned in OR j at time t (overtime)
                                if(Math.abs(cplex.getValue(X[i][j][t]) - 1.0) < epsilon){ //if there is a surgery planned: add +1 to overTimeUsed and do not keep track of idle (idle time during overtime does not count)
                                    overTimeUsed++;
                                    break;
                                }
                            }
                        }
                       
                    }
                }
                

                //checking if all surgeries have been correctly scheduled
                for(int i=1; i<=numberOfSurgeries; i++){
                    int surgeryLength = surgeries[i-1];
                    int scheduled = 0;
                    for(int j=1; j<=numberOfOR; j++){
                        for(int t=1; t<=numberOfTimePeriods+maxOvertime; t++){
                            if(Math.abs(cplex.getValue(X[i-1][j-1][t-1]) - 1.0) < epsilon){
                                scheduled++;
                            }
                        }
                    }
                    if(surgeryLength == scheduled){
                        System.out.println("Surgery " + i + " scheduled succesfully");
                    } else{
                        System.out.println("Surgery " + i + " incorrectly scheduled");
                    }
                }

                System.out.println("\n" + "regular time: " + regularTimeUsed + " units");
                System.out.println("idle time: " + idleTimeUsed + " units");
                System.out.println("over time: " + overTimeUsed + " units");
                System.out.println("obj: " + cplex.getObjValue());
                objectiveValue = cplex.getObjValue();
                double[][][] xMatrix = new double[numberOfSurgeries][numberOfOR][numberOfTimePeriods+maxOvertime];
                double[][][] yMatrix = new double[numberOfSurgeries][numberOfOR][numberOfTimePeriods+maxOvertime];
                double[] zArray = new double[numberOfTimePeriods+maxOvertime];
                int[] timeVector = {regularTimeUsed, idleTimeUsed, overTimeUsed};

                for(int i=0; i<numberOfSurgeries; i++){
                    for(int j=0; j<numberOfOR; j++){
                        for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                            xMatrix[i][j][t] = cplex.getValue(X[i][j][t]);
                            yMatrix[i][j][t] = cplex.getValue(Y[i][j][t]);
                        }
                    }
                 }
                for(int t=0; t<numberOfTimePeriods+maxOvertime;t++){
                    zArray[t] = cplex.getValue(Z[t]);
                }

                resultRescheduled = new CplexResult(xMatrix, yMatrix, zArray, objectiveValue,timeVector); 

                cplex.end();                
            } //end of if(solve)
            //-------------------------------------------------------

            else {
                System.out.println("Model not solved");
                return null;
            }

        } //end of try 

        //catching exception
        catch(IloException exc){
            exc.printStackTrace();
        }
        
        ScheduleInstance newInstance = new ScheduleInstance(instance.getTimeWeights(), instance.getNumberOfOR(), instance.getNumberOfTimePeriods(), instance.getMaxOvertime(), surgeries, instance.getMaxOverlap());
        resultRescheduled.setInstance(newInstance);
        return resultRescheduled;
        
    }

    //full simulation model for simulations with emergency
    public static void simulateWithEmergency(int N, double[] probabilities, double[] timeWeights, String filenameToStore){
        
        double totalObjectiveBIM =0;
        double totalObjectiveNoBIM = 0;
        BufferedWriter writer = null;
        int numberOfTimePeriods = 20;
        int maxOvertime = 16;
        int maxOverlap = 4;
        int numberOfOR = 3;
        double epsilon = 1e-9;
        ArrivalTime emergencyArrival = new ArrivalTime(0.31);
        
       double averageObjEmergency = 0.0;
       double averageObjNoBIM = 0.0;
       double averageObjBIM = 0.0;
       double averageOvertimeNoBIM = 0;
       double averageOvertimeBIM = 0;
       double averageOVertimeReschedule = 0;
        
        try {
            double maxDifference = 0; 
            int maxDifferenceInstance = 0;
            writer = new BufferedWriter(new FileWriter(filenameToStore));
            for(int n=0; n<N; n++){
                writer.write("Instance: " + (n+1) + "\n");
                int[] testWeek = generateWeekOfSurgeries(probabilities);
                ArrayList<ArrayList<Integer>> testInstance1 = applyLPTtoWeeklySchedule(testWeek);
                ArrayList<Integer> testDayArrayList = testInstance1.get(2); //tuesday
                int[] testDay = new int[testDayArrayList.size()];
                int totalTestTimeUnits =0;
                for(int i=0; i<testDay.length; i++){
                    testDay[i] = testDayArrayList.get(i);
                    totalTestTimeUnits+=testDay[i];
                }
                ScheduleInstance testInstance = new ScheduleInstance(timeWeights, numberOfOR, numberOfTimePeriods, maxOvertime, testDay, maxOverlap);
                CplexResult resultWithBIM = modelBIM(testInstance);
                CplexResult resultNoBIM = modelNoBIM(testInstance);
                ArrayList<ArrivalTime> emergencyList = simulateEmergency();
                CplexResult rescheduled = reschedulingWithEmergency(resultWithBIM, emergencyList, testInstance);
                double objectiveWithBIM = resultWithBIM.getObjectiveValue();
                double objectiveNoBIM = resultNoBIM.getObjectiveValue();
                double objectiveRescheduled = rescheduled.getObjectiveValue();
                double difference = Math.abs(objectiveWithBIM-objectiveNoBIM);
                if (difference > maxDifference) {
                    maxDifference = difference;
                    maxDifferenceInstance = (n+1);
                }
                totalObjectiveBIM+=objectiveWithBIM;
                totalObjectiveNoBIM+=objectiveNoBIM;
                
                if (emergencyList.size()==1) {
                    writer.write("Emergencies: t=" + String.format("%.2f", emergencyList.get(0).getTime()) + " (earliest t= " + ((int)Math.ceil(emergencyList.get(0).getTime())) + ") with duration "+ emergencyList.get(0).getDuration() + "\n");
                } else if (emergencyList.size()==2) {
                    writer.write("Emergencies: t=" + String.format("%.2f", emergencyList.get(0).getTime()) + " (earliest t= " + ((int)Math.ceil(emergencyList.get(0).getTime())) + ") with duration " + emergencyList.get(0).getDuration() + "\n");
                    writer.write("Emergencies: t=" + String.format("%.2f", emergencyList.get(1).getTime()) + " (earliest t= " + ((int)Math.ceil(emergencyList.get(1).getTime())) + ") with duration " + emergencyList.get(1).getDuration()+ "\n");
                }
                
                System.out.println();
                writer.write("Surgeries to schedule (excluding emergencies): ");
                for(int j=0; j<testDay.length; j++){
                    System.out.println(testDay[j]);
                    if(!(j==testDay.length-1)){
                        writer.write(testDay[j] + ", ");
                    } else {
                        writer.write(testDay[j] + " (" + totalTestTimeUnits + " total time units)" + "\n");
                    } 
                    
                }
                System.out.println("Total time units in day: " + totalTestTimeUnits + " (" + 1.0*totalTestTimeUnits/2 + " hours)");
                System.out.println("Objective value with emergency robustness: " + objectiveWithBIM);
                System.out.println("Objective value without emergency robustness: " + objectiveNoBIM);
                System.out.println("Objective value with rescheduling:"  + objectiveRescheduled);

                //------------------------------------------------------------- the following is used to write to the file
                
                double[][][] xMatrixBIM = resultWithBIM.getXMatrix();
                double[][][] xMatrixNoBIM = resultNoBIM.getXMatrix();
                double[][][] xMatrixRescheduled = rescheduled.getXMatrix();
                int[] timeVectorBIM = resultWithBIM.getTimeVector();
                int[] timeVectorNoBIM = resultNoBIM.getTimeVector();
                int[] timeVectorRescheduled = rescheduled.getTimeVector();
                int numberofSurgeries = testDay.length;


                //without BIM
                writer.write("Without BIM: ("  + timeVectorNoBIM[0] + ", " + timeVectorNoBIM[1] + ", " + timeVectorNoBIM[2] + ")   (" + resultNoBIM.getObjectiveValue() + ")" +"\n");
                Boolean flag2 = false;
                for(int j=0; j<numberOfOR; j++){
                    for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                        flag2 = false;
                        for(int i=0; i<numberofSurgeries; i++){                            
                            if(Math.abs(xMatrixNoBIM[i][j][t] - 1.0) < epsilon){
                                flag2=true;
                                break;
                            } 
                        }
                        if (flag2) {
                            writer.write("1 ");
                        } else {
                            writer.write("* ");
                        }
                    }
                    writer.write("\n");
                }

                //with BIM
                writer.write("With BIM: ("  + timeVectorBIM[0] + ", " + timeVectorBIM[1] + ", " + timeVectorBIM[2] + ")   (" + resultWithBIM.getObjectiveValue()+ ")" +"\n");                
                Boolean flag = false;
                for(int j=0; j<numberOfOR; j++){
                    for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                        flag = false;
                        for(int i=0; i<numberofSurgeries; i++){                            
                            if(Math.abs(xMatrixBIM[i][j][t] - 1.0) < epsilon){
                                flag=true;
                                break;
                            } 
                        }
                        if (flag) {
                            writer.write("1 ");
                        } else {
                            writer.write("* ");
                        }
                    }
                    writer.write("\n");
                }                

                //for rescheduled:
                writer.write("With rescheduling: ("  + timeVectorRescheduled[0] + ", " + timeVectorRescheduled[1] + ", " + timeVectorRescheduled[2] + ")   (" + rescheduled.getObjectiveValue() + ")" +"\n");
                Boolean flag3 = false;
                Boolean isEmergency = false;
                for(int j=0; j<numberOfOR; j++){
                    for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                        flag3 = false;
                        isEmergency = false;
                        for(int i=0; i<xMatrixRescheduled.length; i++){  
                            if (((i==xMatrixRescheduled.length-1)&&(Math.abs(xMatrixRescheduled[i][j][t] - 1.0) < epsilon) && emergencyList.size()==1) || 
                                ((i==xMatrixRescheduled.length-1)&&(Math.abs(xMatrixRescheduled[i][j][t] - 1.0) < epsilon) && emergencyList.size()==2) ||
                                ((i==xMatrixRescheduled.length-2)&&(Math.abs(xMatrixRescheduled[i][j][t] - 1.0) < epsilon) && emergencyList.size()==2)) { //for the emergency schedule
                                isEmergency = true;
                                break;
                            }                          
                            if(Math.abs(xMatrixRescheduled[i][j][t] - 1.0) < epsilon){
                                flag3=true;
                                break;
                            } 
                        }
                        if (isEmergency) {
                            writer.write("e ");
                        }
                        else if (flag3) {
                            writer.write("1 ");
                        } else {
                            writer.write("* ");
                        }
                    }
                    writer.write("\n");
                }

                writer.write("\n");
                for(int j=0; j<numberOfOR;j++){
                    for(int t=0; t<numberOfTimePeriods+maxOvertime;t++){
                        if (Math.abs(xMatrixRescheduled[xMatrixRescheduled.length-1][j][t] - 1.0) < epsilon) {
                            System.out.println("emergency surgery planned in OR " + (j+1) + " at time " + (t+1));
                        }
                    }
                }

                averageObjNoBIM += (1.0/N)*objectiveNoBIM;
                averageObjBIM += (1.0/N)*objectiveWithBIM;
                averageObjEmergency += (1.0/N)*objectiveRescheduled;
                averageOvertimeNoBIM += (1.0/N)*timeVectorNoBIM[2];
                averageOvertimeBIM += (1.0/N)*timeVectorBIM[2];
                averageOVertimeReschedule += (1.0/N)*timeVectorRescheduled[2];
                
                
            }   //end of iterations

            writer.write("Instance with the biggest difference: " + maxDifferenceInstance + " (" + maxDifference + ")" + "\n");

            writer.write("Average objective value without BIM: " + averageObjNoBIM + "\n");
            writer.write("Average objective value with BIM: " + averageObjBIM + "\n");
            writer.write("Average objective value with emergencies " + averageObjEmergency + "\n");

            writer.write("Average overtime used without BIM (in time units): " + averageOvertimeNoBIM + "\n");
            writer.write("Average overtime used with BIM (in time units): " + averageOvertimeBIM + "\n");
            writer.write("Average overtime used with emergency (in time units): " + averageOVertimeReschedule + "\n");


            System.out.println("average objective with ER: " + averageObjBIM);
            System.out.println("average objective without ER: " + averageObjNoBIM);
            

            
       } catch (IOException e){
            System.err.println("An error occurred while writing to the file.");
            e.printStackTrace();
       } finally {
            try {
                if (writer != null){
                    writer.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    //full simulation model but can choose overlap and number of surgeries (this is the main one to use for the results)
    public static void simulateWithEmergencyAndNumberOfSurgeries(int N, double[] probabilities, double[] timeWeights,int maximumOverlap, String filenameToStore, int numberOfSurgeries){
        
        double totalObjectiveBIM =0;
        double totalObjectiveNoBIM = 0;
        BufferedWriter writer = null;
        int numberOfTimePeriods = 20;
        int maxOvertime = 16;
        int maxOverlap = maximumOverlap;
        int numberOfOR = 3;
        double epsilon = 1e-9;
        ArrivalTime emergencyArrival = new ArrivalTime(0.31);

        
       double averageObjEmergency = 0.0;
       double averageObjNoBIM = 0.0;
       double averageObjBIM = 0.0;
       double averageOvertimeNoBIM = 0;
       double averageOvertimeBIM = 0;
       double averageOVertimeReschedule = 0;
        
        try {
            double maxDifference = 0; 
            int maxDifferenceInstance = 0;
            writer = new BufferedWriter(new FileWriter(filenameToStore));
            for(int n=0; n<N; n++){
                writer.write("Instance: " + (n+1) + "\n");
                

                //generating the surgeries--------------------------------
                int[] dayOfSurgeries = generateDayOfSurgeriesByNumber(numberOfSurgeries);                
                ScheduleInstance testInstance = new ScheduleInstance(timeWeights, numberOfOR, numberOfTimePeriods, maxOvertime, dayOfSurgeries, maxOverlap);

                //total hours of surgery
                int totalTestTimeUnits = 0;
                for(int i=0; i<dayOfSurgeries.length; i++){
                    totalTestTimeUnits+=dayOfSurgeries[i];
                }

                CplexResult resultWithBIM = modelBIM(testInstance);
                CplexResult resultNoBIM = modelNoBIM(testInstance);
                ArrayList<ArrivalTime> emergencyList = simulateEmergency();
                CplexResult rescheduled = reschedulingWithEmergency(resultWithBIM, emergencyList, testInstance);

                if(resultWithBIM == null || resultNoBIM == null || rescheduled == null){
                    continue;
                }

                double objectiveWithBIM = resultWithBIM.getObjectiveValue();
                double objectiveNoBIM = resultNoBIM.getObjectiveValue();
                double objectiveRescheduled = rescheduled.getObjectiveValue();
                double difference = Math.abs(objectiveWithBIM-objectiveNoBIM);
                if (difference > maxDifference) {
                    maxDifference = difference;
                    maxDifferenceInstance = (n+1);
                }
                totalObjectiveBIM+=objectiveWithBIM;
                totalObjectiveNoBIM+=objectiveNoBIM;
                
                if (emergencyList.size()==1) {
                    writer.write("Emergencies: t=" + String.format("%.2f", emergencyList.get(0).getTime()) + " (earliest t= " + ((int)Math.ceil(emergencyList.get(0).getTime())) + ") with duration "+ emergencyList.get(0).getDuration() + "\n");
                } else if (emergencyList.size()==2) {
                    writer.write("Emergencies: t=" + String.format("%.2f", emergencyList.get(0).getTime()) + " (earliest t= " + ((int)Math.ceil(emergencyList.get(0).getTime())) + ") with duration " + emergencyList.get(0).getDuration() + "\n");
                    writer.write("Emergencies: t=" + String.format("%.2f", emergencyList.get(1).getTime()) + " (earliest t= " + ((int)Math.ceil(emergencyList.get(1).getTime())) + ") with duration " + emergencyList.get(1).getDuration()+ "\n");
                }
                
                System.out.println();
                writer.write("Surgeries to schedule (excluding emergencies): ");
                for(int j=0; j<dayOfSurgeries.length; j++){
                    System.out.println(dayOfSurgeries[j]);
                    if(!(j==dayOfSurgeries.length-1)){
                        writer.write(dayOfSurgeries[j] + ", ");
                    } else {
                        writer.write(dayOfSurgeries[j] + " (" + totalTestTimeUnits + " total time units)" + "\n");
                    } 
                    
                }
                System.out.println("Total time units in day: " + totalTestTimeUnits + " (" + 1.0*totalTestTimeUnits/2 + " hours)");
                System.out.println("Objective value with emergency robustness: " + objectiveWithBIM);
                System.out.println("Objective value without emergency robustness: " + objectiveNoBIM);
                System.out.println("Objective value with rescheduling:"  + objectiveRescheduled);

                //------------------------------------------------------------- the following is used to write to the file
                
                double[][][] xMatrixBIM = resultWithBIM.getXMatrix();
                double[][][] xMatrixNoBIM = resultNoBIM.getXMatrix();
                double[][][] xMatrixRescheduled = rescheduled.getXMatrix();
                int[] timeVectorBIM = resultWithBIM.getTimeVector();
                int[] timeVectorNoBIM = resultNoBIM.getTimeVector();
                int[] timeVectorRescheduled = rescheduled.getTimeVector();
                int numberofSurgeries = dayOfSurgeries.length;


                //without BIM
                writer.write("Without BIM: ("  + timeVectorNoBIM[0] + ", " + timeVectorNoBIM[1] + ", " + timeVectorNoBIM[2] + ")   (" + resultNoBIM.getObjectiveValue() + ")" +"\n");
                Boolean flag2 = false;
                for(int j=0; j<numberOfOR; j++){
                    for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                        flag2 = false;
                        for(int i=0; i<numberofSurgeries; i++){                            
                            if(Math.abs(xMatrixNoBIM[i][j][t] - 1.0) < epsilon){
                                flag2=true;
                                break;
                            } 
                        }
                        if (flag2) {
                            writer.write("1 ");
                        } else {
                            writer.write("* ");
                        }
                    }
                    writer.write("\n");
                }

                //with BIM
                writer.write("With BIM: ("  + timeVectorBIM[0] + ", " + timeVectorBIM[1] + ", " + timeVectorBIM[2] + ")   (" + resultWithBIM.getObjectiveValue()+ ")" +"\n");                
                Boolean flag = false;
                for(int j=0; j<numberOfOR; j++){
                    for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                        flag = false;
                        for(int i=0; i<numberofSurgeries; i++){                            
                            if(Math.abs(xMatrixBIM[i][j][t] - 1.0) < epsilon){
                                flag=true;
                                break;
                            } 
                        }
                        if (flag) {
                            writer.write("1 ");
                        } else {
                            writer.write("* ");
                        }
                    }
                    writer.write("\n");
                }                

                //for rescheduled:
                writer.write("With rescheduling: ("  + timeVectorRescheduled[0] + ", " + timeVectorRescheduled[1] + ", " + timeVectorRescheduled[2] + ")   (" + rescheduled.getObjectiveValue() + ")" +"\n");
                Boolean flag3 = false;
                Boolean isEmergency = false;
                for(int j=0; j<numberOfOR; j++){
                    for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                        flag3 = false;
                        isEmergency = false;
                        for(int i=0; i<xMatrixRescheduled.length; i++){  
                            if (((i==xMatrixRescheduled.length-1)&&(Math.abs(xMatrixRescheduled[i][j][t] - 1.0) < epsilon) && emergencyList.size()==1) || 
                                ((i==xMatrixRescheduled.length-1)&&(Math.abs(xMatrixRescheduled[i][j][t] - 1.0) < epsilon) && emergencyList.size()==2) ||
                                ((i==xMatrixRescheduled.length-2)&&(Math.abs(xMatrixRescheduled[i][j][t] - 1.0) < epsilon) && emergencyList.size()==2)) { //for the emergency schedule
                                isEmergency = true;
                                break;
                            }                          
                            if(Math.abs(xMatrixRescheduled[i][j][t] - 1.0) < epsilon){
                                flag3=true;
                                break;
                            } 
                        }
                        if (isEmergency) {
                            writer.write("e ");
                        }
                        else if (flag3) {
                            writer.write("1 ");
                        } else {
                            writer.write("* ");
                        }
                    }
                    writer.write("\n");
                }

                writer.write("\n");
                for(int j=0; j<numberOfOR;j++){
                    for(int t=0; t<numberOfTimePeriods+maxOvertime;t++){
                        if (Math.abs(xMatrixRescheduled[xMatrixRescheduled.length-1][j][t] - 1.0) < epsilon) {
                            System.out.println("emergency surgery planned in OR " + (j+1) + " at time " + (t+1));
                        }
                    }
                }

                averageObjNoBIM += (1.0/N)*objectiveNoBIM;
                averageObjBIM += (1.0/N)*objectiveWithBIM;
                averageObjEmergency += (1.0/N)*objectiveRescheduled;
                averageOvertimeNoBIM += (1.0/N)*timeVectorNoBIM[2];
                averageOvertimeBIM += (1.0/N)*timeVectorBIM[2];
                averageOVertimeReschedule += (1.0/N)*timeVectorRescheduled[2];
                
                
            }   //end of iterations

            writer.write("Instance with the biggest difference: " + maxDifferenceInstance + " (" + maxDifference + ")" + "\n");

            writer.write("Average objective value without BIM: " + averageObjNoBIM + "\n");
            writer.write("Average objective value with BIM: " + averageObjBIM + "\n");
            writer.write("Average objective value with emergencies " + averageObjEmergency + "\n");

            writer.write("Average overtime used without BIM (in time units): " + averageOvertimeNoBIM + "\n");
            writer.write("Average overtime used with BIM (in time units): " + averageOvertimeBIM + "\n");
            writer.write("Average overtime used with emergency (in time units): " + averageOVertimeReschedule + "\n");


            System.out.println("average objective with ER: " + averageObjBIM);
            System.out.println("average objective without ER: " + averageObjNoBIM);
            

            
       } catch (IOException e){
            System.err.println("An error occurred while writing to the file.");
            e.printStackTrace();
       } finally {
            try {
                if (writer != null){
                    writer.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    //simulating but without emergency arrivals
    public static void simulate(int N, double[] probabilities, double[] timeWeights, int maximumOverlap, String filenameToStore){
        double totalObjectiveBIM =0;
        double totalObjectiveNoBIM = 0;
        BufferedWriter writer = null;
        int numberOfTimePeriods = 20;
        int maxOvertime = 16;
        int maxOverlap = maximumOverlap;
        int numberOfOR = 3;
        double epsilon = 1e-9;

        double averageTimeUnitsOfSurgery = 0;
        double averageOverTimeNoBIM = 0;
        double averageOverTimeWithBIM = 0;


       
        
        try {
            double maxDifference = 0; 
            int maxDifferenceInstance = 0;
            writer = new BufferedWriter(new FileWriter(filenameToStore));
            for(int n=0; n<N; n++){
                writer.write("Instance: " + (n+1) + "\n");
                writer.write("regular time:" + numberOfTimePeriods + " overtime:" + maxOvertime + " maxoverlap:" + maxOverlap+ "\n");

                //generating surgeries
                int[] testDay1 = generateDayOfSurgeries(1); 

                //THE FOLLOWING PART WILL MAKE THE DAY INSTANCE 2X THE AMOUNT OF SURGERIES by merging 2 instances
                //-----------------------------------------------------------
                int[] testDay2 = generateDayOfSurgeries(1); //
                int increaseBySurgeries = 2; //this is by how many surgeries we wish to increase the original instance (3 implies that 3 more surgeries will be added) 
                int[] testDay =  new int[testDay1.length+increaseBySurgeries];
                for(int i=0; i<testDay1.length; i++){
                    testDay[i] = testDay1[i];
                }
                //this is by how many surgeries we wish to increase the original instance (3 implies that 3 more surgeries will be added) 
                for(int i=testDay1.length; i<testDay.length; i++){
                    testDay[i] = testDay2[i-testDay1.length];
                }
                //-----------------------------------------------------------


                int totalTestTimeUnits = 0;
                for(int i=0; i<testDay.length;i++){
                    totalTestTimeUnits+=testDay[i];
                }



                ScheduleInstance testInstance = new ScheduleInstance(timeWeights, numberOfOR, numberOfTimePeriods, maxOvertime, testDay, maxOverlap); //only testDay1

                CplexResult resultWithBIM = modelBIM(testInstance);
                CplexResult resultNoBIM = modelNoBIM(testInstance);

                double objectiveWithBIM = resultWithBIM.getObjectiveValue();
                double objectiveNoBIM = resultNoBIM.getObjectiveValue();
                double difference = Math.abs(objectiveWithBIM-objectiveNoBIM);

                if (difference > maxDifference) {
                    maxDifference = difference;
                    maxDifferenceInstance = (n+1);
                }
                totalObjectiveBIM+=objectiveWithBIM;
                totalObjectiveNoBIM+=objectiveNoBIM;
                
                
                System.out.println();
                writer.write("Surgeries to schedule: ");
                for(int j=0; j<testDay.length; j++){
                    System.out.println(testDay[j]);
                    if(!(j==testDay.length-1)){
                        writer.write(testDay[j] + ", ");
                    } else {
                        writer.write(testDay[j] + " (" + totalTestTimeUnits + " total time units)" + "\n");
                    } 
                    
                }
                System.out.println("Total time units in day: " + totalTestTimeUnits + " (" + 1.0*totalTestTimeUnits/2 + " hours)");
                System.out.println("Objective value with emergency robustness: " + objectiveWithBIM);
                System.out.println("Objective value without emergency robustness: " + objectiveNoBIM);

                //------------------------------------------------------------- the following is used to write to the file
                
                double[][][] xMatrixBIM = resultWithBIM.getXMatrix();
                double[][][] xMatrixNoBIM = resultNoBIM.getXMatrix();
                int[] timeVectorBIM = resultWithBIM.getTimeVector();
                int[] timeVectorNoBIM = resultNoBIM.getTimeVector();
                int numberofSurgeries = testDay.length;
                writer.write("With BIM: ("  + timeVectorBIM[0] + ", " + timeVectorBIM[1] + ", " + timeVectorBIM[2] + ")   (" + resultWithBIM.getObjectiveValue()+ ")" +"\n");
                
                Boolean flag = false;
                for(int j=0; j<numberOfOR; j++){
                    for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                        flag = false;
                        for(int i=0; i<numberofSurgeries; i++){                            
                            if(Math.abs(xMatrixBIM[i][j][t] - 1.0) < epsilon){
                                flag=true;
                                break;
                            } 
                        }
                        if (t == numberOfTimePeriods) { //for the space after regular time
                            writer.write("  ");
                        }
                        if (flag) {
                            writer.write("1 ");
                        } else {
                            writer.write("* ");
                        }
                    }
                    writer.write("\n");
                }

                writer.write("Without BIM: ("  + timeVectorNoBIM[0] + ", " + timeVectorNoBIM[1] + ", " + timeVectorNoBIM[2] + ")   (" + resultNoBIM.getObjectiveValue() + ")" +"\n");
                Boolean flag2 = false;
                for(int j=0; j<numberOfOR; j++){
                    for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                        flag2 = false;
                        for(int i=0; i<numberofSurgeries; i++){                            
                            if(Math.abs(xMatrixNoBIM[i][j][t] - 1.0) < epsilon){
                                flag2=true;
                                break;
                            } 
                        }
                        if (t == numberOfTimePeriods) { //for the space after regular time
                            writer.write("  ");
                        }
                        if (flag2) {
                            writer.write("1 ");
                        } else {
                            writer.write("* ");
                        }
                    }
                    writer.write("\n");
                }

                writer.write("\n");
                
                //for averages:
                averageTimeUnitsOfSurgery += (1.0/N)*totalTestTimeUnits;
                averageOverTimeNoBIM += (1.0/N)*timeVectorNoBIM[2];
                averageOverTimeWithBIM += (1.0/N)*timeVectorBIM[2];

            }   //end of iterating
            double averageBIM = totalObjectiveBIM/(1.0*N);
            double averageNoBIM = totalObjectiveNoBIM/(1.0*N);

            writer.write("Instance with the biggest difference: " + maxDifferenceInstance + " (" + maxDifference + ")" + "\n");
            writer.write("Average daily time units (half hours) of elective surgery: " + averageTimeUnitsOfSurgery + "\n");
            writer.write("Average time units (half hours) of overtime with BIM: " + averageOverTimeWithBIM + "\n");
            writer.write("Average time units (half hours) of overtime without BIM: " + averageOverTimeNoBIM + "\n");
            writer.write("average objective with ER: " + averageBIM + "\n");
            writer.write("average objective without ER: " + averageNoBIM + "\n");

            
            System.out.println("average objective with ER: " + averageBIM);
            System.out.println("average objective without ER: " + averageNoBIM);
            
            

            
       } catch (IOException e){
            System.err.println("An error occurred while writing to the file.");
            e.printStackTrace();
       } finally {
            try {
                if (writer != null){
                    writer.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    //simulating without emergency arrivals but can set the amount of surgeries and the maximum overlap
    public static void simulateWithNumberOfSurgeries(int N, double[] probabilities, double[] timeWeights, int maximumOverlap, String filenameToStore, int numberOfSurgeries){
        double totalObjectiveBIM =0;
        double totalObjectiveNoBIM = 0;
        BufferedWriter writer = null;
        int numberOfTimePeriods = 20;
        int maxOvertime = 16;
        int maxOverlap = maximumOverlap;
        int numberOfOR = 3;
        double epsilon = 1e-9;

        double averageTimeUnitsOfSurgery = 0;
        double averageOverTimeNoBIM = 0;
        double averageOverTimeWithBIM = 0;

        
       
        
        try {
            double maxDifference = 0; 
            int maxDifferenceInstance = 0;
            writer = new BufferedWriter(new FileWriter(filenameToStore));
            for(int n=0; n<N; n++){
                writer.write("Instance: " + (n+1) + "\n");
                writer.write("regular time:" + numberOfTimePeriods + " overtime:" + maxOvertime + " maxoverlap:" + maxOverlap+ "\n");

                
                
                //generating surgeries
                //THE FOLLOWING PART WILL MAKE THE DAY INSTANCE add a surgeriesToAddToINstance amount to the surgery list (to increase total amount of surgeries)
                //-----------------------------------------------------------
                // //first we generate 4 days of surgeries
                // int[] testDay1 = generateDayOfSurgeries(1); 
                // int[] testDay2 = generateDayOfSurgeries(2);
                // int[] testDay3 = generateDayOfSurgeries(3); 
                // int[] testDay4 = generateDayOfSurgeries(4);//


                // //this will store final list of surgeries to feed to the models
                // int[] dayOfSurgeries = new int[testDay1.length+surgeriesToAddToInstance];
                // int[] restOfSurgeries = new int[testDay2.length+testDay3.length+testDay4.length]; //the rest of the surgeries from which we will take surgeriesToAddToInstance amount

                // //setting the dayofsurgeries to be day1 at first
                // for(int i=0; i<testDay1.length; i++){
                //     dayOfSurgeries[i] = testDay1[i];
                // }

                // //then we add the surgeriesToAddToInstance other surgeries from restOfsurgeries
                // for(int j=0; j<surgeriesToAddToInstance; j++){
                //     dayOfSurgeries[testDay1.length+j] = restOfSurgeries[j];
                // }                
                //-----------------------------------------------------------


                //We use a day of surgeries as numberOfSurgeries random sampled surgeries
                //-----------------------------------------------------------
                int[] dayOfSurgeries = generateDayOfSurgeriesByNumber(numberOfSurgeries);





                //-----------------------------------------------------------


                int totalTestTimeUnits = 0;
                for(int i=0; i<dayOfSurgeries.length;i++){
                    totalTestTimeUnits+=dayOfSurgeries[i];
                }


                ScheduleInstance testInstance = new ScheduleInstance(timeWeights, numberOfOR, numberOfTimePeriods, maxOvertime, dayOfSurgeries, maxOverlap); //only testDay1

                CplexResult resultWithBIM = modelBIM(testInstance);
                CplexResult resultNoBIM = modelNoBIM(testInstance);

                double objectiveWithBIM = resultWithBIM.getObjectiveValue();
                double objectiveNoBIM = resultNoBIM.getObjectiveValue();
                double difference = Math.abs(objectiveWithBIM-objectiveNoBIM);

                if (difference > maxDifference) {
                    maxDifference = difference;
                    maxDifferenceInstance = (n+1);
                }
                totalObjectiveBIM+=objectiveWithBIM;
                totalObjectiveNoBIM+=objectiveNoBIM;
                
                
                System.out.println();
                writer.write("Surgeries to schedule: ");
                for(int j=0; j<dayOfSurgeries.length; j++){
                    System.out.println(dayOfSurgeries[j]);
                    if(!(j==dayOfSurgeries.length-1)){
                        writer.write(dayOfSurgeries[j] + ", ");
                    } else {
                        writer.write(dayOfSurgeries[j] + " (" + totalTestTimeUnits + " total time units)" + "\n");
                    } 
                    
                }
                System.out.println("Total time units in day: " + totalTestTimeUnits + " (" + 1.0*totalTestTimeUnits/2 + " hours)");
                System.out.println("Objective value with emergency robustness: " + objectiveWithBIM);
                System.out.println("Objective value without emergency robustness: " + objectiveNoBIM);

                //------------------------------------------------------------- the following is used to write to the file
                
                double[][][] xMatrixBIM = resultWithBIM.getXMatrix();
                double[][][] xMatrixNoBIM = resultNoBIM.getXMatrix();
                int[] timeVectorBIM = resultWithBIM.getTimeVector();
                int[] timeVectorNoBIM = resultNoBIM.getTimeVector();
                int numberofSurgeries = dayOfSurgeries.length;
                writer.write("With BIM: ("  + timeVectorBIM[0] + ", " + timeVectorBIM[1] + ", " + timeVectorBIM[2] + ")   (" + resultWithBIM.getObjectiveValue()+ ")" +"\n");
                
                Boolean flag = false;
                for(int j=0; j<numberOfOR; j++){
                    for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                        flag = false;
                        for(int i=0; i<numberofSurgeries; i++){                            
                            if(Math.abs(xMatrixBIM[i][j][t] - 1.0) < epsilon){
                                flag=true;
                                break;
                            } 
                        }
                        if (t == numberOfTimePeriods) { //for the space after regular time
                            writer.write("  ");
                        }
                        if (flag) {
                            writer.write("1 ");
                        } else {
                            writer.write("* ");
                        }
                    }
                    writer.write("\n");
                }

                writer.write("Without BIM: ("  + timeVectorNoBIM[0] + ", " + timeVectorNoBIM[1] + ", " + timeVectorNoBIM[2] + ")   (" + resultNoBIM.getObjectiveValue() + ")" +"\n");
                Boolean flag2 = false;
                for(int j=0; j<numberOfOR; j++){
                    for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                        flag2 = false;
                        for(int i=0; i<numberofSurgeries; i++){                            
                            if(Math.abs(xMatrixNoBIM[i][j][t] - 1.0) < epsilon){
                                flag2=true;
                                break;
                            } 
                        }
                        if (t == numberOfTimePeriods) { //for the space after regular time
                            writer.write("  ");
                        }
                        if (flag2) {
                            writer.write("1 ");
                        } else {
                            writer.write("* ");
                        }
                    }
                    writer.write("\n");
                }

                writer.write("\n");
                
                //for averages:
                averageTimeUnitsOfSurgery += (1.0/N)*totalTestTimeUnits;
                averageOverTimeNoBIM += (1.0/N)*timeVectorNoBIM[2];
                averageOverTimeWithBIM += (1.0/N)*timeVectorBIM[2];

            }   //end of iterating
            double averageBIM = totalObjectiveBIM/(1.0*N);
            double averageNoBIM = totalObjectiveNoBIM/(1.0*N);

            writer.write("Instance with the biggest difference: " + maxDifferenceInstance + " (" + maxDifference + ")" + "\n");
            writer.write("Average daily time units (half hours) of elective surgery: " + averageTimeUnitsOfSurgery + "\n");
            writer.write("Average time units (half hours) of overtime with BIM: " + averageOverTimeWithBIM + "\n");
            writer.write("Average time units (half hours) of overtime without BIM: " + averageOverTimeNoBIM + "\n");
            writer.write("average objective with ER: " + averageBIM + "\n");
            writer.write("average objective without ER: " + averageNoBIM + "\n");

            
            System.out.println("average objective with ER: " + averageBIM);
            System.out.println("average objective without ER: " + averageNoBIM);
            
            

            
       } catch (IOException e){
            System.err.println("An error occurred while writing to the file.");
            e.printStackTrace();
       } finally {
            try {
                if (writer != null){
                    writer.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }    
    }

       
    //method to create a list of surgeries to be scheduled on one day
    //takes as input the day of the week: 1=monday, 2=tuesday,..., 5=friday
    public static int[] generateDayOfSurgeries(int dayOfTheWeek){
        int[] dayOfSurgeries = null; //to return
        double[] probabilities = readProbabilities("durationProbabilities.txt"); //file of probabilities for each of the surgery durations
        int[] weekOfSurgeries = generateWeekOfSurgeries(probabilities); //generates a week of surgeries
        ArrayList<ArrayList<Integer>> weekOfSurgeriesLPT = applyLPTtoWeeklySchedule(weekOfSurgeries); //schedules the full week to all the days using LPT
        for(int day=1; day<=5; day++){ //to get the corresponding day
            if(day==dayOfTheWeek){
                dayOfSurgeries = new int[weekOfSurgeriesLPT.get(day-1).size()]; //set the size of the array of surgeries equal to the size of corresponding arraylist
                for(int j=0; j<dayOfSurgeries.length; j++){
                    dayOfSurgeries[j] = weekOfSurgeriesLPT.get(day-1).get(j); //copy the arraylist onto the array dayofsurgeries
                }
            }
        }       
        return dayOfSurgeries;
    }

    public static int[] generateDayOfSurgeriesByNumber(int desiredNumberOfSurgeries){
        int[] dayOfSurgeries = new int[desiredNumberOfSurgeries];
        double[] probabilities = readProbabilities("durationProbabilities.txt");

        for(int i=0; i<dayOfSurgeries.length; i++){
            dayOfSurgeries[i] = generateSurgeryDuration(probabilities);
        }
        return dayOfSurgeries;
    }

    // the MIP model with the BIM constraint (5)
    public static CplexResult modelBIM(ScheduleInstance instance){
        double[] timeWeights = instance.getTimeWeights();
        int[] surgeries = instance.getSurgeries();
        double idleTimeWeight = timeWeights[1];
        double regularTimeWeight = timeWeights[0];
        double overTimeWeight = timeWeights[2];
        int numberOfSurgeries = instance.getNumberOfSurgeries();
        int numberOfOR = instance.getNumberOfOR();
        int numberOfTimePeriods = instance.getNumberOfTimePeriods();
        int maxOvertime = instance.getMaxOvertime();
        int maxOverlap = instance.getMaxOverlap();

        double epsilon = 1e-9;
        double objectiveValue = Double.NaN;
        CplexResult result = new CplexResult();

        try{
            IloCplex cplex = new IloCplex();


            //VARIABLES
            // Xijt: 3 dimensional array of Xijt binary (1 and 0) with 1 if surgery i uses time slot t in OR j (n*m*T) (also constraint (7a))
            IloNumVar[][][] X = new IloNumVar[numberOfSurgeries][numberOfOR][numberOfTimePeriods+maxOvertime];
            for(int i=1; i<=numberOfSurgeries; i++){
                for(int j=1; j<=numberOfOR; j++){
                    for(int t=1; t<=numberOfTimePeriods+maxOvertime; t++){
                        X[i-1][j-1][t-1] = cplex.boolVar(); //sets them to be binary variables
                    }
                }
            }

            // Yijt: 3 dimensional array of Yijt binary (1 and 0) with 1 if surgery i starts at time slot t in OR j (n*m*T) (also constraint (7b))
            IloNumVar[][][] Y = new IloNumVar[numberOfSurgeries][numberOfOR][numberOfTimePeriods+maxOvertime];
            for(int i=1; i<=numberOfSurgeries; i++){
                for(int j=1; j<=numberOfOR; j++){
                    for(int t=1; t<=numberOfTimePeriods+maxOvertime; t++){
                        Y[i-1][j-1][t-1] = cplex.boolVar(); //sets them to be binary variables
                    }
                }
            }

            // Zt: Array of Zt integer decision variable representing the number of surgeries occupied at time slot t in m ORs. (also constraint (8))
            IloNumVar[] Z = new IloNumVar[numberOfTimePeriods+maxOvertime];
            for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                Z[t] = cplex.intVar(0, Integer.MAX_VALUE);
            }
            //-------------------------------------------------------



            //OBJECTIVE FUNCTION
            IloLinearNumExpr objective = cplex.linearNumExpr();
            for(int i=1; i<=numberOfSurgeries; i++){
                for(int j=1; j<=numberOfOR; j++){
                    for(int t=1; t<=numberOfTimePeriods+maxOvertime; t++){
                        if(t<=numberOfTimePeriods){
                            objective.addTerm((regularTimeWeight-idleTimeWeight), X[i-1][j-1][t-1]);
                        } else if(numberOfTimePeriods < t && t <= numberOfTimePeriods + maxOvertime){
                            objective.addTerm(overTimeWeight, X[i-1][j-1][t-1]);
                        }
                    }
                }
            }
            //-------------------------------------------------------

            //adding minimization objective
            cplex.addMinimize(objective);  
            //-------------------------------------------------------


            //CONSTRAINTS
            //Constraint (1)*** 15/05/2024
            for (int i = 0; i < numberOfSurgeries; i++) {
                IloLinearNumExpr expression1 = cplex.linearNumExpr();
                for (int t = 0; t < numberOfTimePeriods+maxOvertime; t++) {
                    for (int j = 0; j < numberOfOR; j++) {
                        expression1.addTerm(1.0, Y[i][j][t]); // adds 1* Yijt to the expression 1
                    }
                }
                cplex.addEq(1, expression1); //adds the constraint that expression 1 should equal 1
            }

            //Constraint (2) 
            for(int j=0; j<numberOfOR;j++){
                for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                    IloLinearNumExpr expression2 = cplex.linearNumExpr();
                    for(int i=0; i<numberOfSurgeries; i++){
                        expression2.addTerm(1.0, X[i][j][t]); // adds 1*Xijt to the expression 2
                    }
                    cplex.addLe(expression2, 1); //adds the constraint that expression 2 should <= 1
                }
            }

            //Constraint (3)
            for(int i=1; i<=numberOfSurgeries; i++){
                for(int j=1; j<=numberOfOR; j++){
                    for(int t=1; t<=numberOfTimePeriods+maxOvertime; t++){   
                        IloLinearNumExpr expression3 = cplex.linearNumExpr();
                        int min = Math.min(t+surgeries[i-1]-1,numberOfTimePeriods+maxOvertime);
                        for(int r=t; r<=min; r++){ //note that t+surgeries[i] doesnt have -1 since t is index adjusted (starting at 0 until T+L-1)
                            expression3.addTerm(1.0, X[i-1][j-1][r-1]);
                        }
                        IloLinearNumExpr RHS = cplex.linearNumExpr();
                        RHS.addTerm(surgeries[i-1], Y[i-1][j-1][t-1]);
                        cplex.addGe(expression3, RHS);
                    }
                }
            }



            //Constraint (4)
            for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                IloLinearNumExpr expression4 = cplex.linearNumExpr();
                for(int i=0; i<numberOfSurgeries; i++){
                    for(int j=0; j<numberOfOR; j++){
                        expression4.addTerm(1.0, X[i][j][t]);
                    }
                }
                cplex.addEq(expression4, Z[t]); //should i switch these around?
            }

            //Constraint (5) (BIM)
            for(int k=0; k<numberOfTimePeriods; k++){
                int RHS = ((maxOverlap +1)*numberOfOR) - 1;
                IloLinearNumExpr expression5 = cplex.linearNumExpr();
                for(int t=k; t<=k+maxOverlap; t++){
                    expression5.addTerm(1.0, Z[t]);
                }
                for(int i=0; i<numberOfSurgeries; i++){
                    for(int t=k+1; t<k+maxOverlap; t++){
                        for(int j=0; j<numberOfOR; j++){
                            expression5.addTerm(-1.0, Y[i][j][t]);
                        }
                    }
                }
                cplex.addLe(expression5, RHS);
            }        

            //Constraint (6)
            IloLinearNumExpr expression6 = cplex.linearNumExpr();
            for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                expression6.addTerm(1.0, Z[t]);
            }
            double totalSurgeries = 0;
            for(int i=0; i<numberOfSurgeries; i++){
                totalSurgeries+=surgeries[i];
            }
            cplex.addEq(expression6, totalSurgeries);
            //-------------------------------------------------------


            //SOLVE and OUTPUT
            if (cplex.solve()) {
                System.out.println("obj = "+cplex.getObjValue());

                //output solutions for X by surgery
                for(int i=1; i<=numberOfSurgeries; i++){
                    System.out.println();
                    System.out.println("Surgery " + i + " takes " + surgeries[i-1] + " time units");
                    for(int j=1; j<=numberOfOR; j++){
                        for(int t=1; t<=numberOfTimePeriods+maxOvertime; t++){
                            if(Math.abs(cplex.getValue(X[i-1][j-1][t-1]) - 1.0) < epsilon){
                                System.out.println("Surgery " + i + " at OR " + j + " at time t= " + t);
                            }
                        }
                    }
                }
                //-------------------------------------------------------

                
                //output solution for X by operating room
                for(int j=0; j<numberOfOR; j++){
                    System.out.println();
                    System.out.println("Operating room " + (j+1));
                    for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                        for(int i=0; i<numberOfSurgeries; i++){
                            if(Math.abs(cplex.getValue(X[i][j][t]) - 1.0) < epsilon){
                                System.out.println("Surgery " + (i+1) + " at t= " + (t+1));
                            }
                        }
                    }                   
                }
                //-------------------------------------------------------



                //output solution by time usage: regular time, idle time, overtime
                int regularTimeUsed = 0;
                int idleTimeUsed = 0;
                int overTimeUsed = 0;
                boolean idleBoolean = true;
                for(int j=0; j<numberOfOR; j++){ //iterate over operating rooms
                    System.out.println();
                    for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){ //iterate over time periods

                        //boolean that becomes false if OR j at time t has a surgery  that indicates idle time
                        idleBoolean = true;
                       
                        if(t<numberOfTimePeriods){ //during regular time
                            for(int i=0; i<numberOfSurgeries; i++){ //we check if there is a surgery planned in OR j at time t
                                if(Math.abs(cplex.getValue(X[i][j][t]) - 1.0) < epsilon){ //if there is a surgery planned: add +1 to regularTimeUsed and its not idle
                                    regularTimeUsed++;
                                    idleBoolean = false;
                                    break;
                                }
                            }
                            if(idleBoolean){idleTimeUsed++; System.out.println("OR " + (j+1) + " is idle at time " + (t+1));} //else if there is no surgery planned in OR j at time t, we add +1 to idleTimeUsed
                        }

                        else if(!(t<numberOfTimePeriods)){ //else if we are in overtime
                            for(int i=0; i<numberOfSurgeries; i++){ //we check if there is a surgery planned in OR j at time t (overtime)
                                if(Math.abs(cplex.getValue(X[i][j][t]) - 1.0) < epsilon){ //if there is a surgery planned: add +1 to overTimeUsed and do not keep track of idle (idle time during overtime does not count)
                                    overTimeUsed++;
                                    break;
                                }
                            }
                        }
                       
                    }
                }
                

                //checking if all surgeries have been correctly scheduled
                for(int i=1; i<=numberOfSurgeries; i++){
                    int surgeryLength = surgeries[i-1];
                    int scheduled = 0;
                    for(int j=1; j<=numberOfOR; j++){
                        for(int t=1; t<=numberOfTimePeriods+maxOvertime; t++){
                            if(Math.abs(cplex.getValue(X[i-1][j-1][t-1]) - 1.0) < epsilon){
                                scheduled++;
                            }
                        }
                    }
                    if(surgeryLength == scheduled){
                        System.out.println("Surgery " + i + " scheduled succesfully");
                    } else{
                        System.out.println("Surgery " + i + " incorrectly scheduled");
                    }
                }

                System.out.println("\n" + "regular time: " + regularTimeUsed + " units");
                System.out.println("idle time: " + idleTimeUsed + " units");
                System.out.println("over time: " + overTimeUsed + " units");
                System.out.println("obj: " + cplex.getObjValue());
                objectiveValue = cplex.getObjValue();
                double[][][] xMatrix = new double[numberOfSurgeries][numberOfOR][numberOfTimePeriods+maxOvertime];
                double[][][] yMatrix = new double[numberOfSurgeries][numberOfOR][numberOfTimePeriods+maxOvertime];
                double[] zArray = new double[numberOfTimePeriods+maxOvertime];
                int[] timeVector = {regularTimeUsed, idleTimeUsed, overTimeUsed};

                for(int i=0; i<numberOfSurgeries; i++){
                    for(int j=0; j<numberOfOR; j++){
                        for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                            xMatrix[i][j][t] = cplex.getValue(X[i][j][t]);
                            yMatrix[i][j][t] = cplex.getValue(Y[i][j][t]);
                        }
                    }
                 }
                for(int t=0; t<numberOfTimePeriods+maxOvertime;t++){
                    zArray[t] = cplex.getValue(Z[t]);
                }

                result = new CplexResult(xMatrix, yMatrix, zArray, objectiveValue,timeVector); 

                cplex.end();                
            } //end of if(solve)
            //-------------------------------------------------------

            else {
                System.out.println("Model not solved");
                return null;
            }

        } //end of try 

        //catching exception
        catch(IloException exc){
            exc.printStackTrace();
        }
        result.setInstance(instance);
        return result;
    } //end of modelBIM(...) method

    // the MIP model without the BIM constraint 
    public static CplexResult modelNoBIM(ScheduleInstance instance){
        double[] timeWeights = instance.getTimeWeights();
        int[] surgeries = instance.getSurgeries();
        double idleTimeWeight = timeWeights[1];
        double regularTimeWeight = timeWeights[0];
        double overTimeWeight = timeWeights[2];
        int numberOfSurgeries = instance.getNumberOfSurgeries();
        int numberOfOR = instance.getNumberOfOR();
        int numberOfTimePeriods = instance.getNumberOfTimePeriods();
        int maxOvertime = instance.getMaxOvertime();
        int maxOverlap = instance.getMaxOverlap();

        double epsilon = 1e-9;
        double objectiveValue = Double.NaN;
        CplexResult result = new CplexResult();
        try{
            IloCplex cplex = new IloCplex();


            //VARIABLES
            // Xijt: 3 dimensional array of Xijt binary (1 and 0) with 1 if surgery i uses time slot t in OR j (n*m*T) (also constraint (7a))
            IloNumVar[][][] X = new IloNumVar[numberOfSurgeries][numberOfOR][numberOfTimePeriods+maxOvertime];
            for(int i=1; i<=numberOfSurgeries; i++){
                for(int j=1; j<=numberOfOR; j++){
                    for(int t=1; t<=numberOfTimePeriods+maxOvertime; t++){
                        X[i-1][j-1][t-1] = cplex.boolVar(); //sets them to be binary variables
                    }
                }
            }

            // Yijt: 3 dimensional array of Yijt binary (1 and 0) with 1 if surgery i starts at time slot t in OR j (n*m*T) (also constraint (7b))
            IloNumVar[][][] Y = new IloNumVar[numberOfSurgeries][numberOfOR][numberOfTimePeriods+maxOvertime];
            for(int i=1; i<=numberOfSurgeries; i++){
                for(int j=1; j<=numberOfOR; j++){
                    for(int t=1; t<=numberOfTimePeriods+maxOvertime; t++){
                        Y[i-1][j-1][t-1] = cplex.boolVar(); //sets them to be binary variables
                    }
                }
            }

            // Zt: Array of Zt integer decision variable representing the number of surgeries occupied at time slot t in m ORs. (also constraint (8))
            IloNumVar[] Z = new IloNumVar[numberOfTimePeriods+maxOvertime];
            for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                Z[t] = cplex.intVar(0, Integer.MAX_VALUE);
            }
            //-------------------------------------------------------

            //OBJECTIVE FUNCTION
            IloLinearNumExpr objective = cplex.linearNumExpr();
            for(int i=1; i<=numberOfSurgeries; i++){
                for(int j=1; j<=numberOfOR; j++){
                    for(int t=1; t<=numberOfTimePeriods+maxOvertime; t++){
                        if(t<=numberOfTimePeriods){
                            objective.addTerm((regularTimeWeight-idleTimeWeight), X[i-1][j-1][t-1]);
                        } else if(numberOfTimePeriods < t && t <= numberOfTimePeriods + maxOvertime){
                            objective.addTerm(overTimeWeight, X[i-1][j-1][t-1]);
                        }
                    }
                }
            }
            //-------------------------------------------------------

            //adding minimization objective
            cplex.addMinimize(objective);  
            //-------------------------------------------------------

            //CONSTRAINTS
            //Constraint (1)*** 15/05/2024
            for (int i = 0; i < numberOfSurgeries; i++) {
                IloLinearNumExpr expression1 = cplex.linearNumExpr();
                for (int t = 0; t < numberOfTimePeriods+maxOvertime; t++) {
                    for (int j = 0; j < numberOfOR; j++) {
                        expression1.addTerm(1.0, Y[i][j][t]); // adds 1* Yijt to the expression 1
                    }
                }
                cplex.addEq(1, expression1); //adds the constraint that expression 1 should equal 1
            }

            //Constraint (2) 
            for(int j=0; j<numberOfOR;j++){
                for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                    IloLinearNumExpr expression2 = cplex.linearNumExpr();
                    for(int i=0; i<numberOfSurgeries; i++){
                        expression2.addTerm(1.0, X[i][j][t]); // adds 1*Xijt to the expression 2
                    }
                    cplex.addLe(expression2, 1); //adds the constraint that expression 2 should <= 1
                }
            }

            //Constraint (3)
            for(int i=1; i<=numberOfSurgeries; i++){
                for(int j=1; j<=numberOfOR; j++){
                    for(int t=1; t<=numberOfTimePeriods+maxOvertime; t++){   
                        IloLinearNumExpr expression3 = cplex.linearNumExpr();
                        int min = Math.min(t+surgeries[i-1]-1,numberOfTimePeriods+maxOvertime);
                        for(int r=t; r<=min; r++){ //note that t+surgeries[i] doesnt have -1 since t is index adjusted (starting at 0 until T+L-1)
                            expression3.addTerm(1.0, X[i-1][j-1][r-1]);
                        }
                        IloLinearNumExpr RHS = cplex.linearNumExpr();
                        RHS.addTerm(surgeries[i-1], Y[i-1][j-1][t-1]);
                        cplex.addGe(expression3, RHS);
                    }
                }
            }

            //Constraint (4)
            for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                IloLinearNumExpr expression4 = cplex.linearNumExpr();
                for(int i=0; i<numberOfSurgeries; i++){
                    for(int j=0; j<numberOfOR; j++){
                        expression4.addTerm(1.0, X[i][j][t]);
                    }
                }
                cplex.addEq(expression4, Z[t]); //should i switch these around?
            }

            //Constraint (5) (BIM)           
            // for(int k=0; k<numberOfTimePeriods; k++){
            //     int RHS = ((maxOverlap +1)*numberOfOR) - 1;
            //     IloLinearNumExpr expression5 = cplex.linearNumExpr();
            //     for(int t=k; t<=k+maxOverlap; t++){
            //         expression5.addTerm(1.0, Z[t]);
            //     }
            //     for(int i=0; i<numberOfSurgeries; i++){
            //         for(int t=k+1; t<k+maxOverlap; t++){
            //             for(int j=0; j<numberOfOR; j++){
            //                 expression5.addTerm(-1.0, Y[i][j][t]);
            //             }
            //         }
            //     }
            //     cplex.addLe(expression5, RHS);
            // }        

            //Constraint (6)
            IloLinearNumExpr expression6 = cplex.linearNumExpr();
            for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                expression6.addTerm(1.0, Z[t]);
            }
            double totalSurgeries = 0;
            for(int i=0; i<numberOfSurgeries; i++){
                totalSurgeries+=surgeries[i];
            }
            cplex.addEq(expression6, totalSurgeries);
            //-------------------------------------------------------

            //SOLVE and OUTPUT
            if (cplex.solve()) {
                System.out.println("obj = "+cplex.getObjValue());

                //output solutions for X by surgery
                for(int i=1; i<=numberOfSurgeries; i++){
                    System.out.println();
                    System.out.println("Surgery " + i + " takes " + surgeries[i-1] + " time units");
                    for(int j=1; j<=numberOfOR; j++){
                        for(int t=1; t<=numberOfTimePeriods+maxOvertime; t++){
                            if(Math.abs(cplex.getValue(X[i-1][j-1][t-1]) - 1.0) < epsilon){
                                System.out.println("Surgery " + i + " at OR " + j + " at time t= " + t);
                            }
                        }
                    }
                }
                //-------------------------------------------------------

                
                //output solution for X by operating room
                for(int j=0; j<numberOfOR; j++){
                    System.out.println();
                    System.out.println("Operating room " + (j+1));
                    for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                        for(int i=0; i<numberOfSurgeries; i++){
                            if(Math.abs(cplex.getValue(X[i][j][t]) - 1.0) < epsilon){
                                System.out.println("Surgery " + (i+1) + " at t= " + (t+1));
                            }
                        }
                    }                   
                }
                //-------------------------------------------------------



                //output solution by time usage: regular time, idle time, overtime
                int regularTimeUsed = 0;
                int idleTimeUsed = 0;
                int overTimeUsed = 0;
                boolean idleBoolean = true;
                for(int j=0; j<numberOfOR; j++){ //iterate over operating rooms
                    System.out.println();
                    for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){ //iterate over time periods

                        //boolean that becomes false if OR j at time t has a surgery  that indicates idle time
                        idleBoolean = true;
                       
                        if(t<numberOfTimePeriods){ //during regular time
                            for(int i=0; i<numberOfSurgeries; i++){ //we check if there is a surgery planned in OR j at time t
                                if(Math.abs(cplex.getValue(X[i][j][t]) - 1.0) < epsilon){ //if there is a surgery planned: add +1 to regularTimeUsed and its not idle
                                    regularTimeUsed++;
                                    idleBoolean = false;
                                    break;
                                }
                            }
                            if(idleBoolean){idleTimeUsed++; System.out.println("OR " + (j+1) + " is idle at time " + (t+1));} //else if there is no surgery planned in OR j at time t, we add +1 to idleTimeUsed
                        }

                        else if(!(t<numberOfTimePeriods)){ //else if we are in overtime
                            for(int i=0; i<numberOfSurgeries; i++){ //we check if there is a surgery planned in OR j at time t (overtime)
                                if(Math.abs(cplex.getValue(X[i][j][t]) - 1.0) < epsilon){ //if there is a surgery planned: add +1 to overTimeUsed and do not keep track of idle (idle time during overtime does not count)
                                    overTimeUsed++;
                                    break;
                                }
                            }
                        }
                       
                    }
                }
                

                //checking if all surgeries have been correctly scheduled
                for(int i=1; i<=numberOfSurgeries; i++){
                    int surgeryLength = surgeries[i-1];
                    int scheduled = 0;
                    for(int j=1; j<=numberOfOR; j++){
                        for(int t=1; t<=numberOfTimePeriods+maxOvertime; t++){
                            if(Math.abs(cplex.getValue(X[i-1][j-1][t-1]) - 1.0) < epsilon){
                                scheduled++;
                            }
                        }
                    }
                    if(surgeryLength == scheduled){
                        System.out.println("Surgery " + i + " scheduled succesfully");
                    } else{
                        System.out.println("Surgery " + i + " incorrectly scheduled");
                    }
                }

                System.out.println("\n" + "regular time: " + regularTimeUsed + " units");
                System.out.println("idle time: " + idleTimeUsed + " units");
                System.out.println("over time: " + overTimeUsed + " units");
                System.out.println("obj: " + cplex.getObjValue());
                objectiveValue = cplex.getObjValue(); 
                double[][][] xMatrix = new double[numberOfSurgeries][numberOfOR][numberOfTimePeriods+maxOvertime];
                double[][][] yMatrix = new double[numberOfSurgeries][numberOfOR][numberOfTimePeriods+maxOvertime];
                double[] zArray = new double[numberOfTimePeriods+maxOvertime];
                int[] timeVector = {regularTimeUsed, idleTimeUsed, overTimeUsed};

                for(int i=0; i<numberOfSurgeries; i++){
                    for(int j=0; j<numberOfOR; j++){
                        for(int t=0; t<numberOfTimePeriods+maxOvertime; t++){
                            xMatrix[i][j][t] = cplex.getValue(X[i][j][t]);
                            yMatrix[i][j][t] = cplex.getValue(Y[i][j][t]);
                        }
                    }
                 }
                for(int t=0; t<numberOfTimePeriods+maxOvertime;t++){
                    zArray[t] = cplex.getValue(Z[t]);
                }

                result = new CplexResult(xMatrix, yMatrix, zArray, objectiveValue, timeVector); 
                cplex.end();

            } //end of if(solve)
            //-------------------------------------------------------

            //------------------------------------------------------- creating the cplex result class object
            
            else {
                System.out.println("Model not solved");
                return null;
            }

        } //end of try  

        //catching exception
        catch(IloException exc){
            exc.printStackTrace();
        }
        return result;
    } //end of modelNoBIM(...) method

    //method to read 32 surgery durations (1,1.5,...,15.5,16) and their proportional frequency (and thus probability)
    public static double[] readProbabilities(String filename) {
        double[] durationProbabilities = new double[32];
        try {
            File file = new File(filename);
            Scanner scanner = new Scanner(file);
            for (int i = 0; i < 32; i++) {
                String string = scanner.nextLine();
                double nextProb = Double.parseDouble(string);
                durationProbabilities[i] = nextProb;
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }       
        return durationProbabilities;
    }

    //using a distribution for the durations, sample a random surgery duration (and thus random surgery)
    public static int generateSurgeryDuration(double[] probabilities) {
        // Create the cumulative distribution array
        double[] cumulativeProbabilities = new double[probabilities.length];
        cumulativeProbabilities[0] = probabilities[0];
        for (int i = 1; i < probabilities.length; i++) {
            cumulativeProbabilities[i] = cumulativeProbabilities[i-1] + probabilities[i];
        }        
        // Generate a random number between 0 and 1
        Random random = new Random();
        double randomValue = random.nextDouble();
        //System.out.println("Sampled double value: "  + randomValue);        
        int duration = 0; 
        // Find the entry corresponding to the random value
        for(int i=1; i<probabilities.length; i++){
            if(randomValue > cumulativeProbabilities[i-1] && randomValue < cumulativeProbabilities[i]){
                // System.out.println("Duration: " + (i+1) + " time units");
                duration = i+1;
            }
        }
        return duration;
    }

    //sampling n, the number of surgeries in 1 week from a normal distribution
    //TESTED AND WORKS
    public static int generateNumberOfWeeklySurgeries(double mean, double stdDev){
        Random random = new Random();
        double standardNormalValue = random.nextGaussian();
        double sample = standardNormalValue * stdDev + mean;
        return (int) Math.round(sample);
    }

    //generating the week of surgeries by drawing n (sampled from generateNumberOfWeeklySurgeries) surgeries using generateSurgeryDuration
    //TESTED AND WORKS
    public static int[] generateWeekOfSurgeries(double[] probabilities){
        double mean = 26.52777778;
        double stdDev = 3.676070193;
        int[] surgeries;        
        while(true){        
            int totalWeeklyTimeUnits = 0;
            int numberOfWeeklySurgeries = generateNumberOfWeeklySurgeries(mean, stdDev);            
            surgeries = new int[numberOfWeeklySurgeries];
            for(int i=0; i<numberOfWeeklySurgeries; i++){
                surgeries[i] = generateSurgeryDuration(probabilities);
                totalWeeklyTimeUnits+=surgeries[i];
            }

            if(totalWeeklyTimeUnits >= 120 && totalWeeklyTimeUnits<=230){ //90 and 240 chosen to be -+3 standard deviations from the distribution in data (normal), MIGHT CHANGE
                break;
            }
        }
        return surgeries;
    }

    //returns an arraylist of arraylists, where each seperate arraylist is a day of surgeries
    public static ArrayList<ArrayList<Integer>> applyLPTtoWeeklySchedule(int[] weekOfSurgeriesInstance){
        ArrayList<ArrayList<Integer>> dailySchedules = new ArrayList<ArrayList<Integer>>(); //an array of arraylists that will add the surgeries to the correct days
        ArrayList<Integer> a1 = new ArrayList<>();
        ArrayList<Integer> a2 = new ArrayList<>();
        ArrayList<Integer> a3 = new ArrayList<>();
        ArrayList<Integer> a4 = new ArrayList<>();
        ArrayList<Integer> a5 = new ArrayList<>();



        int[] dailyLoad = new int[5]; //array of integers that will keep track of total hours scheduled per day

        ArrayList<Integer> allWeeklySurgeries = new ArrayList<>();
        for(int i=0; i<weekOfSurgeriesInstance.length;i++){
            allWeeklySurgeries.add(weekOfSurgeriesInstance[i]);
        }
        allWeeklySurgeries.sort(Comparator.reverseOrder());
        
        while(!allWeeklySurgeries.isEmpty()){
            //get the day with least load
            //place the next surgery again
            int minLoad = dailyLoad[0];
            int minLoadIndex = 0;
            for(int i=1; i<5; i++){
                if(dailyLoad[i] < minLoad){
                    minLoad = dailyLoad[i];
                    minLoadIndex = i;
                }
            }

            
            if(minLoadIndex == 0){
                a1.add(allWeeklySurgeries.get(0));
                dailyLoad[0]+= allWeeklySurgeries.get(0);
            } 

            if(minLoadIndex == 1){
                a2.add(allWeeklySurgeries.get(0));
                dailyLoad[1]+= allWeeklySurgeries.get(0);
            } 

            if(minLoadIndex == 2){
                a3.add(allWeeklySurgeries.get(0));
                dailyLoad[2]+= allWeeklySurgeries.get(0);
            } 

            if(minLoadIndex == 3){
                a4.add(allWeeklySurgeries.get(0));
                dailyLoad[3]+= allWeeklySurgeries.get(0);
            } 

            if(minLoadIndex == 4){
                a5.add(allWeeklySurgeries.get(0));
                dailyLoad[4]+= allWeeklySurgeries.get(0);
            } 

            allWeeklySurgeries.remove(0);
            
        }

        dailySchedules.add(a1);
        dailySchedules.add(a2);
        dailySchedules.add(a3);
        dailySchedules.add(a4);
        dailySchedules.add(a5);
        return dailySchedules;
    }





} //end of class
