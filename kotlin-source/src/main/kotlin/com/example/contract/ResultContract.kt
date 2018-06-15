package com.example.contract

import com.example.state.ResultState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

/**
 * A implementation of a basic smart contract in Corda.
 *
 * This contract enforces rules regarding the creation of a valid [ResultState], which in turn encapsulates an [Result].
 *
 * For a new [Result] to be issued onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [Result].
 * - An Create() command with the public keys of both the lender and the borrower.
 *
 * All contracts must sub-class the [Contract] interface.
 */
open class ResultContract : Contract {
    companion object {
        @JvmStatic
        val RESULT_CONTRACT_ID = "com.example.contract.ResultContract"
    }

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands.Create>()
        requireThat {
            // Generic constraints around the IOU transaction.
            "No inputs should be consumed when issuing an Result." using (tx.inputs.isEmpty())
            "Only one output state should be created." using (tx.outputs.size == 1)
            val out = tx.outputsOfType<ResultState>().single()
            "The lender and the borrower cannot be the same entity." using (out.lender != out.borrower)
            "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))

            // Contract-specific constraints.
            "The Challenge Name must be non-blank." using (out.challengeName.isNotBlank())
            "The Challenge Year must be non-blank." using (out.challengeYear.isNotBlank())
            "The Challenge PLC must be non-blank." using (out.plc.isNotBlank())
            "The Challenge GPIc must be non-blank." using (out.gpic.isNotBlank())
            "The Challenge Bib must be non-blank." using (out.bib.isNotBlank())
            "The User Name must be non-blank." using (out.name.isNotBlank())
            "The Result's Time value must be non-negative." using (out.time > 0)
            "The User Gender must be non-blank." using (out.gender.isNotBlank())

        }
    }

    /**
     * This contract only implements one command, Create.
     */
    interface Commands : CommandData {
        class Create : Commands
    }
}
