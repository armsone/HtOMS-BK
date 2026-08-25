package com.htoms.brief.ui

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * 차트 Y축 눈금 계산. 0부터 최댓값을 덮는 1-2-5 계열의 보기 좋은 간격을 고른다.
 * 결정적이어서 단위 테스트로 검증할 수 있다.
 */
object ChartSupport {
    fun yTicks(maxValue: Int, desiredCount: Int = 4): List<Int> {
        if (maxValue <= 0) return listOf(0, 1)
        val roughStep = maxValue.toDouble() / desiredCount
        val magnitude = 10.0.pow(floor(log10(roughStep)))
        val residual = roughStep / magnitude
        val niceResidual = when {
            residual <= 1.0 -> 1.0
            residual <= 2.0 -> 2.0
            residual <= 5.0 -> 5.0
            else -> 10.0
        }
        val step = (niceResidual * magnitude).toInt().coerceAtLeast(1)
        val top = (ceil(maxValue.toDouble() / step) * step).toInt()
        return (0..top step step).toList()
    }
}
