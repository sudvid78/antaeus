package io.pleo.antaeus.models

data class Invoice(
    val id: Int,
    val customerId: Int,
    val emailId:String,
    val amount: Money,
    val status: InvoiceStatus,
    val retryCount: Int,
    val timezone: String,
    val idempotencyKey:String?=null
)
