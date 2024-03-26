package dev.nohus.rift.intel.state

data class IntelUnderstanding(
    val systems: List<String>,
    val entities: List<SystemEntity>,
    val kills: List<UnderstandMessageUseCase.Kill>,
    val questions: List<UnderstandMessageUseCase.Question>,
    val movement: UnderstandMessageUseCase.Movement?,
    val reportedNoVisual: Boolean,
    val reportedClear: Boolean,
)
