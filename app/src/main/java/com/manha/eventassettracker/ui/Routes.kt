package com.manha.eventassettracker.ui

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val SCAN = "scan"
    const val EVENTS = "events"
    const val EVENT_DETAIL = "event_detail/{eventId}"
    fun eventDetail(eventId: String) = "event_detail/$eventId"
    const val ASSETS = "assets"
    const val QR_GENERATE = "qr_generate"
    const val QR_REGISTER = "qr_register"
    const val STAFF = "staff"
    const val COMPARE = "compare"
    const val SETTINGS = "settings"
    const val ALL_DATA_REPORT = "all_data_report"
}
