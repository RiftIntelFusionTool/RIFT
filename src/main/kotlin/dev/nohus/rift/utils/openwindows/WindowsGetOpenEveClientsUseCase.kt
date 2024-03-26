package dev.nohus.rift.utils.openwindows

import com.sun.jna.Native
import com.sun.jna.Pointer
import dev.nohus.rift.utils.openwindows.windows.User32

class WindowsGetOpenEveClientsUseCase(
    private val user32: User32,
) : GetOpenEveClientsUseCase {

    private val regex = """^EVE - (?<character>[A-z0-9 '-]{3,37})$""".toRegex()

    override fun invoke(): List<String> {
        val characters = mutableListOf<String>()
        user32.EnumWindows(
            object : User32.WNDENUMPROC {
                override fun callback(hWnd: Pointer?, arg: Pointer?): Boolean {
                    val byteArray = ByteArray(64)
                    user32.GetWindowTextA(hWnd, byteArray, byteArray.size)
                    val title = Native.toString(byteArray)
                    val match = regex.find(title)
                    if (match != null) {
                        characters += match.groups["character"]!!.value
                    }
                    return true
                }
            },
            null,
        )
        return characters
    }
}
