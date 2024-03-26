package dev.nohus.rift.utils.activewindow

import com.sun.jna.Native
import dev.nohus.rift.utils.openwindows.windows.User32

class WindowsGetActiveWindowUseCase(
    private val user32: User32,
) : GetActiveWindowUseCase {

    override fun invoke(): String? {
        val hWnd = user32.GetForegroundWindow()
        val byteArray = ByteArray(64)
        user32.GetWindowTextA(hWnd, byteArray, byteArray.size)
        return Native.toString(byteArray)
    }
}
