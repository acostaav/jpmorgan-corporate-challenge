package com.everis.jpmorgancc.schema;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

/**
 * An JPMorganState schema.
 */
public class JPMorganSchemaV1 extends MappedSchema {
    public JPMorganSchemaV1() {
        super(JPMorganSchema.class, 1, ImmutableList.of(PersistentJPM.class));
    }

    @Entity
    @Table(name = "jpm_states")
    public static class PersistentJPM extends PersistentState {
        @Column(name = "lender") private final String lender;
        @Column(name = "borrower") private final String borrower;
        @Column(name = "challengeName") private final String challengeName;
        @Column(name = "challengeYear") private final int challengeYear;
        @Column(name = "placeCity") private final int placeCity;
        @Column(name = "placeGender") private final int placeGender;
        @Column(name = "bibNumber") private final int bibNumber;
        @Column(name = "firstName") private final String firstName;
        @Column(name = "lastName") private final String lastName;
        @Column(name = "time") private final double time;
        @Column(name = "gender") private final String gender;
        @Column(name = "linear_id") private final UUID linearId;

        public PersistentJPM(String lender, String borrower, String challengeName, int challengeYear, int placeCity, int placeGender, int bibNumber, String firstName, String lastName, Double time, String gender, UUID linearId) {
            this.lender = lender;
            this.borrower = borrower;
            this.challengeName = challengeName;
            this.challengeYear = challengeYear;
            this.placeCity = placeCity;
            this.placeGender = placeGender;
            this.bibNumber = bibNumber;
            this.firstName = firstName;
            this.lastName = lastName;
            this.time = time;
            this.gender = gender;
            this.linearId = linearId;
        }

        // Default constructor required by hibernate.
        public PersistentJPM() {
            this.lender = null;
            this.borrower = null;
            this.challengeName = null;
            this.challengeYear = 0;
            this.placeCity = 0;
            this.placeGender = 0;
            this.bibNumber = 0;
            this.firstName = null;
            this.lastName = null;
            this.time = 0.0;
            this.gender = null;
            this.linearId = null;
        }

        public String getLender() { return lender; }

        public String getBorrower() { return borrower; }

        public String getChallengeName() { return challengeName; }

        public int getChallengeYear() { return challengeYear; }

        public int getPlaceCity() { return placeCity; }

        public int getPlaceGender() { return placeGender; }

        public int getBibNumber() { return bibNumber; }

        public String getFirstName() { return firstName; }

        public String getLastName() { return lastName; }

        public double getTime() { return time; }

        public String getGender() { return gender; }

        public UUID getId() { return linearId; }
    }
}