package com.everis.jpmorgancc.api;

import com.everis.jpmorgancc.flow.JPMorganFlow;
import com.everis.jpmorgancc.state.JPMorganState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;

// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("example")
public class JPMorganApi {
    private final CordaRPCOps rpcOps;
    private final CordaX500Name myLegalName;

    private final List<String> serviceNames = ImmutableList.of("Notary", "Network Map Service");

    static private final Logger logger = LoggerFactory.getLogger(JPMorganApi.class);

    public JPMorganApi(CordaRPCOps rpcOps) {
        this.rpcOps = rpcOps;
        this.myLegalName = rpcOps.nodeInfo().getLegalIdentities().get(0).getName();
    }

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, CordaX500Name> whoami() {
        return ImmutableMap.of("me", myLegalName);
    }

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<CordaX500Name>> getPeers() {
        List<NodeInfo> nodeInfoSnapshot = rpcOps.networkMapSnapshot();
        return ImmutableMap.of("peers", nodeInfoSnapshot
                .stream()
                .map(node -> node.getLegalIdentities().get(0).getName())
                .filter(name -> !name.equals(myLegalName) && !serviceNames.contains(name.getOrganisation()))
                .collect(toList()));
    }

    /**
     * Displays all JPM states that exist in the node's vault.
     */
    @GET
    @Path("results")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<JPMorganState>> getResults() {
        return rpcOps.vaultQuery(JPMorganState.class).getStates();
    }

    /**
     * Initiates a flow to agree an JPM between two parties.
     *
     * Once the flow finishes it will have written the JPM to ledger. Both the lender and the borrower will be able to
     * see it when calling /api/example/results on their respective nodes.
     *
     * This end-point takes a Party name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */
    @PUT
    @Path("add-result")
    public Response addResult(@QueryParam("challengeName") String challengeName,
                              @QueryParam("challengeYear") int challengeYear,
                              @QueryParam("placeCity") int placeCity,
                              @QueryParam("placeGender") int placeGender,
                              @QueryParam("bibNumber") int bibNumber,
                              @QueryParam("firstName") String firstName,
                              @QueryParam("lastName") String lastName,
                              @QueryParam("time") Double time,
                              @QueryParam("gender") String gender,
                              @QueryParam("partyName") CordaX500Name partyName) throws InterruptedException, ExecutionException {


        if (challengeName.isEmpty()) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'challengeName' must be non-blank.\n").build();
        }
        if (challengeYear <= 0) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'challengeYear' must be non-negative.\n").build();
        }
        if (placeCity <= 0) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'placeCity' must be non-negative.\n").build();
        }
        if (placeGender <= 0) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'placeGender' must be non-negative.\n").build();
        }
        if (bibNumber <= 0) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'bibNumber' must be non-negative.\n").build();
        }
        if (firstName.isEmpty()) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'firstName' must be non-blank.\n").build();
        }
        if (lastName.isEmpty()) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'lastName' must be non-blank.\n").build();
        }
        if (time <= 0) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'time' must be non-negative.\n").build();
        }
        if (gender.isEmpty()) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'gender' must be non-blank.\n").build();
        }

        if (partyName == null) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'partyName' missing or has wrong format.\n").build();
        }

        final Party otherParty = rpcOps.wellKnownPartyFromX500Name(partyName);
        if (otherParty == null) {
            return Response.status(BAD_REQUEST).entity("Party named " + partyName + "cannot be found.\n").build();
        }

        try {
            final SignedTransaction signedTx = rpcOps
                    .startTrackedFlowDynamic(JPMorganFlow.Initiator.class, challengeName, challengeYear, placeCity, placeGender, bibNumber, firstName, lastName, time, gender, otherParty)
                    .getReturnValue()
                    .get();


            final String msg = String.format("Transaction id %s committed to ledger.\n", signedTx.getId());
            return Response.status(CREATED).entity(msg).build();

        } catch (Throwable ex) {
            final String msg = ex.getMessage();
            logger.error(ex.getMessage(), ex);
            return Response.status(BAD_REQUEST).entity(msg).build();
        }
    }
}