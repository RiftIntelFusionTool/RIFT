package dev.nohus.rift.utils

import com.sun.jna.platform.win32.Shell32Util
import com.sun.jna.platform.win32.ShlObj

class GetWindowsDocumentsFolderUseCase {

    operator fun invoke(): String {
        return Shell32Util.getFolderPath(ShlObj.CSIDL_MYDOCUMENTS)
    }
}
