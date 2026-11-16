package org.hack4impact.risedc.core.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class StoreSection {
    PRODUCE,
    BAKERY,
    DELI,
    MEAT_AND_SEAFOOD,
    DAIRY_AND_EGGS,
    FROZEN,
    PANTRY,
    SNACKS_AND_DRINKS,
    HOUSEHOLD,
    OTHER,
}
