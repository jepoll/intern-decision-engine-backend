package ee.taltech.inbankbackend.config;


public class BalticCountriesAgeConstants implements IAgeRestriction{
    @Override
    public int getMinimumAge() {
        return 18;
    }

    @Override
    public int getLifeExpectancy() {
        return 75;
    }
}
