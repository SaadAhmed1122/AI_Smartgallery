package com.ai.smartgallery.domain.model

/**
 * Domain model for Tag
 */
data class Tag(
    val id: Long = 0,
    val name: String,
    val color: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
