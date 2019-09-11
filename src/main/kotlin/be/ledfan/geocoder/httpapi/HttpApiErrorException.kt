package be.ledfan.geocoder.httpapi

/**
 * This Exception can be thrown during an HTTP request.
 * The specified message will be returned to the user. For example as json:
 * `{"status": "error", "msg": "<msg_of_exception>"}`
 *
 * Note: only use it for things which make sense to the users of the API.
 * E.g. don't use it for a broken database connection etc.
 */
open class HttpApiErrorException(msg: String) : Exception(msg) {}