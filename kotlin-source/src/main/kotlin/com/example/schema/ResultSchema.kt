package com.example.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * The family of schemas for ResultState.
 */
object ResultSchema

/**
 * An ResultState schema.
 */
object ResultSchemaV1 : MappedSchema(
        schemaFamily = ResultSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentResult::class.java)) {
    @Entity
    @Table(name = "result_states")
    class PersistentResult(
            @Column(name = "lender")
            var lenderName: String,

            @Column(name = "borrower")
            var borrowerName: String,

            @Column(name = "challengeName")
            var challengeName: String,

            @Column(name = "challengeYear")
            var challengeYear: String,

            @Column(name = "plc")
            var plc: String,

            @Column(name = "gpic")
            var gpic: String,

            @Column(name = "bib")
            var bib: String,

            @Column(name = "name")
            var name: String,

            @Column(name = "time")
            var time: Double,

            @Column(name = "gender")
            var gender: String,

            @Column(name = "linear_id")
            var linearId: UUID

    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "", "", "", "", "", "", "", 0.0, "",  UUID.randomUUID())
    }
}
