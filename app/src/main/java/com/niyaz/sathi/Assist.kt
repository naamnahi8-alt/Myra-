 package com.niyaz.sathi
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.*

class AssistService : VoiceInteractionService()
class SessionSvc : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = Sess(this)
}
class Sess(c: Context) : VoiceInteractionSession(c) {
    override fun onShow(args: Bundle?, flags: Int) {
        super.onShow(args, flags)
        context.startActivity(Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("listen", true))
        hide()
    }
}
