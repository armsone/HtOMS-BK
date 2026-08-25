package com.htoms.brief.api

sealed class OMSAPIError(override val message: String) : Exception(message) {
    data object InvalidURL : OMSAPIError("OMS 서버 주소를 만들 수 없습니다.")
    data object InvalidResponse : OMSAPIError("OMS 서버의 응답을 확인할 수 없습니다.")
    data object Unauthorized : OMSAPIError("로그인이 만료되었거나 계정 정보가 올바르지 않습니다.")
    data class Server(val statusCode: Int) : OMSAPIError("OMS 서버 요청에 실패했습니다. ($statusCode)")
    data object MalformedData : OMSAPIError("OMS 서버 데이터 형식이 예상과 다릅니다.")
}
