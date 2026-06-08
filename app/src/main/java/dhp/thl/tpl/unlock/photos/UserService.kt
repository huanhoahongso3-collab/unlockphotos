package dhp.thl.tpl.unlock.photos

import android.os.Process
import android.util.Log
import kotlin.system.exitProcess

class UserService : IUserService.Stub() {
    override fun destroy() {
        exitProcess(0)
    }

    override fun executeCommand(command: String?) {
        if (command == null) return
        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("UserService", "Error executing command: $command", e)
        }
    }
}
