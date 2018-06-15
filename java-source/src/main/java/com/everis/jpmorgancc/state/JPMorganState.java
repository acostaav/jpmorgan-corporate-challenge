package com.everis.jpmorgancc.state;

import com.everis.jpmorgancc.schema.JPMorganSchemaV1;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;

import java.util.Arrays;
import java.util.List;

/**
 * The state object recording JPMorgan agreements between two parties.
 *
 * A state must implement [ContractState] or one of its descendants.
 */
public class JPMorganState implements LinearState, QueryableState {
    private final String challengeName;
    private final Integer challengeYear;
    private final Integer placeCity;
    private final Integer placeGender;
    private final Integer bibNumber;
    private final String firstName;
    private final String lastName;
    private final Double time;
    private final String gender;
    private final Party lender;
    private final Party borrower;
    private final UniqueIdentifier linearId;

    /**
     * @param challengeName the value of the Challenge.
     * @param challengeYear the year of the Challenge.
     * @param placeCity the position of the user in the Challenge.
     * @param placeGender the position of the user in the Challenge considering the gender.
     * @param bibNumber the bib number of the user in the Challenge.
     * @param firstName the first name of the user.
     * @param lastName the last name of the user.
     * @param time the time result in the challenge.
     * @param gender the gender of the user.
     * @param lender the party issuing the JPM.
     * @param borrower the party receiving and approving the JPM.
     */
    public JPMorganState(String challengeName,
                         Integer challengeYear,
                         Integer placeCity,
                         Integer placeGender,
                         Integer bibNumber,
                         String firstName,
                         String lastName,
                         Double time,
                         String gender,
                         Party lender,
                         Party borrower,
                         UniqueIdentifier linearId)
    {
        this.challengeName = challengeName;
        this.challengeYear = challengeYear;
        this.placeCity = placeCity;
        this.placeGender = placeGender;
        this.bibNumber = bibNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.time = time;
        this.gender = gender;
        this.lender = lender;
        this.borrower = borrower;
        this.linearId = linearId;
    }

    public String getChallengeName() { return challengeName; }
    public Integer getChallengeYear() { return challengeYear; }
    public Integer getPlaceCity() { return placeCity; }
    public Integer getPlaceGender() { return placeGender; }
    public Integer getBibNumber() { return bibNumber; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public Double getTime() { return time; }
    public String getGender() { return gender; }
    public Party getLender() { return lender; }
    public Party getBorrower() { return borrower; }

    @Override public UniqueIdentifier getLinearId() { return linearId; }
    @Override public List<AbstractParty> getParticipants() {
        return Arrays.asList(lender, borrower);
    }

    @Override public PersistentState generateMappedObject(MappedSchema schema) {
        if (schema instanceof JPMorganSchemaV1) {
            return new JPMorganSchemaV1.PersistentJPM(
                    this.lender.getName().toString(),
                    this.borrower.getName().toString(),
                    this.challengeName,
                    this.challengeYear,
                    this.placeCity,
                    this.placeGender,
                    this.bibNumber,
                    this.firstName,
                    this.lastName,
                    this.time,
                    this.gender,
                    this.linearId.getId());
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }


    @Override public Iterable<MappedSchema> supportedSchemas() {
        return ImmutableList.of(new JPMorganSchemaV1());
    }

    @Override
    public String toString() {
        return String.format("JPMorganState(challengeName=%s, challengeYear=%s, placeCity=%s, placeGender=%s" +
                ", bibNumber=%s, firstName=%s, lastName=%s, time=%s, gender=%s, lender=%s, borrower=%s" +
                ", linearId=%s)", challengeName, challengeYear, placeCity, placeGender, bibNumber, firstName, lastName, time, gender, lender, borrower, linearId);

    }
}