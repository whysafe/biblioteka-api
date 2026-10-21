package ru.whysafe.biblioteka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/** Правила выдачи. Значения по умолчанию совпадают с application.yml. */
@ConfigurationProperties(prefix = "app.library")
public class LibraryProperties {

    private int maxActiveLoans = 5;
    private int loanDays = 14;
    private int renewalDays = 14;
    private int maxRenewals = 2;
    private BigDecimal finePerDay = new BigDecimal("10.00");
    private BigDecimal maxFine = new BigDecimal("500.00");
    private int holdDays = 3;
    private String expireHoldsCron = "0 5 0 * * *";

    public int getMaxActiveLoans() {
        return maxActiveLoans;
    }

    public void setMaxActiveLoans(int maxActiveLoans) {
        this.maxActiveLoans = maxActiveLoans;
    }

    public int getLoanDays() {
        return loanDays;
    }

    public void setLoanDays(int loanDays) {
        this.loanDays = loanDays;
    }

    public int getRenewalDays() {
        return renewalDays;
    }

    public void setRenewalDays(int renewalDays) {
        this.renewalDays = renewalDays;
    }

    public int getMaxRenewals() {
        return maxRenewals;
    }

    public void setMaxRenewals(int maxRenewals) {
        this.maxRenewals = maxRenewals;
    }

    public BigDecimal getFinePerDay() {
        return finePerDay;
    }

    public void setFinePerDay(BigDecimal finePerDay) {
        this.finePerDay = finePerDay;
    }

    public BigDecimal getMaxFine() {
        return maxFine;
    }

    public void setMaxFine(BigDecimal maxFine) {
        this.maxFine = maxFine;
    }

    public int getHoldDays() {
        return holdDays;
    }

    public void setHoldDays(int holdDays) {
        this.holdDays = holdDays;
    }

    public String getExpireHoldsCron() {
        return expireHoldsCron;
    }

    public void setExpireHoldsCron(String expireHoldsCron) {
        this.expireHoldsCron = expireHoldsCron;
    }
}
