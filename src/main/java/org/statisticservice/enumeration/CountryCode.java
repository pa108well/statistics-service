package org.statisticservice.enumeration;

public enum CountryCode {
    RU("Russia"),
    US("United States"),
    CY("Cyprus");

    private final String countryName;

    CountryCode(String countryName) {
        this.countryName = countryName;
    }

    public String getCountryName() {
        return countryName;
    }

}
