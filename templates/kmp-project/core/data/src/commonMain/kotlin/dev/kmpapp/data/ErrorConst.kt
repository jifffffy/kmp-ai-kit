package dev.kmpapp.data

import dev.kmpapp.common.ErrorModel

object ErrorConst {
    val Unauthorized: ErrorModel = ErrorModel.MessageCode("You must login", 1001)
    val BillingRequired: ErrorModel = ErrorModel.MessageCode("You should fill out billing info", 1002)
    val FillProfileRequired: ErrorModel =
        ErrorModel.MessageCode("You should fill out phone in your profile", 1003)
    val KycRequired: ErrorModel =
        ErrorModel.MessageCode(
            "You should fill out KYC form. And you have to wait until it's status changes to approved",
            1004,
        )
    val NoNetwork = ErrorModel.Message("Error, Check your connection and try again.")

    val SerializationError = ErrorModel.Message("Error, Error serializing data.")

    class ServerUnknownError(
        code: Int,
    ) : ErrorModel.MessageCode("An unknown network error has occurred!", code)

    internal val ServerHandledError =
        mapOf(
            4132 to BillingRequired,
            4155 to FillProfileRequired,
            4161 to KycRequired,
            4162 to KycRequired,
            4168 to KycRequired,
        )
}
