package dev.kmpapp.common

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed class InputValidation {
    data object NotValidated : InputValidation()

    data object Valid : InputValidation()

    data class Invalid(
        val error: ErrorModel,
    ) : InputValidation()
}

@Suppress("NOTHING_TO_INLINE")
@OptIn(ExperimentalContracts::class)
inline fun InputValidation.isInvalid(): Boolean {
    contract { returns(true) implies (this@isInvalid is InputValidation.Invalid) }
    return this is InputValidation.Invalid
}

@Suppress("NOTHING_TO_INLINE")
@OptIn(ExperimentalContracts::class)
inline fun InputValidation.isValid(): Boolean {
    contract { returns(true) implies (this@isValid is InputValidation.Valid) }
    return this is InputValidation.Valid
}

@Suppress("NOTHING_TO_INLINE")
inline fun InputValidation.asInvalid(): InputValidation.Invalid? = this as? InputValidation.Invalid
