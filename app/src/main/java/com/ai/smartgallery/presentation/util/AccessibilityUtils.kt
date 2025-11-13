package com.ai.smartgallery.presentation.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Accessibility utilities for the Smart Gallery app
 */
object AccessibilityUtils {

    /**
     * Minimum touch target size as per Material Design guidelines
     */
    val MIN_TOUCH_TARGET_SIZE = 48.dp

    /**
     * Formats a timestamp for screen readers
     */
    fun formatDateForAccessibility(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    /**
     * Formats file size for screen readers
     */
    fun formatFileSizeForAccessibility(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes bytes"
            bytes < 1024 * 1024 -> "${bytes / 1024} kilobytes"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} megabytes"
            else -> "${bytes / (1024 * 1024 * 1024)} gigabytes"
        }
    }

    /**
     * Formats dimensions for screen readers
     */
    fun formatDimensionsForAccessibility(width: Int, height: Int): String {
        return "$width by $height pixels"
    }

    /**
     * Creates a photo description for screen readers
     */
    fun describePhoto(
        displayName: String,
        dateTaken: Long,
        isVideo: Boolean = false
    ): String {
        val type = if (isVideo) "Video" else "Photo"
        val date = formatDateForAccessibility(dateTaken)
        return "$type: $displayName, taken on $date"
    }

    /**
     * Creates a selection state description for screen readers
     */
    fun describeSelectionState(
        itemName: String,
        isSelected: Boolean,
        position: Int,
        totalCount: Int
    ): String {
        val selectedState = if (isSelected) "selected" else "not selected"
        return "$itemName, $selectedState, item $position of $totalCount"
    }

    /**
     * Describes an action button for screen readers
     */
    fun describeActionButton(
        action: String,
        itemName: String? = null
    ): String {
        return if (itemName != null) {
            "$action $itemName"
        } else {
            action
        }
    }

    /**
     * Describes a filter chip state for screen readers
     */
    fun describeFilterChip(
        filterName: String,
        isSelected: Boolean,
        resultCount: Int? = null
    ): String {
        val selectedState = if (isSelected) "selected" else "not selected"
        val results = if (resultCount != null && resultCount > 0) {
            ", $resultCount ${if (resultCount == 1) "item" else "items"}"
        } else ""
        return "$filterName filter, $selectedState$results"
    }

    /**
     * Describes a dialog for screen readers
     */
    fun describeDialog(
        title: String,
        hasContent: Boolean = true
    ): String {
        return "$title dialog${if (hasContent) ", swipe to explore content" else ""}"
    }
}

/**
 * Extension function to mark a composable as a heading for accessibility
 */
fun Modifier.accessibilityHeading(): Modifier = this.semantics {
    heading()
}

/**
 * Extension function to add a custom content description
 */
fun Modifier.accessibilityDescription(description: String): Modifier = this.semantics {
    contentDescription = description
}

/**
 * Extension function to set the role of a UI element
 */
fun Modifier.accessibilityRole(uiRole: Role): Modifier = this.semantics {
    role = uiRole
}
