package be.ledfan.geocoder

//class DoFirstElse : (() -> Unit, () -> Unit) -> Unit {
//
//    var executedOnce = false;
//
//    override operator fun invoke(firstOnly: () -> Unit, other: () -> Unit) {
//        if (executedOnce) {
//            other()
//        } else {
//            executedOnce = true
//            firstOnly()
//        }
//    }
//}

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
