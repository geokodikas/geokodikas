package be.ledfan.geocoder

fun <T> List<T>.forFirstAndRest(firstOnly: (T) -> Unit, rest: (T) -> Unit) {
    var executedOnce = false;
    forEach {
        if (executedOnce) {
            rest(it)
        } else {
            executedOnce = true
            firstOnly(it)
        }
    }
}
