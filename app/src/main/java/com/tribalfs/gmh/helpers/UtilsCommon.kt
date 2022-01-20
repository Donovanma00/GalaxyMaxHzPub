package com.tribalfs.gmh.helpers

import kotlin.math.abs

object UtilsCommon {
    internal fun List<Int>.closestValue(value: Int) = minByOrNull { abs(value - it) }
}