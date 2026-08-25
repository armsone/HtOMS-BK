package com.htoms.brief.provider

import com.htoms.brief.model.BriefSnapshot

interface BriefProviding {
    suspend fun loadSnapshot(): BriefSnapshot
}
