package dev.nohus.rift.di

import com.sun.jna.Native
import dev.nohus.rift.logging.analytics.Analytics
import dev.nohus.rift.network.EsiErrorLimitInterceptor
import dev.nohus.rift.network.LoggingInterceptor
import dev.nohus.rift.network.RequestExecutor
import dev.nohus.rift.network.RequestExecutorImpl
import dev.nohus.rift.network.UserAgentInterceptor
import dev.nohus.rift.notifications.system.LinuxSendNotificationUseCase
import dev.nohus.rift.notifications.system.MacSendNotificationUseCase
import dev.nohus.rift.notifications.system.SendNotificationUseCase
import dev.nohus.rift.notifications.system.WindowsSendNotificationUseCase
import dev.nohus.rift.utils.GetOperatingSystemUseCase
import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.utils.OperatingSystem.Linux
import dev.nohus.rift.utils.OperatingSystem.MacOs
import dev.nohus.rift.utils.OperatingSystem.Windows
import dev.nohus.rift.utils.activewindow.GetActiveWindowUseCase
import dev.nohus.rift.utils.activewindow.LinuxGetActiveWindowUseCase
import dev.nohus.rift.utils.activewindow.MacGetActiveWindowUseCase
import dev.nohus.rift.utils.activewindow.WindowsGetActiveWindowUseCase
import dev.nohus.rift.utils.directories.AppDirectories
import dev.nohus.rift.utils.openwindows.GetOpenEveClientsUseCase
import dev.nohus.rift.utils.openwindows.LinuxGetOpenEveClientsUseCase
import dev.nohus.rift.utils.openwindows.MacGetOpenEveClientsUseCase
import dev.nohus.rift.utils.openwindows.WindowsGetOpenEveClientsUseCase
import dev.nohus.rift.utils.openwindows.windows.User32
import dev.nohus.rift.utils.osdirectories.LinuxDirectories
import dev.nohus.rift.utils.osdirectories.MacDirectories
import dev.nohus.rift.utils.osdirectories.OperatingSystemDirectories
import dev.nohus.rift.utils.osdirectories.WindowsDirectories
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.time.Duration

@Module
@ComponentScan("dev.nohus.rift")
class KoinModule

val platformModule = module {
    single<OperatingSystem> {
        get<GetOperatingSystemUseCase>()()
    }
    single<OperatingSystemDirectories> {
        when (get<OperatingSystem>()) {
            Linux -> LinuxDirectories()
            Windows -> WindowsDirectories()
            MacOs -> MacDirectories()
        }
    }
    single<GetOpenEveClientsUseCase> {
        when (get<OperatingSystem>()) {
            Linux -> LinuxGetOpenEveClientsUseCase(get())
            Windows -> WindowsGetOpenEveClientsUseCase(get())
            MacOs -> MacGetOpenEveClientsUseCase()
        }
    }
    single<GetActiveWindowUseCase> {
        when (get<OperatingSystem>()) {
            Linux -> LinuxGetActiveWindowUseCase(get())
            Windows -> WindowsGetActiveWindowUseCase(get())
            MacOs -> MacGetActiveWindowUseCase()
        }
    }
    single<SendNotificationUseCase> {
        when (get<OperatingSystem>()) {
            Linux -> LinuxSendNotificationUseCase(get())
            Windows -> WindowsSendNotificationUseCase()
            MacOs -> MacSendNotificationUseCase(get())
        }
    }
}

val factoryModule = module {
    single<OkHttpClient> {
        val directory = get<AppDirectories>().getAppCacheDirectory().resolve("http-cache")
        val size = 50L * 1024 * 1024 // 50MB
        OkHttpClient.Builder()
            .cache(Cache(directory.toFile(), size))
            .addInterceptor(get<UserAgentInterceptor>())
            .addInterceptor(get<LoggingInterceptor>())
            .pingInterval(Duration.ofSeconds(10))
            .build()
    }
    single<OkHttpClient>(qualifier = named("esi")) {
        val directory = get<AppDirectories>().getAppCacheDirectory().resolve("esi-cache")
        val size = 50L * 1024 * 1024 // 50MB
        OkHttpClient.Builder()
            .cache(Cache(directory.toFile(), size))
            .addInterceptor(get<UserAgentInterceptor>())
            .addInterceptor(get<EsiErrorLimitInterceptor>())
            .addInterceptor(get<LoggingInterceptor>())
            .build()
    }
    single<Json> {
        Json {
            ignoreUnknownKeys = true
        }
    }
    single<Json>(qualifier = named("settings")) {
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }
    }
    single<RequestExecutor> { RequestExecutorImpl(get(), get(), get()) }
    single<User32> { Native.load("user32", User32::class.java) }
    single<Analytics> { Analytics() }
}
