package com.xama.client.files

import com.xama.client.Config
import com.xama.client.handleProblem
import com.xama.client.oauthHeaders
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.zalando.riptide.*
import org.zalando.riptide.capture.Capture
import org.zalando.riptide.problem.ProblemRoute
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * see https://developer.xero.com/documentation/files-api/files
 */
class Associations private constructor(){
    data class UserDto(val id: UUID, val name: String?, val firstName: String?, val lastName: String?, val fullName: String?)
    data class FileDto(val id: UUID, val folderId: UUID?, val size: Int, val createdDateUtc: String, val updatedDateUtc: String, val user: UserDto?)
    data class GetFilesResponseDto(val totalCount: Int, val page: Int, val perPage: Int, val items: List<FileDto>)


    /**
     * see https://developer.xero.com/documentation/files-api/types#ObjectTypes
     */
    enum class ObjectType {
        ACCOUNT, // Account
        ACCPAY, // Purchases Invoice
        ACCPAYCREDIT, // Purchases Credit Note
        ACCPAYPAYMENT, // Payment on a Purchases Invoice
        ACCREC, // Sales Invoice
        ACCRECCREDIT, // Sales Credit Note
        ACCRECPAYMENT, // Payment on a sales invoice
        ADJUSTMENT, // Reconciliation adjustment
        APCREDITPAYMENT, // Payment on a purchases credit note
        APOVERPAYMENT, // Purchases overpayment
        APOVERPAYMENTPAYMENT, // Purchases overpayment
        APOVERPAYMENTSOURCEPAYMENT, // The bank transaction part of a purchases overpayment
        APPREPAYMENT, // Purchases prepayment
        APPREPAYMENTPAYMENT, // Purchases prepayment
        APPREPAYMENTSOURCEPAYMENT, // The bank transaction part of a purchases prepayment
        ARCREDITPAYMENT, // Payment on a sales credit note
        AROVERPAYMENT, // Sales overpayment
        AROVERPAYMENTPAYMENT, // Sales overpayment
        AROVERPAYMENTSOURCEPAYMENT, // The bank transaction part of a sales overpayment
        ARPREPAYMENT, // Sales prepayment
        ARPREPAYMENTPAYMENT, // Sales prepayment
        ARPREPAYMENTSOURCEPAYMENT, // The bank transaction part of a sales prepayment
        CASHPAID, // A spend money transaction
        CASHREC, // A receive money transaction
        CONTACT, // Contact
        EXPPAYMENT, // Expense claim payment
        FIXEDASSET, // Fixed Asset
        MANUALJOURNAL, // Manual Journal
        PAYRUN, // Payrun
        PRICELISTITEM, // Item
        PURCHASEORDER, // Purchase order
        RECEIPT, // Expense receipt
        TRANSFER // BankTransfer
    }


    /**
     * see https://developer.xero.com/documentation/files-api/types#ObjectGroupes
     */
    enum class ObjectGroup {
        Account, //	Accounts
        BankTransaction, //	Bank Transactions
        Contact, //	Contacts
        CreditNote, //	Credit Notes
        Invoice, //	Invoices
        Item, //	Items
        ManualJournal, //	Manual Journals
        Overpayment, //	Overpayments
        Payment, //	Payments
        Payrun, //	Not yet available
        Prepayment, //	Prepayments
        PurchaseOrder, //	Not yet available
        Receipt, //	Receipts
        Reconciliation, //	Not yet available
    }


    data class AssociationDto(val id: UUID, val objectId: UUID, val objectType: ObjectType, val objectGroup: ObjectGroup)


    class Client (val http: Http, val config: Config){
        companion object {
            const val BASE_URL = "https://api.xero.com/files.xro/1.0/files/{fileId}/associations"
        }

        //Bindings.on(HttpStatus.Series.SUCCESSFUL).call({ response: ClientHttpResponse, reader: MessageReader -> println(response.body.bufferedReader().use { it.readText() }) }),

        fun getFileAssociations(fileId: UUID): CompletableFuture<List<AssociationDto>> {
            val capture = Capture.empty<List<AssociationDto>>()
            val future = http.get(BASE_URL, fileId)
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    // .ifModifiedSince() TODO
                    .header(HttpHeaders.USER_AGENT, config.userAgent)
                    .headers(oauthHeaders(
                            config = config,
                            httpMethod = HttpMethod.GET,
                            requestPath = "https://api.xero.com/files.xro/1.0/files/$fileId/associations"
                            // TODO query params
                    ))
                    .dispatch(Navigators.series(),
                            Bindings.on(HttpStatus.Series.SUCCESSFUL).call( Types.listOf(AssociationDto::class.java), capture),
                            Bindings.anySeries().call(ProblemRoute.problemHandling(Route.call { p -> handleProblem(p) }))
                    )
                    .thenApply(capture);

            return future
        }
    }

}