package ee.taltech.inbankbackend.config;

import java.util.Random;

/**
 * Holds all necessary constants for the decision engine.
 */
public class DecisionEngineConstants{
    private static final Integer MINIMUM_LOAN_AMOUNT = 2000;
    private static final Integer MAXIMUM_LOAN_AMOUNT = 10000;
    private static final Integer MAXIMUM_LOAN_PERIOD = 60;
    private static final Integer MINIMUM_LOAN_PERIOD = 12;
    private static final Integer MINIMUM_CUSTOMER_AGE = 12;


    public static Integer getMinimumLoanAmount() {
        return MINIMUM_LOAN_AMOUNT;
    }


    public static  Integer getMaximumLoanAmount() {
        return MAXIMUM_LOAN_AMOUNT;
    }


    public static Integer getMaximumLoanPeriod() {
        return MAXIMUM_LOAN_PERIOD;
    }


    public static Integer getMinimumLoanPeriod() {
        return MINIMUM_LOAN_PERIOD;
    }

    public static Integer getMinimumCustomerAge() {
        return MINIMUM_CUSTOMER_AGE;
    }

    public static Integer getCustomerLifeExpectancy(){
        Random rand = new Random();
        return rand.nextInt(70, 80);
    }
}
