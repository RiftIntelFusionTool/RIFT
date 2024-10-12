package dev.nohus.rift.startupwarning

import dev.nohus.rift.startupwarning.GetStartupWarningsUseCase.StartupWarning

data class StartupWarningInputModel(
    val warnings: List<StartupWarning>,
)
