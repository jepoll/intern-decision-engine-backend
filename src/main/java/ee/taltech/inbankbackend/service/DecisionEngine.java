package ee.taltech.inbankbackend.service;

import com.github.vladislavgoltjajev.personalcode.locale.estonia.EstonianPersonalCodeValidator;
import ee.taltech.inbankbackend.config.BalticCountriesAgeConstants;
import ee.taltech.inbankbackend.config.DecisionEngineConstants;
import ee.taltech.inbankbackend.config.ECustomerSegment;
import ee.taltech.inbankbackend.config.IAgeRestriction;
import ee.taltech.inbankbackend.exceptions.*;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import java.time.*;

/**
 * A service class that provides a method for calculating an approved loan amount and period for a customer.
 * The loan amount is calculated based on the customer's credit modifier,
 * which is determined by the last four digits of their ID code.
 */
@Service
public class DecisionEngine {

    // Used to check for the validity of the presented ID code.
    private final EstonianPersonalCodeValidator validator = new EstonianPersonalCodeValidator();
    private int creditModifier = 0;

    private IAgeRestriction ageRestriction = new BalticCountriesAgeConstants();

    /**
     * Calculates the maximum loan amount and period for the customer based on their ID code,
     * the requested loan amount and the loan period.
     * The loan period must be between 12 and 60 months (inclusive).
     * The loan amount must be between 2000 and 10000€ months (inclusive).
     *
     * @param personalCode ID code of the customer that made the request.
     * @param loanAmount   Requested loan amount
     * @param loanPeriod   Requested loan period
     * @return A Decision object containing the approved loan amount and period, and an error message (if any)
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException   If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException   If the requested loan period is invalid
     * @throws NoValidLoanException         If there is no valid loan found for the given ID code, loan amount and loan period
     */
    public Decision calculateApprovedLoan(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException,
            NoValidLoanException, AgeRestrictionException {
        try {
            verifyInputs(personalCode, loanAmount, loanPeriod);
            verifyAge(personalCode, loanPeriod);
        } catch (Exception e) {
            return new Decision(null, null, e.getMessage());
        }

        int outputLoanAmount;
        creditModifier = getCreditModifier(personalCode);

        if (creditModifier == 0) {
            throw new NoValidLoanException("No valid loan found!");
        }

        while (highestValidLoanAmount(loanPeriod) < DecisionEngineConstants.getMinimumLoanAmount()) {
            loanPeriod++;
        }
        double creditScore = getCreditScore(loanAmount, loanPeriod);
        String errorMessage = null;
        if(creditScore < 1)
            errorMessage = "Load amount with this period is not approved";


        if (loanPeriod <= DecisionEngineConstants.getMaximumLoanPeriod()) {
            outputLoanAmount = Math.min(DecisionEngineConstants.getMaximumLoanAmount(), highestValidLoanAmount(loanPeriod));
        } else {
            throw new NoValidLoanException("Loan period is not valid");
        }


        if(outputLoanAmount < DecisionEngineConstants.getMinimumLoanAmount()){
            loanPeriod = highestValidLoanPeriod(loanAmount).intValue();
            outputLoanAmount = loanAmount.intValue();
        }

        outputLoanAmount = Math.min(DecisionEngineConstants.getMaximumLoanAmount(), highestValidLoanAmount(loanPeriod));
        loanPeriod = Math.max(DecisionEngineConstants.getMinimumLoanPeriod(), Math.min(DecisionEngineConstants.getMaximumLoanPeriod(), loanPeriod));

        return new Decision(outputLoanAmount, loanPeriod, errorMessage);
    }

    private void verifyAge(String personalCode, int loanPeriod) throws AgeRestrictionException{
        int age = getCustomerAge(personalCode);
        if(age < ageRestriction.getMinimumAge()){
            throw new AgeRestrictionException("You must be above the age of " + age);
        }else if(age * 12 > ageRestriction.getLifeExpectancy() * 12 - loanPeriod){
            throw new AgeRestrictionException("You are above the age of " + (ageRestriction.getLifeExpectancy() - loanPeriod / 12));
        }
    }

    private int getCustomerAge(String personalCode) {
        String year = "";
        if(personalCode == null){
            return 0;
        }
        if(Integer.valueOf(personalCode.substring(0, 1)) <= 2){
            year = "18";
        }
        else if(Integer.valueOf(personalCode.substring(0, 1)) <= 4){
            year = "19";
        }
        else if(Integer.valueOf(personalCode.substring(0, 1)) <= 6){
            year = "20";
        }
        else if(Integer.valueOf(personalCode.substring(0, 1)) <= 8){
            year = "21";
        }
        year += personalCode.substring(1, 3);
        int month = Integer.parseInt(personalCode.substring(3, 5));
        int day = Integer.parseInt(personalCode.substring(5, 7));

        LocalDate birthDate = LocalDate.of(Integer.valueOf(year), month, day);
        LocalDate currentDate = LocalDate.now();


        return Period.between(birthDate, currentDate).getYears();
    }

    private double getCreditScore(Long loanAmount, int loanPeriod){
        return ((double) creditModifier / loanAmount) * loanPeriod;
    }

    private Long highestValidLoanPeriod(Long loanAmount){
        return creditModifier / loanAmount;
    }

    /**
     * Calculates the largest valid loan for the current credit modifier and loan period.
     *
     * @return Largest valid loan amount
     */
    private int highestValidLoanAmount(int loanPeriod) {
        return creditModifier * loanPeriod;
    }

    /**
     * Calculates the credit modifier of the customer to according to the last four digits of their ID code.
     * Debt - 0000...2499
     * Segment 1 - 2500...4999
     * Segment 2 - 5000...7499
     * Segment 3 - 7500...9999
     *
     * @param personalCode ID code of the customer that made the request.
     * @return Segment to which the customer belongs.
     */
    private int getCreditModifier(String personalCode) {
        int segment = Integer.parseInt(personalCode.substring(personalCode.length() - 4));
        ECustomerSegment customerSegment = determineCustomerSegment(segment);

        return customerSegment.getCreditModifier();
    }

    /**
     * Finds the customer segment based on their last four digits of ID code.
     * Debt - 0000...2499
     * Segment 1 - 2500...4999
     * Segment 2 - 5000...7499
     * Segment 3 - 7500...9999
     *
     * @param segment last four digits of personal code.
     * @return Segment to which the customer belongs.
     */
    private ECustomerSegment determineCustomerSegment(int segment) {
        ECustomerSegment customerSegment;
        if (segment < 2500) {
            customerSegment = ECustomerSegment.DEBT;
        } else if (segment < 5000) {
            customerSegment = ECustomerSegment.SEGMENT_1;
        } else if (segment < 7500) {
            customerSegment = ECustomerSegment.SEGMENT_2;
        }else{
            customerSegment = ECustomerSegment.SEGMENT_3;
        }

        return customerSegment;
    }

    /**
     * Verify that all inputs are valid according to business rules.
     * If inputs are invalid, then throws corresponding exceptions.
     *
     * @param personalCode Provided personal ID code
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException If the requested loan period is invalid
     */
    private void verifyInputs(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException {

        if (!validator.isValid(personalCode)) {
            throw new InvalidPersonalCodeException("Invalid personal ID code!");
        }
        if (!(DecisionEngineConstants.getMinimumLoanAmount() <= loanAmount)
                || !(loanAmount <= DecisionEngineConstants.getMaximumLoanAmount())) {
            throw new InvalidLoanAmountException("Invalid loan amount!");
        }
        if (!(DecisionEngineConstants.getMinimumLoanPeriod() <= loanPeriod)
                || !(loanPeriod <= DecisionEngineConstants.getMaximumLoanPeriod())) {
            throw new InvalidLoanPeriodException("Invalid loan period!");
        }

    }
}
