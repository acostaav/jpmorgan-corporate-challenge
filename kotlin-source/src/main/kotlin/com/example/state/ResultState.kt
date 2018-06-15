package com.example.state;

import com.example.schema.ResultSchemaV1
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

/**
 * The state object recording User Records agreements between two parties.
 *
 * A state must implement [ContractState] or one of its descendants.
 *
 * @param challengeName the value of the Challenge.
 * @param challengeYear the year of the Challenge.
 * @param plc the position of the user in the Challenge.
 * @param gbic the position of the user in the Challenge considering the gender.
 * @param bib the bib number of the user in the Challenge.
 * @param name the name of the user.
 * @param time the time result in the challenge.
 * @param gender the gender of the user.
 * @param lender the party registering the result.
 * @param borrower the party receiving and approving the result.
 */
data class ResultState(val challengeName: String,
                       val challengeYear: String,
                       val plc: String,
                       val gpic: String,
                       val bib: String,
                       val name: String,
                       val time: Double,
                       val gender: String,
                       val lender: Party,
                       val borrower: Party,
                       override val linearId: UniqueIdentifier = UniqueIdentifier()):
        LinearState, QueryableState {

    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(lender, borrower)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is ResultSchemaV1 -> ResultSchemaV1.PersistentResult(
                    this.lender.name.toString(),
                    this.borrower.name.toString(),
                    this.challengeName,
                    this.challengeYear,
                    this.plc,
                    this.gpic,
                    this.bib,
                    this.name,
                    this.time,
                    this.gender,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(ResultSchemaV1)
}
