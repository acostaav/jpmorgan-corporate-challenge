package com.everis.jpmorgancc.contract;

import com.everis.jpmorgancc.state.JPMorganState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;

import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * A implementation of a basic smart contract in Corda.
 *
 * This contract enforces rules regarding the creation of a valid [JPMorganState], which in turn encapsulates an [JPM].
 *
 * For a new [JPM] to be issued onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [JPM].
 * - An Create() command with the public keys of both the lender and the borrower.
 *
 * All contracts must sub-class the [Contract] interface.
 */
public class JPMorganContract implements Contract {
    public static final String JPM_CONTRACT_ID = "com.everis.jpmorgancc.contract.JPMorganContract";

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    @Override
    public void verify(LedgerTransaction tx) {
        final CommandWithParties<Commands.Create> command = requireSingleCommand(tx.getCommands(), Commands.Create.class);
        requireThat(require -> {
            // Generic constraints around the JPM transaction.
            require.using("No inputs should be consumed when issuing an JPM.",
                    tx.getInputs().isEmpty());
            require.using("Only one output state should be created.",
                    tx.getOutputs().size() == 1);
            final JPMorganState out = tx.outputsOfType(JPMorganState.class).get(0);
            require.using("The lender and the borrower cannot be the same entity.",
                    out.getLender() != out.getBorrower());
            require.using("All of the participants must be signers.",
                    command.getSigners().containsAll(out.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList())));

            // JPM-specific constraints.
            require.using("The Challenge Name must be non-blank.", !out.getChallengeName().isEmpty());
            require.using("The Year value must be non-negative.", out.getChallengeYear() > 0);
            require.using("The PLC value must be non-negative.", out.getPlaceCity() > 0);
            require.using("The GPIc value must be non-negative.", out.getPlaceGender() > 0);
            require.using("The Bib value must be non-negative.", out.getBibNumber() > 0);
            require.using("The First Name must be non-blank.", !out.getFirstName().isEmpty());
            require.using("The Last Name must be non-blank.", !out.getLastName().isEmpty());
            require.using("The Time value must be non-negative.", out.getTime() > 0);
            require.using("The Gender value must be non-blank.", !out.getGender().isEmpty());

            return null;
        });
    }

    /**
     * This contract only implements one command, Create.
     */
    public interface Commands extends CommandData {
        class Create implements Commands {}
    }
}