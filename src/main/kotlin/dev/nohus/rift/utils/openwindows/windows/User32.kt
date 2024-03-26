package dev.nohus.rift.utils.openwindows.windows

import com.sun.jna.Pointer
import com.sun.jna.win32.StdCallLibrary

@Suppress("SpellCheckingInspection", "FunctionName")
interface User32 : StdCallLibrary {
    interface WNDENUMPROC : StdCallLibrary.StdCallCallback {
        fun callback(hWnd: Pointer?, arg: Pointer?): Boolean
    }

    fun EnumWindows(lpEnumFunc: WNDENUMPROC?, userData: Pointer?): Boolean
    fun GetWindowTextA(hWnd: Pointer?, lpString: ByteArray?, nMaxCount: Int): Int
    fun GetForegroundWindow(): Pointer?
}
