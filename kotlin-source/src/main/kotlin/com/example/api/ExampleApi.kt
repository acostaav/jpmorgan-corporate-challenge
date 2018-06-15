package com.example.api

import com.example.flow.ExampleFlow.Initiator
import com.example.state.ResultState
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.BAD_REQUEST
import javax.ws.rs.core.Response.Status.CREATED

val SERVICE_NAMES = listOf("Notary", "Network Map Service")

// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("example")
class ExampleApi(private val rpcOps: CordaRPCOps) {
    private val myLegalName: CordaX500Name = rpcOps.nodeInfo().legalIdentities.first().name

    companion object {
        private val logger: Logger = loggerFor<ExampleApi>()
    }

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = rpcOps.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                //filter out myself, notary and eventual network map started by driver
                .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })
    }



    /**
     * Displays all Results states that exist in the node's vault.
     *
     * TO_DO Devolver ResultState
     */
    @GET
    @Path("results")
    @Produces(MediaType.APPLICATION_JSON)
    fun getResults() = rpcOps.vaultQueryBy<ResultState>().states


    /**
     * Initiates a flow to agree an IOU between two parties.
     */
    @PUT
    @Path("add-result")
    fun addResult(@QueryParam("challengeName") challengeName: String,
                  @QueryParam("challengeYear") challengeYear: String,
                  @QueryParam("plc") plc: String,
                  @QueryParam("gpic") gpic: String,
                  @QueryParam("bib") bib: String,
                  @QueryParam("name") name: String,
                  @QueryParam("time") time: Double,
                  @QueryParam("gender") gender: String,
                  @QueryParam("partyName") partyName: CordaX500Name?): Response {

        //TODO Add validations on parameters

//        if (iouValue <= 0 ) {
//            return Response.status(BAD_REQUEST).entity("Query parameter 'iouValue' must be non-negative.\n").build()
//        }
        if (partyName == null) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'partyName' missing or has wrong format.\n").build()
        }

        val otherParty = rpcOps.wellKnownPartyFromX500Name(partyName) ?: return Response.status(BAD_REQUEST).entity("Party named $partyName cannot be found.\n").build()

        return try {

            //TODO To use objects, is needed override the white list
            //val userResult = UserResult(challengeName, challengeYear, plc, gpic, bib, name, time, gender)

            val elements = arrayListOf<String>()
            elements.add(challengeName)
            elements.add(challengeYear)
            elements.add(plc)
            elements.add(gpic)
            elements.add(bib)
            elements.add(name)
            elements.add(gender)

            val signedTx = rpcOps.startTrackedFlow(::Initiator, elements, time, otherParty).returnValue.getOrThrow()
            Response.status(CREATED).entity("Transaction id ${signedTx.id} committed to ledger.\n").build()

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }



}