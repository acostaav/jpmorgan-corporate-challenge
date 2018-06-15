package com.everis.jpmorgancc.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.everis.jpmorgancc.contract.JPMorganContract;
import com.everis.jpmorgancc.state.JPMorganState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;

import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * This flow allows two parties (the [Initiator] and the [Acceptor]) to come to an agreement about the JPM encapsulated
 * within an [JPMorganState].
 *
 * In our simple example, the [Acceptor] always accepts a valid JPM.
 *
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 *
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 */
public class JPMorganFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final String challengeName;
        private final Integer challengeYear;
        private final Integer placeCity;
        private final Integer placeGender;
        private final Integer bibNumber;
        private final String firstName;
        private final String lastName;
        private final Double time;
        private final String gender;
        private final Party otherParty;

        private final Step GENERATING_TRANSACTION = new Step("Generating transaction based on new JPM.");
        private final Step VERIFYING_TRANSACTION = new Step("Verifying contract constraints.");
        private final Step SIGNING_TRANSACTION = new Step("Signing transaction with our private key.");
        private final Step GATHERING_SIGS = new Step("Gathering the counterparty's signature.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final Step FINALISING_TRANSACTION = new Step("Obtaining notary signature and recording transaction.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        // The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
        // checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call()
        // function.
        private final ProgressTracker progressTracker = new ProgressTracker(
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        );

        public Initiator(String challengeName, int challengeYear, int placeCity, int placeGender, int bibNumber, String firstName, String lastName, Double time, String gender, Party otherParty) {
            this.challengeName = challengeName;
            this.challengeYear = challengeYear;
            this.placeCity = placeCity;
            this.placeGender = placeGender;
            this.bibNumber = bibNumber;
            this.firstName = firstName;
            this.lastName = lastName;
            this.time = time;
            this.gender = gender;
            this.otherParty = otherParty;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Obtain a reference to the notary we want to use.
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // Stage 1.
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            // Generate an unsigned transaction.
            Party me = getServiceHub().getMyInfo().getLegalIdentities().get(0);
            JPMorganState JPMorganState = new JPMorganState(challengeName, challengeYear, placeCity, placeGender, bibNumber, firstName, lastName, time, gender, me, otherParty, new UniqueIdentifier());
            final Command<JPMorganContract.Commands.Create> txCommand = new Command<>(
                    new JPMorganContract.Commands.Create(),
                    ImmutableList.of(JPMorganState.getLender().getOwningKey(), JPMorganState.getBorrower().getOwningKey()));
            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(JPMorganState, JPMorganContract.JPM_CONTRACT_ID)
                    .addCommand(txCommand);

            // Stage 2.
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            // Verify that the transaction is valid.
            txBuilder.verify(getServiceHub());

            // Stage 3.
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            // Sign the transaction.
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Stage 4.
            progressTracker.setCurrentStep(GATHERING_SIGS);
            // Send the state to the counterparty, and receive it back with their signature.
            FlowSession otherPartySession = initiateFlow(otherParty);
            final SignedTransaction fullySignedTx = subFlow(
                    new CollectSignaturesFlow(partSignedTx, ImmutableSet.of(otherPartySession), CollectSignaturesFlow.Companion.tracker()));

            // Stage 5.
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(new FinalityFlow(fullySignedTx));
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Acceptor extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartyFlow;

        public Acceptor(FlowSession otherPartyFlow) {
            this.otherPartyFlow = otherPartyFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                    super(otherPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("This must be an JPM transaction.", output instanceof JPMorganState);
                        JPMorganState jpm = (JPMorganState) output;

                        require.using("I won't accept Challenge Name with a blank value.", !jpm.getChallengeName().isEmpty());
                        require.using("I won't accept Year values with a negative value.", jpm.getChallengeYear() > 0);
                        require.using("I won't accept PLC values with a negative value.", jpm.getPlaceCity() > 0);
                        require.using("I won't accept GPIc values with a negative value.", jpm.getPlaceGender() > 0);
                        require.using("I won't accept Bib values with a negative value.", jpm.getBibNumber() > 0);
                        require.using("I won't accept First Name with a blank value.", !jpm.getFirstName().isEmpty());
                        require.using("I won't accept Last Name with a blank value.", !jpm.getLastName().isEmpty());
                        require.using("I won't accept time values with a negative value.", jpm.getTime() > 0);
                        require.using("I won't accept user gender with a blank value.", !jpm.getGender().isEmpty());

                        return null;
                    });
                }
            }

            return subFlow(new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker()));
        }
    }
}
