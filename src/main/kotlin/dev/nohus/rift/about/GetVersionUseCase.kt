package dev.nohus.rift.about

import dev.nohus.rift.BuildConfig
import org.koin.core.annotation.Single

@Single
class GetVersionUseCase {

    operator fun invoke(): String {
        @Suppress("KotlinConstantConditions")
        val suffix = BuildConfig.environment.let { if (it != "prod") "-$it" else "" }
        return "Version ${BuildConfig.version}$suffix"
    }
}
